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
package com.xwiki.licensing.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicenseValidator;
import com.xwiki.licensing.SignedLicense;

/**
 * Default implementation of {@link LicenseValidator}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLicenseValidator implements LicenseValidator, Initializable
{
    private static final Map<LicenseType, List<String>> VALID_CERTIFICATES = new HashMap<LicenseType, List<String>>();
    static {
        VALID_CERTIFICATES.put(LicenseType.FREE,  Arrays.asList("eicpWbt5RNWbOa4uiDqK5aOpr0E="));
        VALID_CERTIFICATES.put(LicenseType.TRIAL, Arrays.asList("o6yt/slI4P6qUQF8dpC5yYaJDA4="));
        VALID_CERTIFICATES.put(LicenseType.PAID,  Arrays.asList("HW571yMdXhhx59oF96hKBNgh30M="));
    }

    private static final List<String> REVOKED_CERTIFICATES = Collections.emptyList();

    private static final String GET_USERS_QUERY = "from doc.object(XWiki.XWikiUsers) as user";

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    private InstanceIdManager instanceIdManager;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64Encoder;

    private long cachedUserCount;

    @Override
    public void initialize() throws InitializationException
    {
        instanceIdManager.initializeInstanceId();

        try {
            cachedUserCount =
                (long) queryManager.createQuery(GET_USERS_QUERY, Query.XWQL).addFilter(countFilter).execute().get(0);
        } catch (QueryException e) {
            // Lucky guy will not be restricted, ignoring since it is not major.
            cachedUserCount = 0;
        }
    }

    @Override
    public boolean isApplicable(License license)
    {
        return license.isApplicableTo(instanceIdManager.getInstanceId());
    }

    @Override
    public boolean isSigned(License license)
    {
        return license instanceof SignedLicense
            && checkCertificates(license, ((SignedLicense) license).getCertificates());
    }

    private boolean checkCertificates(License license, Collection<X509CertifiedPublicKey> certificates)
    {
        if (certificates.size() < 3) {
            return false;
        }

        Iterator<X509CertifiedPublicKey> iterator = certificates.iterator();
        iterator.next();
        try {
            if (!VALID_CERTIFICATES.get(license.getType())
                .contains(base64Encoder.encode(iterator.next().getSubjectKeyIdentifier()))) {
                return false;
            }
            while (iterator.hasNext()) {
                if (REVOKED_CERTIFICATES.contains(base64Encoder.encode(iterator.next().getSubjectKeyIdentifier()))) {
                    return false;
                }
            }
        } catch (IOException exception) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isValid(License license)
    {
        return license.getExpirationDate() >= new Date().getTime()
            && license.getMaxUserCount() >= cachedUserCount;
    }
}
