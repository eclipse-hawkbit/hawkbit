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
    public static SoftwareModulesMatcher containsExactly(final List<DmfSoftwareModule> expectedModules) {
        return new SoftwareModulesMatcher(expectedModules);
    }

    private static class SoftwareModulesMatcher extends BaseMatcher<Set<SoftwareModule>> {

        private final List<DmfSoftwareModule> expectedModules;

        public SoftwareModulesMatcher(final List<DmfSoftwareModule> expectedModules) {
            this.expectedModules = expectedModules;
        }

        boolean containsExactly(final Object actual) {
            if (actual == null) {
                return expectedModules == null;
            }

            @SuppressWarnings("unchecked")
            final Collection<SoftwareModule> modules = (Collection<SoftwareModule>) actual;

            return expectedModules.stream().allMatch(e -> existsIn(e, modules));
        }

        private static boolean existsIn(final DmfSoftwareModule module, final Collection<SoftwareModule> actual) {
            return actual.stream()
                    .anyMatch(e -> module.getModuleType().equals(e.getType().getKey())
                            && module.getModuleVersion().equals(e.getVersion())
                            && module.getArtifacts().size() == e.getArtifacts().size());
        }

        @Override
        public boolean matches(final Object actualValue) {
            return containsExactly(actualValue);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendValue(expectedModules);
        }
    }

}
