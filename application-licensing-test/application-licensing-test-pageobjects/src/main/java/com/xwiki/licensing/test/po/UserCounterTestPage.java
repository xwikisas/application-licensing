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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

public class UserCounterPage extends ViewPage
{
    public static final DocumentReference testPage = new DocumentReference("xwiki", "Test", "UserCounterTestPage");

    @FindBy(id = "params_user")
    private WebElement userParam;

    @FindBy(id = "params_limit")
    private WebElement limitParam;

    @FindBy(id = "userCount")
    private WebElement userCount;

    @FindBy(id = "isUserUnderLimit")
    private WebElement isUserUnderLimit;

    public static UserCounterTestPage gotoPage(TestUtils setup, String user, Integer limit) throws Exception
    {
        // Make sure we're on the right subwiki.
        String currentWiki = setup.getCurrentWiki();
        setup.setCurrentWiki(UserCounterTestPage.testPage.getWikiReference().getName());

        // Create the page if it doesn't exist.
        try (InputStream content = UserCounterTestPage.class.getResourceAsStream("/UserCounterTestPage.wiki")) {
            setup.rest().savePage(testPage, IOUtils.toString(content, StandardCharsets.UTF_8), "");
        }

        Map<String, Object> queryParams = new HashMap<>();
        if (user != null) {
            queryParams.put("user", user);
        }
        if (limit != null) {
            queryParams.put("limit", limit);
        }
        setup.gotoPage(testPage, "view", queryParams);
        setup.setCurrentWiki(currentWiki);
        return new UserCounterTestPage();
    }

    public static void deletePage(TestUtils setup)
    {
        setup.deletePage(testPage);
    }

    public int getUserCount()
    {
        return Integer.parseInt(userCount.getText());
    }

    public boolean isUserUnderLimit()
    {
        return Boolean.parseBoolean(isUserUnderLimit.getText());
    }

    public int getLimitParam()
    {
        return Integer.parseInt(limitParam.getText());
    }

    public String getUserParam()
    {
        return userParam.getText();
    }
}
