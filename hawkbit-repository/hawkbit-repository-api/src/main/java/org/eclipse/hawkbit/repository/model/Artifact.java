/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;

/**
 * Binaries for a {@link SoftwareModule} Note: the decision which artifacts have
 * to be downloaded are done on the device side. e.g. Full Package, Signatures,
 * binary deltas
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
     * @return SHA-256 hash of the artifact.
     */
    String getSha256Hash();

    /**
     * @return size of the artifact in bytes.
     */
    long getSize();
}