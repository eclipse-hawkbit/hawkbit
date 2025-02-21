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

    // deprecated format from the times where os, application and runtime where statically defined
    @Schema(hidden = true)
    private MgmtSoftwareModuleAssignment os;

    @Schema(hidden = true)
    private MgmtSoftwareModuleAssignment runtime;

    @Schema(hidden = true)
    private MgmtSoftwareModuleAssignment application;
    // deprecated format - END

    private List<MgmtSoftwareModuleAssignment> modules;

    @Schema(description = "The type of the distribution set", example = "test_default_ds_type")
    private String type;
}