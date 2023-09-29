/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiActivateAutoConfirmation {

    @JsonProperty(required = false)
    @Schema(example = "exampleUser")
    private final String initiator;

    @JsonProperty(required = false)
    @Schema(example = "exampleRemark")
    private final String remark;

    /**
     * Constructor.
     *
     * @param initiator
     *            can be null
     * @param remark
     *            can be null
     */
    @JsonCreator
    public DdiActivateAutoConfirmation(@JsonProperty(value = "initiator") final String initiator,
            @JsonProperty(value = "remark") final String remark) {
        this.initiator = initiator;
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "DdiActivateAutoConfirmation [initiator=" + initiator + ", remark=" + remark + ", toString()="
                + super.toString() + "]";
    }

    public String getInitiator() {
        return initiator;
    }

    public String getRemark() {
        return remark;
    }
}
