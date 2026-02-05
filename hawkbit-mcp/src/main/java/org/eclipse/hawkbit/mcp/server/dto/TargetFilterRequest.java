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
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;

/**
 * Sealed interface for target filter query management operations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TargetFilterRequest.Create.class, name = "Create"),
        @JsonSubTypes.Type(value = TargetFilterRequest.Update.class, name = "Update"),
        @JsonSubTypes.Type(value = TargetFilterRequest.Delete.class, name = "Delete")
})
public sealed interface TargetFilterRequest
        permits TargetFilterRequest.Create, TargetFilterRequest.Update, TargetFilterRequest.Delete {

    /**
     * Request to create a new target filter.
     *
     * @param body the request body containing filter data (name, query)
     */
    record Create(MgmtTargetFilterQueryRequestBody body) implements TargetFilterRequest {}

    /**
     * Request to update an existing target filter.
     *
     * @param filterId the target filter ID
     * @param body     the request body containing updated filter data
     */
    record Update(Long filterId, MgmtTargetFilterQueryRequestBody body) implements TargetFilterRequest {}

    /**
     * Request to delete a target filter.
     *
     * @param filterId the target filter ID to delete
     */
    record Delete(Long filterId) implements TargetFilterRequest {}
}
