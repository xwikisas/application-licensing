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
package com.xwiki.licensing.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LicensingTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void generateLicense() throws Exception
    {
        // Step 1: Generate Certificates and Keys by executing a script.

        // Delete pages that we create in the test
        getUtil().rest().deletePage("License", "GenerateCertificatesAndKeys");

        // Create a page in which we populate the stores
        String content = "{{velocity}}\n"
            + "#set($keystore = $services.crypto.store.getX509FileKeyStore('license-keystore'))\n"
            + "#set($certstore = $services.crypto.store.getX509SpaceCertificateStore("
                + "$services.model.resolveSpace('License.Certificates')))\n"
            + "#set($binaryEncoder = "
                + "$services.component.componentManager.getInstance('org.xwiki.crypto.BinaryStringEncoder','Base64'))\n"
            + "#if ($request.proceed == 1)\n"
            + "#set($keyPair = $services.crypto.rsa.generateKeyPair())\n"
            + "#set($rootCA = $services.crypto.rsa.issueRootCACertificate($keyPair, "
                + "'CN=Licence Root CA,OU=Licensing,O=XWiki SAS,L=Paris,C=FR', 730000))\n"
            + "#set($keyPair = $services.crypto.rsa.generateKeyPair())\n"
            + "#set($freeInterCA = $services.crypto.rsa.issueIntermediateCertificate($rootCA, $keyPair, "
                + "'CN=Free License Intermediate CA,OU=Licensing,O=XWiki SAS,L=Paris,C=FR', 365000))\n"
            + "#set($keyPair = $services.crypto.rsa.generateKeyPair())\n"
            + "#set($trialInterCA = $services.crypto.rsa.issueIntermediateCertificate($rootCA, $keyPair, "
                + "'CN=Trial License Intermediate CA,OU=Licensing,O=XWiki SAS,L=Paris,C=FR', 365000))\n"
            + "#set($keyPair = $services.crypto.rsa.generateKeyPair())\n"
            + "#set($paidInterCA = $services.crypto.rsa.issueIntermediateCertificate($rootCA, $keyPair, "
                + "'CN=Paid License Intermediate CA,OU=Licensing,O=XWiki SAS,L=Paris,C=FR', 365000))\n"
            + "#set($keyPair = $services.crypto.rsa.generateKeyPair())\n"
            + "#set($freeKeyPair = $services.crypto.rsa.issueCertificate($freeInterCA, $keyPair, "
                + "\"CN=Free License Issuer ${datetool.year},OU=Licensing,O=XWiki SAS,L=Paris,C=FR\", 74000, "
                    + "[$services.crypto.x509name.createX509Rfc822Name('free@acme.com')]))\n"
            + "#set($keyPair = $services.crypto.rsa.generateKeyPair())\n"
            + "#set($trialKeyPair = $services.crypto.rsa.issueCertificate($trialInterCA, $keyPair, "
                + "\"CN=Trial License Issuer ${datetool.year},OU=Licensing,O=XWiki SAS,L=Paris,C=FR\", 74000, "
                    + "[$services.crypto.x509name.createX509Rfc822Name('trial@acme.com')]))\n"
            + "#set($keyPair = $services.crypto.rsa.generateKeyPair())\n"
            + "#set($paidKeyPair = $services.crypto.rsa.issueCertificate($paidInterCA, $keyPair, "
                + "\"CN=Paid License Issuer ${datetool.year},OU=Licensing,O=XWiki SAS,L=Paris,C=FR\", 74000, "
                    + "[$services.crypto.x509name.createX509Rfc822Name('paid@acme.com')]))\n"
            + "#set($discard = $keystore.store($rootCA, 'rootPassword'))\n"
            + "#set($discard = $keystore.store($freeInterCA, 'freePassword'))\n"
            + "#set($discard = $keystore.store($trialInterCA, 'trialPassword'))\n"
            + "#set($discard = $keystore.store($paidInterCA, 'paidPassword'))\n"
            + "#set($discard = $keystore.store($freeKeyPair, 'freePassword'))\n"
            + "#set($discard = $keystore.store($trialKeyPair, 'trialPassword'))\n"
            + "#set($discard = $keystore.store($paidKeyPair, 'paidPassword'))\n"
            + "#set($discard = $certstore.store($rootCA.certificate))\n"
            + "#set($discard = $certstore.store($freeInterCA.certificate))\n"
            + "#set($discard = $certstore.store($trialInterCA.certificate))\n"
            + "#set($discard = $certstore.store($paidInterCA.certificate))\n"
            + "#set($discard = $certstore.store($freeKeyPair.certificate))\n"
            + "#set($discard = $certstore.store($trialKeyPair.certificate))\n"
            + "#set($discard = $certstore.store($paidKeyPair.certificate))\n"
            + "Free: $binaryEncoder.encode($freeInterCA.certificate.subjectKeyIdentifier)\n"
            + "Trial: $binaryEncoder.encode($trialInterCA.certificate.subjectKeyIdentifier)\n"
            + "Paid: $binaryEncoder.encode($paidInterCA.certificate.subjectKeyIdentifier)\n"
            + "#else\n"
            + "  #foreach ($certificate in $certstore.getAllCertificates())\n"
            + "    * $certificate.subject.name $binaryEncoder.encode($certificate.subjectKeyIdentifier)\n"
            + "  #end\n"
            + "#end"
            + "{{/velocity}}";
        getUtil().createPage("License", "GenerateCertificatesAndKeys", content, "GenerateCertificatesAndKeys");
        getUtil().gotoPage("License", "GenerateCertificatesAndKeys", "view", "proceed=1");
        ViewPage vp = new ViewPage();
        assertTrue(vp.getContent().contains("Free:"));
        assertTrue(vp.getContent().contains("Paid:"));
        assertTrue(vp.getContent().contains("Trial:"));

        // Step 2: Add a new license details and generate a license
        content = "{{include reference='License.Code.LicenseDetailsMacros'/}}\n"
            + "\n"
            + "{{velocity}}\n"
            + "#addLicenseDetails('John' 'john@acme.com' 'b6ad6165-daaf-41a1-8a3f-9aa81451c402' "
                + "'com.xwiki.activedirectory:application-activedirectory-main,"
                    + "com.xwiki.activedirectory:application-activedirectory-api' 'trial' true)\n"
            + "success"
            + "{{/velocity}}";
        vp = getUtil().createPage("License", "AddLicenseDetails", content, "AddLicenseDetails");
        assertEquals("success", vp.getContent());
    }
}
