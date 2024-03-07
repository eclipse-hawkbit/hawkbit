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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;

/**
 * <p>
 * Software provisioning permissions that are technically available as
 * {@linkplain GrantedAuthority} based on the authenticated users identity
 * context.
 * </p>
 *
 * <p>
 * The permissions cover CRUD operations for various areas within eclipse
 * hawkBit, like targets, software-artifacts, distribution sets, config-options
 * etc.
 * </p>
 */
@Slf4j
public final class SpPermission {

    /**
     * Permission to read the targets (list and filter).
     */
    public static final String READ_TARGET = "READ_TARGET";

    /**
     * Permission to read the target security token. The security token is security
     * concerned and should be protected. So the combination
     * {@linkplain #READ_TARGET} and {@code READ_TARGET_SEC_TOKEN} is necessary to
     * be able to read the security token of a target.
     */
    public static final String READ_TARGET_SEC_TOKEN = "READ_TARGET_SECURITY_TOKEN";

    /**
     * Permission to change/edit/update targets and to assign updates.
     */
    public static final String UPDATE_TARGET = "UPDATE_TARGET";

    /**
     * Permission to add new targets including their meta information.
     */
    public static final String CREATE_TARGET = "CREATE_TARGET";

    /**
     * Permission to delete targets.
     */
    public static final String DELETE_TARGET = "DELETE_TARGET";

    /**
     * Permission to read distributions and artifacts.
     */
    public static final String READ_REPOSITORY = "READ_REPOSITORY";

    /**
     * Permission to edit/update distributions and artifacts.
     */
    public static final String UPDATE_REPOSITORY = "UPDATE_REPOSITORY";

    /**
     * Permission to add distributions and artifacts.
     */
    public static final String CREATE_REPOSITORY = "CREATE_REPOSITORY";

    /**
     * Permission to delete distributions and artifacts.
     */
    public static final String DELETE_REPOSITORY = "DELETE_REPOSITORY";

    /**
     * Permission to administrate the system on a global, i.e. tenant
     * independent scale. That includes the deletion of tenants.
     */
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    /**
     * Permission to download repository artifacts of a software module.
     */
    public static final String DOWNLOAD_REPOSITORY_ARTIFACT = "DOWNLOAD_REPOSITORY_ARTIFACT";

    /**
     * Permission to administrate the tenant settings.
     */
    public static final String TENANT_CONFIGURATION = "TENANT_CONFIGURATION";

    /**
     * Permission to read a rollout.
     */
    public static final String READ_ROLLOUT = "READ_ROLLOUT";

    /**
     * Permission to create a rollout.
     */
    public static final String CREATE_ROLLOUT = "CREATE_ROLLOUT";

    /**
     * Permission to update a rollout.
     */
    public static final String UPDATE_ROLLOUT = "UPDATE_ROLLOUT";

    /**
     * Permission to delete a rollout.
     */
    public static final String DELETE_ROLLOUT = "DELETE_ROLLOUT";

    /**
     * Permission to start/stop/resume a rollout.
     */
    public static final String HANDLE_ROLLOUT = "HANDLE_ROLLOUT";

    /**
     * Permission to approve or deny a rollout prior to starting.
     */
    public static final String APPROVE_ROLLOUT = "APPROVE_ROLLOUT";

    private SpPermission() {
        // Constants only
    }

    /**
     * Return all permission.
     * @return all permissions
     */
    public static List<String> getAllAuthorities() {
        final List<String> allPermissions = new ArrayList<>();
        final Field[] declaredFields = SpPermission.class.getDeclaredFields();
        for (final Field field : declaredFields) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                try {
                    final String role = (String) field.get(null);
                    allPermissions.add(role);
                } catch (final IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return allPermissions;
    }

    /**
     * <p>
     * Contains all the spring security evaluation expressions for the
     * {@link PreAuthorize} annotation for method security.
     * </p>
     *
     * <p>
     * Examples:
     *
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
    public static final class SpringEvalExpressions {
        /*
         * Spring security eval expressions.
         */
        public static final String BRACKET_OPEN = "(";
        public static final String BRACKET_CLOSE = ")";
        public static final String HAS_AUTH_PREFIX = "hasAuthority" + BRACKET_OPEN + "'";
        public static final String HAS_AUTH_SUFFIX = "'" + BRACKET_CLOSE;
        public static final String HAS_AUTH_AND = " and ";

        /**
         * The role which contains in the spring security context in case an
         * controller is authenticated.
         */
        public static final String CONTROLLER_ROLE = "ROLE_CONTROLLER";

        /**
         * The role which contained in the spring security context in case that a
         * controller is authenticated, but only as 'anonymous'.
         */
        public static final String CONTROLLER_ROLE_ANONYMOUS = "ROLE_CONTROLLER_ANONYMOUS";

        /**
         * The role which contains the spring security context in case the
         * system is executing code which is necessary to be privileged.
         */
        public static final String SYSTEM_ROLE = "ROLE_SYSTEM_CODE";

        /**
         * The spring security eval expression operator {@code or}.
         */
        public static final String HAS_AUTH_OR = " or ";

        /**
         * Spring security eval hasAnyRole expression to check if the spring
         * context contains system code role
         * {@link SpringEvalExpressions#SYSTEM_ROLE}.
         */
        public static final String IS_SYSTEM_CODE = HAS_AUTH_PREFIX + SYSTEM_ROLE + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#UPDATE_TARGET} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_UPDATE_TARGET = HAS_AUTH_PREFIX + UPDATE_TARGET + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#SYSTEM_ADMIN} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_SYSTEM_ADMIN = HAS_AUTH_PREFIX + SYSTEM_ADMIN + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_TARGET} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_READ_TARGET = HAS_AUTH_PREFIX + READ_TARGET + HAS_AUTH_SUFFIX + HAS_AUTH_OR
                + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_TARGET_SEC_TOKEN} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_READ_TARGET_SEC_TOKEN = HAS_AUTH_PREFIX + READ_TARGET_SEC_TOKEN
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#CREATE_TARGET} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_CREATE_TARGET = HAS_AUTH_PREFIX + CREATE_TARGET + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#DELETE_TARGET} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_DELETE_TARGET = HAS_AUTH_PREFIX + DELETE_TARGET + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_REPOSITORY} and
         * {@link SpPermission#UPDATE_TARGET} or {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET = BRACKET_OPEN + HAS_AUTH_PREFIX
                + READ_REPOSITORY + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + UPDATE_TARGET + HAS_AUTH_SUFFIX
                + BRACKET_CLOSE + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#CREATE_REPOSITORY} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_CREATE_REPOSITORY = HAS_AUTH_PREFIX + CREATE_REPOSITORY + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#DELETE_REPOSITORY} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_DELETE_REPOSITORY = HAS_AUTH_PREFIX + DELETE_REPOSITORY + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_REPOSITORY} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_READ_REPOSITORY = HAS_AUTH_PREFIX + READ_REPOSITORY + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#UPDATE_REPOSITORY} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_UPDATE_REPOSITORY = HAS_AUTH_PREFIX + UPDATE_REPOSITORY + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_REPOSITORY} and
         * {@link SpPermission#UPDATE_REPOSITORY} or {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_READ_REPOSITORY_AND_UPDATE_REPOSITORY = BRACKET_OPEN + HAS_AUTH_PREFIX
                + READ_REPOSITORY + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + UPDATE_REPOSITORY
                + HAS_AUTH_SUFFIX + BRACKET_CLOSE + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_REPOSITORY} and
         * {@link SpPermission#READ_TARGET} or {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET = BRACKET_OPEN + HAS_AUTH_PREFIX
                + READ_REPOSITORY + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + READ_TARGET + HAS_AUTH_SUFFIX
                + BRACKET_CLOSE + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#DOWNLOAD_REPOSITORY_ARTIFACT} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_DOWNLOAD_ARTIFACT = HAS_AUTH_PREFIX + DOWNLOAD_REPOSITORY_ARTIFACT
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAnyRole expression to check if the spring
         * context contains the anonymous role or the controller specific role
         * {@link SpringEvalExpressions#CONTROLLER_ROLE}.
         */
        public static final String IS_CONTROLLER = "hasAnyRole('" + CONTROLLER_ROLE_ANONYMOUS + "', '" + CONTROLLER_ROLE
                + "')";

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#CREATE_REPOSITORY} and
         * {@link SpPermission#CREATE_TARGET} or {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_CREATE_REPOSITORY_AND_CREATE_TARGET = BRACKET_OPEN + HAS_AUTH_PREFIX
                + CREATE_REPOSITORY + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + CREATE_TARGET + HAS_AUTH_SUFFIX
                + BRACKET_CLOSE + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_ROLLOUT} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_READ = HAS_AUTH_PREFIX + READ_ROLLOUT + HAS_AUTH_SUFFIX
                + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_ROLLOUT} and
         * {@link SpPermission#READ_TARGET} or {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ = BRACKET_OPEN + HAS_AUTH_PREFIX
                + READ_ROLLOUT + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + READ_TARGET + HAS_AUTH_SUFFIX
                + BRACKET_CLOSE + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#CREATE_ROLLOUT} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE = HAS_AUTH_PREFIX + CREATE_ROLLOUT
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#HANDLE_ROLLOUT} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE = HAS_AUTH_PREFIX + HANDLE_ROLLOUT
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#APPROVE_ROLLOUT} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_APPROVE = HAS_AUTH_PREFIX + APPROVE_ROLLOUT
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#UPDATE_ROLLOUT} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE = HAS_AUTH_PREFIX + UPDATE_ROLLOUT
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#DELETE_ROLLOUT} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE = HAS_AUTH_PREFIX + DELETE_ROLLOUT
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#TENANT_CONFIGURATION} or
         * {@link #IS_SYSTEM_CODE}.
         */
        public static final String HAS_AUTH_TENANT_CONFIGURATION = HAS_AUTH_PREFIX + TENANT_CONFIGURATION
                + HAS_AUTH_SUFFIX + HAS_AUTH_OR + IS_SYSTEM_CODE;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#IS_CONTROLLER} or
         * {@link #HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET}.
         */
        public static final String IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET =
                IS_CONTROLLER + HAS_AUTH_OR + HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#IS_CONTROLLER} or
         * {@link #HAS_AUTH_READ_REPOSITORY}.
         */
        public static final String IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY =
                IS_CONTROLLER + HAS_AUTH_OR + HAS_AUTH_READ_REPOSITORY;
        private SpringEvalExpressions() {
            // utility class
        }
    }
}
