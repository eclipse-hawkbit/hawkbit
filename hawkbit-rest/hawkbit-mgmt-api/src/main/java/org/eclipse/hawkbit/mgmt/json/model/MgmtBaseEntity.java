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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for BaseEntity to RESTful API representation.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class MgmtBaseEntity extends RepresentationModel<MgmtBaseEntity> {

    @JsonProperty
    @Schema(description = "Entity was originally created by (User, AMQP-Controller, anonymous etc.)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "bumlux")
    private String createdBy;

    @JsonProperty
    @Schema(description = "Entity was originally created at (timestamp UTC in milliseconds)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "1691065905897")
    private Long createdAt;

    @JsonProperty
    @Schema(description = "Entity was last modified by (User, AMQP-Controller, anonymous etc.)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "bumlux")
    private String lastModifiedBy;

    @JsonProperty
    @Schema(description = "Entity was last modified at (timestamp UTC in milliseconds)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "1691065906407")
    private Long lastModifiedAt;

    /**
     * Added for backwards compatibility
     *
     * @return the unique identifier of the {@link MgmtBaseEntity}.
     */
    @JsonIgnore
    public Link getId() {
        return this.getRequiredLink("self");
    }
}