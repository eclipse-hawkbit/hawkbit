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
    private Long typeId;

    @JsonProperty
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
