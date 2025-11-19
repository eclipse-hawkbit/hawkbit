/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.im.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.eclipse.hawkbit.auth.SpPermission;
import org.junit.jupiter.api.Test;

/**
 * Test {@link SpPermission}.
 * <p/>
 * Feature: Unit Tests - Security<br/>
 * Story: Permission Test
 */
final class SpPermissionTest {

    /**
     * Double-checks that all permissions doesn't contain any hierarchies.
     */
    @Test
    void allAuthoritiesShouldNotContainHierarchies() {
        final Collection<String> allAuthorities = SpPermission.getAllAuthorities();
        assertThat(allAuthorities).isNotEmpty().as("Are not hierarchies").allMatch(permission -> !permission.contains("\n"));
    }
}