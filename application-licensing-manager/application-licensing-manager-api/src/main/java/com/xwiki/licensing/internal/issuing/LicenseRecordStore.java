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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.licensing.LicenseType;

/**
 * Reads and writes the license records ({@code License.Data.<licenseId>} pages). It writes them the way the
 * {@code License.Code.LicenseDetailsMacros} macros do, so that the records created by the API can't be told apart
 * from the ones created by Sales.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = LicenseRecordStore.class)
@Singleton
public class LicenseRecordStore
{
    /**
     * The space holding the code and configuration of the License Manager.
     */
    public static final List<String> CODE_SPACE = List.of("License", "Code");

    /**
     * The space holding the license records.
     */
    public static final List<String> DATA_SPACE = List.of(CODE_SPACE.get(0), "Data");

    /**
     * The class of the license records.
     */
    public static final LocalDocumentReference DETAILS_CLASS =
        new LocalDocumentReference(CODE_SPACE, "LicenseDetailsClass");

    static final String LICENSE_ID = "licenseId";

    static final String TYPE = "type";

    static final String FIRST_NAME = "firstName";

    static final String LAST_NAME = "lastName";

    static final String EMAIL = "email";

    static final String INSTANCE_ID = "instanceId";

    static final String FEATURE_ID = "featureId";

    static final String DEPENDENT_FEATURE_IDS = "dependentFeatureIds";

    static final String EXTENSION_VERSION = "extensionVersion";

    static final String SUPPORT = "support";

    static final String MAX_USER_COUNT = "maxUserCount";

    static final String ISSUE_DATE = "issueDate";

    static final String EXPIRATION_DATE = "expirationDate";

    static final String HAS_LICENSE = "hasLicense";

    static final String SEND_EMAILS = "sendEmails";

    private static final String FROM_RECORDS = "from doc.object(License.Code.LicenseDetailsClass) license ";

    private static final String LATEST_FIRST =
        "order by license.issueDate desc, license.expirationDate desc, doc.creationDate desc";

    /**
     * Same filter and order as the {@code #getLicenseQuery} macro, restricted to the records that have a signed
     * license (the macro also matches the requests that Sales hasn't generated yet).
     */
    private static final String LATEST_QUERY = FROM_RECORDS
        + "where license.instanceId = :instanceId "
        + "and (license.featureId = :featureId or :featureId member of license.dependentFeatureIds) "
        + "and license.hasLicense = 1 " + LATEST_FIRST;

    private static final String INSTANCE_QUERY = FROM_RECORDS + "where license.instanceId = :instanceId";

    private static final String GENERATED_QUERY = INSTANCE_QUERY + " and license.hasLicense = 1 " + LATEST_FIRST;

    private static final String INSTANCE_ID_PARAMETER = INSTANCE_ID;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ApiConfiguration configuration;

    /**
     * Creates the record of a license. The record page must not exist yet.
     *
     * @param licenseRecord the record to save; its license id names the page
     * @throws XWikiException if the page already exists or can't be saved
     */
    public void create(LicenseRecord licenseRecord) throws XWikiException
    {
        XWikiContext xcontext = this.contextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(getReference(licenseRecord.getLicenseId()), xcontext);
        // Same check as #addLicenseDetails: never overwrite an existing record.
        if (!document.isNew()) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                String.format("The license record [%s] already exists.", document.getDocumentReference()));
        }
        document = document.clone();

        BaseObject object = document.newXObject(DETAILS_CLASS, xcontext);
        object.set(LICENSE_ID, licenseRecord.getLicenseId(), xcontext);
        object.set(TYPE, licenseRecord.getType().name().toLowerCase(Locale.ROOT), xcontext);
        object.set(FIRST_NAME, StringUtils.defaultString(licenseRecord.getFirstName()), xcontext);
        object.set(LAST_NAME, StringUtils.defaultString(licenseRecord.getLastName()), xcontext);
        object.set(EMAIL, StringUtils.defaultString(licenseRecord.getEmail()), xcontext);
        object.set(INSTANCE_ID, licenseRecord.getInstanceId(), xcontext);
        object.set(FEATURE_ID, licenseRecord.getFeatureId(), xcontext);
        object.set(DEPENDENT_FEATURE_IDS, new ArrayList<>(licenseRecord.getDependentFeatureIds()), xcontext);
        object.set(EXTENSION_VERSION, StringUtils.defaultString(licenseRecord.getExtensionVersion()), xcontext);
        object.set(SUPPORT, StringUtils.defaultString(licenseRecord.getSupport()), xcontext);
        if (licenseRecord.getMaxUserCount() != null) {
            object.set(MAX_USER_COUNT, licenseRecord.getMaxUserCount(), xcontext);
        }
        object.set(ISSUE_DATE, licenseRecord.getIssueDate(), xcontext);
        object.set(EXPIRATION_DATE, licenseRecord.getExpirationDate(), xcontext);
        object.set(HAS_LICENSE, toInt(licenseRecord.hasLicense()), xcontext);
        object.set(SEND_EMAILS, toInt(licenseRecord.isSendEmails()), xcontext);

        UserReference author = this.configuration.getRecordAuthor();
        DocumentAuthors authors = document.getAuthors();
        authors.setCreator(author);
        authors.setEffectiveMetadataAuthor(author);
        authors.setOriginalMetadataAuthor(author);
        authors.setContentAuthor(author);
        // Same comment as #generateLicense, since the API creates the record and the license at once.
        xcontext.getWiki().saveDocument(document, "License generated", true, xcontext);
    }

    /**
     * @param licenseId a license id
     * @return the record of the license, or empty if there is none
     * @throws XWikiException if the record page can't be read
     */
    public Optional<LicenseRecord> get(String licenseId) throws XWikiException
    {
        return read(getReference(licenseId));
    }

    /**
     * Deletes the record of a license, without sending it to the recycle bin. Used to undo a failed issuance.
     *
     * @param licenseId a license id
     * @throws XWikiException if the record page can't be deleted
     */
    public void delete(String licenseId) throws XWikiException
    {
        XWikiContext xcontext = this.contextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(getReference(licenseId), xcontext);
        if (!document.isNew()) {
            xcontext.getWiki().deleteDocument(document, false, xcontext);
        }
    }

    /**
     * Finds the license currently in use for a feature on an instance, choosing among the generated licenses the
     * way the {@code #getLicenseQuery} macro does: latest issue date first, then latest expiration date.
     *
     * @param instanceId an instance id
     * @param featureId a feature, matched against the licensed feature and its dependent features
     * @return the record of the latest license, or empty if the feature was never licensed on the instance
     * @throws XWikiException if the records can't be read
     * @throws QueryException if the query fails
     */
    public Optional<LicenseRecord> getLatest(String instanceId, String featureId)
        throws XWikiException, QueryException
    {
        List<String> results = createQuery(LATEST_QUERY).bindValue(INSTANCE_ID_PARAMETER, instanceId)
            .bindValue(FEATURE_ID, featureId).setLimit(1).execute();
        return results.isEmpty() ? Optional.empty() : read(resolve(results.get(0)));
    }

    /**
     * @param instanceId an instance id
     * @return whether the store holds any license record (generated or not) for the instance
     * @throws QueryException if the query fails
     */
    public boolean hasRecords(String instanceId) throws QueryException
    {
        return !createQuery(INSTANCE_QUERY).bindValue(INSTANCE_ID_PARAMETER, instanceId).setLimit(1).execute()
            .isEmpty();
    }

    /**
     * @param instanceId an instance id
     * @return the records of the generated licenses of the instance, the latest issued first
     * @throws XWikiException if the records can't be read
     * @throws QueryException if the query fails
     */
    public List<LicenseRecord> getGenerated(String instanceId) throws XWikiException, QueryException
    {
        List<LicenseRecord> records = new ArrayList<>();
        for (Object fullName : createQuery(GENERATED_QUERY).bindValue(INSTANCE_ID_PARAMETER, instanceId).execute()) {
            read(resolve((String) fullName)).ifPresent(records::add);
        }
        return records;
    }

    private Query createQuery(String statement) throws QueryException
    {
        return this.queryManager.createQuery(statement, Query.XWQL).setWiki(this.configuration.getMainWiki().getName());
    }

    private DocumentReference getReference(String licenseId)
    {
        return new DocumentReference(this.configuration.getMainWiki().getName(), DATA_SPACE, licenseId);
    }

    private DocumentReference resolve(String fullName)
    {
        return this.documentReferenceResolver.resolve(fullName, this.configuration.getMainWiki());
    }

    @SuppressWarnings("unchecked")
    private Optional<LicenseRecord> read(DocumentReference reference) throws XWikiException
    {
        XWikiContext xcontext = this.contextProvider.get();
        BaseObject object = xcontext.getWiki().getDocument(reference, xcontext).getXObject(DETAILS_CLASS);
        if (object == null) {
            return Optional.empty();
        }

        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setLicenseId(object.getStringValue(LICENSE_ID));
        licenseRecord.setType(toLicenseType(object.getStringValue(TYPE)));
        licenseRecord.setFirstName(object.getStringValue(FIRST_NAME));
        licenseRecord.setLastName(object.getStringValue(LAST_NAME));
        licenseRecord.setEmail(object.getStringValue(EMAIL));
        licenseRecord.setInstanceId(object.getStringValue(INSTANCE_ID));
        licenseRecord.setFeatureId(object.getStringValue(FEATURE_ID));
        licenseRecord.setDependentFeatureIds(object.getListValue(DEPENDENT_FEATURE_IDS));
        licenseRecord.setExtensionVersion(object.getStringValue(EXTENSION_VERSION));
        licenseRecord.setSupport(object.getStringValue(SUPPORT));
        if (object.safeget(MAX_USER_COUNT) != null) {
            licenseRecord.setMaxUserCount(object.getLongValue(MAX_USER_COUNT));
        }
        licenseRecord.setIssueDate(object.getDateValue(ISSUE_DATE));
        licenseRecord.setExpirationDate(object.getDateValue(EXPIRATION_DATE));
        licenseRecord.setHasLicense(object.getIntValue(HAS_LICENSE) == 1);
        licenseRecord.setSendEmails(object.getIntValue(SEND_EMAILS) == 1);
        return Optional.of(licenseRecord);
    }

    private static LicenseType toLicenseType(String value)
    {
        return StringUtils.isBlank(value) ? null : LicenseType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private static Integer toInt(boolean value)
    {
        return value ? 1 : 0;
    }
}
