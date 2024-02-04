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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for BaseEntity to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class MgmtBaseEntity extends RepresentationModel<MgmtBaseEntity> {

    @JsonProperty
    @Schema(example = "bumlux")
    private String createdBy;
    @JsonProperty
    @Schema(example = "1691065905897")
    private Long createdAt;
    @JsonProperty
    @Schema(example = "bumlux")
    private String lastModifiedBy;
    @JsonProperty
    @Schema(example = "1691065906407")
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

    @JsonIgnore
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @param createdAt the createdAt to set
     */
    @JsonIgnore
    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    @JsonIgnore
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @param lastModifiedAt
     *            the lastModifiedAt to set
     */
    @JsonIgnore
    public void setLastModifiedAt(final Long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}