/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * <p>
 * After the SP Target has executed an action, received by a GET(URL) request it
 * reports the completion of it to the SP Server with a action status message,
 * i.e. with a PUT message to the feedback channel, i.e. PUT URL/feedback. This
 * message could be used not only at the end of execution but also as status
 * updates during a longer lasting execution period. The format of each action
 * answer message is defined below at each action. But it is expected, that the
 * contents of the message answers have all a similar structure: The content
 * starts with a generic header and additional elements. *
 * </p>
 *
 * <p>
 * The answer header would look like: { "id": "51659181", "time":
 * "20140511T121314", "status": { "execution": "closed", "result": { "final":
 * "success", "progress": {} } "details": [], } }
 * </p>
 *
 *
 *
 *
 *
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("SP Target Action Feedback")
public class ActionFeedback {

    @ApiModelProperty(value = ApiModelProperties.ACTION_ID)
    private final Long id;

    @ApiModelProperty(value = ApiModelProperties.TARGET_TIME)
    private final String time;

    @ApiModelProperty(value = ApiModelProperties.TARGET_STATUS, required = true)
    @NotNull
    private final Status status;

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
    public ActionFeedback(@JsonProperty("id") final Long id, @JsonProperty("time") final String time,
            @JsonProperty("status") final Status status) {
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

    public Status getStatus() {
        return status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ActionFeedback [id=" + id + ", time=" + time + ", status=" + status + "]";
    }

}
