/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for ActionStatus to RESTful API representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtActionStatus {

    @JsonProperty("id")
    private Long statusId;

    @JsonProperty
    private String type;

    @JsonProperty
    private List<String> messages;

    @JsonProperty
    private Long reportedAt;

    /**
     * @return the statusId
     */
    public Long getStatusId() {
        return statusId;
    }

    /**
     * @param statusId
     *            the statusId to set
     */
    public void setStatusId(final Long statusId) {
        this.statusId = statusId;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * @return the messages
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * @param messages
     *            the messages to set
     */
    public void setMessages(final List<String> messages) {
        this.messages = messages;
    }

    /**
     * @return the reportedAt
     */
    public Long getReportedAt() {
        return reportedAt;
    }

    /**
     * @param reportedAt
     *            the reportedAt to set
     */
    public void setReportedAt(final Long reportedAt) {
        this.reportedAt = reportedAt;
    }

}
