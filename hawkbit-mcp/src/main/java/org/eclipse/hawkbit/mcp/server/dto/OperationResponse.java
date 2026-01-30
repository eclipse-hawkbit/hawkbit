/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.dto;

/**
 * Unified response wrapper for management operations.
 *
 * @param <T>       the type of the data payload
 * @param operation the operation that was performed
 * @param success   whether the operation was successful
 * @param message   optional message (typically for success confirmations or error details)
 * @param data      the operation result data (e.g., created/updated entity)
 */
public record OperationResponse<T>(
        String operation,
        boolean success,
        String message,
        T data
) {

    /**
     * Creates a successful response with data.
     */
    public static <T> OperationResponse<T> success(final String operation, final T data) {
        return new OperationResponse<>(operation, true, null, data);
    }

    /**
     * Creates a successful response with a message (no data).
     */
    public static <T> OperationResponse<T> success(final String operation, final String message) {
        return new OperationResponse<>(operation, true, message, null);
    }

    /**
     * Creates a successful response with both message and data.
     */
    public static <T> OperationResponse<T> success(final String operation, final String message, final T data) {
        return new OperationResponse<>(operation, true, message, data);
    }

    /**
     * Creates a failure response with an error message.
     */
    public static <T> OperationResponse<T> failure(final String operation, final String message) {
        return new OperationResponse<>(operation, false, message, null);
    }
}
