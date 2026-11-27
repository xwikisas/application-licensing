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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.licensing.internal.issuing.LicensedDependencyResolver.LicensedDependencies;

import static com.xwiki.licensing.internal.issuing.LicensedDependencyResolver.LICENSOR_ID;
import static com.xwiki.licensing.internal.issuing.LicensedDependencyResolver.PACKAGE_ID_PROPERTY;
import static com.xwiki.licensing.internal.issuing.LicensedDependencyResolver.PACKAGE_VERSION_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LicensedDependencyResolver}. Each rule comes from the {@code #getLicensedExtension},
 * {@code #setExtraLicenseDetails} and {@code #getDependentFeatureIds} macros.
 *
 * @version $Id$
 */
@ComponentTest
class LicensedDependencyResolverTest
{
    private static final String NAMESPACE = "wiki:xwiki";

    private static final String FEATURE = "com.xwiki.pro:pro-ui";

    @InjectMockComponents
    private LicensedDependencyResolver resolver;

    @MockComponent
    private ExtensionManager extensionManager;

    @MockComponent
    private ExtensionRepositoryManager repositoryManager;

    @MockComponent
    private ApiConfiguration configuration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    /**
     * The extensions known to the store, by id (the walk resolves dependencies by id, whatever their version).
     */
    private final Map<String, Extension> extensions = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.configuration.getMainWiki()).thenReturn(new WikiReference("xwiki"));
        when(this.extensionManager.resolveExtension(any(ExtensionDependency.class), eq(NAMESPACE)))
            .thenAnswer(invocation -> find(invocation.<ExtensionDependency>getArgument(0).getId()));
        when(this.extensionManager.resolveExtension(any(ExtensionId.class)))
            .thenAnswer(invocation -> find(invocation.<ExtensionId>getArgument(0).getId()));
    }

    @Test
    void collectsTheTransitivePaidModulesOfTheLatestStableVersion() throws Exception
    {
        when(this.repositoryManager.resolveVersions(FEATURE, 0, -1)).thenReturn(versions("1.0", "2.0", "3.0-rc-1"));
        // The feature itself depends on the licensor, but it is licensed anyway so it isn't a dependent feature.
        addExtension(FEATURE, "2.0", dependency(LICENSOR_ID), dependency("com.xwiki.pro:pro-api"),
            dependency("com.xwiki.date:date-ui"), dependency("org.xwiki.platform:not-walked"),
            optional("com.xwiki.optional:optional-api"), dependency("com.xwiki.weak:weak-api"),
            dependency("com.xwiki.unknown:unknown"));
        addExtension("com.xwiki.pro:pro-api", "2.0", dependency(LICENSOR_ID));
        addExtension("com.xwiki.date:date-ui", "1.0", dependency("com.xwiki.date:date-api"));
        addExtension("com.xwiki.date:date-api", "1.0", dependency(LICENSOR_ID));
        addExtension("org.xwiki.platform:not-walked", "1.0", dependency(LICENSOR_ID));
        addExtension("com.xwiki.optional:optional-api", "1.0", dependency(LICENSOR_ID));
        addExtension("com.xwiki.weak:weak-api", "1.0", optional(LICENSOR_ID));

        LicensedDependencies result = this.resolver.resolve(FEATURE, null).get();

        assertEquals("2.0", result.getExtensionVersion());
        assertEquals(Set.of("com.xwiki.pro:pro-api", "com.xwiki.date:date-api"), result.getDependentFeatureIds());
    }

    @Test
    void addsThePaidModulesOfTheLicensingPackage() throws Exception
    {
        Extension feature = addExtension(FEATURE, "1.5", dependency("com.xwiki.pro:pro-api"));
        when(feature.getProperty(PACKAGE_ID_PROPERTY)).thenReturn("com.xwiki.pro:pro-bundle");
        when(feature.getProperty(PACKAGE_VERSION_PROPERTY)).thenReturn("1.7");
        addExtension("com.xwiki.pro:pro-bundle", "1.7", dependency(FEATURE), dependency("com.xwiki.app:app-ui"));
        addExtension("com.xwiki.pro:pro-api", "1.5", dependency(LICENSOR_ID));
        addExtension("com.xwiki.app:app-ui", "1.0", dependency(LICENSOR_ID));

        LicensedDependencies result = this.resolver.resolve(FEATURE, "1.5").get();

        assertEquals("1.5", result.getExtensionVersion());
        assertEquals(Set.of("com.xwiki.pro:pro-api", "com.xwiki.app:app-ui"), result.getDependentFeatureIds());
        verify(this.extensionManager).resolveExtension(new ExtensionId("com.xwiki.pro:pro-bundle", "1.7"));
    }

    @Test
    void theLicensingPackageDefaultsToTheVersionOfTheFeature() throws Exception
    {
        Extension feature = addExtension(FEATURE, "1.5");
        when(feature.getProperty(PACKAGE_ID_PROPERTY)).thenReturn("com.xwiki.pro:pro-bundle");

        assertTrue(this.resolver.resolve(FEATURE, "1.5").get().getDependentFeatureIds().isEmpty());

        // The package can't be resolved, which the macro ignores as well.
        verify(this.extensionManager).resolveExtension(new ExtensionId("com.xwiki.pro:pro-bundle", "1.5"));
        assertEquals("Failed to resolve [com.xwiki.pro:pro-bundle/1.5]: Unknown extension com.xwiki.pro:pro-bundle",
            this.logCapture.getMessage(0));
    }

    @Test
    void stopsAfterTheMaximumNumberOfSteps() throws Exception
    {
        // A dependency cycle would loop forever without the cap.
        addExtension(FEATURE, "1.0", dependency(FEATURE));

        assertTrue(this.resolver.resolve(FEATURE, "1.0").get().getDependentFeatureIds().isEmpty());

        verify(this.extensionManager, times(LicensedDependencyResolver.MAX_STEPS))
            .resolveExtension(any(ExtensionDependency.class), eq(NAMESPACE));
    }

    @Test
    void emptyWhenTheFeatureCantBeResolved() throws Exception
    {
        when(this.repositoryManager.resolveVersions(FEATURE, 0, -1)).thenReturn(versions("1.0-SNAPSHOT"));
        when(this.repositoryManager.resolveVersions("com.xwiki:unknown", 0, -1))
            .thenThrow(new ResolveException("unknown"));

        // No stable version.
        assertEquals(Optional.empty(), this.resolver.resolve(FEATURE, ""));
        // No version at all.
        assertEquals(Optional.empty(), this.resolver.resolve("com.xwiki:unknown", null));
        // Unknown version.
        assertEquals(Optional.empty(), this.resolver.resolve(FEATURE, "9.9"));

        assertEquals("Failed to resolve the versions of [com.xwiki:unknown]: unknown", this.logCapture.getMessage(0));
        assertEquals("Failed to resolve [com.xwiki.pro:pro-ui/9.9]: Unknown extension com.xwiki.pro:pro-ui",
            this.logCapture.getMessage(1));
    }

    private Extension find(String id) throws ResolveException
    {
        Extension extension = this.extensions.get(id);
        if (extension == null) {
            throw new ResolveException("Unknown extension " + id);
        }
        return extension;
    }

    private Extension addExtension(String id, String version, ExtensionDependency... dependencies)
    {
        Extension extension = mock(Extension.class, id);
        when(extension.getId()).thenReturn(new ExtensionId(id, version));
        when(extension.getDependencies()).thenReturn(List.of(dependencies));
        this.extensions.put(id, extension);
        return extension;
    }

    private static ExtensionDependency dependency(String id)
    {
        return new DefaultExtensionDependency(id, new DefaultVersionConstraint("1.0"));
    }

    private static ExtensionDependency optional(String id)
    {
        return new DefaultExtensionDependency(id, new DefaultVersionConstraint("1.0"), true);
    }

    private static CollectionIterableResult<Version> versions(String... values)
    {
        List<Version> versions = new ArrayList<>();
        for (String value : values) {
            versions.add(new DefaultVersion(value));
        }
        return new CollectionIterableResult<>(versions.size(), 0, versions);
    }
}
