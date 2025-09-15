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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.function.SingletonSupplier;

/**
 * <p>
 * Software provisioning permissions that are technically available as {@linkplain GrantedAuthority} based on
 * the authenticated users identity context.
 * </p>
 *
 * <p>
 * The permissions cover CRUD operations for various areas within eclipse hawkBit, like targets, software-artifacts,
 * distribution sets, config-options etc.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class SpPermission {

    // Permission prefixes
    public static final String CREATE_PREFIX = "CREATE_";
    public static final String READ_PREFIX = "READ_";
    public static final String UPDATE_PREFIX = "UPDATE_";
    public static final String DELETE_PREFIX = "DELETE_";

    // Permission groups
    public static final String TARGET = "TARGET";
    public static final String TARGET_TYPE = "TARGET_TYPE";
    public static final String SOFTWARE_MODULE = "SOFTWARE_MODULE";
    public static final String SOFTWARE_MODULE_TYPE = "SOFTWARE_MODULE_TYPE";
    public static final String DISTRIBUTION_SET = "DISTRIBUTION_SET";
    public static final String DISTRIBUTION_SET_TYPE = "DISTRIBUTION_SET_TYPE";
    public static final String ROLLOUT = "ROLLOUT";
    public static final String TENANT_CONFIGURATION = "TENANT_CONFIGURATION";

    public static final String CREATE_TARGET = CREATE_PREFIX + TARGET;
    public static final String READ_TARGET = READ_PREFIX + TARGET;
    public static final String UPDATE_TARGET = UPDATE_PREFIX + TARGET;
    public static final String DELETE_TARGET = DELETE_PREFIX + TARGET;
    /**
     * Permission to read the target security token. The security token is security concerned and should be protected. So the combination
     * {@linkplain #READ_TARGET} and {@code READ_TARGET_SEC_TOKEN} is necessary to be able to read the security token of a target.
     */
    public static final String READ_TARGET_SECURITY_TOKEN = READ_TARGET + "_SECURITY_TOKEN";

    public static final String READ_TARGET_TYPE = READ_PREFIX + TARGET_TYPE;
    public static final String UPDATE_TARGET_TYPE = UPDATE_PREFIX + TARGET_TYPE;
    public static final String DELETE_TARGET_TYPE = DELETE_PREFIX + TARGET_TYPE;

    public static final String READ_DISTRIBUTION_SET = READ_PREFIX + DISTRIBUTION_SET;
    public static final String UPDATE_DISTRIBUTION_SET = UPDATE_PREFIX + DISTRIBUTION_SET;

    /**
     * Deprecated since 0.10.0, use {@link #SOFTWARE_MODULE_DOWNLOAD_ARTIFACT} instead
     *
     * @deprecated since 0.10.0, use {@link #SOFTWARE_MODULE_DOWNLOAD_ARTIFACT} instead
     */
    @Deprecated(since = "0.10.0", forRemoval = true)
    public static final String DOWNLOAD_REPOSITORY_ARTIFACT = "DOWNLOAD_REPOSITORY_ARTIFACT";
    public static final String SOFTWARE_MODULE_DOWNLOAD_ARTIFACT = "SM_DOWNLOAD_ARTIFACT";

    /**
     * Permission to read the tenant settings.
     */
    public static final String READ_TENANT_CONFIGURATION = READ_PREFIX + TENANT_CONFIGURATION;
    /**
     * Permission to read the gateway security token. The gateway security token is security
     * concerned and should be protected. So in addition to {@linkplain #READ_TENANT_CONFIGURATION},
     * {@code READ_GATEWAY_SEC_TOKEN} is necessary to read gateway security token. {@link #TENANT_CONFIGURATION}
     * implies both permissions - so it is sufficient to read the gateway security token.
     */
    public static final String READ_GATEWAY_SECURITY_TOKEN = "READ_GATEWAY_SECURITY_TOKEN";

    public static final String CREATE_ROLLOUT = CREATE_PREFIX + ROLLOUT;
    public static final String READ_ROLLOUT = READ_PREFIX + ROLLOUT;
    public static final String UPDATE_ROLLOUT = UPDATE_PREFIX + ROLLOUT;
    public static final String DELETE_ROLLOUT = DELETE_PREFIX + ROLLOUT;
    /** Permission to approve or deny a rollout prior to starting. */
    public static final String APPROVE_ROLLOUT = "APPROVE_" + ROLLOUT;
    /** Permission to start/stop/resume a rollout. */
    public static final String HANDLE_ROLLOUT = "HANDLE_" + ROLLOUT;

    /** Permission to administrate the system on a global, i.e. tenant independent scale. That includes the deletion of tenants. */
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    public static final String IMPLY = " > ";
    public static final String IMPLY_CREATE = IMPLY + CREATE_PREFIX;
    public static final String IMPLY_READ = IMPLY + READ_PREFIX;
    public static final String IMPLY_UPDATE = IMPLY + UPDATE_PREFIX;
    public static final String IMPLY_DELETE = IMPLY + DELETE_PREFIX;
    private static final String LINE_BREAK = "\n";

    // @formatter:off
    public static final String TARGET_HIERARCHY =
            CREATE_TARGET + IMPLY_READ + TARGET_TYPE + LINE_BREAK +
            READ_TARGET + IMPLY_READ + TARGET_TYPE + LINE_BREAK +
            UPDATE_TARGET + IMPLY_READ + TARGET_TYPE + LINE_BREAK +
            DELETE_TARGET + IMPLY_READ + TARGET_TYPE + LINE_BREAK;
    public static final String SOFTWARE_MODULE_HIERARCHY =
            CREATE_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK +
            READ_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK +
            UPDATE_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK +
            DELETE_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK;
    public static final String DISTRIBUTION_SET_HIERARCHY =
            CREATE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + DISTRIBUTION_SET_TYPE + LINE_BREAK +
            READ_PREFIX + DISTRIBUTION_SET + IMPLY_READ + DISTRIBUTION_SET_TYPE + LINE_BREAK +
            UPDATE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + DISTRIBUTION_SET_TYPE + LINE_BREAK +
            DELETE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + DISTRIBUTION_SET_TYPE + LINE_BREAK  +
            CREATE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK +
            READ_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK +
            UPDATE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK +
            DELETE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + LINE_BREAK;
    public static final String TENANT_CONFIGURATION_HIERARCHY =
            TENANT_CONFIGURATION + IMPLY_CREATE + TENANT_CONFIGURATION + LINE_BREAK +
            TENANT_CONFIGURATION + IMPLY_READ + TENANT_CONFIGURATION + LINE_BREAK +
            TENANT_CONFIGURATION + IMPLY_UPDATE + TENANT_CONFIGURATION + LINE_BREAK +
            TENANT_CONFIGURATION + IMPLY_DELETE + TENANT_CONFIGURATION + LINE_BREAK +
            TENANT_CONFIGURATION + IMPLY + READ_GATEWAY_SECURITY_TOKEN + LINE_BREAK;

    // @formatter:on
    private static final SingletonSupplier<List<String>> ALL_AUTHORITIES = SingletonSupplier.of(() -> {
        final List<String> allPermissions = new ArrayList<>();

        // groups with access, canonical
        for (final String group : new String[] {
                TARGET, TARGET_TYPE,
                SOFTWARE_MODULE, SOFTWARE_MODULE_TYPE, DISTRIBUTION_SET, DISTRIBUTION_SET_TYPE,
                ROLLOUT,
                TENANT_CONFIGURATION }) {
            for (final String access_prefix : new String[] { CREATE_PREFIX, READ_PREFIX, UPDATE_PREFIX, DELETE_PREFIX }) {
                allPermissions.add(access_prefix + group);
            }
        }
        // special
        allPermissions.add(READ_TARGET_SECURITY_TOKEN);
        allPermissions.add(READ_GATEWAY_SECURITY_TOKEN);
        allPermissions.add(SOFTWARE_MODULE_DOWNLOAD_ARTIFACT);
        allPermissions.add(APPROVE_ROLLOUT);
        allPermissions.add(HANDLE_ROLLOUT);

        // coarse-grained - maybe to be deprecated
        for (final String access_prefix : new String[] { CREATE_PREFIX, READ_PREFIX, UPDATE_PREFIX, DELETE_PREFIX }) {
            allPermissions.add(access_prefix + "REPOSITORY");
        }
        allPermissions.add(TENANT_CONFIGURATION);

        // system permission, (!) take care with
        allPermissions.add(SYSTEM_ADMIN);

        // add deprecated permissions
        allPermissions.add(DOWNLOAD_REPOSITORY_ARTIFACT);

        return Collections.unmodifiableList(allPermissions);
    });

    /**
     * Return all permission.
     *
     * @return all permissions
     */
    public static List<String> getAllAuthorities() {
        return ALL_AUTHORITIES.get();
    }
}