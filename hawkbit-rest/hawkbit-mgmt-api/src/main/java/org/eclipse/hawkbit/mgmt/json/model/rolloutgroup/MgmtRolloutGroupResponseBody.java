/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.rolloutgroup;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Model for the rollout group annotated with json-annotations for easier
 * serialization and de-serialization.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutGroupResponseBody extends MgmtRolloutGroup {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "63")
    private Long rolloutGroupId;

    @JsonProperty(required = true)
    @Schema(example = "ready")
    private String status;

    @Schema(example = "4")
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
