/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

/**
 * {@link ArtifactStoreException} is thrown in case storing of an artifact was
 * not successful.
 */
public class ArtifactStoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ArtifactStoreException with message and cause.
     * 
     * @param message
     *            the message of the exception
     * @param cause
     *            of the exception
     */
    public ArtifactStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a ArtifactStoreException with message.
     * 
     * @param message
     *            the message of the exception
     */
    public ArtifactStoreException(final String message) {
        super(message);
    }

    /**
     * Constructs a ArtifactStoreException with cause.
     * 
     * @param cause
     *            of the exception
     */
    public ArtifactStoreException(final Throwable cause) {
        super(cause);
    }
}
