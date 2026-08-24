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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.licensing.test.po.UserCounterPage;

/**
 * Verify that users over the user limit are marked and counted correctly.
 *
 * @version $Id$
 * @since 1.33.0
 */
@UITest
class UserCounterIT
{
    public static final LocalDocumentReference testPage = new LocalDocumentReference("Test", "UserCounterTestPage");

    private static final String PASSWORD = "password";

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterAll
    void afterAll(TestUtils setup)
    {
        deleteUserCounterTestPage(setup);
    }

    @BeforeEach
    void beforeEach(TestUtils setup) throws Exception
    {
        Assertions.assertEquals(0, this.getUserCountOnInstance(setup));
    }

    @AfterEach
    void afterEach(TestUtils setup) throws Exception
    {
        Assertions.assertEquals(0, this.getUserCountOnInstance(setup));
    }

    /**
     * This test documents the behavior of isUserUnderLimit() for special users.
     */
    @Test
    void otherUserTest(TestUtils setup) throws Exception
    {
        // 0 users on the instance for these tests.
        Assertions.assertEquals(0, this.getUserCountOnInstance(setup));

        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.XWikiGuest", 1));
        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.superadmin", 1));
        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.NonExistentUser", 1));
        // Unlimited limit.
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.XWikiGuest", -1));
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.superadmin", -1));
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.NonExistentUser", -1));
    }

    /**
     * Test for checking infinite user limits.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false)
    void infiniteLimitTest(WikiReference wikiReference, TestUtils setup) throws Exception
    {
        String subwikiName = wikiReference.getName();
        createUsers(setup, "xwiki", 1, 2, true);
        createUsers(setup, "xwiki", 2, 3, false);
        createUsers(setup, subwikiName, 3, 4, true);

        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_1", -1));
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_2", -1));
        Assertions.assertTrue(isUserUnderLimit(setup, subwikiName + ":XWiki.User_3", -1));

        Assertions.assertEquals(2, this.getUserCountOnInstance(setup));

        deleteUsers(setup, "xwiki", 1, 3);
        deleteUsers(setup, subwikiName, 3, 4);
    }

    /**
     * Check that no unexpected behavior happens if the number of users is lower than the requested limit.
     */
    @Test
    void lowerThanRequestedMainWiki(TestUtils setup) throws Exception
    {
        createUsers(setup, "xwiki", 1, 3, true);

        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_1", 5));
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_2", 5));

        Assertions.assertEquals(2, this.getUserCountOnInstance(setup));

        deleteUsers(setup, "xwiki", 1, 3);
    }

    /**
     * Check that no unexpected behavior happens if the number of users is higher than the requested limit.
     */
    @Test
    void moreThanRequestedMainWiki(TestUtils setup) throws Exception
    {
        createUsers(setup, "xwiki", 1, 5, true);

        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_1", 2));
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_2", 2));
        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.User_3", 2));
        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.User_4", 2));

        Assertions.assertEquals(4, this.getUserCountOnInstance(setup));

        deleteUsers(setup, "xwiki", 1, 5);
    }

    /**
     * Test that inactive users are disregarded when computing the user limit.
     */
    @Test
    void inactiveUsersMainWiki(TestUtils setup) throws Exception
    {
        createUsers(setup, "xwiki", 1, 2, false);
        createUsers(setup, "xwiki", 2, 4, true);

        // Inactive users don't count towards the limit.
        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.User_1", 1));
        // The rest of the users.
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_2", 1));
        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.User_3", 1));

        Assertions.assertEquals(2, this.getUserCountOnInstance(setup));

        deleteUsers(setup, "xwiki", 1, 4);
    }

    /**
     * Check that no unexpected behavior happens if the number of users on all wikis is lower than the requested limit.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false)
    void lowerThanRequestedSubwiki(WikiReference wikiReference, TestUtils setup) throws Exception
    {
        String subwikiName = wikiReference.getName();
        createUsers(setup, "xwiki", 1, 3, true);
        createUsers(setup, subwikiName, 3, 5, true);

        // Test user on main wiki
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_1", 5));
        // Test user on main wiki who would be under limit if not for the subwiki users.
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_2", 5));
        // Test user on subwiki
        Assertions.assertTrue(isUserUnderLimit(setup, subwikiName + ":XWiki.User_3", 5));
        // Test user on main wiki who would be under limit if not for the main wiki users.
        Assertions.assertTrue(isUserUnderLimit(setup, subwikiName + ":XWiki.User_4", 5));

        Assertions.assertEquals(4, this.getUserCountOnInstance(setup));

        deleteUsers(setup, "xwiki", 1, 3);
        deleteUsers(setup, subwikiName, 3, 5);
    }

    /**
     * Test that users from all subwikis are counted towards the user limit.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false)
    void subwikiLimitUsesAllWikis(WikiReference wikiReference, TestUtils setup) throws Exception
    {
        String subwikiName = wikiReference.getName();
        createUsers(setup, "xwiki", 1, 3, true);
        createUsers(setup, subwikiName, 3, 6, true);
        createUsers(setup, "xwiki", 6, 7, true);

        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_1", 4));
        Assertions.assertTrue(isUserUnderLimit(setup, "xwiki:XWiki.User_2", 4));
        Assertions.assertTrue(isUserUnderLimit(setup, subwikiName + ":XWiki.User_3", 4));
        Assertions.assertTrue(isUserUnderLimit(setup, subwikiName + ":XWiki.User_4", 4));
        Assertions.assertFalse(isUserUnderLimit(setup, subwikiName + ":XWiki.User_5", 4));
        Assertions.assertFalse(isUserUnderLimit(setup, "xwiki:XWiki.User_6", 4));

        Assertions.assertEquals(6, this.getUserCountOnInstance(setup));

        deleteUsers(setup, "xwiki", 1, 3);
        deleteUsers(setup, subwikiName, 3, 6);
        deleteUsers(setup, "xwiki", 6, 7);
    }

    private int getUserCountOnInstance(TestUtils setup) throws Exception
    {
        return gotoUserCounterTestPage(setup, null, null).getUserCount();
    }

    /**
     * Get the result of a isUserUnderLimit() call, by evaluating it in a velocity script.
     */
    private boolean isUserUnderLimit(TestUtils setup, String user, int limit) throws Exception
    {
        return gotoUserCounterTestPage(setup, user, limit).isUserUnderLimit();
    }

    /**
     * Create user pages like XWiki.User_{startInclusive}, ... XWiki.User_{endExclusive-1}.
     */
    private void createUsers(TestUtils setup, String subwiki, int startInclusive, int endExclusive, boolean active)
        throws Exception
    {
        Assertions.assertTrue(startInclusive <= endExclusive);
        // Make sure we're on the right subwiki.
        String currentWiki = setup.getCurrentWiki();
        setup.setCurrentWiki(subwiki);

        setup.loginAsSuperAdmin();

        for (int i = startInclusive; i < endExclusive; i++) {
            DocumentReference documentReference = new DocumentReference(subwiki, "XWiki", "User_" + i);
            Assertions.assertFalse(setup.rest().exists(documentReference));
            setup.addObject(documentReference, "XWiki.XWikiUsers", "password", PASSWORD, "active", active);

            try {
                setup.getDriver().waitUntilCondition(driver -> false, 1);
            } catch (Exception e) {
                // Ignored. We want this to fail after 1 second, to ensure that the users are created in the expected
                // order (i.e. no equal creation dates for 2 users in the tests, since users are ordered by date).
            }
            System.out.println("Creating " + documentReference + " at " + new Date());
        }

        setup.setCurrentWiki(currentWiki);
    }

    /**
     * Delete user pages like XWiki.User_{startInclusive}, ... XWiki.User_{endExclusive-1}.
     */
    private void deleteUsers(TestUtils setup, String subwiki, int startInclusive, int endExclusive) throws Exception
    {
        Assertions.assertTrue(startInclusive < endExclusive);
        // Make sure we're on the right subwiki.
        String currentWiki = setup.getCurrentWiki();
        setup.setCurrentWiki(subwiki);

        setup.loginAsSuperAdmin();

        for (int i = startInclusive; i < endExclusive; i++) {
            DocumentReference documentReference = new DocumentReference(subwiki, "XWiki", "User_" + i);
            Assertions.assertTrue(setup.rest().exists(documentReference));
            setup.deletePage(documentReference);
            System.out.println("Deleting " + documentReference + " at " + new Date());
        }

        setup.setCurrentWiki(currentWiki);
    }

    private UserCounterPage gotoUserCounterTestPage(TestUtils setup, String user, Integer limit) throws Exception
    {
        // Create the page if it doesn't exist.
        try (InputStream content = UserCounterPage.class.getResourceAsStream("/UserCounterTestPage.wiki")) {
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
        return new UserCounterPage();
    }

    private void deleteUserCounterTestPage(TestUtils setup)
    {
        setup.deletePage(testPage);
    }
}
