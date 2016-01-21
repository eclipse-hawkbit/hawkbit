/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import org.springframework.hateoas.ResourceSupport;

/**
 * {@link ControllerBase} resource content.
 *
 */
public class ControllerBase extends ResourceSupport {

    private final Config config;

    /**
     * Constructor.
     *
     * @param config
     *            configuration of the SP target
     */
    public ControllerBase(final Config config) {
        super();
        this.config = config;
    }

    /**
     * @return the config
     */
    public Config getConfig() {
        return config;
    }

}
