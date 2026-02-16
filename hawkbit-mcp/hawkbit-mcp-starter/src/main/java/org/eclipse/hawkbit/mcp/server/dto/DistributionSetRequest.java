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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPut;

/**
 * Sealed interface for distribution set management operations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DistributionSetRequest.Create.class, name = "Create"),
        @JsonSubTypes.Type(value = DistributionSetRequest.Update.class, name = "Update"),
        @JsonSubTypes.Type(value = DistributionSetRequest.Delete.class, name = "Delete")
})
public sealed interface DistributionSetRequest
        permits DistributionSetRequest.Create, DistributionSetRequest.Update, DistributionSetRequest.Delete {

    /**
     * Request to create a new distribution set.
     *
     * @param body the request body containing distribution set data (name, version, type)
     */
    record Create(MgmtDistributionSetRequestBodyPost body) implements DistributionSetRequest {}

    /**
     * Request to update an existing distribution set.
     *
     * @param distributionSetId the distribution set ID
     * @param body              the request body containing updated distribution set data
     */
    record Update(Long distributionSetId, MgmtDistributionSetRequestBodyPut body) implements DistributionSetRequest {}

    /**
     * Request to delete a distribution set.
     *
     * @param distributionSetId the distribution set ID to delete
     */
    record Delete(Long distributionSetId) implements DistributionSetRequest {}
}
