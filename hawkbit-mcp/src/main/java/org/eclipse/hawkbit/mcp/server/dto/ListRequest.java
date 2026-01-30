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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Common request parameters for list operations.
 */
public record ListRequest(
        @JsonPropertyDescription("RSQL filter query (e.g., 'name==test*')")
        String rsql,

        @JsonPropertyDescription("Number of items to skip (default: 0)")
        Integer offset,

        @JsonPropertyDescription("Maximum number of items to return (default: 50)")
        Integer limit
) {

    public static final int DEFAULT_OFFSET = 0;
    public static final int DEFAULT_LIMIT = 50;

    public int getOffsetOrDefault() {
        return offset != null ? offset : DEFAULT_OFFSET;
    }

    public int getLimitOrDefault() {
        return limit != null ? limit : DEFAULT_LIMIT;
    }

    public String getRsqlOrNull() {
        return rsql != null && !rsql.isBlank() ? rsql : null;
    }
}
