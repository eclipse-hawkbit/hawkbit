/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.rolloutgroup;

import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for the rollout group annotated with json-annotations for easier
 * serialization and de-serialization.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutGroupResponseBody extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    private Long rolloutGroupId;

    @JsonProperty(required = true)
    private String status;

    /**
     * @return the rolloutGroupId
     */
    public Long getRolloutGroupId() {
        return rolloutGroupId;
    }

    /**
     * @param rolloutGroupId
     *            the rolloutGroupId to set
     */
    public void setRolloutGroupId(final Long rolloutGroupId) {
        this.rolloutGroupId = rolloutGroupId;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
    }
}
