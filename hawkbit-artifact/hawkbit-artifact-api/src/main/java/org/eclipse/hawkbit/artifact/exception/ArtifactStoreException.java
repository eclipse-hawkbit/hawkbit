/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.exception;

import java.io.Serial;

/**
 * {@link ArtifactStoreException} is thrown in case storing of an artifact was not successful.
 */
public class ArtifactStoreException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public  ArtifactStoreException(final String message) {
        this(message, null);
    }

    public ArtifactStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }
}