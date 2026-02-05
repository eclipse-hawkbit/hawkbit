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

/**
 * Interface for authentication validation.
 * Implementations can validate credentials against hawkBit or provide no-op validation.
 */
public interface AuthenticationValidator {

    /**
     * Validates the given authorization header.
     *
     * @param authHeader the Authorization header value
     * @return validation result
     */
    ValidationResult validate(String authHeader);

    /**
     * Result of authentication validation.
     */
    enum ValidationResult {
        /**
         * Credentials are valid (authenticated user).
         */
        VALID,

        /**
         * No credentials provided.
         */
        MISSING_CREDENTIALS,

        /**
         * Credentials are invalid (401 from hawkBit).
         */
        INVALID_CREDENTIALS,

        /**
         * hawkBit is unavailable or returned unexpected error.
         */
        HAWKBIT_ERROR
    }
}
