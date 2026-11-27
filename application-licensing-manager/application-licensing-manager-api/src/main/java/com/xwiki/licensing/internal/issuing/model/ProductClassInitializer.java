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
package com.xwiki.licensing.internal.issuing.model;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Declares the class of the products of the License Issuing API. A product is a named set of features licensed
 * together, stored on the page {@code License.Code.API.Products.<productId>}. The page name is the product id used in
 * API requests.
 *
 * @version $Id$
 * @since 1.34
 */
@Component
@Named(ProductClassInitializer.CLASS_NAME)
@Singleton
public class ProductClassInitializer extends AbstractApiClassInitializer
{
    /**
     * The serialized local reference of the class.
     */
    public static final String CLASS_NAME = "License.Code.API.ProductClass";

    /**
     * The local reference of the class.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(CODE_SPACE, "ProductClass");

    /**
     * The space holding the product pages.
     */
    public static final List<String> PRODUCTS_SPACE = List.of("License", "Code", "API", "Products");

    /**
     * The licensed features, one license being issued per feature.
     */
    public static final String FEATURE_IDS = "featureIds";

    /**
     * Optional versions of the features, one {@code featureId=version} per line.
     */
    public static final String EXTENSION_VERSIONS = "extensionVersions";

    /**
     * The features licensed again on a renewal, empty meaning all of them.
     */
    public static final String RENEWED_FEATURE_IDS = "renewedFeatureIds";

    /**
     * Default constructor.
     */
    public ProductClassInitializer()
    {
        super(CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        addTextListField(xclass, FEATURE_IDS, "Feature ids");
        xclass.addTextAreaField(EXTENSION_VERSIONS, "Extension versions (featureId=version, one per line)", 60, 5,
            TextAreaClass.ContentType.PURE_TEXT);
        addTextListField(xclass, RENEWED_FEATURE_IDS, "Feature ids renewed");
    }
}
