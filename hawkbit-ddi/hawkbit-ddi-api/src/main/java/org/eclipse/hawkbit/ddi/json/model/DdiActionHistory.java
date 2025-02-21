/**
 * Copyright (c) 2017 Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;

/**
 * Provide action history information to the controller as part of response to
 * {@link DdiRootControllerRestApi#getControllerDeploymentBaseAction} and
 * {@link DdiRootControllerRestApi#getConfirmationBaseAction}:
 * <ol>
 *     <li>Current action status at the server</li>
 *     <li>List of messages from action history</li>
 * </ol>
 * that were sent to server earlier by the controller using {@link DdiActionFeedback}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "status", "messages" })
public class DdiActionHistory {

    @Schema(description = "Status of the deployment based on previous feedback by the device", example = "RUNNING")
    private final String status;

    @Schema(description = "Messages are previously sent to the feedback channel in LIFO order by the device. Note: The first status message is set by the system and describes the trigger of the deployment")
    private final List<String> messages;

    /**
     * Parameterized constructor for creating {@link DdiActionHistory}.
     *
     * @param status is the current action status at the server
     * @param messages is a list of messages retrieved from action history.
     */
    @JsonCreator
    public DdiActionHistory(
            @JsonProperty("status") final String status,
            @JsonProperty("messages") final List<String> messages) {
        this.status = status;
        this.messages = messages;
    }
}