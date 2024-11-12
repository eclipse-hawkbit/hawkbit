/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

/**
 * Exception thrown in case that the artifact could not be read.
 */
public class ArtifactFileNotFoundException extends RuntimeException {

    /**
     * Creates the Exception from it's cause
     *
     * @param cause the original exception
     */
    public ArtifactFileNotFoundException(final Exception cause) {
        super(cause);
    }
}
