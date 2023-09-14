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

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link DdiControllerBase} resource content.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiControllerBase extends RepresentationModel<DdiControllerBase> {

    @JsonProperty
    private DdiConfig config;

    /**
     * Constructor.
     *
     * @param config
     *            configuration of the SP target
     */
    public DdiControllerBase(final DdiConfig config) {
        this.config = config;
    }

    public DdiControllerBase() {
        // needed for json create
    }

    public DdiConfig getConfig() {
        return config;
    }

}
