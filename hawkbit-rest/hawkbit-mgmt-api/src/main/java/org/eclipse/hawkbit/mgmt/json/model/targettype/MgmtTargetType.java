/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    private Long moduleId;

    @JsonProperty
    private String key;

    @JsonProperty
    private boolean deleted;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(final Long moduleId) {
        this.moduleId = moduleId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

}
