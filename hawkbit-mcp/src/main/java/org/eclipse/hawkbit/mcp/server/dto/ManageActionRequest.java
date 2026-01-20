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

import java.util.List;

/**
 * Request wrapper for action management operations.
 * Actions are created indirectly via distribution set assignment, so only DELETE operations are supported.
 *
 * @param operation the operation to perform (DELETE, DELETE_BATCH)
 * @param actionId  the action ID (for DELETE single action)
 * @param actionIds list of action IDs (for DELETE_BATCH)
 * @param rsql      RSQL filter query (alternative for DELETE_BATCH)
 */
public record ManageActionRequest(
        ActionOperation operation,
        Long actionId,
        List<Long> actionIds,
        String rsql
) {}
