/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation of action update status.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfActionUpdateStatus {

    private final Long actionId;
    private final DmfActionStatus actionStatus;
    private final long timestamp;
    private final Long softwareModuleId;
    private final List<String> message;
    private final Integer code;

    @JsonCreator
    public DmfActionUpdateStatus(
            @JsonProperty(value = "actionId", required = true) final Long actionId,
            @JsonProperty(value = "actionStatus", required = true) final DmfActionStatus actionStatus,
            @JsonProperty(value = "timestamp") final Long timestamp,
            @JsonProperty("softwareModuleId") final Long softwareModuleId,
            @JsonProperty("message") final List<String> message,
            @JsonProperty("code") final Integer code) {
        this.actionId = actionId;
        this.actionStatus = actionStatus;
        this.timestamp = timestamp != null ? timestamp : System.currentTimeMillis();
        this.softwareModuleId = softwareModuleId;
        this.message = message;
        this.code = code;
    }

    public DmfActionUpdateStatus(final Long actionId, final DmfActionStatus actionStatus) {
        this(actionId, actionStatus, null, null, null, 0);
    }

    public List<String> getMessage() {
        if (message == null) {
            return Collections.emptyList();
        }

        return message;
    }
}