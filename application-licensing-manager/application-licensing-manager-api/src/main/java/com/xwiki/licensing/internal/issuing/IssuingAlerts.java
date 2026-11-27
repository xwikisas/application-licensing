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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.SessionFactory;
import org.xwiki.mail.VoidMailListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

/**
 * Emails the alert recipients of a policy when the account reaches a limit (spec 5.3). Each alert is sent at most
 * once a day per account; the deduplication is kept in memory, so a restart may send it again.
 *
 * @version $Id$
 * @since 1.34
 */
@Component(roles = IssuingAlerts.class)
@Singleton
public class IssuingAlerts
{
    /**
     * The group emailed when the policy has no alert recipients.
     */
    static final String DEFAULT_RECIPIENTS = "XWikiAdminGroup";

    private static final String CHARSET = StandardCharsets.UTF_8.name();

    private final Map<DocumentReference, LocalDate> rateAlerts = new ConcurrentHashMap<>();

    @Inject
    private Logger logger;

    @Inject
    private MailSender mailSender;

    @Inject
    private MailSenderConfiguration mailConfiguration;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    private GroupManager groupManager;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    private ApiConfiguration configuration;

    /**
     * Tells the alert recipients that the account reached its daily issuance limit. Never fails: a mail error is only
     * logged.
     *
     * @param policy the policy of the account
     */
    public void rateLimitReached(Policy policy)
    {
        LocalDate today = LocalDate.now();
        if (today.equals(this.rateAlerts.put(policy.getAccount(), today))) {
            return;
        }
        String account = policy.getAccount().toString();
        send(policy, String.format("License Issuing API: daily limit reached by [%s]", account), String.format(
            "The account [%s] reached its limit of %d licenses issued in 24 hours, so its License Issuing API "
                + "requests are refused until older licenses leave the 24 hours window.%n%n"
                + "If this isn't expected (a runaway pipeline or a leaked password), disable the account's policy.",
            account, policy.getMaxIssuancesPerDay()));
    }

    private void send(Policy policy, String subject, String text)
    {
        try {
            Set<Address> recipients = getRecipients(policy);
            if (recipients.isEmpty()) {
                this.logger.warn("No recipient with an email for the alert [{}].", subject);
                return;
            }
            Session session = this.sessionFactory.create(Collections.emptyMap());
            MimeMessage message = new MimeMessage(session);
            if (StringUtils.isNotBlank(this.mailConfiguration.getFromAddress())) {
                message.setFrom(new InternetAddress(this.mailConfiguration.getFromAddress()));
            }
            message.setRecipients(Message.RecipientType.TO, recipients.toArray(new Address[0]));
            message.setSubject(subject, CHARSET);
            message.setText(text, CHARSET);
            this.mailSender.sendAsynchronously(List.of(message), session, new VoidMailListener());
        } catch (MessagingException | GroupException e) {
            this.logger.warn("Failed to send the alert [{}].", subject, e);
        }
    }

    private Set<Address> getRecipients(Policy policy) throws GroupException
    {
        List<DocumentReference> groups = policy.getAlertRecipients();
        if (groups.isEmpty()) {
            groups = List.of(new DocumentReference(this.configuration.getMainWiki().getName(), "XWiki",
                DEFAULT_RECIPIENTS));
        }
        Set<Address> recipients = new LinkedHashSet<>();
        for (DocumentReference group : groups) {
            for (DocumentReference member : this.groupManager.getMembers(group, true)) {
                InternetAddress email =
                    this.userPropertiesResolver.resolve(this.userReferenceResolver.resolve(member)).getEmail();
                if (email != null) {
                    recipients.add(email);
                }
            }
        }
        return recipients;
    }
}
