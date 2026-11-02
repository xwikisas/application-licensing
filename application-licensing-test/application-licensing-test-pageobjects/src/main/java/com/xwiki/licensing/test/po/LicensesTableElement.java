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
import org.xwiki.test.ui.po.TableElement;

/**
 * Table displayed by the licensing pages.
 *
 * @version $Id$
 * @since 1.32.5
 */
public class LicensesTableElement extends TableElement
{
    private final WebElement rowsContainer;

    /**
     * @param table the table element
     */
    public LicensesTableElement(WebElement table)
    {
        super(table);
        List<WebElement> bodies = table.findElements(By.tagName("tbody"));
        this.rowsContainer = bodies.isEmpty() ? table : bodies.get(0);
    }

    // The TableElement implementation looks for the cells in the whole page, so the following methods are overwritten
    // to look for them only inside this table.

    @Override
    public List<WebElement> getColumn(int columnNumber)
    {
        if (numberOfRows() == 0 || numberOfColumns() < columnNumber || columnNumber < 0) {
            return null;
        }
        // XPath indexes start from 1.
        int index = columnNumber + 1;
        return this.rowsContainer.findElements(By.xpath(".//tr/th[" + index + "] | .//tr/td[" + index + "]"));
    }

    @Override
    public List<WebElement> getRow(int rowNumber)
    {
        if (numberOfRows() <= rowNumber || rowNumber < 0) {
            return null;
        }
        // XPath indexes start from 1.
        int index = rowNumber + 1;
        return this.rowsContainer.findElements(By.xpath(".//tr[" + index + "]/th | .//tr[" + index + "]/td"));
    }

    /**
     * @return the values of the first column, without the column header
     */
    public List<String> getFirstColumnValues()
    {
        return getColumn(0).stream().skip(1).map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * @param firstColumnValue the value of the first column of the row
     * @param columnHeader the header of the column
     * @return the value of the cell, or {@code null} if the table has no such row or column
     */
    public String getCellValue(String firstColumnValue, String columnHeader)
    {
        List<WebElement> row = getRow(firstColumnValue);
        int columnNumber = getColumnNumber(columnHeader);
        return row == null || columnNumber < 0 ? null : row.get(columnNumber).getText();
    }
}
