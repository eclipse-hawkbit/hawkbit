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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Json representation of Target used in batch download and update request.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfTarget {

    @JsonProperty
    private Long actionId;

    @JsonProperty
    private String controllerId;

    @JsonProperty
    private String targetSecurityToken;

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }

    public String getControllerId() {
        return controllerId;
    }

    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    public String getTargetSecurityToken() {
        return targetSecurityToken;
    }

    public void setTargetSecurityToken(final String targetSecurityToken) {
        this.targetSecurityToken = targetSecurityToken;
    }

    @Override
    public String toString() {
        return String.format(
                "DmfTarget [actionId=%d controllerId='%s']",
                actionId, controllerId);
    }
}
