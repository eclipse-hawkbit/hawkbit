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

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPut;

/**
 * Request wrapper for distribution set management operations.
 * Reuses existing {@link MgmtDistributionSetRequestBodyPost} for CREATE and
 * {@link MgmtDistributionSetRequestBodyPut} for UPDATE.
 *
 * @param operation         the operation to perform (CREATE, UPDATE, DELETE)
 * @param distributionSetId the distribution set ID (required for UPDATE/DELETE)
 * @param createBody        the request body for CREATE operation
 * @param updateBody        the request body for UPDATE operation
 */
public record ManageDistributionSetRequest(
        Operation operation,
        Long distributionSetId,
        MgmtDistributionSetRequestBodyPost createBody,
        MgmtDistributionSetRequestBodyPut updateBody
) {}
