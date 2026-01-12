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

import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPut;

/**
 * Request wrapper for rollout management operations including CRUD and lifecycle.
 * Reuses existing {@link MgmtRolloutRestRequestBodyPost} for CREATE and
 * {@link MgmtRolloutRestRequestBodyPut} for UPDATE.
 *
 * @param operation  the operation to perform (CREATE, UPDATE, DELETE, START, PAUSE, STOP, RESUME, APPROVE, DENY, RETRY, TRIGGER_NEXT_GROUP)
 * @param rolloutId  the rollout ID (required for UPDATE/DELETE/lifecycle operations)
 * @param createBody the request body for CREATE operation
 * @param updateBody the request body for UPDATE operation
 * @param remark     optional remark for APPROVE/DENY operations
 */
public record ManageRolloutRequest(
        RolloutOperation operation,
        Long rolloutId,
        MgmtRolloutRestRequestBodyPost createBody,
        MgmtRolloutRestRequestBodyPut updateBody,
        String remark
) {}
