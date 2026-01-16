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

import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;

/**
 * Request wrapper for target filter query management operations.
 * Reuses existing {@link MgmtTargetFilterQueryRequestBody} for CREATE/UPDATE data.
 *
 * @param operation the operation to perform (CREATE, UPDATE, DELETE)
 * @param filterId  the target filter query ID (required for UPDATE/DELETE)
 * @param body      the request body containing filter data (for CREATE/UPDATE)
 */
public record ManageTargetFilterRequest(
        Operation operation,
        Long filterId,
        MgmtTargetFilterQueryRequestBody body
) {}
