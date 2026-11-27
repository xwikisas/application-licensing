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

import java.util.List;
import java.util.Map;

/**
 * A named set of features licensed together, as configured on {@code License.Code.API.Products.<id>}.
 *
 * @version $Id$
 * @since 1.34
 */
public class Product
{
    private final String id;

    private final List<String> featureIds;

    private final Map<String, String> extensionVersions;

    private final List<String> renewedFeatureIds;

    /**
     * @param id the product id
     * @param featureIds the licensed features
     * @param extensionVersions the versions to use for some features, by feature id
     * @param renewedFeatureIds the features licensed again on a renewal, empty meaning all of them
     */
    public Product(String id, List<String> featureIds, Map<String, String> extensionVersions,
        List<String> renewedFeatureIds)
    {
        this.id = id;
        this.featureIds = List.copyOf(featureIds);
        this.extensionVersions = Map.copyOf(extensionVersions);
        this.renewedFeatureIds = List.copyOf(renewedFeatureIds);
    }

    /**
     * @return the product id
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the licensed features, one license being issued per feature
     */
    public List<String> getFeatureIds()
    {
        return this.featureIds;
    }

    /**
     * @param featureId a feature of the product
     * @return the version to use to compute the licensed dependencies of the feature, or {@code null} to use the
     *     latest stable version
     */
    public String getExtensionVersion(String featureId)
    {
        return this.extensionVersions.get(featureId);
    }

    /**
     * @return the features licensed again on a renewal (all the features when none is configured)
     */
    public List<String> getRenewedFeatureIds()
    {
        return this.renewedFeatureIds.isEmpty() ? this.featureIds : this.renewedFeatureIds;
    }
}
