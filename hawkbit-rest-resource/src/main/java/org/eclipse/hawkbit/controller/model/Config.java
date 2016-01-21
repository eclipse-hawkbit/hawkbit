/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Standard configuration for the target.
 *
 *
 *
 *
 *
 */
@ApiModel(ApiModelProperties.TARGET_CONFIGURATION)
public class Config {

    @ApiModelProperty(value = ApiModelProperties.TARGET_POLL_TIME)
    private final Polling polling;

    /**
     * Constructor.
     *
     * @param polling
     *            configuration of the SP target
     */
    public Config(final Polling polling) {
        super();
        this.polling = polling;
    }

    public Polling getPolling() {
        return polling;
    }

}
