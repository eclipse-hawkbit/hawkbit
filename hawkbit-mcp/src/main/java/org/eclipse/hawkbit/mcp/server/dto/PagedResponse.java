/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.dto;

import java.util.List;

/**
 * Generic paged response for MCP tool results.
 *
 * @param <T> the type of items in the response
 */
public record PagedResponse<T>(
        List<T> content,
        long total,
        int offset,
        int limit
) {

    public static <T> PagedResponse<T> of(final List<T> content, final long total, final int offset, final int limit) {
        return new PagedResponse<>(content, total, offset, limit);
    }
}
