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
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaLocalArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaLocalArtifact_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
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
     * between a target and a local file assignment through the assigen action
     * of the target. All actions are included, not only active actions.
     * 
     * @param target
     *            the target to verfiy if the given artifact is currently
     *            assigned or had been assigned
     * @param localArtifact
     *            the local artifact to check wherever the target had ever been
     *            assigned
     * @return a specification to use with spring JPA
     */
    public static Specification<JpaAction> hasTargetAssignedArtifact(final Target target,
            final LocalArtifact localArtifact) {
        return (actionRoot, query, criteriaBuilder) -> {
            final Join<JpaAction, JpaDistributionSet> dsJoin = actionRoot.join(JpaAction_.distributionSet);
            final SetJoin<JpaDistributionSet, JpaSoftwareModule> modulesJoin = dsJoin.join(JpaDistributionSet_.modules);
            final ListJoin<JpaSoftwareModule, JpaLocalArtifact> artifactsJoin = modulesJoin
                    .join(JpaSoftwareModule_.artifacts);
            return criteriaBuilder.and(
                    criteriaBuilder.equal(artifactsJoin.get(JpaLocalArtifact_.filename), localArtifact.getFilename()),
                    criteriaBuilder.equal(actionRoot.get(JpaAction_.target), target));
        };
    }
}
