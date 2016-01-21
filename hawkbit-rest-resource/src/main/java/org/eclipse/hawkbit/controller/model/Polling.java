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
 * Polling interval for the SP target.
 *
 *
 *
 *
 *
 */
@ApiModel(value = ApiModelProperties.TARGET_POLL_TIME)
public class Polling {

    @ApiModelProperty(value = ApiModelProperties.TARGET_SLEEP)
    private final String sleep;

    /**
     * Constructor.
     *
     * @param sleep
     *            between polls
     */
    public Polling(final String sleep) {
        super();
        this.sleep = sleep;
    }

    public String getSleep() {
        return sleep;
    }

}
