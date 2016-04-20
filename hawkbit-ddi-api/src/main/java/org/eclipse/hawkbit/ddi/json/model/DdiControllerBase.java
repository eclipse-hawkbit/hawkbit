/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import org.springframework.hateoas.ResourceSupport;

/**
 * {@link DdiControllerBase} resource content.
 */
public class DdiControllerBase extends ResourceSupport {

    private final DdiConfig config;

    /**
     * Constructor.
     *
     * @param config
     *            configuration of the SP target
     */
    public DdiControllerBase(final DdiConfig config) {
        super();
        this.config = config;
    }

    public DdiConfig getConfig() {
        return config;
    }

}
