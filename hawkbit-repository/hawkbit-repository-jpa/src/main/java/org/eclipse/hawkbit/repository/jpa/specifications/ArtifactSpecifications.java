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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact_;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class for {@link JpaArtifact}s {@link Specification}s. The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArtifactSpecifications {

    public static Specification<JpaArtifact> bySoftwareModuleId(final Long softwareModuleId) {
        return (targetRoot, query, cb) -> cb.equal(
                targetRoot.get(JpaArtifact_.softwareModule).get(AbstractJpaBaseEntity_.id), softwareModuleId);
    }
}