/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.specifications;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.SetJoin;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action_;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.LocalArtifact_;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule_;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class for {@link Action}s {@link Specification}s. The class provides
 * Spring Data JPQL Specifications.
 *
 */
public class ActionSpecifications {

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
    public static Specification<Action> hasTargetAssignedArtifact(final Target target,
            final LocalArtifact localArtifact) {
        return (actionRoot, query, criteriaBuilder) -> {
            final Join<Action, DistributionSet> dsJoin = actionRoot.join(Action_.distributionSet);
            final SetJoin<DistributionSet, SoftwareModule> modulesJoin = dsJoin.join(DistributionSet_.modules);
            final ListJoin<SoftwareModule, LocalArtifact> artifactsJoin = modulesJoin.join(SoftwareModule_.artifacts);
            return criteriaBuilder.and(criteriaBuilder.equal(artifactsJoin.get(LocalArtifact_.id), localArtifact.getId()),
                    criteriaBuilder.equal(actionRoot.get(Action_.target), target));
        };
    }
}
