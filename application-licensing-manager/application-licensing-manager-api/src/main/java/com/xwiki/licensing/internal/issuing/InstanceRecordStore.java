/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.licensing.internal.issuing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.ACCOUNT;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.CLASS_NAME;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.CLASS_REFERENCE;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.CREATED_DATE;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.DOMAIN;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.ENVIRONMENT;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.INSTANCES_SPACE;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.INSTANCE_ID;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.ORIGIN;
import static com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer.PRODUCT;

/**
 * Writes and lists the instance records ({@code License.API.Instances.<instanceId>}). They are read one by one with
 * {@link ApiConfiguration#getInstance(String)}.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = InstanceRecordStore.class)
@Singleton
public class InstanceRecordStore
{
    private static final String ACCOUNTS_PARAMETER = "accounts";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactSerializer;

    @Inject
    private ApiConfiguration configuration;

    /**
     * Creates an instance record. The record page must not exist yet.
     *
     * @param instance the record to save
     * @throws XWikiException if the page already exists or can't be saved
     */
    public void create(InstanceRecord instance) throws XWikiException
    {
        XWikiContext xcontext = this.contextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(getReference(instance.getInstanceId()), xcontext);
        if (!document.isNew()) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                String.format("The instance record [%s] already exists.", document.getDocumentReference()));
        }
        document = document.clone();

        BaseObject object = document.newXObject(CLASS_REFERENCE, xcontext);
        object.set(INSTANCE_ID, instance.getInstanceId(), xcontext);
        object.set(ACCOUNT, serializeAccount(instance.getAccount()), xcontext);
        object.set(ENVIRONMENT, instance.getEnvironment().getValue(), xcontext);
        object.set(DOMAIN, StringUtils.defaultString(instance.getDomain()), xcontext);
        object.set(PRODUCT, instance.getProduct(), xcontext);
        object.set(ORIGIN, instance.getOrigin().name().toLowerCase(Locale.ROOT), xcontext);
        object.set(CREATED_DATE, instance.getCreatedDate(), xcontext);
        save(document, "Instance record created", xcontext);
    }

    /**
     * Changes the domain of an instance record.
     *
     * @param instanceId the instance id
     * @param domain the new domain
     * @throws XWikiException if the record can't be saved
     */
    public void updateDomain(String instanceId, String domain) throws XWikiException
    {
        XWikiContext xcontext = this.contextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(getReference(instanceId), xcontext).clone();
        BaseObject object = document.getXObject(CLASS_REFERENCE);
        if (object == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                String.format("The instance record [%s] doesn't exist.", document.getDocumentReference()));
        }
        object.set(DOMAIN, domain, xcontext);
        save(document, "Domain updated", xcontext);
    }

    /**
     * Deletes an instance record, without sending it to the recycle bin. Used to undo a failed issuance.
     *
     * @param instanceId the instance id
     * @throws XWikiException if the record can't be deleted
     */
    public void delete(String instanceId) throws XWikiException
    {
        XWikiContext xcontext = this.contextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(getReference(instanceId), xcontext);
        if (!document.isNew()) {
            xcontext.getWiki().deleteDocument(document, false, xcontext);
        }
    }

    /**
     * @param account an account
     * @param environment only the instances of this environment, or {@code null}
     * @param product only the instances of this product, or {@code null}
     * @return the records of the instances owned by the account, oldest first
     * @throws XWikiException if a record can't be read
     * @throws QueryException if the query fails
     */
    public List<InstanceRecord> getInstances(DocumentReference account, Environment environment, String product)
        throws XWikiException, QueryException
    {
        StringBuilder statement = new StringBuilder("select doc.name from Document doc, doc.object(")
            .append(CLASS_NAME).append(") instance where instance.account in (:")
            .append(ACCOUNTS_PARAMETER).append(')');
        if (environment != null) {
            statement.append(" and instance.environment = :environment");
        }
        if (product != null) {
            statement.append(" and instance.product = :product");
        }
        statement.append(" order by doc.creationDate, doc.name");
        Query query = this.queryManager.createQuery(statement.toString(), Query.XWQL)
            .setWiki(this.configuration.getMainWiki().getName())
            .bindValue(ACCOUNTS_PARAMETER, getAccountValues(account));
        if (environment != null) {
            query.bindValue(ENVIRONMENT, environment.getValue());
        }
        if (product != null) {
            query.bindValue(PRODUCT, product);
        }

        List<InstanceRecord> instances = new ArrayList<>();
        for (Object name : query.execute()) {
            Optional<InstanceRecord> instance = this.configuration.getInstance((String) name);
            instance.ifPresent(instances::add);
        }
        return instances;
    }

    /**
     * @param account an account
     * @return the values the account may be stored as in the records: relative to the main wiki (the API writes this
     *     one) or absolute (as an admin may write it)
     */
    public List<String> getAccountValues(DocumentReference account)
    {
        return List.of(serializeAccount(account), this.serializer.serialize(account));
    }

    private String serializeAccount(DocumentReference account)
    {
        return this.compactSerializer.serialize(account, this.configuration.getMainWiki());
    }

    private DocumentReference getReference(String instanceId)
    {
        return new DocumentReference(this.configuration.getMainWiki().getName(), INSTANCES_SPACE, instanceId);
    }

    private void save(XWikiDocument document, String comment, XWikiContext xcontext) throws XWikiException
    {
        UserReference author = this.configuration.getRecordAuthor();
        DocumentAuthors authors = document.getAuthors();
        if (document.isNew()) {
            authors.setCreator(author);
        }
        authors.setEffectiveMetadataAuthor(author);
        authors.setOriginalMetadataAuthor(author);
        authors.setContentAuthor(author);
        xcontext.getWiki().saveDocument(document, comment, true, xcontext);
    }
}
