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

import java.util.List;
import java.util.stream.Collectors;

import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Utility method for creation of <tt>GrantedAuthority</tt> collections etc.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PermissionUtils {

	/**
	 * Returns all authorities.
	 * 
	 * @return a list of {@link GrantedAuthority}
	 */
	public static List<GrantedAuthority> createAllAuthorityList() {
		return SpPermission.getAllAuthorities().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}
}