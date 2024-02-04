/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A json annotated rest model for NamedEntity to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class MgmtNamedEntity extends MgmtBaseEntity {

    @JsonProperty(required = true)
    @Schema(example = "Name of entity")
    private String name;
    @JsonProperty
    @Schema(example = "Description of entity")
    private String description;
}