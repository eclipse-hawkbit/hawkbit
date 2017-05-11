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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.builder.GenericTargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetFilterQuerySpecification;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetFilterQueryManagement implements TargetFilterQueryManagement {

    private final TargetFilterQueryRepository targetFilterQueryRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final DistributionSetManagement distributionSetManagement;

    @Autowired
    JpaTargetFilterQueryManagement(final TargetFilterQueryRepository targetFilterQueryRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final DistributionSetManagement distributionSetManagement) {
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetFilterQuery createTargetFilterQuery(final TargetFilterQueryCreate c) {
        final JpaTargetFilterQueryCreate create = (JpaTargetFilterQueryCreate) c;

        return targetFilterQueryRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteTargetFilterQuery(final Long targetFilterQueryId) {
        findTargetFilterQueryById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));

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
    public Page<TargetFilterQuery> findTargetFilterQueryByFilter(final Pageable pageable, final String rsqlFilter) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!Strings.isNullOrEmpty(rsqlFilter)) {
            specList = Collections.singletonList(
                    RSQLUtility.parse(rsqlFilter, TargetFilterQueryFields.class, virtualPropertyReplacer));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryByQuery(final Pageable pageable, final String query) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!Strings.isNullOrEmpty(query)) {
            specList = Collections.singletonList(TargetFilterQuerySpecification.equalsQuery(query));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryByAutoAssignDS(final Pageable pageable, final Long setId,
            final String rsqlFilter) {
        final List<Specification<JpaTargetFilterQuery>> specList = Lists.newArrayListWithExpectedSize(2);

        final DistributionSet distributionSet = findDistributionSetAndThrowExceptionIfNotFound(setId);

        specList.add(TargetFilterQuerySpecification.byAutoAssignDS(distributionSet));

        if (!Strings.isNullOrEmpty(rsqlFilter)) {
            specList.add(RSQLUtility.parse(rsqlFilter, TargetFilterQueryFields.class, virtualPropertyReplacer));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findTargetFilterQueryWithAutoAssignDS(final Pageable pageable) {
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
    public Optional<TargetFilterQuery> findTargetFilterQueryByName(final String targetFilterQueryName) {
        return targetFilterQueryRepository.findByName(targetFilterQueryName);
    }

    @Override
    public Optional<TargetFilterQuery> findTargetFilterQueryById(final Long targetFilterQueryId) {
        return Optional.ofNullable(targetFilterQueryRepository.findOne(targetFilterQueryId));
    }

    @Override
    @Transactional
    public TargetFilterQuery updateTargetFilterQuery(final TargetFilterQueryUpdate u) {
        final GenericTargetFilterQueryUpdate update = (GenericTargetFilterQueryUpdate) u;

        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(update.getId());

        update.getName().ifPresent(targetFilterQuery::setName);
        update.getQuery().ifPresent(targetFilterQuery::setQuery);

        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    @Override
    @Transactional
    public TargetFilterQuery updateTargetFilterQueryAutoAssignDS(final Long queryId, final Long dsId) {
        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(queryId);

        targetFilterQuery.setAutoAssignDistributionSet(
                Optional.ofNullable(dsId).map(this::findDistributionSetAndThrowExceptionIfNotFound).orElse(null));

        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    private JpaDistributionSet findDistributionSetAndThrowExceptionIfNotFound(final Long setId) {
        return (JpaDistributionSet) distributionSetManagement.findDistributionSetByIdWithDetails(setId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, setId));
    }

    private JpaTargetFilterQuery findTargetFilterQueryOrThrowExceptionIfNotFound(final Long queryId) {
        return targetFilterQueryRepository.findById(queryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, queryId));
    }

    @Override
    public boolean verifyTargetFilterQuerySyntax(final String query) {
        RSQLUtility.parse(query, TargetFields.class, virtualPropertyReplacer);
        return true;
    }

}
