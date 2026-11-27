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
package com.xwiki.licensing.manager.rest.model;

import org.xwiki.stability.Unstable;

/**
 * The licensee written into the licenses when the request uses the {@code customer} licensee mode.
 *
 * @version $Id$
 * @since 1.34
 */
@Unstable
public class LicenseeDetails
{
    private String firstName;

    private String lastName;

    private String email;

    /**
     * @return the first name
     */
    public String getFirstName()
    {
        return this.firstName;
    }

    /**
     * @param firstName the first name
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * @return the last name
     */
    public String getLastName()
    {
        return this.lastName;
    }

    /**
     * @param lastName the last name
     */
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    /**
     * @return the email
     */
    public String getEmail()
    {
        return this.email;
    }

    /**
     * @param email the email
     */
    public void setEmail(String email)
    {
        this.email = email;
    }
}
