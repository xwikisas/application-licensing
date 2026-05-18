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

import java.util.Map;
import java.util.stream.IntStream;

import javax.print.Doc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Licensing application.
 *
 * @version $Id$
 */
@UITest
class UserStressTestLicensingIT
{
    private static final String PASSWORD = "password";

    private static final int USERS_COUNT = 10_000;

    // The tested user limit MUST be lower than the amount of users on the instance, otherwise the method returns
    // TRUE without actually populating the user list.
    private static final String TEST_PAGE_SCRIPT =
        "{{velocity}}\n" + "$services.licensing.licensor.isCurrentUserUnderLimit($xwiki.getUser().getUser()"
            + ".getUserReference(), " + (USERS_COUNT - 100) + ")\n" + "{{/velocity}}";

    private static final DocumentReference testPage = new DocumentReference("xwiki", "Test", "Test");

    private void createUsers(TestUtils setup) {
        IntStream.range(2, USERS_COUNT).parallel().forEach(i -> {
            DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User_" + i);
            setup.addObject(documentReference, "XWiki.XWikiUsers", "password", PASSWORD);
        });
    }

    private void deleteUsers(TestUtils setup) {
        IntStream.range(2, USERS_COUNT).parallel().forEach(i -> {
            DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User_" + i);
            setup.deletePage(documentReference);
        });
    }

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
        setup.gotoPage(testPage, "view");
        assertTrue(setup.isInViewMode());
        // We didn't crash (probably).
    }
}
