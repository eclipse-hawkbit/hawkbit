/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.rolloutgroup;

import java.util.HashMap;
import java.util.Map;

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
public class MgmtRolloutGroupResponseBody extends MgmtRolloutGroup {

    @JsonProperty(value = "id", required = true)
    private Long rolloutGroupId;

    @JsonProperty(required = true)
    private String status;

    private int totalTargets;

    private Map<String, Long> totalTargetsPerStatus;

    public Long getRolloutGroupId() {
        return rolloutGroupId;
    }

    public void setRolloutGroupId(final Long rolloutGroupId) {
        this.rolloutGroupId = rolloutGroupId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public int getTotalTargets() {
        return totalTargets;
    }

    public void setTotalTargets(final int totalTargets) {
        this.totalTargets = totalTargets;
    }

    public Map<String, Long> getTotalTargetsPerStatus() {
        return totalTargetsPerStatus;
    }

    public void addTotalTargetsPerStatus(final String status, final Long totalTargetCountByStatus) {
        if (totalTargetsPerStatus == null) {
            totalTargetsPerStatus = new HashMap<>();
        }

        totalTargetsPerStatus.put(status, totalTargetCountByStatus);
    }

}
