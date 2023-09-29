/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.targettype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

/**
 * A json annotated rest model for TargetType to RESTful API
 * representation.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetType extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "26")
    private Long typeId;

    @JsonProperty
    @Schema(example = "rgb(255,255,255")
    private String colour;

    /**
     * @return target type ID
     */
    public Long getTypeId() {
        return typeId;
    }

    /**
     * @param typeId
     *          Target type ID
     */
    public void setTypeId(final Long typeId) {
        this.typeId = typeId;
    }

    /**
     * 
     * @return the colour
     */
    public String getColour() {
        return colour;
    }

    /**
     * @param colour
     *            the colour to set
     */
    public void setColour(String colour) {
        this.colour = colour;
    }
}
