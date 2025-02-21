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
import lombok.ToString;

/**
 * Json representation of Target used in batch download and update request.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfTarget {

    private final Long actionId;
    private final String controllerId;
    @ToString.Exclude
    private final String targetSecurityToken;

    @JsonCreator
    public DmfTarget(
            @JsonProperty("actionId") final Long actionId,
            @JsonProperty("controllerId") final String controllerId,
            @JsonProperty("targetSecurityToken") final String targetSecurityToken) {
        this.actionId = actionId;
        this.controllerId = controllerId;
        this.targetSecurityToken = targetSecurityToken;
    }
}