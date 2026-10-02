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

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.po.FormContainerElement;

/**
 * Page that generates new intermediate CA certificates from the current ones, keeping their key pairs. Before
 * generating, the page lists the current root and intermediate certificates. After generating, it lists the generated
 * certificates.
 *
 * @version $Id$
 * @since 1.32.5
 */
public class GenerateIntermediateCertificatesPage extends AbstractLicenseCertificatesPage
{
    private static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(Arrays.asList("License", "Code"), "GenerateIntermediateCertificates");

    private static final List<String> TYPES = Arrays.asList("free", "trial", "paid");

    private static final String CURRENT_CERTIFICATES = "currentCertificates";

    private static final String GENERATED_CERTIFICATES = "generatedCertificates";

    private static final String KEY_IDENTIFIER = "Key identifier";

    @FindBy(id = "generateIntermediateCertificates")
    private WebElement generateButton;

    public static GenerateIntermediateCertificatesPage gotoPage()
    {
        getUtil().gotoPage(REFERENCE);
        return new GenerateIntermediateCertificatesPage();
    }

    /**
     * Fills the form and submits it.
     *
     * @param rootPassword the password of the root CA private key
     * @param validity the validity of the new certificates, in days
     * @param types the license types (free, trial or paid) of the intermediate certificates to generate
     * @return the page displaying the result
     */
    public GenerateIntermediateCertificatesPage generate(String rootPassword, int validity, String... types)
    {
        FormContainerElement form = new FormContainerElement(By.id("generateIntermediateCertificatesForm"));
        form.setFieldValue(By.id("rootPassword"), rootPassword);
        form.setFieldValue(By.id("validity"), String.valueOf(validity));
        List<String> selectedTypes = Arrays.asList(types);
        for (String type : TYPES) {
            form.setCheckBox(By.id("generate_" + type), selectedTypes.contains(type));
        }
        clickAndWaitForPageReload(this.generateButton);
        return new GenerateIntermediateCertificatesPage();
    }

    /**
     * @param certificate the label of a current certificate (e.g. "Paid Intermediate CA"), listed before generating
     * @return the key identifier of the certificate
     */
    public String getKeyIdentifier(String certificate)
    {
        return getTable(CURRENT_CERTIFICATES).getCellValue(certificate, KEY_IDENTIFIER);
    }

    /**
     * @param certificate the label of a current certificate (e.g. "Paid Intermediate CA"), listed before generating
     * @return the serial number of the certificate
     */
    public String getSerial(String certificate)
    {
        return getTable(CURRENT_CERTIFICATES).getCellValue(certificate, "Serial");
    }

    /**
     * @return the labels of the generated certificates (e.g. "Paid Intermediate CA")
     */
    public List<String> getGeneratedCertificates()
    {
        return getTable(GENERATED_CERTIFICATES).getFirstColumnValues();
    }

    /**
     * @param certificate the label of a generated certificate
     * @return the key identifier of the generated certificate
     */
    public String getGeneratedKeyIdentifier(String certificate)
    {
        return getTable(GENERATED_CERTIFICATES).getCellValue(certificate, KEY_IDENTIFIER);
    }

    /**
     * @param certificate the label of a generated certificate
     * @return the serial number of the certificate that was replaced
     */
    public String getOldSerial(String certificate)
    {
        return getTable(GENERATED_CERTIFICATES).getCellValue(certificate, "Old serial");
    }

    /**
     * @param certificate the label of a generated certificate
     * @return the serial number of the generated certificate
     */
    public String getNewSerial(String certificate)
    {
        return getTable(GENERATED_CERTIFICATES).getCellValue(certificate, "New serial");
    }

    /**
     * @param certificate the label of a generated certificate
     * @return {@code true} if the generated certificate was stored in the certificate store
     */
    public boolean isStored(String certificate)
    {
        return "yes".equals(getTable(GENERATED_CERTIFICATES).getCellValue(certificate, "Stored"));
    }

    /**
     * @return the text of the success message displayed after generating the certificates
     */
    public String getSuccessMessage()
    {
        return getDriver().findElement(By.cssSelector("#xwikicontent .box.successmessage")).getText();
    }
}
