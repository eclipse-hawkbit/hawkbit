/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for System Configuration for PUT.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemTenantConfigurationValueRequest {

    @JsonProperty(required = true)
    private Object value;

    /**
     * 
     * @return the value of the MgmtSystemTenantConfigurationValueRequest
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the MgmtSystemTenantConfigurationValueRequest
     * 
     * @param value
     */
    public void setValue(final Object value) {
        this.value = value;
    }

}
