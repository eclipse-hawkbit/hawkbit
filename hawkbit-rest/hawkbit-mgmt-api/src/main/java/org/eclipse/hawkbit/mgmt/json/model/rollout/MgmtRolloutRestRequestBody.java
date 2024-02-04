/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.rollout;

import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for request containing a rollout body e.g. in a POST request of
 * creating a rollout via REST API.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutRestRequestBody extends AbstractMgmtRolloutConditionsEntity {

    @Schema(example = "id==targets-*")
    private String targetFilterQuery;
    @Schema(example = "6")
    private long distributionSetId;
    @Schema(example = "5")
    private Integer amountGroups;
    @Schema(example = "1691065781929")
    private Long forcetime;
    @Schema(example = "1691065780929")
    private Long startAt;
    @JsonProperty
    @Schema(example = "400")
    private Integer weight;
    @JsonProperty
    @Schema(example = "true")
    private boolean dynamic;
    @JsonProperty
    @Schema(example = "false")
    private Boolean confirmationRequired;
    private MgmtActionType type;
    private List<MgmtRolloutGroup> groups;
}
