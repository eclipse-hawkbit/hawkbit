/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.audit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants related to security.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityLogger {

    public static final Logger LOGGER = LoggerFactory.getLogger("SERVER.SECURITY");
}