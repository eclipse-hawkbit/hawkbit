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
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;

/**
 * Sealed interface for target management operations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TargetRequest.Create.class, name = "Create"),
        @JsonSubTypes.Type(value = TargetRequest.Update.class, name = "Update"),
        @JsonSubTypes.Type(value = TargetRequest.Delete.class, name = "Delete")
})
public sealed interface TargetRequest
        permits TargetRequest.Create, TargetRequest.Update, TargetRequest.Delete {

    /**
     * Request to create a new target.
     *
     * @param body the request body containing target data (controllerId, name, description)
     */
    record Create(MgmtTargetRequestBody body) implements TargetRequest {}

    /**
     * Request to update an existing target.
     *
     * @param controllerId the target controller ID
     * @param body         the request body containing updated target data
     */
    record Update(String controllerId, MgmtTargetRequestBody body) implements TargetRequest {}

    /**
     * Request to delete a target.
     *
     * @param controllerId the target controller ID to delete
     */
    record Delete(String controllerId) implements TargetRequest {}
}
