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

import javax.inject.Named;

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
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xwiki.licensing.LicenseType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicenseRecordStore}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({ DefaultConverterManager.class, EnumConverter.class, ConvertUtilsConverter.class })
class LicenseRecordStoreTest
{
    private static final String LICENSE_ID = "c0ffee00-0000-4000-8000-000000000001";

    private static final String INSTANCE_ID = "11111111-2222-3333-4444-555555555555";

    private static final String FEATURE_ID = "com.xwiki.pro:xwiki-pro-ui";

    private static final DocumentReference RECORD =
        new DocumentReference("xwiki", List.of("License", "Data"), LICENSE_ID);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private LicenseRecordStore store;

    @MockComponent
    private ApiConfiguration configuration;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private QueryManager queryManager;

    private final UserReference recordAuthor = mock(UserReference.class);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.configuration.getMainWiki()).thenReturn(new WikiReference("xwiki"));
        when(this.configuration.getRecordAuthor()).thenReturn(this.recordAuthor);
        saveDetailsClass();
    }

    @Test
    void createAndGet() throws Exception
    {
        LicenseRecord licenseRecord = createRecord();

        this.store.create(licenseRecord);

        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(RECORD, this.oldcore.getXWikiContext());
        assertEquals("License generated", document.getComment());
        assertTrue(document.isMinorEdit());
        assertEquals(this.recordAuthor, document.getAuthors().getCreator());
        assertEquals(this.recordAuthor, document.getAuthors().getEffectiveMetadataAuthor());
        assertEquals(this.recordAuthor, document.getAuthors().getContentAuthor());
        // The type is stored in lower case, as Sales stores it.
        BaseObject object = document.getXObject(LicenseRecordStore.DETAILS_CLASS);
        assertEquals("paid", object.getStringValue("type"));
        assertEquals(1, object.getIntValue("hasLicense"));

        LicenseRecord read = this.store.get(LICENSE_ID).get();
        assertEquals(LICENSE_ID, read.getLicenseId());
        assertEquals(LicenseType.PAID, read.getType());
        assertEquals("Jane", read.getFirstName());
        assertEquals("Doe", read.getLastName());
        assertEquals("jane@example.com", read.getEmail());
        assertEquals(INSTANCE_ID, read.getInstanceId());
        assertEquals(FEATURE_ID, read.getFeatureId());
        assertEquals(List.of("com.xwiki.pro:pro-api", "com.xwiki.date:date-api"), read.getDependentFeatureIds());
        assertEquals("1.2", read.getExtensionVersion());
        assertEquals("gold", read.getSupport());
        assertEquals(-1L, read.getMaxUserCount());
        assertEquals(licenseRecord.getIssueDate(), read.getIssueDate());
        assertEquals(licenseRecord.getExpirationDate(), read.getExpirationDate());
        assertTrue(read.hasLicense());
        assertFalse(read.isSendEmails());
    }

    @Test
    void createWithoutOptionalDetails() throws Exception
    {
        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setLicenseId(LICENSE_ID);
        licenseRecord.setType(LicenseType.TRIAL);
        licenseRecord.setInstanceId(INSTANCE_ID);
        licenseRecord.setFeatureId(FEATURE_ID);
        licenseRecord.setSendEmails(true);

        this.store.create(licenseRecord);

        LicenseRecord read = this.store.get(LICENSE_ID).get();
        assertEquals(LicenseType.TRIAL, read.getType());
        assertEquals("", read.getEmail());
        assertTrue(read.getDependentFeatureIds().isEmpty());
        assertNull(read.getMaxUserCount());
        assertFalse(read.hasLicense());
        assertTrue(read.isSendEmails());
    }

    @Test
    void createNeverOverwritesARecord() throws Exception
    {
        this.store.create(createRecord());

        XWikiException exception = assertThrows(XWikiException.class, () -> this.store.create(createRecord()));

        assertTrue(exception.getMessage().contains("The license record [xwiki:License.Data." + LICENSE_ID
            + "] already exists."));
    }

    @Test
    void delete() throws Exception
    {
        this.store.create(createRecord());

        this.store.delete(LICENSE_ID);
        // Deleting a missing record does nothing.
        this.store.delete(LICENSE_ID);

        assertEquals(Optional.empty(), this.store.get(LICENSE_ID));
    }

    @Test
    void getWithoutDetailsObject() throws Exception
    {
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(RECORD), this.oldcore.getXWikiContext());

        assertEquals(Optional.empty(), this.store.get(LICENSE_ID));
    }

    @Test
    void getWithoutType() throws Exception
    {
        // A request saved by Sales before choosing the type.
        XWikiDocument document = new XWikiDocument(RECORD);
        BaseObject object = new BaseObject();
        object.setXClassReference(LicenseRecordStore.DETAILS_CLASS);
        object.setStringValue("licenseId", LICENSE_ID);
        document.addXObject(object);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        LicenseRecord read = this.store.get(LICENSE_ID).get();

        assertEquals(LICENSE_ID, read.getLicenseId());
        assertNull(read.getType());
    }

    @Test
    void getLatest() throws Exception
    {
        this.store.create(createRecord());
        Query query = mockQuery("from doc.object(License.Code.LicenseDetailsClass) license "
            + "where license.instanceId = :instanceId "
            + "and (license.featureId = :featureId or :featureId member of license.dependentFeatureIds) "
            + "and license.hasLicense = 1 "
            + "order by license.issueDate desc, license.expirationDate desc, doc.creationDate desc");
        when(query.<String>execute()).thenReturn(List.of("License.Data." + LICENSE_ID), List.of());

        assertEquals(LICENSE_ID, this.store.getLatest(INSTANCE_ID, "com.xwiki.pro:pro-api").get().getLicenseId());
        assertEquals(Optional.empty(), this.store.getLatest(INSTANCE_ID, "com.xwiki.other:other"));

        verify(query).bindValue("featureId", "com.xwiki.pro:pro-api");
    }

    @Test
    void hasRecords() throws Exception
    {
        Query query = mockQuery(
            "from doc.object(License.Code.LicenseDetailsClass) license where license.instanceId = :instanceId");
        when(query.<String>execute()).thenReturn(List.of("License.Data." + LICENSE_ID), List.of());

        assertTrue(this.store.hasRecords(INSTANCE_ID));
        assertFalse(this.store.hasRecords("other"));
    }

    @Test
    void getGenerated() throws Exception
    {
        this.store.create(createRecord());
        Query query = mockQuery("from doc.object(License.Code.LicenseDetailsClass) license "
            + "where license.instanceId = :instanceId and license.hasLicense = 1 "
            + "order by license.issueDate desc, license.expirationDate desc, doc.creationDate desc");
        // A record deleted since the query ran is skipped.
        when(query.<String>execute()).thenReturn(List.of("License.Data." + LICENSE_ID, "License.Data.deleted"));

        List<LicenseRecord> records = this.store.getGenerated(INSTANCE_ID);

        assertEquals(1, records.size());
        assertEquals(LICENSE_ID, records.get(0).getLicenseId());
    }

    private Query mockQuery(String statement) throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(statement, Query.XWQL)).thenReturn(query);
        when(query.setWiki("xwiki")).thenReturn(query);
        when(query.bindValue(anyString(), any()))
            .thenReturn(query);
        when(query.setLimit(1)).thenReturn(query);
        return query;
    }

    private static LicenseRecord createRecord()
    {
        LicenseRecord licenseRecord = new LicenseRecord();
        licenseRecord.setLicenseId(LICENSE_ID);
        licenseRecord.setType(LicenseType.PAID);
        licenseRecord.setFirstName("Jane");
        licenseRecord.setLastName("Doe");
        licenseRecord.setEmail("jane@example.com");
        licenseRecord.setInstanceId(INSTANCE_ID);
        licenseRecord.setFeatureId(FEATURE_ID);
        licenseRecord.setDependentFeatureIds(List.of("com.xwiki.pro:pro-api", "com.xwiki.date:date-api"));
        licenseRecord.setExtensionVersion("1.2");
        licenseRecord.setSupport("gold");
        licenseRecord.setMaxUserCount(-1L);
        // The database keeps seconds only.
        licenseRecord.setIssueDate(new Date(1795000000000L));
        licenseRecord.setExpirationDate(new Date(1826536000000L));
        licenseRecord.setHasLicense(true);
        return licenseRecord;
    }

    /**
     * Declares the properties of {@code License.Code.LicenseDetailsClass} as the XAR does.
     */
    private void saveDetailsClass() throws Exception
    {
        XWikiDocument classDocument = new XWikiDocument(
            new DocumentReference(LicenseRecordStore.DETAILS_CLASS, new WikiReference("xwiki")));
        BaseClass xclass = classDocument.getXClass();
        xclass.addTextField("licenseId", "License Id", 60);
        xclass.addStaticListField("type", "License Type", "free=Free|trial=Trial|paid=Paid");
        xclass.addTextField("firstName", "Licensee First Name", 60);
        xclass.addTextField("lastName", "Licensee Last Name", 60);
        xclass.addEmailField("email", "Licensee Email", 60);
        xclass.addTextField("instanceId", "Instance Id", 60);
        xclass.addTextField("featureId", "Extension Id", 60);
        xclass.addStaticListField("dependentFeatureIds", "Dependent Extension Ids", 60, true, true, "", "input",
            "|, ");
        xclass.addTextField("extensionVersion", "Extension Version", 60);
        xclass.addStaticListField("support", "Support Level", "");
        xclass.addNumberField("maxUserCount", "User Limit", 30, "long");
        xclass.addDateField("issueDate", "Issue Date", "dd/MM/yyyy");
        xclass.addDateField("expirationDate", "Expiration Date", "dd/MM/yyyy");
        xclass.addBooleanField("hasLicense", "Has License");
        xclass.addBooleanField("sendEmails", "Send emails");
        this.oldcore.getSpyXWiki().saveDocument(classDocument, this.oldcore.getXWikiContext());
    }
}
