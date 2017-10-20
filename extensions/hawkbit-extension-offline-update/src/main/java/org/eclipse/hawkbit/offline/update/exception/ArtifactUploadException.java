/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * The {@link ArtifactUploadException} is thrown if there is any error while
 * uploading the artifacts.
 */
public class ArtifactUploadException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor to create {@link ArtifactUploadException}.
     *
     * @param t
     *            {@link Throwable}.
     */
    public ArtifactUploadException(Throwable t) {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED, t);
    }
}
