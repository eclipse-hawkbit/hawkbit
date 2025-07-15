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
 * authentication Allows direct access to the current Authentication object obtained from the SecurityContext
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

    public static final String BRACKET_OPEN = "(";
    public static final String BRACKET_CLOSE = ")";
    public static final String HAS_AUTH_PREFIX = "hasAuthority" + BRACKET_OPEN + "'";
    public static final String HAS_AUTH_SUFFIX = "'" + BRACKET_CLOSE;
    public static final String HAS_AUTH_AND = " and ";
    public static final String HAS_AUTH_OR = " or ";

    /**
     * The role which contains in the spring security context in case ancontroller is authenticated.
     */
    public static final String CONTROLLER_ROLE = "ROLE_CONTROLLER";
    /**
     * The role which contained in the spring security context in case that a controller is authenticated, but only as 'anonymous'.
     */
    public static final String CONTROLLER_ROLE_ANONYMOUS = "ROLE_CONTROLLER_ANONYMOUS";

    public static final String IS_SYSTEM_CODE = HAS_AUTH_PREFIX + SpRole.SYSTEM_ROLE + HAS_AUTH_SUFFIX;

    public static final String HAS_AUTH_SYSTEM_ADMIN = HAS_AUTH_PREFIX + SpPermission.SYSTEM_ADMIN + HAS_AUTH_SUFFIX;

    public static final String HAS_AUTH_CREATE_TARGET = HAS_AUTH_PREFIX + SpPermission.CREATE_TARGET + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_UPDATE_TARGET = HAS_AUTH_PREFIX + SpPermission.UPDATE_TARGET + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_READ_TARGET = HAS_AUTH_PREFIX + SpPermission.READ_TARGET + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_DELETE_TARGET = HAS_AUTH_PREFIX + SpPermission.DELETE_TARGET + HAS_AUTH_SUFFIX;

    public static final String HAS_AUTH_CREATE_TARGET_TYPE = HAS_AUTH_PREFIX + SpPermission.CREATE_TARGET_TYPE + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_UPDATE_TARGET_TYPE = HAS_AUTH_PREFIX + SpPermission.UPDATE_TARGET_TYPE + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_READ_TARGET_TYPE = HAS_AUTH_PREFIX + SpPermission.READ_TARGET_TYPE + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_DELETE_TARGET_TYPE = HAS_AUTH_PREFIX + SpPermission.DELETE_TARGET_TYPE + HAS_AUTH_SUFFIX;

    public static final String HAS_AUTH_UPDATE_DISTRIBUTION_SET = HAS_AUTH_PREFIX + SpPermission.UPDATE_DISTRIBUTION_SET + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_READ_DISTRIBUTION_SET = HAS_AUTH_PREFIX + SpPermission.READ_DISTRIBUTION_SET + HAS_AUTH_SUFFIX;

    public static final String HAS_AUTH_CREATE_REPOSITORY = HAS_AUTH_PREFIX + SpPermission.CREATE_REPOSITORY + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_READ_REPOSITORY = HAS_AUTH_PREFIX + SpPermission.READ_REPOSITORY + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_UPDATE_REPOSITORY = HAS_AUTH_PREFIX + SpPermission.UPDATE_REPOSITORY + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_DELETE_REPOSITORY = HAS_AUTH_PREFIX + SpPermission.DELETE_REPOSITORY + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_DOWNLOAD_ARTIFACT = HAS_AUTH_PREFIX + SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET = BRACKET_OPEN + HAS_AUTH_PREFIX
            + SpPermission.READ_REPOSITORY + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + SpPermission.UPDATE_TARGET + HAS_AUTH_SUFFIX
            + BRACKET_CLOSE;

    public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE = HAS_AUTH_PREFIX + SpPermission.CREATE_ROLLOUT + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_READ = HAS_AUTH_PREFIX + SpPermission.READ_ROLLOUT + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE = HAS_AUTH_PREFIX + SpPermission.UPDATE_ROLLOUT + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE = HAS_AUTH_PREFIX + SpPermission.DELETE_ROLLOUT + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_APPROVE = HAS_AUTH_PREFIX + SpPermission.APPROVE_ROLLOUT + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE = HAS_AUTH_PREFIX + SpPermission.HANDLE_ROLLOUT + HAS_AUTH_SUFFIX;

    public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ = BRACKET_OPEN + HAS_AUTH_PREFIX
            + SpPermission.READ_ROLLOUT + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + SpPermission.READ_TARGET + HAS_AUTH_SUFFIX
            + BRACKET_CLOSE;

    public static final String HAS_AUTH_TENANT_CONFIGURATION_READ = HAS_AUTH_PREFIX + SpPermission.READ_TENANT_CONFIGURATION + HAS_AUTH_SUFFIX;
    public static final String HAS_AUTH_TENANT_CONFIGURATION = HAS_AUTH_PREFIX + SpPermission.TENANT_CONFIGURATION + HAS_AUTH_SUFFIX;

    public static final String IS_CONTROLLER = "hasAnyRole('" + CONTROLLER_ROLE_ANONYMOUS + "', '" + CONTROLLER_ROLE + "')";
    public static final String IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET = IS_CONTROLLER + HAS_AUTH_OR + HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET;
}