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

public interface SoftwareModule extends NamedVersionedEntity {

    /**
     * @param artifact
     *            is added to the assigned {@link Artifact}s.
     */
    void addArtifact(LocalArtifact artifact);

    /**
     * @param artifact
     *            is added to the assigned {@link Artifact}s.
     */
    void addArtifact(ExternalArtifact artifact);

    /**
     * @param artifactId
     *            to look for
     * @return found {@link Artifact}
     */
    Optional<LocalArtifact> getLocalArtifact(Long artifactId);

    /**
     * @param fileName
     *            to look for
     * @return found {@link Artifact}
     */
    Optional<LocalArtifact> getLocalArtifactByFilename(String fileName);

    /**
     * @return the artifacts
     */
    List<Artifact> getArtifacts();

    /**
     * @return local artifacts only
     */
    List<LocalArtifact> getLocalArtifacts();

    String getVendor();

    /**
     * @param artifact
     *            is removed from the assigned {@link LocalArtifact}s.
     */
    void removeArtifact(LocalArtifact artifact);

    /**
     * @param artifact
     *            is removed from the assigned {@link ExternalArtifact}s.
     */
    void removeArtifact(ExternalArtifact artifact);

    void setVendor(String vendor);

    SoftwareModuleType getType();

    boolean isDeleted();

    void setDeleted(boolean deleted);

    void setType(SoftwareModuleType type);

    /**
     * @return immutable list of meta data elements.
     */
    List<SoftwareModuleMetadata> getMetadata();

    /**
     * @return the assignedTo
     */
    List<DistributionSet> getAssignedTo();

}