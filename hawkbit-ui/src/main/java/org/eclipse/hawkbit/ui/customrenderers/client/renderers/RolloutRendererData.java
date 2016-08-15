/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.io.Serializable;

/**
 * RendererData class with name and status.
 */
public class RolloutRendererData implements Serializable {

    private static final long serialVersionUID = -5018181529953620263L;

    private String name;

    private String status;

    /**
     * Initialize the RendererData empty.
     */
    public RolloutRendererData() {
        // Needed by Vaadin for compiling the widget set.
    }

    /**
     * Initialize the RendererData.
     *
     * @param name
     *            Name of the Rollout.
     * @param status
     *            Status of Rollout.
     */
    public RolloutRendererData(final String name, final String status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }
}
