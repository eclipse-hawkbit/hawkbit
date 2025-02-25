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
import org.springframework.hateoas.RepresentationModel;

/**
 * A json annotated rest model for BaseEntity to RESTful API representation.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class MgmtBaseEntity extends RepresentationModel<MgmtBaseEntity> {

    @Schema(description = "Entity was originally created by (User, AMQP-Controller, anonymous etc.)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "bumlux")
    private String createdBy;

    @Schema(description = "Entity was originally created at (timestamp UTC in milliseconds)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "1691065905897")
    private Long createdAt;

    @Schema(description = "Entity was last modified by (User, AMQP-Controller, anonymous etc.)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "bumlux")
    private String lastModifiedBy;

    @Schema(description = "Entity was last modified at (timestamp UTC in milliseconds)",
            accessMode = Schema.AccessMode.READ_ONLY, example = "1691065906407")
    @EqualsAndHashCode.Exclude
    private Long lastModifiedAt;
}