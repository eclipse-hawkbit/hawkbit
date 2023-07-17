/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetStatistics {
  private static final String TOTAL = "total";

  @JsonProperty("actions")
  private Map<String, Long> totalActionsPerStatus;

  @JsonProperty("rollouts")
  private Map<String, Long> totalRolloutsPerStatus;

  @JsonProperty
  private Long totalAutoAssignments;
  private MgmtDistributionSetStatistics() {
    // Private constructor to enforce the use of the builder pattern
  }

  public Map<String, Long> getTotalActionsPerStatus() {
    return totalActionsPerStatus;
  }

  public Map<String, Long> getTotalRolloutsPerStatus() {
    return totalRolloutsPerStatus;
  }

  public Long getTotalAutoAssignments() {
    return totalAutoAssignments;
  }

  public static class Builder {
    private final Map<String, Long> totalActionsPerStatus;
    private final Map<String, Long> totalRolloutsPerStatus;
    private Long totalAutoAssignments;
    private final boolean fullRepresentation;

    public Builder(boolean fullRepresentation) {
      totalActionsPerStatus = new HashMap<>();
      totalRolloutsPerStatus = new HashMap<>();
      this.fullRepresentation = fullRepresentation;
    }

    public Builder addTotalActionPerStatus(String status, Long count) {
      totalActionsPerStatus.put(status, count);
      return this;
    }

    public Builder addTotalRolloutPerStatus(String status, Long count) {
      totalRolloutsPerStatus.put(status, count);
      return this;
    }

    public Builder addTotalAutoAssignments(Long count) {
      totalAutoAssignments = count;
      return this;
    }

    public MgmtDistributionSetStatistics build() {
      MgmtDistributionSetStatistics statistics = new MgmtDistributionSetStatistics();
      statistics.totalActionsPerStatus = calculateTotalWithStatus(totalActionsPerStatus);
      statistics.totalRolloutsPerStatus = calculateTotalWithStatus(totalRolloutsPerStatus);
      statistics.totalAutoAssignments = fullRepresentation ? (totalAutoAssignments == null ?  Long.valueOf(0) : totalAutoAssignments) : totalAutoAssignments;
      return statistics;
    }

    private Map<String, Long> calculateTotalWithStatus(Map<String, Long> statusMap) {
      if (!fullRepresentation) {
        if (statusMap.isEmpty()) {
          return statusMap;
        }
      }
      long total = statusMap.values().stream().mapToLong(Long::longValue).sum();
      statusMap.put(TOTAL, total);
      return statusMap;
    }
  }
}

