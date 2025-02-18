/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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
import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

/**
 * Test {@link SpPermission}.
 */
@Feature("Unit Tests - Security")
@Story("Permission Test")
final class SpPermissionTest {

    @Test
    @Description("Try to double check if all permissions works as expected")
    void shouldReturnAllPermissions() {
        List<String> expected = new LinkedList<>();
        ReflectionUtils.doWithFields(SpPermission.class, f -> {
            if (ReflectionUtils.isPublicStaticFinal(f) && String.class.equals(f.getType())) {
                try {
                    expected.add((String) f.get(null));
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    // skip
                }
            }
        });
        final Collection<String> allAuthorities = SpPermission.getAllAuthorities();
        assertThat(allAuthorities)
                .hasSize(20)
                .containsAll(expected);
    }
}