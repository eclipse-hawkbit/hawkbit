/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.lang.annotation.Target;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * the {@link SoftwareModuleNotAssignedToTargetException} is thrown when a
 * {@link SoftwareModule} is requested as part of an {@link Action} that has
 * however never been assigned to the {@link Target}.
 */
public class SoftwareModuleNotAssignedToTargetException extends EntityNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param moduleId thats is not assigned to given {@link Target}
     * @param controllerId of the {@link Target} where given {@link SoftwareModule} is
     *         not part of
     */
    public SoftwareModuleNotAssignedToTargetException(final Long moduleId, final String controllerId) {
        super("No assignment found for " + SoftwareModule.class.getSimpleName() + " with id {" + moduleId + "} to "
                + Target.class.getSimpleName() + " with id {" + controllerId + "}.");
    }

}
