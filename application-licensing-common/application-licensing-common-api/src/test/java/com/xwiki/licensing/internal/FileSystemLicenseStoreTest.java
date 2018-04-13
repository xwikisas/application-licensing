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
package com.xwiki.licensing.internal;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.FileLicenseStoreReference;
import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseSerializer;
import com.xwiki.licensing.LicenseStore;
import com.xwiki.licensing.LicenseStoreReference;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;
import com.xwiki.licensing.SignedLicense;
import com.xwiki.licensing.test.LicensingComponentList;
import com.xwiki.licensing.test.SignedLicenseTestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link FileSystemLicenseStore}.
 *
 * @version $Id$
 */
@LicensingComponentList
public class FileSystemLicenseStoreTest
{
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Rule
    public MockitoComponentMockingRule<LicenseStore> mockedStore =
        new MockitoComponentMockingRule<>(FileSystemLicenseStore.class);

    @Rule
    public AllLogRule logRule = new AllLogRule();

    private static final File TEST_DIR = new File("target/tmp");
    private static final File SINGLE_STORE_FILE = new File(TEST_DIR, "single.license");
    private static final File MULTI_STORE_DIR = new File(TEST_DIR, "licenses");
    private static final LicenseStoreReference SINGLE_STORE_REFERENCE = new FileLicenseStoreReference(SINGLE_STORE_FILE, false);
    private static final LicenseStoreReference MULTI_STORE_REFERENCE = new FileLicenseStoreReference(MULTI_STORE_DIR, true);
    private static final License testLicense = new License();
    private static final String xmlLicense = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
        + "    <modelVersion>2.0.0</modelVersion>\n"
        + "    <type>FREE</type>\n"
        + "    <licensed>\n"
        + "        <features>\n"
        + "            <feature>\n"
        + "                <id>test</id>\n"
        + "                <version>1.0</version>\n"
        + "            </feature>\n"
        + "        </features>\n"
        + "    </licensed>\n"
        + "    <licencee>\n"
        + "        <firstName>John</firstName>\n"
        + "        <lastName>Doe</lastName>\n"
        + "        <email>john.doe@example.com</email>\n"
        + "        <meta key=\"support\">silver</meta>\n"
        + "    </licencee>\n"
        + "</license>\n";

    private LicenseStore store;

    private void checkSampleLicense(License license)
    {
        assertThat(license.getId().toString(), equalTo("00000000-0000-0000-0000-000000000000"));
        assertThat(license.getType(), equalTo(LicenseType.FREE));
        assertThat(license.getFeatureIds(), containsInAnyOrder(new LicensedFeatureId("test", "1.0")));
        assertThat(license.getLicensee(), not(nullValue()));
        assertThat(license.getLicensee().size(), equalTo(4));
        assertThat(license.getLicensee().get("firstName"), equalTo("John"));
        assertThat(license.getLicensee().get("lastName"), equalTo("Doe"));
        assertThat(license.getLicensee().get("email"), equalTo("john.doe@example.com"));
        assertThat(license.getLicensee().get("support"), equalTo("silver"));
    }

    @Before
    public void setUp() throws Exception
    {
        store = mockedStore.getComponentUnderTest();
        FileUtils.deleteDirectory(TEST_DIR);
        TEST_DIR.mkdirs();
    }

    @After
    public void deleteTestFiles() throws Exception
    {
        FileUtils.deleteDirectory(TEST_DIR);
    }

    @Test
    public void testStoreUnsignedLicenseToFile() throws Exception
    {
        LicenseSerializer serializer = mockedStore.getInstance(LicenseSerializer.TYPE_STRING, "xml");
        when(serializer.serialize(testLicense)).thenReturn(xmlLicense);

        store.store(SINGLE_STORE_REFERENCE, testLicense);

        assertThat(SINGLE_STORE_FILE.exists(), is(true));
        assertThat(FileUtils.readFileToString(SINGLE_STORE_FILE, UTF8), equalTo(xmlLicense));
    }

    @Test
    public void testStoreSignedLicenseToFile() throws Exception
    {
        SignedLicenseTestUtils utils = mockedStore.getInstance(SignedLicenseTestUtils.class);
        SignedLicense signedLicense = utils.getSignedLicense();
        byte[] signedLicenseBytes = signedLicense.getEncoded();

        store.store(SINGLE_STORE_REFERENCE, signedLicense);

        assertThat(SINGLE_STORE_FILE.exists(), is(true));
        assertThat(FileUtils.readFileToByteArray(SINGLE_STORE_FILE), equalTo(signedLicenseBytes));
    }

    @Test
    public void testStoreUnsignedLicenseToFolder() throws Exception
    {
        LicenseSerializer serializer = mockedStore.getInstance(LicenseSerializer.TYPE_STRING, "xml");
        when(serializer.serialize(testLicense)).thenReturn(xmlLicense);

        store.store(MULTI_STORE_REFERENCE, testLicense);

        assertThat(MULTI_STORE_DIR.exists(), is(true));

        File storeFile = new File(MULTI_STORE_DIR, testLicense.getId() + ".license");
        assertThat(storeFile.exists(), is(true));
        assertThat(FileUtils.readFileToString(storeFile, UTF8), equalTo(xmlLicense));
    }

    @Test
    public void testStoreSignedLicenseToFolder() throws Exception
    {
        SignedLicenseTestUtils utils = mockedStore.getInstance(SignedLicenseTestUtils.class);
        SignedLicense signedLicense = utils.getSignedLicense();
        byte[] signedLicenseBytes = signedLicense.getEncoded();

        store.store(MULTI_STORE_REFERENCE, signedLicense);

        assertThat(MULTI_STORE_DIR.exists(), is(true));

        File storeFile = new File(MULTI_STORE_DIR, signedLicense.getId() + ".license");
        assertThat(storeFile.exists(), is(true));
        assertThat(FileUtils.readFileToByteArray(storeFile), equalTo(signedLicenseBytes));
    }

    @Test
    public void testRetrieveUnsignedLicenseFromFile() throws Exception
    {
        FileUtils.writeStringToFile(SINGLE_STORE_FILE, xmlLicense, UTF8);

        License license = store.retrieve(SINGLE_STORE_REFERENCE);

        checkSampleLicense(license);
    }

    @Test
    public void testRetrieveSignedLicenseFromFile() throws Exception
    {
        SignedLicenseTestUtils utils = mockedStore.getInstance(SignedLicenseTestUtils.class);
        SignedLicense signedLicense = utils.getSignedLicense();
        byte[] signedLicenseBytes = signedLicense.getEncoded();

        FileUtils.writeByteArrayToFile(SINGLE_STORE_FILE, signedLicenseBytes);

        License license = store.retrieve(SINGLE_STORE_REFERENCE);

        assertThat(license, equalTo(signedLicense));
    }


    @Test
    public void testRetrieveUnsignedLicenseFromFolder() throws Exception
    {
        MULTI_STORE_DIR.mkdir();
        File storeFile = new File(MULTI_STORE_DIR, testLicense.getId() + ".license");
        FileUtils.writeStringToFile(storeFile, xmlLicense, UTF8);

        License license = store.retrieve(MULTI_STORE_REFERENCE, testLicense.getId());

        checkSampleLicense(license);
    }

    @Test
    public void testRetrieveSignedLicenseFromFolder() throws Exception
    {
        SignedLicenseTestUtils utils = mockedStore.getInstance(SignedLicenseTestUtils.class);
        SignedLicense signedLicense = utils.getSignedLicense();
        byte[] signedLicenseBytes = signedLicense.getEncoded();

        MULTI_STORE_DIR.mkdir();
        File storeFile = new File(MULTI_STORE_DIR, signedLicense.getId() + ".license");
        FileUtils.writeByteArrayToFile(storeFile, signedLicenseBytes);

        License license = store.retrieve(MULTI_STORE_REFERENCE, signedLicense.getId());

        assertThat(license, equalTo(signedLicense));
    }

    @Test
    public void testDeleteUnsignedLicenseFromFile() throws Exception
    {
        FileUtils.writeStringToFile(SINGLE_STORE_FILE, xmlLicense, UTF8);

        assertThat(SINGLE_STORE_FILE.exists(), is(true));

        store.delete(SINGLE_STORE_REFERENCE);

        assertThat(SINGLE_STORE_FILE.exists(), is(false));
    }

    @Test
    public void testDeleteUnsignedLicenseFromFolder() throws Exception
    {
        MULTI_STORE_DIR.mkdir();
        File storeFile = new File(MULTI_STORE_DIR, testLicense.getId() + ".license");
        FileUtils.writeStringToFile(storeFile, xmlLicense, UTF8);

        assertThat(storeFile.exists(), is(true));

        store.delete(MULTI_STORE_REFERENCE, testLicense.getId());

        assertThat(storeFile.exists(), is(false));
    }

    @Test
    public void testIterateOverLicenseStore() throws Exception
    {
        SignedLicenseTestUtils utils = mockedStore.getInstance(SignedLicenseTestUtils.class);
        SignedLicense signedLicense = utils.getSignedLicense();
        byte[] signedLicenseBytes = signedLicense.getEncoded();

        MULTI_STORE_DIR.mkdir();
        File unsignedStoreFile = new File(MULTI_STORE_DIR, testLicense.getId() + ".license");
        FileUtils.writeStringToFile(unsignedStoreFile, xmlLicense, UTF8);
        File signedStoreFile = new File(MULTI_STORE_DIR, signedLicense.getId() + ".license");
        FileUtils.writeByteArrayToFile(signedStoreFile, signedLicenseBytes);

        boolean unsigned = false, signed = false;
        for(License license : store.getIterable(MULTI_STORE_REFERENCE)) {
            if (license instanceof SignedLicense) {
                assertThat(signed, is(false));
                assertThat(license, equalTo(signedLicense));
                signed = true;
            } else {
                assertThat(unsigned, is(false));
                checkSampleLicense(license);
                unsigned = true;
            }
        }
        assertThat(signed, is(true));
        assertThat(unsigned, is(true));
    }

}
