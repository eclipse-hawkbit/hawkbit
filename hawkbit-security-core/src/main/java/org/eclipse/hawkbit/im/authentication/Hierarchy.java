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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Hierarchy {

    public static final String DEFAULT =
            SpPermission.TARGET_HIERARCHY +
            SpPermission.REPOSITORY_HIERARCHY +
            SpPermission.TENANT_CONFIGURATION_HIERARCHY +
            SpRole.DEFAULT_ROLE_HIERARCHY;
}