/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.systemmanagement;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Body for system statistics.
 */
@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemStatisticsRest {

    @JsonProperty
    private long overallTargets;
    @JsonProperty
    private long overallArtifacts;
    @JsonProperty
    private long overallArtifactVolumeInBytes;
    @JsonProperty
    private long overallActions;
    @JsonProperty
    private long overallTenants;
    @JsonProperty
    private List<MgmtSystemTenantServiceUsage> tenantStats;
}