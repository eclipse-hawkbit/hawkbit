/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import java.util.List;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.SetJoin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.model.Action;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class for {@link Action}s {@link Specification}s. The class provides Spring Data JPQL Specifications.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActionSpecifications {

    public static Specification<JpaAction> byTargetIdAndIsActive(final Long targetId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.target).get(AbstractJpaBaseEntity_.id), targetId),
                cb.equal(root.get(JpaAction_.active), true));
    }

    public static Specification<JpaAction> byTargetControllerId(final String controllerId) {
        return (root, query, cb) -> cb.equal(root.get(JpaAction_.target).get(JpaTarget_.controllerId), controllerId);
    }

    public static Specification<JpaAction> byTargetIdAndIsActiveAndStatus(final Long targetId, final Action.Status status) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.target).get(AbstractJpaBaseEntity_.id), targetId),
                cb.equal(root.get(JpaAction_.active), true),
                cb.equal(root.get(JpaAction_.status), status));
    }

    public static Specification<JpaAction> byTargetControllerIdAndActive(final String controllerId, final boolean active) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.target).get(JpaTarget_.controllerId), controllerId),
                cb.equal(root.get(JpaAction_.active), active));
    }

    public static Specification<JpaAction> byTargetControllerIdAndIsActiveAndStatus(final String controllerId, final Action.Status status) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.target).get(JpaTarget_.controllerId), controllerId),
                cb.equal(root.get(JpaAction_.active), true),
                cb.equal(root.get(JpaAction_.status), status));
    }

    /**
     * Returns active actions by target controller that has null or non-null depending on <code>isNull</code> value.
     *
     * @param controllerId controller id
     * @param isNull if <code>true</code> return with <code>null</code> weight, otherwise with non-<code>null</code>
     * @return the matching action s.
     */
    public static Specification<JpaAction> byTargetControllerIdAndActiveAndWeightIsNull(final String controllerId, final boolean isNull) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.target).get(JpaTarget_.controllerId), controllerId),
                cb.equal(root.get(JpaAction_.active), true),
                isNull ? cb.isNull(root.get(JpaAction_.weight)) : cb.isNotNull(root.get(JpaAction_.weight)));
    }

    public static Specification<JpaAction> byDistributionSetIdAndActive(final Long distributionSetId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.distributionSet).get(AbstractJpaBaseEntity_.id), distributionSetId),
                cb.equal(root.get(JpaAction_.active), true));
    }

    public static Specification<JpaAction> byRolloutIdAndActive(final Long rolloutId) {
        return (root,  query,  cb) -> cb.and(
                cb.equal(root.get(JpaAction_.rollout).get(AbstractJpaBaseEntity_.id), rolloutId),
                cb.equal(root.get(JpaAction_.active), true)
        );
    }

    public static Specification<JpaAction> byRolloutIdAndActiveAndStatusIsNot(final Long rolloutId, final List<Action.Status> statuses) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.rollout).get(AbstractJpaBaseEntity_.id), rolloutId),
                cb.equal(root.get(JpaAction_.active), true),
                cb.not(root.get(JpaAction_.status).in(statuses))
        );
    }

    public static Specification<JpaAction> byControllerIdAndIdIn(final String controllerId, final List<Long> actionIds) {
        return ((root, query, cb) -> {
                final Join<JpaAction, JpaTarget> targetJoin = root.join(JpaAction_.target);
                return cb.and(
                        cb.equal(targetJoin.get(JpaTarget_.controllerId), controllerId),
                        root.get(AbstractJpaBaseEntity_.id).in(actionIds)
                );
        });

    }

    public static Specification<JpaAction> byDistributionSetIdAndActiveAndStatusIsNot(final Long distributionSetId, final Action.Status status) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get(JpaAction_.distributionSet).get(AbstractJpaBaseEntity_.id), distributionSetId),
                cb.equal(root.get(JpaAction_.active), true),
                cb.notEqual(root.get(JpaAction_.status), status));
    }

    /**
     * Specification which joins all necessary tables to retrieve the dependency between a target and a local file assignment through the
     * assigned action of the target. All actions are included, not only active actions.
     *
     * @param controllerId the target to verify if the given artifact is currently assigned or had been assigned
     * @param sha1Hash of the local artifact to check wherever the target had ever been assigned
     * @return a specification to use with spring JPA
     */
    public static Specification<JpaAction> hasTargetAssignedArtifact(final String controllerId, final String sha1Hash) {
        return (actionRoot, query, criteriaBuilder) -> {
            final Join<JpaAction, JpaDistributionSet> dsJoin = actionRoot.join(JpaAction_.distributionSet);
            final SetJoin<JpaDistributionSet, JpaSoftwareModule> modulesJoin = dsJoin.join(JpaDistributionSet_.modules);
            final ListJoin<JpaSoftwareModule, JpaArtifact> artifactsJoin = modulesJoin.join(JpaSoftwareModule_.artifacts);
            return criteriaBuilder.and(
                    criteriaBuilder.equal(artifactsJoin.get(JpaArtifact_.sha1Hash), sha1Hash),
                    criteriaBuilder.equal(actionRoot.get(JpaAction_.target).get(JpaTarget_.controllerId), controllerId));
        };
    }

    /**
     * Specification which joins all necessary tables to retrieve the dependency between a target and a local file assignment through the
     * assigned action of the target. All actions are included, not only active actions.
     *
     * @param targetId the target to verify if the given artifact is currently assigned or had been assigned
     * @param sha1Hash of the local artifact to check wherever the target had ever been assigned
     * @return a specification to use with spring JPA
     */
    public static Specification<JpaAction> hasTargetAssignedArtifact(final Long targetId, final String sha1Hash) {
        return (actionRoot, query, criteriaBuilder) -> {
            final Join<JpaAction, JpaDistributionSet> dsJoin = actionRoot.join(JpaAction_.distributionSet);
            final SetJoin<JpaDistributionSet, JpaSoftwareModule> modulesJoin = dsJoin.join(JpaDistributionSet_.modules);
            final ListJoin<JpaSoftwareModule, JpaArtifact> artifactsJoin = modulesJoin.join(JpaSoftwareModule_.artifacts);
            return criteriaBuilder.and(
                    criteriaBuilder.equal(artifactsJoin.get(JpaArtifact_.sha1Hash), sha1Hash),
                    criteriaBuilder.equal(actionRoot.get(JpaAction_.target).get(AbstractJpaBaseEntity_.id), targetId));
        };
    }
}