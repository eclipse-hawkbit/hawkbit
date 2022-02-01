/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.ReflectionUtils;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test {@link SpPermission}.
 */
@Feature("Unit Tests - Security")
@Story("Permission Test")
public final class SpPermissionTest {

	@Test
	@Description("Verify the get permission function")
	public void testGetPermissions() {
		final int allPermission = 18;
		final Collection<String> allAuthorities = SpPermission.getAllAuthorities();
		final List<GrantedAuthority> allAuthoritiesList = PermissionUtils.createAllAuthorityList();
		assertThat(allAuthorities).hasSize(allPermission);
		assertThat(allAuthoritiesList).hasSize(allPermission);
		assertThat(allAuthoritiesList.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsAll(allAuthorities);
	}

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
        assertThat(SpPermission.getAllAuthorities()).containsAll(expected);
    }
}
