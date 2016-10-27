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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetFilterQuerySpecification;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
public class JpaTargetFilterQueryManagement implements TargetFilterQueryManagement {

    @Autowired
    private TargetFilterQueryRepository targetFilterQueryRepository;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetFilterQuery createTargetFilterQuery(final TargetFilterQuery customTargetFilter) {

        if (targetFilterQueryRepository.findByName(customTargetFilter.getName()) != null) {
            throw new EntityAlreadyExistsException(customTargetFilter.getName());
        }
        return targetFilterQueryRepository.save((JpaTargetFilterQuery) customTargetFilter);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteTargetFilterQuery(final Long targetFilterQueryId) {
        targetFilterQueryRepository.delete(targetFilterQueryId);
    }

    @Override
    public Page<TargetFilterQuery> findAllTargetFilterQuery(final Pageable pageable) {
        return convertPage(targetFilterQueryRepository.findAll(pageable), pageable);
    }

    @Override
    public Long countAllTargetFilterQuery() {
        return targetFilterQueryRepository.count();
    }

    private static Page<TargetFilterQuery> convertPage(final Page<JpaTargetFilterQuery> findAll,
            final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryByName(final Pageable pageable, final String name) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!Strings.isNullOrEmpty(name)) {
            specList = Collections.singletonList(TargetFilterQuerySpecification.likeName(name));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryByFilter(@NotNull final Pageable pageable,
            final String rsqlFilter) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!Strings.isNullOrEmpty(rsqlFilter)) {
            specList = Collections.singletonList(
                    RSQLUtility.parse(rsqlFilter, TargetFilterQueryFields.class, virtualPropertyReplacer));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryByAutoAssignDS(@NotNull final Pageable pageable,
            final DistributionSet distributionSet) {
        return findTargetFilterQueryByAutoAssignDS(pageable, distributionSet, null);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryByAutoAssignDS(@NotNull final Pageable pageable,
            final DistributionSet distributionSet, final String rsqlFilter) {
        final List<Specification<JpaTargetFilterQuery>> specList = new ArrayList<>(2);
        if (distributionSet != null) {
            specList.add(TargetFilterQuerySpecification.byAutoAssignDS(distributionSet));
        }
        if (!Strings.isNullOrEmpty(rsqlFilter)) {
            specList.add(RSQLUtility.parse(rsqlFilter, TargetFilterQueryFields.class, virtualPropertyReplacer));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryWithAutoAssignDS(@NotNull final Pageable pageable) {
        final List<Specification<JpaTargetFilterQuery>> specList = Collections
                .singletonList(TargetFilterQuerySpecification.withAutoAssignDS());
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    private Page<JpaTargetFilterQuery> findTargetFilterQueryByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaTargetFilterQuery>> specList) {
        if (specList == null || specList.isEmpty()) {
            return targetFilterQueryRepository.findAll(pageable);
        }

        final Specifications<JpaTargetFilterQuery> specs = SpecificationsBuilder.combineWithAnd(specList);
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
    public TargetFilterQuery updateTargetFilterQuery(final Long queryId, final String name, final String query) {
        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(queryId);

        if (name != null) {
            targetFilterQuery.setName(name);
        }

        if (query != null) {
            targetFilterQuery.setQuery(query);
        }

        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetFilterQuery updateTargetFilterQuery(final Long queryId, final Long dsAutoAssign) {
        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(queryId);

        if (dsAutoAssign != null) {
            targetFilterQuery
                    .setAutoAssignDistributionSet(findDistributionSetAndThrowExceptionIfNotFound(dsAutoAssign));
        } else {
            targetFilterQuery.setAutoAssignDistributionSet(null);
        }

        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    private JpaDistributionSet findDistributionSetAndThrowExceptionIfNotFound(final Long setId) {
        final JpaDistributionSet set = (JpaDistributionSet) distributionSetManagement
                .findDistributionSetByIdWithDetails(setId);

        if (set == null) {
            throw new EntityNotFoundException("Distribution set cannot be updated as it does not exixt" + setId);
        }
        return set;
    }

    private JpaTargetFilterQuery findTargetFilterQueryOrThrowExceptionIfNotFound(final Long queryId) {
        return Optional.ofNullable(targetFilterQueryRepository.findOne(queryId)).orElseThrow(
                () -> new EntityNotFoundException("TargetFilterQuery with given ID " + queryId + " not found!"));
    }

    @Override
    public boolean verifyTargetFilterQuerySyntax(final String query) {
        RSQLUtility.parse(query, TargetFields.class, virtualPropertyReplacer);
        return true;
    }

}
