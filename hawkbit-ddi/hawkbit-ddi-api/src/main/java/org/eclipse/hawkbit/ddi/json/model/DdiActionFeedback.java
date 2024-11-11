/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * <p>
 * After the HawkBit Target has executed an action, received by a GET(URL)
 * request it reports the completion of it to the HawkBit Server with a action
 * status message, i.e. with a PUT message to the feedback channel, i.e. PUT
 * URL/feedback. This message could be used not only at the end of execution but
 * also as status updates during a longer lasting execution period. The format
 * of each action answer message is defined below at each action. But it is
 * expected, that the contents of the message answers have all a similar
 * structure: The content starts with a generic header and additional elements.
 * *
 * </p>
 *
 * <p>
 * The answer header would look like: { "time": "20140511T121314", "status": {
 * "execution": "closed", "result": { "final": "success", "progress": {} }
 * "details": [], } }
 * </p>
 */
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiActionFeedback {

    @Schema(description = "Timestamp of the action", example = "2023-08-03T12:31:41.890992967Z")
    private final String time;

    @NotNull
    @Valid
    private final DdiStatus status;

    /**
     * Constructs an action-feedback
     *
     * @param time time of feedback
     * @param status status to be appended to the action
     */
    @JsonCreator
    public DdiActionFeedback(
            @JsonProperty(value = "time") final String time,
            @JsonProperty(value = "status", required = true) final DdiStatus status) {
        this.time = time;
        this.status = status;
    }
}