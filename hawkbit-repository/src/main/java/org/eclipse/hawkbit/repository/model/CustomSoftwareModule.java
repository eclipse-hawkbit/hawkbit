/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;

/**
 * Use to display software modules for the selected distribution.
 *
 *
 *
 *
 */
public class CustomSoftwareModule implements Serializable {

    private static final long serialVersionUID = 6144585781451168439L;

    private final SoftwareModule softwareModule;

    private final boolean assigned;

    /**
     * Constructor.
     *
     * @param softwareModule
     *            entity.
     * @param assigned
     *            as true if the software module is assigned and false if not
     *            assigned.
     */
    public CustomSoftwareModule(final SoftwareModule softwareModule, final boolean assigned) {
        this.softwareModule = softwareModule;
        this.assigned = assigned;
    }

    /**
     * @return the softwareModule
     */
    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    /**
     * @return the assigned
     */
    public boolean isAssigned() {
        return assigned;
    }
}
