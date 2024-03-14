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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A json annotated rest model for System Configuration for PUT.
 */
@Getter
@EqualsAndHashCode
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemTenantConfigurationValueRequest {

    @JsonProperty(required = true)
    @Schema(description = "Current value of of configuration parameter", example = "exampleToken")
    private Serializable value;

    public MgmtSystemTenantConfigurationValueRequest setValue(final Object value) {
        if (!(value instanceof Serializable)) {
            throw new IllegalArgumentException("The value must be a instance of " + Serializable.class.getName());
        }
        this.value = (Serializable) value;
        return this;
    }
}