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
package com.xwiki.licensing.manager;

import org.xwiki.stability.Unstable;

/**
 * A License Issuing API request that can't be served. It carries the HTTP status and the error code returned to the
 * client.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
public class IssuingException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final int status;

    private final String code;

    private final Long retryAfter;

    /**
     * @param status the HTTP status returned to the client
     * @param code the error code returned to the client (e.g. {@code productNotAllowed})
     * @param message the error message returned to the client
     */
    public IssuingException(int status, String code, String message)
    {
        this(status, code, message, null, null);
    }

    /**
     * @param status the HTTP status returned to the client
     * @param code the error code returned to the client
     * @param message the error message returned to the client
     * @param cause the cause, which is logged but not returned to the client
     */
    public IssuingException(int status, String code, String message, Throwable cause)
    {
        this(status, code, message, null, cause);
    }

    /**
     * @param status the HTTP status returned to the client
     * @param code the error code returned to the client
     * @param message the error message returned to the client
     * @param retryAfter the number of seconds after which the client may try again, or {@code null}
     * @param cause the cause, or {@code null}
     */
    public IssuingException(int status, String code, String message, Long retryAfter, Throwable cause)
    {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.retryAfter = retryAfter;
    }

    /**
     * @return the HTTP status returned to the client
     */
    public int getStatus()
    {
        return this.status;
    }

    /**
     * @return the error code returned to the client
     */
    public String getCode()
    {
        return this.code;
    }

    /**
     * @return the number of seconds after which the client may try again (sent as the {@code Retry-After} header), or
     *     {@code null}
     */
    public Long getRetryAfter()
    {
        return this.retryAfter;
    }
}
