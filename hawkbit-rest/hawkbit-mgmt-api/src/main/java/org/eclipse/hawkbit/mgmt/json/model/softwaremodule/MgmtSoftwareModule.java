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
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for SoftwareModule to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModule extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "6")
    private Long moduleId;
    @JsonProperty(required = true)
    @Schema(example = "1.0.0")
    private String version;
    @JsonProperty(required = true)
    @Schema(example = "os")
    private String type;
    @Schema(example = "OS")
    private String typeName;
    @JsonProperty
    @Schema(example = "Vendor Limited, California")
    private String vendor;
    @JsonProperty
    @Schema(example = "false")
    private boolean deleted;
    @JsonProperty
    @Schema(example = "false")
    private boolean encrypted;
}