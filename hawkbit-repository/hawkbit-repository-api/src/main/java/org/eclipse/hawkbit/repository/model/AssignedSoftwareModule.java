/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;
import org.eclipse.hawkbit.repository.Identifiable;

/**
 * Use to display software modules for the selected distribution.
 */
@Data
public class AssignedSoftwareModule implements Serializable, Identifiable<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final SoftwareModule softwareModule;

    private final boolean assigned;

    /**
     * Constructor.
     *
     * @param softwareModule entity.
     * @param assigned as true if the software module is assigned and false if not
     *         assigned.
     */
    public AssignedSoftwareModule(final SoftwareModule softwareModule, final boolean assigned) {
        this.softwareModule = softwareModule;
        this.assigned = assigned;
    }

    @Override
    public Long getId() {
        return softwareModule.getId();
    }
}
