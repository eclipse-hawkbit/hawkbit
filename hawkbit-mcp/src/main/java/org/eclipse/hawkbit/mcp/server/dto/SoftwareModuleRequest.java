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
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPut;

/**
 * Sealed interface for software module management operations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SoftwareModuleRequest.Create.class, name = "Create"),
        @JsonSubTypes.Type(value = SoftwareModuleRequest.Update.class, name = "Update"),
        @JsonSubTypes.Type(value = SoftwareModuleRequest.Delete.class, name = "Delete")
})
public sealed interface SoftwareModuleRequest
        permits SoftwareModuleRequest.Create, SoftwareModuleRequest.Update, SoftwareModuleRequest.Delete {

    /**
     * Request to create a new software module.
     *
     * @param body the request body containing software module data (name, version, type)
     */
    record Create(MgmtSoftwareModuleRequestBodyPost body) implements SoftwareModuleRequest {}

    /**
     * Request to update an existing software module.
     *
     * @param softwareModuleId the software module ID
     * @param body             the request body containing updated software module data
     */
    record Update(Long softwareModuleId, MgmtSoftwareModuleRequestBodyPut body) implements SoftwareModuleRequest {}

    /**
     * Request to delete a software module.
     *
     * @param softwareModuleId the software module ID to delete
     */
    record Delete(Long softwareModuleId) implements SoftwareModuleRequest {}
}
