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
 * Standard configuration for the target.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfig {

    @JsonProperty
    private DdiPolling polling;

    /**
     * Constructor.
     *
     * @param polling
     *            configuration of the SP target
     */
    public DdiConfig(final DdiPolling polling) {
        this.polling = polling;
    }

    /**
     * Constructor.
     */
    public DdiConfig() {
        // needed for json create.
    }

    public DdiPolling getPolling() {
        return polling;
    }

}
