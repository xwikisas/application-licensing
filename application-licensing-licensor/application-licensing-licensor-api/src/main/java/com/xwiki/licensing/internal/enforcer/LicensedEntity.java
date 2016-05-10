package com.xwiki.licensing.internal.enforcer;

import com.xwiki.licensing.License;

/**
 * Simple interface that allow getting the license of a licenced entity.
 *
 * @version $Id$
 */
public interface LicensedEntity
{
    License getLicense();
}
