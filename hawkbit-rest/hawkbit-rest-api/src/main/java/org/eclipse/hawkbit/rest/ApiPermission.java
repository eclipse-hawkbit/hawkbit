/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Permission names to be used with {@link RequiredPermissions}.
 * <p/>
 * The API modules are also used by clients (SDK, UI, MCP) and do not depend on the server side
 * {@code org.eclipse.hawkbit.auth.SpPermission}, so the permission names are mirrored here. A test ensures that the
 * constants match exactly the {@code SpPermission} authorities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPermission {

    public static final String CREATE_TARGET = "CREATE_TARGET";
    public static final String READ_TARGET = "READ_TARGET";
    public static final String UPDATE_TARGET = "UPDATE_TARGET";
    public static final String DELETE_TARGET = "DELETE_TARGET";
    public static final String READ_TARGET_SECURITY_TOKEN = "READ_TARGET_SECURITY_TOKEN";

    public static final String CREATE_TARGET_TYPE = "CREATE_TARGET_TYPE";
    public static final String READ_TARGET_TYPE = "READ_TARGET_TYPE";
    public static final String UPDATE_TARGET_TYPE = "UPDATE_TARGET_TYPE";
    public static final String DELETE_TARGET_TYPE = "DELETE_TARGET_TYPE";

    public static final String CREATE_SOFTWARE_MODULE = "CREATE_SOFTWARE_MODULE";
    public static final String READ_SOFTWARE_MODULE = "READ_SOFTWARE_MODULE";
    public static final String UPDATE_SOFTWARE_MODULE = "UPDATE_SOFTWARE_MODULE";
    public static final String DELETE_SOFTWARE_MODULE = "DELETE_SOFTWARE_MODULE";
    public static final String READ_SOFTWARE_MODULE_ARTIFACT = "READ_SOFTWARE_MODULE_ARTIFACT";

    public static final String CREATE_SOFTWARE_MODULE_TYPE = "CREATE_SOFTWARE_MODULE_TYPE";
    public static final String READ_SOFTWARE_MODULE_TYPE = "READ_SOFTWARE_MODULE_TYPE";
    public static final String UPDATE_SOFTWARE_MODULE_TYPE = "UPDATE_SOFTWARE_MODULE_TYPE";
    public static final String DELETE_SOFTWARE_MODULE_TYPE = "DELETE_SOFTWARE_MODULE_TYPE";

    public static final String CREATE_DISTRIBUTION_SET = "CREATE_DISTRIBUTION_SET";
    public static final String READ_DISTRIBUTION_SET = "READ_DISTRIBUTION_SET";
    public static final String UPDATE_DISTRIBUTION_SET = "UPDATE_DISTRIBUTION_SET";
    public static final String DELETE_DISTRIBUTION_SET = "DELETE_DISTRIBUTION_SET";

    public static final String CREATE_DISTRIBUTION_SET_TYPE = "CREATE_DISTRIBUTION_SET_TYPE";
    public static final String READ_DISTRIBUTION_SET_TYPE = "READ_DISTRIBUTION_SET_TYPE";
    public static final String UPDATE_DISTRIBUTION_SET_TYPE = "UPDATE_DISTRIBUTION_SET_TYPE";
    public static final String DELETE_DISTRIBUTION_SET_TYPE = "DELETE_DISTRIBUTION_SET_TYPE";

    public static final String CREATE_AUTO_ASSIGNMENT = "CREATE_AUTO_ASSIGNMENT";
    public static final String READ_AUTO_ASSIGNMENT = "READ_AUTO_ASSIGNMENT";
    public static final String UPDATE_AUTO_ASSIGNMENT = "UPDATE_AUTO_ASSIGNMENT";
    public static final String DELETE_AUTO_ASSIGNMENT = "DELETE_AUTO_ASSIGNMENT";
    public static final String APPROVE_AUTO_ASSIGNMENT = "APPROVE_AUTO_ASSIGNMENT";
    public static final String HANDLE_AUTO_ASSIGNMENT = "HANDLE_AUTO_ASSIGNMENT";

    public static final String CREATE_ROLLOUT = "CREATE_ROLLOUT";
    public static final String READ_ROLLOUT = "READ_ROLLOUT";
    public static final String UPDATE_ROLLOUT = "UPDATE_ROLLOUT";
    public static final String DELETE_ROLLOUT = "DELETE_ROLLOUT";
    public static final String APPROVE_ROLLOUT = "APPROVE_ROLLOUT";
    public static final String HANDLE_ROLLOUT = "HANDLE_ROLLOUT";

    public static final String TENANT_CONFIGURATION = "TENANT_CONFIGURATION";
    public static final String CREATE_TENANT_CONFIGURATION = "CREATE_TENANT_CONFIGURATION";
    public static final String READ_TENANT_CONFIGURATION = "READ_TENANT_CONFIGURATION";
    public static final String UPDATE_TENANT_CONFIGURATION = "UPDATE_TENANT_CONFIGURATION";
    public static final String DELETE_TENANT_CONFIGURATION = "DELETE_TENANT_CONFIGURATION";
    public static final String READ_GATEWAY_SECURITY_TOKEN = "READ_GATEWAY_SECURITY_TOKEN";
}
