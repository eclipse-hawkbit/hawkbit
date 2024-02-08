/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
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

/**
 * A json annotated rest model for Type to RESTful API representation.
 *
 */
public abstract class MgmtTypeEntity extends MgmtNamedEntity {

    @JsonProperty(required = true)
    @Schema(name = "Key that can be interpreted by the target", example = "id.t23")
    private String key;

    @JsonProperty
    @Schema(description = "Colour assigned to the entity that could be used for representation purposes",
            example = "brown")
    private String colour;

    @JsonProperty
    @Schema(description = "Deleted flag, used for soft deleted entities", example = "false")
    private boolean deleted;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }
}
