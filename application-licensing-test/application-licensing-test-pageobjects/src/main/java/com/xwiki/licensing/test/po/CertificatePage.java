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
package com.xwiki.licensing.test.po;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Page that displays a certificate and the active licenses that depend on it. After regenerating these licenses, the
 * page lists the regenerated licenses.
 *
 * @version $Id$
 * @since 1.32.5
 */
public class CertificatePage extends AbstractLicenseCertificatesPage
{
    private static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(Arrays.asList("License", "Certificates"), "WebHome");

    private static final String CERTIFICATE_LICENSES = "certificateLicenses";

    private static final String REGENERATED_LICENSES = "regeneratedLicenses";

    @FindBy(id = "regenerateLicenses")
    private WebElement regenerateLicensesButton;

    public static CertificatePage gotoPage(String keyIdentifier)
    {
        getUtil().gotoPage(REFERENCE, "view",
            "certificate=" + URLEncoder.encode(keyIdentifier, StandardCharsets.UTF_8));
        return new CertificatePage();
    }

    /**
     * @return the ids of the licenses listed for this certificate
     */
    public List<String> getLicenseIds()
    {
        return getTable(CERTIFICATE_LICENSES).getFirstColumnValues();
    }

    /**
     * Regenerates the listed licenses.
     *
     * @return the page listing the regenerated licenses
     */
    public CertificatePage regenerateLicenses()
    {
        clickAndWaitForPageReload(this.regenerateLicensesButton);
        return new CertificatePage();
    }

    /**
     * @return the ids of the regenerated licenses, listed after regenerating the licenses
     */
    public List<String> getRegeneratedLicenseIds()
    {
        return getTable(REGENERATED_LICENSES).getFirstColumnValues();
    }

    /**
     * @param licenseId the id of a regenerated license, listed after regenerating the licenses
     * @return the id of the license that replaces it
     */
    public String getNewLicenseId(String licenseId)
    {
        return getTable(REGENERATED_LICENSES).getCellValue(licenseId, "New License");
    }
}
