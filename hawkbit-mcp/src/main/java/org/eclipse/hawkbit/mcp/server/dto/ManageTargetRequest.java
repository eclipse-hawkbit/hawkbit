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

import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;

/**
 * Request wrapper for target management operations.
 * Reuses existing {@link MgmtTargetRequestBody} for CREATE/UPDATE data.
 *
 * @param operation    the operation to perform (CREATE, UPDATE, DELETE)
 * @param controllerId the target controller ID (required for UPDATE/DELETE, used as identifier for CREATE)
 * @param body         the request body containing target data (for CREATE/UPDATE)
 */
public record ManageTargetRequest(
        Operation operation,
        String controllerId,
        MgmtTargetRequestBody body
) {}
