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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.rollout.AbstractMgmtRolloutConditionsEntity;

/**
 * Model for defining the Attributes of a Rollout Group
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutGroup extends AbstractMgmtRolloutConditionsEntity {

    @Schema(description = "The name of the entity", example = "controllerId==exampleTarget*")
    private String targetFilterQuery;

    @Schema(description = "Percentage of remaining and matching targets that should be added to this group",
            example = "20.0")
    private Float targetPercentage;

    @Schema(description = "(Available with user consent flow active) If the confirmation is required for this " +
            "rollout group. Confirmation is required per default", example = "false")
    private Boolean confirmationRequired;
}