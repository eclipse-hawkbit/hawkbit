/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A json annotated rest model for ActionStatus to RESTful API representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtActionStatus {

    @JsonProperty("id")
    @Schema(example = "21")
    private Long statusId;

    @JsonProperty
    @Schema(example = "running")
    private String type;

    @JsonProperty
    private List<String> messages;

    @JsonProperty
    @Schema(example = "1691065929524")
    private Long reportedAt;    
    
    @JsonProperty
    @Schema(example = "200")
    private Integer code;

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

    /**
     * @return the code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * @param code
     *            the reported code to set
     */
    public void setCode(final Integer code) {
        this.code = code;
    }
}
