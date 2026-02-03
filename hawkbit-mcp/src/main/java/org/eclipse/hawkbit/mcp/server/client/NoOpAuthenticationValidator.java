/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.client;

import lombok.extern.slf4j.Slf4j;

/**
 * No-operation authentication validator that always returns VALID.
 * Used when authentication validation is disabled via configuration.
 */
@Slf4j
public class NoOpAuthenticationValidator implements AuthenticationValidator {

    public NoOpAuthenticationValidator() {
        log.info("Authentication validation disabled - using no-op validator");
    }

    @Override
    public ValidationResult validate(final String authHeader) {
        return ValidationResult.VALID;
    }
}
