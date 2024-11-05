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

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Thrown if user tries to add a {@link SoftwareModule} to a
 * {@link DistributionSet} that is not defined by the
 * {@link DistributionSetType}.
 */
public class UnsupportedSoftwareModuleForThisDistributionSetException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new UnsupportedSoftwareModuleForThisDistributionSetException
     * with {@link SpServerError#SP_DS_MODULE_UNSUPPORTED} error.
     */
    public UnsupportedSoftwareModuleForThisDistributionSetException() {
        super(SpServerError.SP_DS_MODULE_UNSUPPORTED);
    }

    /**
     * @param cause for the exception
     */
    public UnsupportedSoftwareModuleForThisDistributionSetException(final Throwable cause) {
        super(SpServerError.SP_DS_MODULE_UNSUPPORTED, cause);
    }

    /**
     * @param message of the error
     */
    public UnsupportedSoftwareModuleForThisDistributionSetException(final String message) {
        super(message, SpServerError.SP_DS_MODULE_UNSUPPORTED);
    }
}
