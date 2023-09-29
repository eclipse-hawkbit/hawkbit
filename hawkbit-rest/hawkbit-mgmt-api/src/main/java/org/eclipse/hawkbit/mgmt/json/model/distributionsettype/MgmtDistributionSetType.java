/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionsettype;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for SoftwareModuleType to RESTful API
 * representation.
 *
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetType extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "99")
    private Long moduleId;

    @JsonProperty(required = true)
    @Schema(example = "os_app")
    private String key;
    
    @JsonProperty
    @Schema(example = "false")
    private boolean deleted;
    
    @JsonProperty
    @Schema(example = "black")
    private String colour;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * @return the moduleId
     */
    public Long getModuleId() {
        return moduleId;
    }

    /**
     * @param moduleId
     *            the moduleId to set
     */
    public void setModuleId(final Long moduleId) {
        this.moduleId = moduleId;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
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
