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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;

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

    /**
     * Permission to add new targets including their meta information.
     */
    public static final String CREATE_TARGET = "CREATE_TARGET";
    /**
     * Permission to read the targets (list and filter).
     */
    public static final String READ_TARGET = "READ_TARGET";
    /**
     * Permission to change/edit/update targets and to assign updates.
     */
    public static final String UPDATE_TARGET = "UPDATE_TARGET";
    /**
     * Permission to delete targets.
     */
    public static final String DELETE_TARGET = "DELETE_TARGET";
    /**
     * Permission to read the target security token. The security token is security
     * concerned and should be protected. So the combination
     * {@linkplain #READ_TARGET} and {@code READ_TARGET_SEC_TOKEN} is necessary to
     * be able to read the security token of a target.
     */
    public static final String READ_TARGET_SEC_TOKEN = "READ_TARGET_SECURITY_TOKEN";

    public static final String CREATE_TARGET_TYPE = "CREATE_TARGET_TYPE";
    public static final String READ_TARGET_TYPE = "READ_TARGET_TYPE";
    public static final String UPDATE_TARGET_TYPE = "UPDATE_TARGET_TYPE";
    public static final String DELETE_TARGET_TYPE = "DELETE_TARGET_TYPE";

    public static final String CREATE_DISTRIBUTION_SET = "CREATE_DISTRIBUTION_SET";
    public static final String READ_DISTRIBUTION_SET = "READ_DISTRIBUTION_SET";
    public static final String UPDATE_DISTRIBUTION_SET = "UPDATE_DISTRIBUTION_SET";
    public static final String DELETE_DISTRIBUTION_SET = "DELETE_DISTRIBUTION_SET";

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
     * Permission to download repository artifacts of a software module.
     */
    public static final String DOWNLOAD_REPOSITORY_ARTIFACT = "DOWNLOAD_REPOSITORY_ARTIFACT";

    /**
     * Permission to read the tenant settings.
     */
    public static final String READ_TENANT_CONFIGURATION = "READ_TENANT_CONFIGURATION";
    /**
     * Permission to read the gateway security token. The gateway security token is security
     * concerned and should be protected. So in addition to {@linkplain #READ_TENANT_CONFIGURATION},
     * {@code READ_GATEWAY_SEC_TOKEN} is necessary to read gateway security token. {@link #TENANT_CONFIGURATION}
     * implies both permissions - so it is sufficient to read the gateway security token.
     */
    public static final String READ_GATEWAY_SEC_TOKEN = "READ_GATEWAY_SECURITY_TOKEN";
    /**
     * Permission to administrate the tenant settings.
     */
    public static final String TENANT_CONFIGURATION = "TENANT_CONFIGURATION";

    /**
     * Permission to create a rollout.
     */
    public static final String CREATE_ROLLOUT = "CREATE_ROLLOUT";
    /**
     * Permission to read a rollout.
     */
    public static final String READ_ROLLOUT = "READ_ROLLOUT";
    /**
     * Permission to update a rollout.
     */
    public static final String UPDATE_ROLLOUT = "UPDATE_ROLLOUT";
    /**
     * Permission to delete a rollout.
     */
    public static final String DELETE_ROLLOUT = "DELETE_ROLLOUT";
    /**
     * Permission to approve or deny a rollout prior to starting.
     */
    public static final String APPROVE_ROLLOUT = "APPROVE_ROLLOUT";
    /**
     * Permission to start/stop/resume a rollout.
     */
    public static final String HANDLE_ROLLOUT = "HANDLE_ROLLOUT";

    /**
     * Permission to administrate the system on a global, i.e. tenant independent scale. That includes the deletion of tenants.
     */
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    private static final String IMPLIES = " > ";
    private static final String LINE_BREAK = "\n";
    public static final String TARGET_HIERARCHY =
            CREATE_TARGET + IMPLIES + CREATE_TARGET_TYPE + LINE_BREAK +
            READ_TARGET + IMPLIES + READ_TARGET_TYPE + LINE_BREAK +
            UPDATE_TARGET + IMPLIES + UPDATE_TARGET_TYPE + LINE_BREAK +
            DELETE_TARGET + IMPLIES + DELETE_TARGET_TYPE + LINE_BREAK;
    public static final String REPOSITORY_HIERARCHY =
            CREATE_REPOSITORY + IMPLIES + CREATE_DISTRIBUTION_SET + LINE_BREAK +
            READ_REPOSITORY + IMPLIES + READ_DISTRIBUTION_SET + LINE_BREAK +
            UPDATE_REPOSITORY + IMPLIES + UPDATE_DISTRIBUTION_SET + LINE_BREAK +
            DELETE_REPOSITORY + IMPLIES + DELETE_DISTRIBUTION_SET + LINE_BREAK;
    public static final String TENANT_CONFIGURATION_HIERARCHY =
            TENANT_CONFIGURATION + IMPLIES + READ_TENANT_CONFIGURATION + LINE_BREAK +
            TENANT_CONFIGURATION + IMPLIES + READ_GATEWAY_SEC_TOKEN + LINE_BREAK;

    /**
     * Return all permission.
     *
     * @return all permissions
     */
    public static List<String> getAllAuthorities() {
        final List<String> allPermissions = new ArrayList<>();
        final Field[] declaredFields = SpPermission.class.getDeclaredFields();
        for (final Field field : declaredFields) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) &&
                    String.class.equals(field.getType())) {
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
}