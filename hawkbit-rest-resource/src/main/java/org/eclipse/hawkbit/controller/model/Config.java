/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

/**
 * Standard configuration for the target.
 *
 */
public class Config {

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
