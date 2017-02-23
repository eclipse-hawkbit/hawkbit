/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.SetJoin;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.Action;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class for {@link Action}s {@link Specification}s. The class provides
 * Spring Data JPQL Specifications.
 *
 */
public final class ActionSpecifications {

    private ActionSpecifications() {
        // utility class
    }

    /**
     * Specification which joins all necessary tables to retrieve the dependency
     * between a target and a local file assignment through the assigned action
     * of the target. All actions are included, not only active actions.
     * 
     * @param controllerId
     *            the target to verify if the given artifact is currently
     *            assigned or had been assigned
     * @param sha1Hash
     *            of the local artifact to check wherever the target had ever
     *            been assigned
     * @return a specification to use with spring JPA
     */
    public static Specification<JpaAction> hasTargetAssignedArtifact(final String controllerId, final String sha1Hash) {
        return (actionRoot, query, criteriaBuilder) -> {
            final Join<JpaAction, JpaDistributionSet> dsJoin = actionRoot.join(JpaAction_.distributionSet);
            final SetJoin<JpaDistributionSet, JpaSoftwareModule> modulesJoin = dsJoin.join(JpaDistributionSet_.modules);
            final ListJoin<JpaSoftwareModule, JpaArtifact> artifactsJoin = modulesJoin
                    .join(JpaSoftwareModule_.artifacts);
            return criteriaBuilder.and(criteriaBuilder.equal(artifactsJoin.get(JpaArtifact_.sha1Hash), sha1Hash),
                    criteriaBuilder.equal(actionRoot.get(JpaAction_.target).get(JpaTarget_.controllerId),
                            controllerId));
        };
    }

    /**
     * Specification which joins all necessary tables to retrieve the dependency
     * between a target and a local file assignment through the assigned action
     * of the target. All actions are included, not only active actions.
     * 
     * @param targetId
     *            the target to verify if the given artifact is currently
     *            assigned or had been assigned
     * @param sha1Hash
     *            of the local artifact to check wherever the target had ever
     *            been assigned
     * @return a specification to use with spring JPA
     */
    public static Specification<JpaAction> hasTargetAssignedArtifact(final Long targetId, final String sha1Hash) {
        return (actionRoot, query, criteriaBuilder) -> {
            final Join<JpaAction, JpaDistributionSet> dsJoin = actionRoot.join(JpaAction_.distributionSet);
            final SetJoin<JpaDistributionSet, JpaSoftwareModule> modulesJoin = dsJoin.join(JpaDistributionSet_.modules);
            final ListJoin<JpaSoftwareModule, JpaArtifact> artifactsJoin = modulesJoin
                    .join(JpaSoftwareModule_.artifacts);
            return criteriaBuilder.and(criteriaBuilder.equal(artifactsJoin.get(JpaArtifact_.sha1Hash), sha1Hash),
                    criteriaBuilder.equal(actionRoot.get(JpaAction_.target).get(JpaTarget_.id), targetId));
        };
    }
}
