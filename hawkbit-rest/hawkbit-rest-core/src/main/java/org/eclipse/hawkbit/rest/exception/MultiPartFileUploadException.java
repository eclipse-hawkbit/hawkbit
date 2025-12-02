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

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if a multipart exception occurred.
 */
public final class MultiPartFileUploadException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MultiPartFileUploadException(final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED, cause.getMessage(), cause);
    }
}