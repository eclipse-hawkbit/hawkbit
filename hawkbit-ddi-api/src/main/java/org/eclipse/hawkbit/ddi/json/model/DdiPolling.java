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
 * Polling interval for the SP target.
 */
public class DdiPolling {

    private final String sleep;

    /**
     * Constructor.
     *
     * @param sleep
     *            between polls
     */
    public DdiPolling(final String sleep) {
        super();
        this.sleep = sleep;
    }

    public String getSleep() {
        return sleep;
    }

}
