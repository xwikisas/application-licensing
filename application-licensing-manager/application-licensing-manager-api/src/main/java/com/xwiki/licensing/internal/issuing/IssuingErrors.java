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

import com.xwiki.licensing.manager.IssuingException;

/**
 * The error codes of the License Issuing API (spec section 4.5) and helpers to create the matching exceptions.
 *
 * @version $Id$
 * @since 1.34
 */
public final class IssuingErrors
{
    /**
     * Malformed input.
     */
    public static final String INVALID_REQUEST = "invalidRequest";

    /**
     * The requested product isn't configured.
     */
    public static final String UNKNOWN_PRODUCT = "unknownProduct";

    /**
     * The {@code customer} licensee mode needs the customer details.
     */
    public static final String LICENSEE_REQUIRED = "licenseeRequired";

    /**
     * The calling account has no policy.
     */
    public static final String NO_POLICY = "noPolicy";

    /**
     * The policy of the calling account is disabled.
     */
    public static final String POLICY_DISABLED = "policyDisabled";

    /**
     * The policy doesn't allow the environment.
     */
    public static final String ENVIRONMENT_NOT_ALLOWED = "environmentNotAllowed";

    /**
     * The policy doesn't allow the product.
     */
    public static final String PRODUCT_NOT_ALLOWED = "productNotAllowed";

    /**
     * The policy doesn't allow the duration.
     */
    public static final String DURATION_NOT_ALLOWED = "durationNotAllowed";

    /**
     * The policy doesn't allow the support level.
     */
    public static final String SUPPORT_NOT_ALLOWED = "supportNotAllowed";

    /**
     * The policy doesn't allow the licensee mode, or the request sends a licensee the mode doesn't use.
     */
    public static final String LICENSEE_NOT_ALLOWED = "licenseeNotAllowed";

    /**
     * A bound of the policy is exceeded (user count, batch size, daily limit, production expiration limit).
     */
    public static final String LIMIT_EXCEEDED = "limitExceeded";

    /**
     * The account may not choose the id of a new instance.
     */
    public static final String INSTANCE_ID_NOT_ALLOWED = "instanceIdNotAllowed";

    /**
     * The instance is already used for the other environment.
     */
    public static final String ENVIRONMENT_CONFLICT = "environmentConflict";

    /**
     * The instance is licensed for another product.
     */
    public static final String PRODUCT_CONFLICT = "productConflict";

    /**
     * The instance is owned by another account.
     */
    public static final String INSTANCE_ID_TAKEN = "instanceIdTaken";

    /**
     * The instance has licenses from before the API and hasn't been imported.
     */
    public static final String INSTANCE_NOT_IMPORTED = "instanceNotImported";

    /**
     * The license doesn't exist or doesn't belong to one of the account's instances.
     */
    public static final String NOT_FOUND = "notFound";

    /**
     * The daily issuance limit is reached.
     */
    public static final String RATE_LIMITED = "rateLimited";

    /**
     * The request isn't authenticated.
     */
    public static final String UNAUTHENTICATED = "unauthenticated";

    /**
     * A signing, storage or configuration failure.
     */
    public static final String INTERNAL_ERROR = "internalError";

    private static final int BAD_REQUEST = 400;

    private static final int FORBIDDEN = 403;

    private static final int NOT_FOUND_STATUS = 404;

    private static final int CONFLICT = 409;

    private static final int TOO_MANY_REQUESTS = 429;

    private static final int SERVER_ERROR = 500;

    private IssuingErrors()
    {
    }

    /**
     * @param code the error code
     * @param message the error message
     * @return a 400 error
     */
    public static IssuingException badRequest(String code, String message)
    {
        return new IssuingException(BAD_REQUEST, code, message);
    }

    /**
     * @param code the error code
     * @param message the error message
     * @return a 403 error
     */
    public static IssuingException forbidden(String code, String message)
    {
        return new IssuingException(FORBIDDEN, code, message);
    }

    /**
     * @param message the error message
     * @return a 404 error
     */
    public static IssuingException notFound(String message)
    {
        return new IssuingException(NOT_FOUND_STATUS, NOT_FOUND, message);
    }

    /**
     * @param code the error code
     * @param message the error message
     * @return a 409 error
     */
    public static IssuingException conflict(String code, String message)
    {
        return new IssuingException(CONFLICT, code, message);
    }

    /**
     * @param message the error message
     * @param retryAfter the number of seconds after which the client may try again
     * @return a 429 error
     */
    public static IssuingException rateLimited(String message, long retryAfter)
    {
        return new IssuingException(TOO_MANY_REQUESTS, RATE_LIMITED, message, retryAfter, null);
    }

    /**
     * @param message the error message
     * @param cause the cause, or {@code null}
     * @return a 500 error
     */
    public static IssuingException internal(String message, Throwable cause)
    {
        return new IssuingException(SERVER_ERROR, INTERNAL_ERROR, message, cause);
    }
}
