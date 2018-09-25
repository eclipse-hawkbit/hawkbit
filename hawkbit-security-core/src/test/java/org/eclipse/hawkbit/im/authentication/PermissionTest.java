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
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test {@link SpPermission}.
 */
@Features("Unit Tests - Security")
@Stories("Permission Test")
public final class PermissionTest {

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
}
