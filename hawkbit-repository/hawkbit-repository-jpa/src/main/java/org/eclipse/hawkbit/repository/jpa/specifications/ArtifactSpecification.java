/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact_;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for
 * {@link org.eclipse.hawkbit.repository.model.Artifact}s. The class provides
 * Spring Data JPQL Specifications.
 *
 */
public final class ArtifactSpecification {
    private ArtifactSpecification() {
        // utility class
    }

    public static Specification<JpaArtifact> byId(final Long artifactId) {
        return (artifactRoot, query, cb) -> cb.equal(artifactRoot.get(JpaArtifact_.ID), artifactId);
    }

    public static Specification<JpaArtifact> bySha1(final String sha1) {
        return (artifactRoot, query, cb) -> cb.equal(artifactRoot.get(JpaArtifact_.sha1Hash), sha1);
    }

    public static Specification<JpaArtifact> byFilename(final String filename) {
        return (artifactRoot, query, cb) -> cb.equal(artifactRoot.get(JpaArtifact_.filename), filename);
    }

}
