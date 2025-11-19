/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.auth;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <p>
 * Contains all the spring security evaluation expressions for the {@link PreAuthorize} annotation for method security.
 * </p>
 * <p>
 * Examples:
 * {@code
 * hasRole([role])   Returns true if the current principal has the specified role.
 * hasAnyRole([role1,role2])  Returns true if the current principal has any of the supplied roles (given as a comma-separated list of strings)
 * principal   Allows direct access to the principal object representing the current user
 * auth Allows direct access to the current Authentication object obtained from the SecurityContext
 * permitAll   Always evaluates to true
 * denyAll  Always evaluates to false
 * isAnonymous()  Returns true if the current principal is an anonymous user
 * isRememberMe() Returns true if the current principal is a remember-me user
 * isAuthenticated() Returns true if the user is not anonymous
 * isFullyAuthenticated()  Returns true if the user is not an anonymous or a remember-me user
 * }
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpringEvalExpressions {

    public static final String IS_SYSTEM_CODE = "hasAuthority('ROLE_SYSTEM_CODE')";
    public static final String HAS_AUTH_SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    public static final String PERMISSION_GROUP_PLACEHOLDER = "${permissionGroup}";
    // evaluated to <permission>_<permissionGroup> (e.g. CREATE_DISTRIBUTION_SET)
    public static final String HAS_CREATE_REPOSITORY = "hasPermission(#root, 'CREATE_${permissionGroup}')";
    public static final String HAS_READ_REPOSITORY = "hasPermission(#root, 'READ_${permissionGroup}')";
    public static final String HAS_UPDATE_REPOSITORY = "hasPermission(#root, 'UPDATE_${permissionGroup}')";
    public static final String HAS_DELETE_REPOSITORY = "hasPermission(#root, 'DELETE_${permissionGroup}')";

    public static final String IS_CONTROLLER = "hasAnyRole('" + SpRole.CONTROLLER_ROLE_ANONYMOUS + "', '" + SpRole.CONTROLLER_ROLE + "')";
}