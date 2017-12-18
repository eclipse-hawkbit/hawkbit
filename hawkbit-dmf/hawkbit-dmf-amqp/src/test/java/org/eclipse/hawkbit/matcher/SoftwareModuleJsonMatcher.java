/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.matcher;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;

/**
 * Set matcher for {@link SoftwareModule} and a list of
 * {@link org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule}.
 */
public final class SoftwareModuleJsonMatcher {

    /**
     * Creates a matcher that matches when the list of repository software
     * modules arelogically equal to the specified JSON software modules.
     * <p>
     * If the specified repository software modules are <code>null</code> then
     * the created matcher will only match if the JSON software modules are
     * <code>null</code>
     * <p>
     * For example:
     * 
     * <pre>
     * List<SoftwareModule> modules;
     * List<org.eclipse.hawkbit.dmf.json.model.SoftwareModule> expectedModules;
     * 
     * assertThat(modules, containsExactly(expectedModules));
     * </pre>
     * 
     * @param expectedModules
     *            the json sofware modules.
     */
    @Factory
    public static SoftwareModulesMatcher containsExactly(final List<DmfSoftwareModule> expectedModules) {
        return new SoftwareModulesMatcher(expectedModules);
    }

    private static class SoftwareModulesMatcher extends BaseMatcher<Set<SoftwareModule>> {

        private final List<DmfSoftwareModule> expectedModules;

        public SoftwareModulesMatcher(final List<DmfSoftwareModule> expectedModules) {
            this.expectedModules = expectedModules;
        }

        static boolean containsExactly(final Object actual, final List<DmfSoftwareModule> expected) {
            if (actual == null) {
                return expected == null;
            }

            @SuppressWarnings("unchecked")
            final Collection<SoftwareModule> modules = (Collection<SoftwareModule>) actual;

            if (modules.size() != expected.size()) {
                return false;
            }

            for (final SoftwareModule repoSoftwareModule : modules) {
                boolean containsElement = false;

                for (final DmfSoftwareModule jsonSoftwareModule : expected) {
                    if (!jsonSoftwareModule.getModuleId().equals(repoSoftwareModule.getId())) {
                        continue;
                    }
                    containsElement = true;

                    if (!jsonSoftwareModule.getModuleType().equals(repoSoftwareModule.getType().getKey())) {
                        return false;
                    }
                    if (!jsonSoftwareModule.getModuleVersion().equals(repoSoftwareModule.getVersion())) {
                        return false;
                    }
                    if (jsonSoftwareModule.getArtifacts().size() != repoSoftwareModule.getArtifacts().size()) {
                        return false;
                    }
                }

                if (!containsElement) {
                    return false;
                }

            }

            return true;
        }

        @Override
        public boolean matches(final Object actualValue) {
            return containsExactly(actualValue, expectedModules);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(expectedModules);
        }
    }

}
