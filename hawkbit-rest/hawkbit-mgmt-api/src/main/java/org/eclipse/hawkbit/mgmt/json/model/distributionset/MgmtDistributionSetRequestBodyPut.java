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
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty
    @Schema(example = "dsOne")
    private String name;
    @JsonProperty
    @Schema(example = "Description of the distribution set.")
    private String description;
    @JsonProperty
    @Schema(example = "1.0.0")
    private String version;
    @JsonProperty
    @Schema(example = "false")
    private Boolean requiredMigrationStep;
}
