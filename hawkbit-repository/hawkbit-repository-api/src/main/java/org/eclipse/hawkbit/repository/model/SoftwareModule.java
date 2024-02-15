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

import java.util.List;
import java.util.Optional;

/**
 * Software package as sub element of a {@link DistributionSet}.
 */
public interface SoftwareModule extends NamedVersionedEntity {

    /**
     * Maximum length of software vendor.
     */
    int VENDOR_MAX_SIZE = 256;

    /**
     * @return the type of the software module
     */
    SoftwareModuleType getType();

    /**
     * @return immutable list of all artifacts
     */
    List<Artifact> getArtifacts();

    /**
     * @return the vendor of this {@link SoftwareModule}.
     */
    String getVendor();

    /**
     * @return {@code true} if this software module is marked as encrypted
     *         otherwise {@code false}
     */
    boolean isEncrypted();

    /**
     * @return <code>true</code> if this {@link SoftwareModule} is locked. If so it's 'functional'
     *         properties (e.g. software modules) could not be modified anymore.
     */
    boolean isLocked();

    /**
     * @return {@code true} if this {@link SoftwareModule} is marked as deleted
     *         otherwise {@code false}
     */
    boolean isDeleted();

    /**
     * @param artifactId
     *            to look for
     * @return found {@link Artifact}
     */
    default Optional<Artifact> getArtifact(final Long artifactId) {
        return getArtifacts().stream().filter(artifact -> artifact.getId().equals(artifactId)).findAny();
    }

    /**
     * @param fileName
     *            to look for
     * @return found {@link Artifact}
     */
    default Optional<Artifact> getArtifactByFilename(final String fileName) {
        return getArtifacts().stream().filter(artifact -> artifact.getFilename().equalsIgnoreCase(fileName.trim()))
                .findAny();
    }
}