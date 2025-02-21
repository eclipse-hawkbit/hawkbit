/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation of auto confirmation config.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfAutoConfirmation {

    private final boolean enabled;
    private final String initiator;
    private final String remark;

    @JsonCreator
    public DmfAutoConfirmation(
            @JsonProperty("enabled") final boolean enabled,
            @JsonProperty("initiator") final String initiator,
            @JsonProperty("remark") final String remark) {
        this.enabled = enabled;
        this.initiator = initiator;
        this.remark = remark;
    }
}