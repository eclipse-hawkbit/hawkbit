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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

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

    @JsonProperty
    private Long softwareModuleId;

    @Setter(AccessLevel.NONE)
    @JsonProperty
    private List<String> message;

    @JsonProperty
    private Integer code;

    public DmfActionUpdateStatus(@JsonProperty(value = "actionId", required = true) final Long actionId,
            @JsonProperty(value = "actionStatus", required = true) final DmfActionStatus actionStatus, @JsonProperty(value = "timestamp", required = false) final Long timestamp) {
        this.actionId = actionId;
        this.actionStatus = actionStatus;
        this.timestamp = timestamp != null ? timestamp : System.currentTimeMillis();
    }

    @JsonIgnore
    public Optional<Integer> getCode() {
        return Optional.ofNullable(code);
    }

    public List<String> getMessage() {
        if (message == null) {
            return Collections.emptyList();
        }

        return message;
    }

    public boolean addMessage(final String message) {
        if (this.message == null) {
            this.message = new ArrayList<>();
        }

        return this.message.add(message);
    }

    public boolean addMessage(final Collection<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }

        if (message == null) {
            message = new ArrayList<>(messages);
            return true;
        }

        return message.addAll(messages);
    }
}