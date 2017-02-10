/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collections;

import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;

/**
 * A collection of static helper methods for the {@link JpaRolloutManagement}
 */
final class JpaRolloutHelper {
    private JpaRolloutHelper() {
    }

    /**
     * In case the given group is missing conditions or actions, they will be
     * set from the supplied default conditions.
     * 
     * @param create
     *            group to check
     * @param conditions
     *            default conditions and actions
     */
    static JpaRolloutGroup prepareRolloutGroupWithDefaultConditions(final RolloutGroupCreate create,
            final RolloutGroupConditions conditions) {
        final JpaRolloutGroup group = ((JpaRolloutGroupCreate) create).build();

        if (group.getSuccessCondition() == null) {
            group.setSuccessCondition(conditions.getSuccessCondition());
        }
        if (group.getSuccessConditionExp() == null) {
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());
        }
        if (group.getSuccessAction() == null) {
            group.setSuccessAction(conditions.getSuccessAction());
        }
        if (group.getSuccessActionExp() == null) {
            group.setSuccessActionExp(conditions.getSuccessActionExp());
        }

        if (group.getErrorCondition() == null) {
            group.setErrorCondition(conditions.getErrorCondition());
        }
        if (group.getErrorConditionExp() == null) {
            group.setErrorConditionExp(conditions.getErrorConditionExp());
        }
        if (group.getErrorAction() == null) {
            group.setErrorAction(conditions.getErrorAction());
        }
        if (group.getErrorActionExp() == null) {
            group.setErrorActionExp(conditions.getErrorActionExp());
        }

        return group;
    }

    /**
     * Builds a {@link Specification} to search a rollout by name or
     * description.
     * 
     * @param searchText
     *            search string
     * @param isDeleted
     *            <code>true</code> if deleted rollouts should be included in
     *            the search. Otherwise <code>false</code>
     * @return criteria specification with a query for name or description of a
     *         rollout
     */
    static Specification<JpaRollout> likeNameOrDescription(final String searchText, final boolean isDeleted) {
        return (rolloutRoot, query, criteriaBuilder) -> {
            final String searchTextToLower = searchText.toLowerCase();
            return criteriaBuilder.and(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(rolloutRoot.get(JpaRollout_.name)), searchTextToLower),
                    criteriaBuilder.like(criteriaBuilder.lower(rolloutRoot.get(JpaRollout_.description)),
                            searchTextToLower)),
                    criteriaBuilder.equal(rolloutRoot.get(JpaRollout_.deleted), isDeleted));
        };
    }

    static Page<Rollout> convertPage(final Page<JpaRollout> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    static Slice<Rollout> convertPage(final Slice<JpaRollout> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, 0);
    }

}
