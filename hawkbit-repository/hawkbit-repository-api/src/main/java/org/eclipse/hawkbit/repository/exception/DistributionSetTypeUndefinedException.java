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
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;

/**
 * Thrown if the user tries to assign modules to a {@link DistributionSet} that
 * has to {@link DistributionSetType} defined.
 *
 *
 *
 *
 */
public class DistributionSetTypeUndefinedException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_DS_TYPE_UNDEFINED} error.
     */
    public DistributionSetTypeUndefinedException() {
        super(SpServerError.SP_DS_TYPE_UNDEFINED);
    }

    /**
     * @param cause
     *            for the exception
     */
    public DistributionSetTypeUndefinedException(final Throwable cause) {
        super(SpServerError.SP_DS_TYPE_UNDEFINED, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public DistributionSetTypeUndefinedException(final String message) {
        super(message, SpServerError.SP_DS_TYPE_UNDEFINED);
    }
}
