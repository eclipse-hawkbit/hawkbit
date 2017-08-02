/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;
import java.util.Optional;

/**
 * Software package as sub element of a {@link DistributionSet}.
 *
 */
public interface SoftwareModule extends NamedVersionedEntity {
    /**
     * Maximum length of software vendor.
     */
    int VENDOR_MAX_SIZE = 256;

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

    /**
     * @return immutable list of all artifacts
     */
    List<Artifact> getArtifacts();

    /**
     * @return the vendor of this software module
     */
    String getVendor();

    /**
     * @return the type of the software module
     */
    SoftwareModuleType getType();

    /**
     * @return {@code true} if this software module is marked as deleted
     *         otherwise {@code false}
     */
    boolean isDeleted();

    /**
     * @return immutable list of {@link DistributionSet}s the module is assigned
     *         to
     */
    List<DistributionSet> getAssignedTo();

}
