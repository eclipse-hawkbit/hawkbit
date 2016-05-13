/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.specifications.TargetFilterQuerySpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;

/**
 * Business service facade for managing {@link TargetFilterQuery}s.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class TargetFilterQueryManagement {

    @Autowired
    private TargetFilterQueryRepository targetFilterQueryRepository;

    /**
     * creating new {@link TargetFilterQuery}.
     *
     * @param customTargetFilter
     * @return the created {@link TargetFilterQuery}
     */
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    public TargetFilterQuery createTargetFilterQuery(@NotNull final TargetFilterQuery customTargetFilter) {

        if (targetFilterQueryRepository.findByName(customTargetFilter.getName()) != null) {
            throw new EntityAlreadyExistsException(customTargetFilter.getName());
        }
        return targetFilterQueryRepository.save(customTargetFilter);
    }

    /**
     * Delete target filter query.
     *
     * @param targetFilterQueryId
     *            IDs of target filter query to be deleted
     */
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    public void deleteTargetFilterQuery(@NotNull final Long targetFilterQueryId) {
        targetFilterQueryRepository.delete(targetFilterQueryId);
    }

    /**
     *
     * Retrieves all target filter query{@link TargetFilterQuery}.
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<TargetFilterQuery> findAllTargetFilterQuery(@NotNull final Pageable pageable) {
        return targetFilterQueryRepository.findAll(pageable);
    }

    /**
     * Retrieves all target filter query which {@link TargetFilterQuery}.
     *
     *
     * @param pageable
     *            pagination parameter
     * @param name
     *            target filter query name
     * @return the page with the found {@link TargetFilterQuery}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<TargetFilterQuery> findTargetFilterQueryByFilters(@NotNull final Pageable pageable, final String name) {
        final List<Specification<TargetFilterQuery>> specList = new ArrayList<>();
        if (!Strings.isNullOrEmpty(name)) {
            specList.add(TargetFilterQuerySpecification.likeName(name));
        }
        return findTargetFilterQueryByCriteriaAPI(pageable, specList);
    }

    /**
     *
     * @param pageable
     *            pagination parameter
     * @param specList
     *            list of @link {@link Specification}
     * @return the page with the found {@link TargetFilterQuery}
     */
    private Page<TargetFilterQuery> findTargetFilterQueryByCriteriaAPI(@NotNull final Pageable pageable,
            final List<Specification<TargetFilterQuery>> specList) {
        if (specList == null || specList.isEmpty()) {
            return targetFilterQueryRepository.findAll(pageable);
        }

        final Specifications<TargetFilterQuery> specs = SpecificationsBuilder.combineWithAnd(specList);
        return targetFilterQueryRepository.findAll(specs, pageable);
    }

    /**
     * Find target filter query by name.
     *
     * @param targetFilterQueryName
     *            Target filter query name
     * @return the found {@link TargetFilterQuery}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public TargetFilterQuery findTargetFilterQueryByName(@NotNull final String targetFilterQueryName) {
        return targetFilterQueryRepository.findByName(targetFilterQueryName);
    }

    /**
     * Find target filter query by id.
     *
     * @param targetFilterQueryId
     *            Target filter query id
     * @return the found {@link TargetFilterQuery}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public TargetFilterQuery findTargetFilterQueryById(@NotNull final Long targetFilterQueryId) {
        return targetFilterQueryRepository.findOne(targetFilterQueryId);
    }

    /**
     * updates the {@link TargetFilterQuery}.
     *
     * @param targetFilterQuery
     *            to be updated
     * @return the updated {@link TargetFilterQuery}
     */
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public TargetFilterQuery updateTargetFilterQuery(@NotNull final TargetFilterQuery targetFilterQuery) {
        Assert.notNull(targetFilterQuery.getId());
        return targetFilterQueryRepository.save(targetFilterQuery);
    }

}
