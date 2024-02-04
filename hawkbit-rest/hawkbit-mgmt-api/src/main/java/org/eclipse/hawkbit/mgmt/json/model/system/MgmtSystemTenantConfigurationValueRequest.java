/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.system;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * A json annotated rest model for System Configuration for PUT.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemTenantConfigurationValueRequest {

    @Getter
    @JsonProperty(required = true)
    @Schema(example = "exampleToken")
    private Serializable value;


    public void setValue(final Object value) {
        if (!(value instanceof Serializable)) {
            throw new IllegalArgumentException("The value must be a instance of " + Serializable.class.getName());
        }
        this.value = (Serializable) value;
    }
}
