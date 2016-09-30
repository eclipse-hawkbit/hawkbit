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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of action update status.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionUpdateStatus {
    @JsonProperty(required = true)
    private Long actionId;
    @JsonProperty
    private Long softwareModuleId;
    @JsonProperty(required = true)
    private ActionStatus actionStatus;
    @JsonProperty
    private List<String> message;

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }

    public Long getSoftwareModuleId() {
        return softwareModuleId;
    }

    public void setSoftwareModuleId(final Long softwareModuleId) {
        this.softwareModuleId = softwareModuleId;
    }

    public ActionStatus getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(final ActionStatus actionStatus) {
        this.actionStatus = actionStatus;
    }

    public List<String> getMessage() {
        if (message == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(message);
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

        if (this.message == null) {
            this.message = new ArrayList<>(messages);
            return true;
        }

        return this.message.addAll(messages);
    }

}
