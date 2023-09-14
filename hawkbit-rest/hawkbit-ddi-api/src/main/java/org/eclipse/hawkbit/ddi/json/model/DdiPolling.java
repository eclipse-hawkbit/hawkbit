/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Polling interval for the SP target.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiPolling {

    @JsonProperty
    private String sleep;

    /**
     * Constructor.
     *
     * @param sleep
     *            between polls
     */
    public DdiPolling(final String sleep) {
        this.sleep = sleep;
    }

    /**
     * Constructor.
     *
     */
    public DdiPolling() {
        // needed for json create
    }

    public String getSleep() {
        return sleep;
    }

}
