/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Payload to activate the auto-confirmation by given initiator and remark.
 */
@Data
@Accessors(chain = true)
@ToString
public class MgmtTargetAutoConfirmUpdate {

    @JsonProperty
    @Schema(description = "(Optional) Initiator set on activation", example = "custom_initiator_value")
    private final String initiator;

    @JsonProperty
    @Schema(description = "(Optional) Remark set on activation", example = "custom_remark")
    private final String remark;

    /**
     * Constructor.
     *
     * @param initiator can be null
     * @param remark can be null
     */
    @JsonCreator
    public MgmtTargetAutoConfirmUpdate(
            @JsonProperty(value = "initiator") final String initiator, @JsonProperty(value = "remark") final String remark) {
        this.initiator = initiator;
        this.remark = remark;
    }
}