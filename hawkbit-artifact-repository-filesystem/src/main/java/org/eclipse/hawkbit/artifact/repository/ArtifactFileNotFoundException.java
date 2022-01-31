/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

/**
 * Exception thrown in case that the artifact could not be read.
 */
public class ArtifactFileNotFoundException extends RuntimeException {

    /**
     * Creates the Exception from it's cause
     * @param cause the original exception
     */
    public ArtifactFileNotFoundException(final Exception cause) {
        super(cause);
    }
}
