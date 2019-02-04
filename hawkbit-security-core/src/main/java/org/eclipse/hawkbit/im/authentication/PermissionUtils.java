/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Utility method for creation of <tt>GrantedAuthority</tt> collections etc.
 */
public final class PermissionUtils {

	private PermissionUtils() {

	}

	/**
	 * Returns all authorities.
	 * 
	 * @return a list of {@link GrantedAuthority}
	 */
	public static List<GrantedAuthority> createAllAuthorityList() {
		return SpPermission.getAllAuthorities().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}
}
