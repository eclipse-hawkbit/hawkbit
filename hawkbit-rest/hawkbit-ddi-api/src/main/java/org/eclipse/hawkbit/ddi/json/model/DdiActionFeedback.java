/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
 * The answer header would look like: { "id": "51659181", "time":
 * "20140511T121314", "status": { "execution": "closed", "result": { "final":
 * "success", "progress": {} } "details": [], } }
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiActionFeedback {
    private final Long id;
    private final String time;

    @NotNull
    @Valid
    private final DdiStatus status;

    /**
     * Constructor.
     *
     * @param id
     *            of the actions the feedback is for
     * @param time
     *            of the feedback
     * @param status
     *            is the feedback itself
     */
    @JsonCreator
    public DdiActionFeedback(@JsonProperty("id") final Long id, @JsonProperty("time") final String time,
            @JsonProperty("status") final DdiStatus status) {
        this.id = id;
        this.time = time;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTime() {
        return time;
    }

    public DdiStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ActionFeedback [id=" + id + ", time=" + time + ", status=" + status + "]";
    }

}
