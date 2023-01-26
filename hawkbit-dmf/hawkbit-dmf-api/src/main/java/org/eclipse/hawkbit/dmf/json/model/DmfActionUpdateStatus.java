/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/**
 * JSON representation of action update status.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfActionUpdateStatus {

    private final Long actionId;
    private final DmfActionStatus actionStatus;

    @JsonProperty
    private Long softwareModuleId;

    @JsonProperty
    private List<String> message;

    @JsonProperty
    private Integer code;

    public DmfActionUpdateStatus(@JsonProperty(value = "actionId", required = true) final Long actionId,
            @JsonProperty(value = "actionStatus", required = true) final DmfActionStatus actionStatus) {
        this.actionId = actionId;
        this.actionStatus = actionStatus;
    }

    public Long getActionId() {
        return actionId;
    }

    public Long getSoftwareModuleId() {
        return softwareModuleId;
    }

    public void setSoftwareModuleId(final Long softwareModuleId) {
        this.softwareModuleId = softwareModuleId;
    }

    public DmfActionStatus getActionStatus() {
        return actionStatus;
    }

    @JsonIgnore
    public Optional<Integer> getCode() {
        return Optional.ofNullable(code);
    }

    public void setCode(final Integer code) {
        this.code = code;
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

    public void setCode(final int code) {
        this.code = code;
    }

}
