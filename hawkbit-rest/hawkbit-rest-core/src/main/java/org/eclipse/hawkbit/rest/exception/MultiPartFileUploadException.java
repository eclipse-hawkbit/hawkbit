/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if a multi part exception occurred.
 *
 */
public final class MultiPartFileUploadException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * @param cause
     *            for the exception
     */
    public MultiPartFileUploadException(final Throwable cause) {
        super(cause.getMessage(), SpServerError.SP_ARTIFACT_UPLOAD_FAILED, cause);
    }

}
