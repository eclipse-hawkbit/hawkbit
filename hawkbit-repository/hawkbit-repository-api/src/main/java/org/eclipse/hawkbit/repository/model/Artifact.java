/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;

/**
 * Binaries for a {@link SoftwareModule} Note: the decision which artifacts have
 * to be downloaded are done on the device side. e.g. Full Package, Signatures,
 * binary deltas
 *
 */
public interface Artifact extends TenantAwareBaseEntity {
    /**
     * @return the filename that was provided during upload.
     */
    String getFilename();

    /**
     * @return {@link SoftwareModule} this {@link Artifact} belongs to.
     */
    SoftwareModule getSoftwareModule();

    /**
     * @return MD5 hash of the artifact.
     */
    String getMd5Hash();

    /**
     * @return SHA-1 hash of the artifact in Base16 format that identifies the
     *         {@link AbstractDbArtifact} in the system.
     */
    String getSha1Hash();

    /**
     * @return size of the artifact in bytes.
     */
    long getSize();

}
