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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssignment;

/**
 * A json annotated rest model for DistributionSet for POST.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetRequestBodyPost extends MgmtDistributionSetRequestBodyPut {

    @Schema(description = "Array of Software Modules assigned to the distribution set. " +
            "Software Modules assigned to Distribution Set would define the 'complete' property of DistributionSet." +
            "Assigned Software Modules must be 'complete' in order DistributionSet to be 'complete'." +
            "Distribution Sets that are not 'complete' cannot be used in a Rollout/Auto-Assignment", example = """
            [
              {
                "id": 1
              },
              {
                "id": 2
              }
            ]""")
    private List<MgmtSoftwareModuleAssignment> modules;

    @Schema(description = "The type of the distribution set", example = "test_default_ds_type")
    private String type;
}