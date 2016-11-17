/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if DS creation failed.
 *
 *
 *
 *
 */
public final class DistributionSetCreationFailedMissingMandatoryModuleException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_DS_CREATION_FAILED_MISSING_MODULE} error.
     */
    public DistributionSetCreationFailedMissingMandatoryModuleException() {
        super(SpServerError.SP_DS_CREATION_FAILED_MISSING_MODULE);
    }

    /**
     * @param cause
     *            for the exception
     */
    public DistributionSetCreationFailedMissingMandatoryModuleException(final Throwable cause) {
        super(SpServerError.SP_DS_CREATION_FAILED_MISSING_MODULE, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public DistributionSetCreationFailedMissingMandatoryModuleException(final String message) {
        super(message, SpServerError.SP_DS_CREATION_FAILED_MISSING_MODULE);
    }
}
