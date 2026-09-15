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

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test for creating many users on an instance.
 * This test takes a very long time to set up (~40 min for 10k users), so it should be run sparingly, whenever there are
 * changes that you think might crash instances with 10k users.
 *
 * @version $Id$
 * @since 1.33.0
 */
@UITest
class UserStressTestLicensingIT
{
    private static final String PASSWORD = "password";

    private static final int USER_COUNT = 10_000;

    // The tested user limit MUST be lower than the amount of users on the instance, otherwise the method returns
    // TRUE without actually populating the user list.
    private static final String TEST_PAGE_SCRIPT = String.format("{{velocity}}\n"
        + "$services.licensing.licensor.isUserUnderLimit($xwiki.getUser().getUser().getUserReference(), %s)\n"
        + "{{/velocity}}", USER_COUNT - 100);

    private static final DocumentReference testPage = new DocumentReference("xwiki", "Test", "Test");

    private static final String TEST_STRING = "Hello!";

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        createUsers(setup);
    }

    @Test
    void stressTest(TestReference testReference, TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // A lot of users on the test instance shouldn't be crashing the licensor when it tries to compute the user
        // limit.
        setup.createPage(testPage, TEST_PAGE_SCRIPT);
        setup.gotoPage(testPage, "view");
        // To test that we didn't crash, edit a random page and save it.
        setup.gotoPage(testPage, "edit");
        WikiEditPage wikiEditPage = new WikiEditPage();
        wikiEditPage.setContent(TEST_STRING);
        wikiEditPage.clickSaveAndView();
        assertTrue(setup.isInViewMode());
    }

    private void createUsers(TestUtils setup)
    {
        // First user is XWiki.Admin, so start counting from 2.
        IntStream.range(2, USER_COUNT).parallel().forEach(i -> {
            DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User_" + i);
            setup.addObject(documentReference, "XWiki.XWikiUsers", "password", PASSWORD);
        });
    }

    private void deleteUsers(TestUtils setup)
    {
        // First user is XWiki.Admin, so start counting from 2.
        IntStream.range(2, USER_COUNT).parallel().forEach(i -> {
            DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User_" + i);
            setup.deletePage(documentReference);
        });
    }
}
