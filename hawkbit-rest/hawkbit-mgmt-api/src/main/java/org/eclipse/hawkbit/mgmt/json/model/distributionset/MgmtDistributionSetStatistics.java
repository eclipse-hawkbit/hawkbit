/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@ToString
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

    public static class Builder {

        private final Map<String, Long> totalActionsPerStatus;
        private final Map<String, Long> totalRolloutsPerStatus;
        private final boolean fullRepresentation;
        private Long totalAutoAssignments;

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
            statistics.totalAutoAssignments = calculateTotalAutoAssignments();
            return statistics;
        }

        private Map<String, Long> calculateTotalWithStatus(Map<String, Long> statusMap) {
            if (!fullRepresentation && statusMap.isEmpty()) {
                return statusMap;
            }

            long total = statusMap.values().stream().mapToLong(Long::longValue).sum();
            statusMap.put(TOTAL, total);
            return statusMap;
        }

        private Long calculateTotalAutoAssignments() {
            if (fullRepresentation) {
                return totalAutoAssignments == null ? Long.valueOf(0) : totalAutoAssignments;
            }

            return totalAutoAssignments;
        }
    }
}