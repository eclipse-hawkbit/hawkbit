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

    public static final String READ_REPOSITORY = "READ_REPOSITORY";
    public static final String UPDATE_REPOSITORY = "UPDATE_REPOSITORY";
    public static final String CREATE_REPOSITORY = "CREATE_REPOSITORY";
    public static final String DELETE_REPOSITORY = "DELETE_REPOSITORY";
    public static final String DOWNLOAD_REPOSITORY_ARTIFACT = "DOWNLOAD_REPOSITORY_ARTIFACT";

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

    public static final String IMPLY_CREATE = " > " + CREATE_PREFIX;
    public static final String IMPLY_READ = " > " + READ_PREFIX;
    public static final String IMPLY_UPDATE = " > " + UPDATE_PREFIX;
    public static final String IMPLY_DELETE = " > " + DELETE_PREFIX;

    // @formatter:off
    public static final String TARGET_HIERARCHY =
            CREATE_TARGET + IMPLY_READ + TARGET_TYPE + "\n" +
            READ_TARGET + IMPLY_READ + TARGET_TYPE + "\n" +
            UPDATE_TARGET + IMPLY_READ + TARGET_TYPE + "\n" +
            DELETE_TARGET + IMPLY_READ + TARGET_TYPE + "\n";
    public static final String REPOSITORY_HIERARCHY =
            CREATE_REPOSITORY + IMPLY_CREATE + SOFTWARE_MODULE + "\n" +
            READ_REPOSITORY + IMPLY_READ + SOFTWARE_MODULE + "\n" +
            UPDATE_REPOSITORY + IMPLY_UPDATE + SOFTWARE_MODULE + "\n" +
            DELETE_REPOSITORY + IMPLY_DELETE + SOFTWARE_MODULE + "\n" +
            CREATE_REPOSITORY + IMPLY_CREATE + SOFTWARE_MODULE_TYPE + "\n" +
            READ_REPOSITORY + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n" +
            UPDATE_REPOSITORY + IMPLY_UPDATE + SOFTWARE_MODULE_TYPE + "\n" +
            DELETE_REPOSITORY + IMPLY_DELETE + SOFTWARE_MODULE_TYPE + "\n" +
            CREATE_REPOSITORY + IMPLY_CREATE + DISTRIBUTION_SET + "\n" +
            READ_REPOSITORY + IMPLY_READ + DISTRIBUTION_SET + "\n" +
            UPDATE_REPOSITORY + IMPLY_UPDATE + DISTRIBUTION_SET + "\n" +
            DELETE_REPOSITORY + IMPLY_DELETE + DISTRIBUTION_SET + "\n" +
            CREATE_REPOSITORY + IMPLY_CREATE + DISTRIBUTION_SET_TYPE + "\n" +
            READ_REPOSITORY + IMPLY_READ + DISTRIBUTION_SET_TYPE + "\n" +
            UPDATE_REPOSITORY + IMPLY_UPDATE + DISTRIBUTION_SET_TYPE + "\n" +
            DELETE_REPOSITORY + IMPLY_DELETE + DISTRIBUTION_SET_TYPE + "\n";
    public static final String SOFTWARE_MODULE_HIERARCHY =
            CREATE_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n" +
            READ_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n" +
            UPDATE_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n" +
            DELETE_PREFIX + SOFTWARE_MODULE + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n";
    public static final String DISTRIBUTION_SET_HIERARCHY =
            CREATE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n" +
            READ_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n" +
            UPDATE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n" +
            DELETE_PREFIX + DISTRIBUTION_SET + IMPLY_READ + SOFTWARE_MODULE_TYPE + "\n";
    public static final String TENANT_CONFIGURATION_HIERARCHY =
            TENANT_CONFIGURATION + IMPLY_CREATE + TENANT_CONFIGURATION + "\n" +
            TENANT_CONFIGURATION + IMPLY_READ + TENANT_CONFIGURATION + "\n" +
            TENANT_CONFIGURATION + IMPLY_UPDATE + TENANT_CONFIGURATION + "\n" +
            TENANT_CONFIGURATION + IMPLY_DELETE + TENANT_CONFIGURATION + "\n" +
            TENANT_CONFIGURATION + " > " + READ_GATEWAY_SECURITY_TOKEN + "\n";

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
        allPermissions.add(DOWNLOAD_REPOSITORY_ARTIFACT);
        allPermissions.add(APPROVE_ROLLOUT);
        allPermissions.add(HANDLE_ROLLOUT);

        // coarse-grained - maybe to be deprecated
        for (final String access_prefix : new String[] { CREATE_PREFIX, READ_PREFIX, UPDATE_PREFIX, DELETE_PREFIX }) {
            allPermissions.add(access_prefix + "REPOSITORY");
        }
        allPermissions.add(TENANT_CONFIGURATION);

        // system permission, (!) take care with
        allPermissions.add(SYSTEM_ADMIN);

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