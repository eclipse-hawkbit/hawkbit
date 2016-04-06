/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;

/**
 * <p>
 * Software provisioning permissions that are technically available as
 * {@link GrantedAuthority} based on the authenticated users identity context.
 * </p>
 *
 * <p>
 * The Permissions cover CRUD for two data areas of SP:<br/>
 * <br/>
 * XX_Target_CRUD which covers the following entities: {@link Target} entities
 * including metadata, {@link TargetTag}s, {@link TargetRegistrationRule}s<br/>
 * XX_Repository CRUD which covers: {@link DistributionSet}s,
 * {@link SoftwareModule}s, DS Tags<br/>
 * </p>
 *
 *
 *
 *
 *
 */
public final class SpPermission {

    /**
     * Permission to read the targets from the
     * {@link ProvisioningTargetRepository} including their meta information,
     * {@link ProvisioningTargetFilter}s and target changing entities (
     * {@link DistributionSetApplier} and {@link TargetRegistrationRule}). That
     * corresponds in REST API to GET.
     */
    public static final String READ_TARGET = "READ_TARGET";

    /**
     * Permission to read the target security token. The security token is
     * security concerned and should be protected. So the combination
     * {@link #READ_TARGET} and {@link #READ_TARGET_SEC_TOKEN} is necessary to
     * able to read the security token of an target.
     */
    public static final String READ_TARGET_SEC_TOKEN = "READ_TARGET_SECURITY_TOKEN";

    /**
     * Permission to change/edit/update targets in the
     * {@link ProvisioningTargetRepository} including their meta information and
     * or/relations or {@link DistributionSet} assignment,
     * {@link ProvisioningTargetFilter}s and target changing entities (
     * {@link DistributionSetApplier} and {@link TargetRegistrationRule}). That
     * corresponds in REST API to POST.
     */
    public static final String UPDATE_TARGET = "UPDATE_TARGET";

    /**
     * Permission to add new targets to the {@link ProvisioningTargetRepository}
     * including their meta information and or/relations or
     * {@link DistributionSet} assignment.That corresponds in REST API to PUT.
     */
    public static final String CREATE_TARGET = "CREATE_TARGET";

    /**
     * Permission to delete targets in the {@link ProvisioningTargetRepository},
     * {@link ProvisioningTargetFilter}s and target changing entities (
     * {@link DistributionSetApplier} and {@link TargetRegistrationRule}). That
     * corresponds in REST API to DELETE.
     */
    public static final String DELETE_TARGET = "DELETE_TARGET";

    /**
     * Permission to read {@link DistributionSet}s and/or {@link OsPackage}s.
     * That corresponds in REST API to GET.
     */
    public static final String READ_REPOSITORY = "READ_REPOSITORY";

    /**
     * Permission to edit/update {@link DistributionSet}s including their
     * {@link OsPackage} assignment and/or {@link OsPackage}s. That corresponds
     * in REST API to POST.
     */
    public static final String UPDATE_REPOSITORY = "UPDATE_REPOSITORY";

    /**
     * Permission to add {@link DistributionSet}s and/or {@link OsPackage}s to
     * the repository. That corresponds in REST API to PUT.
     */
    public static final String CREATE_REPOSITORY = "CREATE_REPOSITORY";

    /**
     * Permission to delete {@link DistributionSet}s and/or {@link OsPackage}s
     * from the repository. That corresponds in REST API to DELETE.
     */
    public static final String DELETE_REPOSITORY = "DELETE_REPOSITORY";

    /**
     * Permission to monitor the SP system. E.g. retrieving health, monitor
     * checks through REST API provided by the spring actuator.
     */
    public static final String SYSTEM_MONITOR = "SYSTEM_MONITOR";

    /**
     * Permission to retrieve diagnosis of the SP system. E.g. retrieving
     * metrics, configuration through REST API provided by the spring actuator.
     */
    public static final String SYSTEM_DIAG = "SYSTEM_DIAG";

    /**
     * Permission to administrate the system on a global, i.e. tenant
     * independent scale. Thta inlcuds the deletion of tenants.
     */
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    /**
     * Permission to download repository artifact of an software module.
     */
    public static final String DOWNLOAD_REPOSITORY_ARTIFACT = "DOWNLOAD_REPOSITORY_ARTIFACT";

    /**
     * Permission to administrate the tenant settings.
     */
    public static final String TENANT_CONFIGURATION = "TENANT_CONFIGURATION";

    /**
     * Permission to administrate a rollout management.
     */
    public static final String ROLLOUT_MANAGEMENT = "ROLLOUT_MANAGEMENT";

    private SpPermission() {
        // Constants only
    }

    /**
     * Contains all the spring security evaluation expressions for the
     * {@link PreAuthorize} annotation for method security.
     * <p/>
     * Examples:
     * <p/>
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
     * 
     *
     *
     *
     */
    public static final class SpringEvalExpressions {
        /*
         * Spring security eval expressions.
         */
        private static final String HAS_AUTH_PREFIX = "hasAuthority('";
        private static final String HAS_AUTH_SUFFIX = "')";
        private static final String HAS_AUTH_AND = " and ";

        /**
         * The role which contains in the spring security context in case an
         * controller is authenticated.
         */
        public static final String CONTROLLER_ROLE = "ROLE_CONTROLLER";

        /**
         * The role which contains in the spring security context in case an
         * controller is authenticated but only as anonymous.
         */
        public static final String CONTROLLER_ROLE_ANONYMOUS = "ROLE_CONTROLLER_ANONYMOUS";

        /**
         * The role which contains in the spring security context in case an
         * controller is authenticated to download artifacts.
         */
        public static final String CONTROLLER_DOWNLOAD_ROLE = "ROLE_CONTROLLER_DOWNLOAD";

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
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#UPDATE_TARGET}.
         */
        public static final String HAS_AUTH_UPDATE_TARGET = HAS_AUTH_PREFIX + UPDATE_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#SYSTEM_ADMIN}.
         */
        public static final String HAS_AUTH_SYSTEM_ADMIN = HAS_AUTH_PREFIX + SYSTEM_ADMIN + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_TARGET}.
         */
        public static final String HAS_AUTH_READ_TARGET = HAS_AUTH_PREFIX + READ_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#CREATE_TARGET}.
         */
        public static final String HAS_AUTH_CREATE_TARGET = HAS_AUTH_PREFIX + CREATE_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#DELETE_TARGET}.
         */
        public static final String HAS_AUTH_DELETE_TARGET = HAS_AUTH_PREFIX + DELETE_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_REPOSITORY} and
         * {@link SpPermission#UPDATE_TARGET}.
         */
        public static final String HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET = HAS_AUTH_PREFIX + READ_REPOSITORY
                + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + UPDATE_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#CREATE_REPOSITORY}.
         */
        public static final String HAS_AUTH_CREATE_REPOSITORY = HAS_AUTH_PREFIX + CREATE_REPOSITORY + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#DELETE_REPOSITORY}.
         */
        public static final String HAS_AUTH_DELETE_REPOSITORY = HAS_AUTH_PREFIX + DELETE_REPOSITORY + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_REPOSITORY}.
         */
        public static final String HAS_AUTH_READ_REPOSITORY = HAS_AUTH_PREFIX + READ_REPOSITORY + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#UPDATE_REPOSITORY}.
         */
        public static final String HAS_AUTH_UPDATE_REPOSITORY = HAS_AUTH_PREFIX + UPDATE_REPOSITORY + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#READ_REPOSITORY} and
         * {@link SpPermission#READ_TARGET}.
         */
        public static final String HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET = HAS_AUTH_PREFIX + READ_REPOSITORY
                + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + READ_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#DOWNLOAD_REPOSITORY_ARTIFACT}.
         */
        public static final String HAS_AUTH_DOWNLOAD_ARTIFACT = HAS_AUTH_PREFIX + DOWNLOAD_REPOSITORY_ARTIFACT
                + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAnyRole expression to check if the spring
         * context contains the anoynmous role or the controller specific role
         * {@link SpPermission#CONTROLLER_ROLE}.
         */
        public static final String IS_CONTROLLER = "hasAnyRole('" + CONTROLLER_ROLE_ANONYMOUS + "', '" + CONTROLLER_ROLE
                + "')";

        /**
         * Spring security eval hasAuthority expression to check if the spring
         * context contains the role to allow controllers to download specific
         * role {@link SpPermission#CONTROLLER_DOWNLOAD_ROLE}.
         */
        public static final String HAS_CONTROLLER_DOWNLOAD = HAS_AUTH_PREFIX + CONTROLLER_DOWNLOAD_ROLE
                + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAnyRole expression to check if the spring
         * context contains system code role
         * {@link SpringEvalExpressions#SYSTEM_ROLE}.
         */
        public static final String IS_SYSTEM_CODE = HAS_AUTH_PREFIX + SYSTEM_ROLE + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#CREATE_REPOSITORY} and
         * {@link SpPermission#CREATE_TARGET}.
         */
        public static final String HAS_AUTH_CREATE_REPOSITORY_AND_CREATE_TARGET = HAS_AUTH_PREFIX + CREATE_REPOSITORY
                + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + CREATE_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#ROLLOUT_MANAGEMENT}
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_READ = HAS_AUTH_PREFIX + ROLLOUT_MANAGEMENT
                + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#ROLLOUT_MANAGEMENT} and
         * {@link SpPermission#UPDATE_TARGET}.
         */
        public static final String HAS_AUTH_ROLLOUT_MANAGEMENT_WRITE = HAS_AUTH_PREFIX + ROLLOUT_MANAGEMENT
                + HAS_AUTH_SUFFIX + HAS_AUTH_AND + HAS_AUTH_PREFIX + UPDATE_TARGET + HAS_AUTH_SUFFIX;

        /**
         * Spring security eval hasAuthority expression to check if spring
         * context contains {@link SpPermission#TENANT_CONFIGURATION}
         */
        public static final String HAS_AUTH_TENANT_CONFIGURATION = HAS_AUTH_PREFIX + TENANT_CONFIGURATION
                + HAS_AUTH_SUFFIX;

        private SpringEvalExpressions() {
            // utility class
        }
    }
}
