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

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Base class for the pages that handle licensing certificates operations.
 *
 * @version $Id$
 * @since 1.32.5
 */
public abstract class AbstractLicenseCertificatesPage extends ViewPage
{
    /**
     * @return the error messages listed in the error box displayed in the page content
     */
    public List<String> getErrors()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector("#xwikicontent .box.errormessage li")).stream()
            .map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * @param tableId the id of the table
     * @return the table with the given id
     */
    protected LicensesTableElement getTable(String tableId)
    {
        return new LicensesTableElement(getDriver().findElement(By.id(tableId)));
    }

    /**
     * Clicks on the given element and waits for the page to be reloaded.
     *
     * @param element the element to click
     */
    protected void clickAndWaitForPageReload(WebElement element)
    {
        getDriver().addPageNotYetReloadedMarker();
        element.click();
        getDriver().waitUntilPageIsReloaded();
    }
}
