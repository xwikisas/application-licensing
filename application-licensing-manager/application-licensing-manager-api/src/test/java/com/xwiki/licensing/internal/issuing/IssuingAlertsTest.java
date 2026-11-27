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
package com.xwiki.licensing.internal.issuing;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Named;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IssuingAlerts}.
 *
 * @version $Id$
 */
@ComponentTest
class IssuingAlertsTest
{
    private static final DocumentReference ACCOUNT = new DocumentReference("xwiki", "XWiki", "partner");

    private static final DocumentReference SALES = new DocumentReference("xwiki", "XWiki", "SalesGroup");

    private static final DocumentReference ADMINS = new DocumentReference("xwiki", "XWiki", "XWikiAdminGroup");

    private static final DocumentReference ALICE = new DocumentReference("xwiki", "XWiki", "alice");

    private static final DocumentReference BOB = new DocumentReference("xwiki", "XWiki", "bob");

    @InjectMockComponents
    private IssuingAlerts alerts;

    @MockComponent
    private MailSender mailSender;

    @MockComponent
    private MailSenderConfiguration mailConfiguration;

    @MockComponent
    private SessionFactory sessionFactory;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private ApiConfiguration configuration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final Session session = Session.getInstance(new Properties());

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.configuration.getMainWiki()).thenReturn(new WikiReference("xwiki"));
        when(this.sessionFactory.create(Collections.emptyMap())).thenReturn(this.session);
        when(this.mailConfiguration.getFromAddress()).thenReturn("noreply@example.com");
        mockUser(ALICE, "alice@example.com");
        mockUser(BOB, null);
    }

    @Test
    void rateLimitReachedOncePerDay() throws Exception
    {
        when(this.groupManager.getMembers(SALES, true)).thenReturn(List.of(ALICE, BOB));
        Policy policy = Policy.builder(ACCOUNT).maxIssuancesPerDay(25).alertRecipients(List.of(SALES)).build();

        this.alerts.rateLimitReached(policy);
        this.alerts.rateLimitReached(policy);

        verify(this.mailSender, times(1)).sendAsynchronously(argThat(messages -> {
            MimeMessage message = (MimeMessage) messages.iterator().next();
            try {
                // Bob has no email.
                assertArrayEquals(new Address[] { new InternetAddress("alice@example.com") },
                    message.getRecipients(Message.RecipientType.TO));
                assertArrayEquals(new Address[] { new InternetAddress("noreply@example.com") }, message.getFrom());
                assertEquals("License Issuing API: daily limit reached by [xwiki:XWiki.partner]",
                    message.getSubject());
                assertTrue(((String) message.getContent()).contains("limit of 25 licenses issued in 24 hours"));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            return true;
        }), eq(this.session), any(MailListener.class));
    }

    @Test
    void rateLimitReachedWithoutRecipientsEmailsTheAdmins() throws Exception
    {
        when(this.mailConfiguration.getFromAddress()).thenReturn(null);
        when(this.groupManager.getMembers(ADMINS, true)).thenReturn(List.of(ALICE));

        this.alerts.rateLimitReached(Policy.builder(ACCOUNT).build());

        verify(this.mailSender).sendAsynchronously(argThat(messages -> {
            try {
                MimeMessage message = (MimeMessage) messages.iterator().next();
                assertNull(message.getHeader("From"));
                assertEquals("alice@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            return true;
        }), eq(this.session), any(MailListener.class));
    }

    @Test
    void rateLimitReachedWithoutEmail() throws Exception
    {
        when(this.groupManager.getMembers(SALES, true)).thenReturn(List.of(BOB));

        this.alerts.rateLimitReached(Policy.builder(ACCOUNT).alertRecipients(List.of(SALES)).build());

        verify(this.mailSender, never()).sendAsynchronously(any(), any(), any());
        assertEquals("No recipient with an email for the alert "
            + "[License Issuing API: daily limit reached by [xwiki:XWiki.partner]].", this.logCapture.getMessage(0));
    }

    @Test
    void rateLimitReachedWithAGroupError() throws Exception
    {
        when(this.groupManager.getMembers(SALES, true)).thenThrow(new GroupException("failed"));

        this.alerts.rateLimitReached(Policy.builder(ACCOUNT).alertRecipients(List.of(SALES)).build());

        verify(this.mailSender, never()).sendAsynchronously(any(), any(), any());
        assertEquals("Failed to send the alert [License Issuing API: daily limit reached by [xwiki:XWiki.partner]].",
            this.logCapture.getMessage(0));
    }

    private void mockUser(DocumentReference user, String email) throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(user)).thenReturn(userReference);
        UserProperties properties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(properties);
        when(properties.getEmail()).thenReturn(email != null ? new InternetAddress(email) : null);
    }
}
