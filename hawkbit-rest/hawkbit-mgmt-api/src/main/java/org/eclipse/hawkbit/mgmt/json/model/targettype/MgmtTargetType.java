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
import org.eclipse.hawkbit.mgmt.json.model.MgmtTypeEntity;

/**
 * A json annotated rest model for TargetType to RESTful API
 * representation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetType extends MgmtTypeEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "26")
    private Long typeId;

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
}
