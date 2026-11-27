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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xwiki.licensing.internal.issuing.model.InstanceClassInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InstanceRecordStore}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({ DefaultConverterManager.class, EnumConverter.class, ConvertUtilsConverter.class })
class InstanceRecordStoreTest
{
    private static final String INSTANCE_ID = "9f1c2e4a-7b3d-4c8e-a1f0-2d5e6b7c8a90";

    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final DocumentReference RECORD =
        new DocumentReference("xwiki", List.of("License", "API", "Instances"), INSTANCE_ID);

    private static final List<String> ACCOUNT_VALUES = List.of("XWiki.partner", "xwiki:XWiki.partner");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private InstanceRecordStore store;

    @MockComponent
    private ApiConfiguration configuration;

    @MockComponent
    private QueryManager queryManager;

    private final UserReference recordAuthor = mock(UserReference.class);

    private final Date created = new Date(1_800_000_000_000L);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.configuration.getMainWiki()).thenReturn(new WikiReference("xwiki"));
        when(this.configuration.getRecordAuthor()).thenReturn(this.recordAuthor);
        XWikiDocument classDocument = new XWikiDocument(
            new DocumentReference(InstanceClassInitializer.CLASS_REFERENCE, new WikiReference("xwiki")));
        BaseClass xclass = classDocument.getXClass();
        xclass.addTextField(InstanceClassInitializer.INSTANCE_ID, "Instance id", 60);
        xclass.addUsersField(InstanceClassInitializer.ACCOUNT, "Account", false);
        xclass.addStaticListField(InstanceClassInitializer.ENVIRONMENT, "Environment", "prod|test");
        xclass.addTextField(InstanceClassInitializer.DOMAIN, "Domain", 60);
        xclass.addTextField(InstanceClassInitializer.PRODUCT, "Product", 30);
        xclass.addStaticListField(InstanceClassInitializer.ORIGIN, "Origin", "api|import");
        xclass.addDateField(InstanceClassInitializer.CREATED_DATE, "Created", null, 0);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, this.oldcore.getXWikiContext());
    }

    @Test
    void create() throws Exception
    {
        this.store.create(new InstanceRecord(INSTANCE_ID, ACCOUNT, Environment.TEST, null, "bundle",
            InstanceRecord.Origin.API, this.created));

        XWikiDocument document = getRecord();
        assertEquals("Instance record created", document.getComment());
        assertTrue(document.isMinorEdit());
        assertEquals(this.recordAuthor, document.getAuthors().getCreator());
        assertEquals(this.recordAuthor, document.getAuthors().getEffectiveMetadataAuthor());
        assertEquals(this.recordAuthor, document.getAuthors().getOriginalMetadataAuthor());
        assertEquals(this.recordAuthor, document.getAuthors().getContentAuthor());
        BaseObject object = document.getXObject(InstanceClassInitializer.CLASS_REFERENCE);
        assertEquals(INSTANCE_ID, object.getStringValue(InstanceClassInitializer.INSTANCE_ID));
        // Relative to the main wiki, as an admin would select the user.
        assertEquals("XWiki.partner", object.getStringValue(InstanceClassInitializer.ACCOUNT));
        assertEquals("test", object.getStringValue(InstanceClassInitializer.ENVIRONMENT));
        assertEquals("", object.getStringValue(InstanceClassInitializer.DOMAIN));
        assertEquals("bundle", object.getStringValue(InstanceClassInitializer.PRODUCT));
        assertEquals("api", object.getStringValue(InstanceClassInitializer.ORIGIN));
        assertEquals(this.created, object.getDateValue(InstanceClassInitializer.CREATED_DATE));

        // Never overwrite a record.
        InstanceRecord again = new InstanceRecord(INSTANCE_ID, ACCOUNT, Environment.PROD, "wiki.example.com",
            "bundle", InstanceRecord.Origin.API, this.created);
        assertThrows(XWikiException.class, () -> this.store.create(again));
    }

    @Test
    void updateDomainAndDelete() throws Exception
    {
        this.store.create(new InstanceRecord(INSTANCE_ID, ACCOUNT, Environment.PROD, "old.example.com", "bundle",
            InstanceRecord.Origin.IMPORT, this.created));

        this.store.updateDomain(INSTANCE_ID, "new.example.com");

        XWikiDocument document = getRecord();
        assertEquals("Domain updated", document.getComment());
        assertEquals(this.recordAuthor, document.getAuthors().getContentAuthor());
        BaseObject object = document.getXObject(InstanceClassInitializer.CLASS_REFERENCE);
        assertEquals("new.example.com", object.getStringValue(InstanceClassInitializer.DOMAIN));
        assertEquals("import", object.getStringValue(InstanceClassInitializer.ORIGIN));

        this.store.delete(INSTANCE_ID);
        assertTrue(getRecord().isNew());
        // Deleting a missing record does nothing.
        this.store.delete(INSTANCE_ID);
        assertFalse(this.oldcore.getSpyXWiki().exists(RECORD, this.oldcore.getXWikiContext()));
    }

    @Test
    void updateDomainOfAMissingRecord()
    {
        assertThrows(XWikiException.class, () -> this.store.updateDomain(INSTANCE_ID, "new.example.com"));
    }

    @Test
    void getInstances() throws Exception
    {
        InstanceRecord first = new InstanceRecord("first", ACCOUNT, Environment.PROD, null, "bundle",
            InstanceRecord.Origin.API, this.created);
        when(this.configuration.getInstance("first")).thenReturn(Optional.of(first));
        when(this.configuration.getInstance("deleted")).thenReturn(Optional.empty());
        mockQuery("select doc.name from Document doc, doc.object(License.Code.API.InstanceClass) instance "
            + "where instance.account in (:accounts) order by doc.creationDate, doc.name", List.of("first", "deleted"));

        assertEquals(List.of(first), this.store.getInstances(ACCOUNT, null, null));
    }

    @Test
    void getInstancesFiltered() throws Exception
    {
        Query query = mockQuery("select doc.name from Document doc, doc.object(License.Code.API.InstanceClass) "
            + "instance where instance.account in (:accounts) and instance.environment = :environment "
            + "and instance.product = :product order by doc.creationDate, doc.name", List.of());
        when(query.bindValue("environment", "test")).thenReturn(query);
        when(query.bindValue("product", "bundle")).thenReturn(query);

        assertEquals(List.of(), this.store.getInstances(ACCOUNT, Environment.TEST, "bundle"));
    }

    @Test
    void getAccountValues()
    {
        assertEquals(ACCOUNT_VALUES, this.store.getAccountValues(ACCOUNT));
    }

    private Query mockQuery(String statement, List<Object> results) throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(statement, Query.XWQL)).thenReturn(query);
        when(query.setWiki("xwiki")).thenReturn(query);
        when(query.bindValue("accounts", ACCOUNT_VALUES)).thenReturn(query);
        when(query.execute()).thenReturn(results);
        return query;
    }

    private XWikiDocument getRecord() throws Exception
    {
        return this.oldcore.getSpyXWiki().getDocument(RECORD, this.oldcore.getXWikiContext());
    }
}
