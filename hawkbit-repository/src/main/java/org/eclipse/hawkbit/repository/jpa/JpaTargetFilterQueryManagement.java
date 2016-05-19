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

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class JpaTargetFilterQueryManagement implements TargetFilterQueryManagement {

    @Autowired
    private TargetFilterQueryRepository targetFilterQueryRepository;

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetFilterQuery createTargetFilterQuery(final TargetFilterQuery customTargetFilter) {

        if (targetFilterQueryRepository.findByName(customTargetFilter.getName()) != null) {
            throw new EntityAlreadyExistsException(customTargetFilter.getName());
        }
        return targetFilterQueryRepository.save(customTargetFilter);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteTargetFilterQuery(final Long targetFilterQueryId) {
        targetFilterQueryRepository.delete(targetFilterQueryId);
    }

    @Override
    public Page<TargetFilterQuery> findAllTargetFilterQuery(final Pageable pageable) {
        return targetFilterQueryRepository.findAll(pageable);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryByFilters(final Pageable pageable, final String name) {
        final List<Specification<TargetFilterQuery>> specList = new ArrayList<>();
        if (!Strings.isNullOrEmpty(name)) {
            specList.add(TargetFilterQuerySpecification.likeName(name));
        }
        return findTargetFilterQueryByCriteriaAPI(pageable, specList);
    }

    private Page<TargetFilterQuery> findTargetFilterQueryByCriteriaAPI(final Pageable pageable,
            final List<Specification<TargetFilterQuery>> specList) {
        if (specList == null || specList.isEmpty()) {
            return targetFilterQueryRepository.findAll(pageable);
        }

        final Specifications<TargetFilterQuery> specs = SpecificationsBuilder.combineWithAnd(specList);
        return targetFilterQueryRepository.findAll(specs, pageable);
    }

    @Override
    public TargetFilterQuery findTargetFilterQueryByName(final String targetFilterQueryName) {
        return targetFilterQueryRepository.findByName(targetFilterQueryName);
    }

    @Override
    public TargetFilterQuery findTargetFilterQueryById(final Long targetFilterQueryId) {
        return targetFilterQueryRepository.findOne(targetFilterQueryId);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetFilterQuery updateTargetFilterQuery(final TargetFilterQuery targetFilterQuery) {
        Assert.notNull(targetFilterQuery.getId());
        return targetFilterQueryRepository.save(targetFilterQuery);
    }

}
