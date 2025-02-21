/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A json annotated rest model for DistributionSet for PUT/POST.
 */
@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetRequestBodyPut {

    @Schema(description = "The name of the entity", example = "dsOne")
    private String name;

    @Schema(description = "The description of the entity", example = "Description of the distribution set.")
    private String description;

    @Schema(description = "Package version", example = "1.0.0")
    private String version;

    @Schema(description = """
            Should be set only if change of locked state is requested. If put, the distribution set locked flag will be
            set to the requested. Note: unlock (i.e. set this property to false) with extreme care!
            In general once distribution set is locked it shall not be unlocked. Note that it could have been assigned /
            deployed to targets.""",
            example = "true")
    private Boolean locked;

    @Schema(description = """
            True if DS is a required migration step for another DS. As a result the DS’s assignment will not be cancelled
            when another DS is assigned (note: updatable only if DS is not yet assigned to a target)""", example = "false")
    private Boolean requiredMigrationStep;
}