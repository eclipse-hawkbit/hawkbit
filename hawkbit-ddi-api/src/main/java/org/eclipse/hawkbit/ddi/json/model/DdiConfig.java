/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

/**
 * Standard configuration for the target.
 */
public class DdiConfig {

    private final DdiPolling polling;

    /**
     * Constructor.
     *
     * @param polling
     *            configuration of the SP target
     */
    public DdiConfig(final DdiPolling polling) {
        super();
        this.polling = polling;
    }

    public DdiPolling getPolling() {
        return polling;
    }

}
