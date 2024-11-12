/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants related to security.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

    /**
     * Logger prefix used for security logging.
     */
    public static final String SECURITY_LOG_PREFIX = "server-security";
}