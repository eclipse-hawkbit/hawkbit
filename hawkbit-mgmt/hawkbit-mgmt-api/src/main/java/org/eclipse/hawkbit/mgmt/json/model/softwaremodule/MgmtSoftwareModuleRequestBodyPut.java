/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremodule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Request Body for SoftwareModule PUT.
 */
@Data
@Accessors(chain = true)
@ToString
public class MgmtSoftwareModuleRequestBodyPut {

    @Schema(example = "SM Description")
    private String description;

    @Schema(example = "SM Vendor Name")
    private String vendor;

    @Schema(description = """
            Should be set only if change of locked state is requested. If put, the software module locked flag will be
            set to the requested. Note: unlock (i.e. set this property to false) with extreme care!
            In general once software module is locked it shall not be unlocked. Note that it could have been assigned /
            deployed to targets.""",
            example = "true")
    private Boolean locked;
}