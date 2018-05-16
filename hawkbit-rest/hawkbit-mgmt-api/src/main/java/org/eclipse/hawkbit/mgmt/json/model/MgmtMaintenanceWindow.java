/**
 * Copyright (c) Siemens AG, 2018
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON model for Management API to define the maintenance window.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtMaintenanceWindow extends MgmtMaintenanceWindowRequestBody {

    /**
     * Time in {@link TimeUnit#MILLISECONDS} of the next maintenance window
     * start
     */
    @JsonProperty
    private long nextStartAt;

    public long getNextStartAt() {
        return nextStartAt;
    }

    public void setNextStartAt(final long nextStartAt) {
        this.nextStartAt = nextStartAt;
    }
}
