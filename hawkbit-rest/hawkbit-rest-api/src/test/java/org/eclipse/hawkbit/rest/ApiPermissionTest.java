/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.auth.SpPermission;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - REST API<br/>
 * Story: Required permissions
 */
class ApiPermissionTest {

    /**
     * Verifies that {@link ApiPermission} mirrors exactly the {@link SpPermission} authorities, so the documented
     * required permissions are always existing ones.
     */
    @Test
    void apiPermissionsMatchSpPermissions() {
        final Map<String, String> apiPermissions = Arrays.stream(ApiPermission.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
                .collect(Collectors.toMap(Field::getName, ApiPermissionTest::value));

        // constant name is the same as its value - as for SpPermission
        assertThat(apiPermissions).allSatisfy((name, value) -> assertThat(value).isEqualTo(name));
        assertThat(apiPermissions.values()).containsExactlyInAnyOrderElementsOf(SpPermission.getAllAuthorities());
    }

    private static String value(final Field field) {
        try {
            return (String) field.get(null);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
