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

import java.util.List;

/**
 * Sealed interface for action management operations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ActionRequest.Delete.class, name = "Delete"),
        @JsonSubTypes.Type(value = ActionRequest.DeleteBatch.class, name = "DeleteBatch")
})
public sealed interface ActionRequest
        permits ActionRequest.Delete, ActionRequest.DeleteBatch {

    /**
     * Request to delete a single action.
     *
     * @param actionIds list containing the single action ID to delete (use single-element list)
     */
    record Delete(List<Long> actionIds) implements ActionRequest {}

    /**
     * Request to delete multiple actions.
     *
     * @param actionIds list of action IDs to delete (mutually exclusive with rsql)
     * @param rsql      RSQL filter query for selecting actions to delete (mutually exclusive with actionIds)
     */
    record DeleteBatch(List<Long> actionIds, String rsql) implements ActionRequest {}
}
