package com.xwiki.licensing.internal;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.avalon.framework.activity.Initializable;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xwiki.licensing.License;
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
    private static final String GET_USERS_QUERY = "from doc.object(XWiki.XWikiUsers) as user";

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    private long cachedUserCount;

    @Override
    public void initialize() throws Exception
    {
        cachedUserCount =
            (long) queryManager.createQuery(GET_USERS_QUERY, Query.XWQL).addFilter(countFilter).execute().get(0);
    }

    @Override
    public boolean isValid(License license)
    {
        return license instanceof SignedLicense
            && license.getExpirationDate() >= new Date().getTime()
            && license.getMaxUserCount() >= cachedUserCount;
    }
}
