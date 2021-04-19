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
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericTargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetFilterQuerySpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetFilterQueryManagement implements TargetFilterQueryManagement {

    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final TargetManagement targetManagement;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final DistributionSetManagement distributionSetManagement;
    private final QuotaManagement quotaManagement;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final TenantAware tenantAware;

    private final Database database;

    JpaTargetFilterQueryManagement(final TargetFilterQueryRepository targetFilterQueryRepository,
            final TargetManagement targetManagement, final VirtualPropertyReplacer virtualPropertyReplacer,
            final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final Database database, final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware) {
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.targetManagement = targetManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.database = database;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantAware = tenantAware;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetFilterQuery create(final TargetFilterQueryCreate c) {
        final JpaTargetFilterQueryCreate create = (JpaTargetFilterQueryCreate) c;

        // enforce the 'max targets per auto assign' quota right here even if
        // the result of the filter query can vary over time
        if (create.getAutoAssignDistributionSetId().isPresent()) {
            WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).validate(create);
            create.getQuery().ifPresent(this::assertMaxTargetsQuota);
        }

        return targetFilterQueryRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long targetFilterQueryId) {
        if (!targetFilterQueryRepository.existsById(targetFilterQueryId)) {
            throw new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId);
        }

        targetFilterQueryRepository.deleteById(targetFilterQueryId);
    }

    @Override
    public Page<TargetFilterQuery> findAll(final Pageable pageable) {
        return convertPage(targetFilterQueryRepository.findAll(pageable), pageable);
    }

    @Override
    public long count() {
        return targetFilterQueryRepository.count();
    }

    private static Page<TargetFilterQuery> convertPage(final Page<JpaTargetFilterQuery> findAll,
            final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<TargetFilterQuery> findByName(final Pageable pageable, final String name) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!StringUtils.isEmpty(name)) {
            specList = Collections.singletonList(TargetFilterQuerySpecification.likeName(name));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findByRsql(final Pageable pageable, final String rsqlFilter) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!StringUtils.isEmpty(rsqlFilter)) {
            specList = Collections.singletonList(
                    RSQLUtility.parse(rsqlFilter, TargetFilterQueryFields.class, virtualPropertyReplacer, database));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findByQuery(final Pageable pageable, final String query) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!StringUtils.isEmpty(query)) {
            specList = Collections.singletonList(TargetFilterQuerySpecification.equalsQuery(query));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findByAutoAssignDSAndRsql(final Pageable pageable, final long setId,
            final String rsqlFilter) {
        final List<Specification<JpaTargetFilterQuery>> specList = Lists.newArrayListWithExpectedSize(2);

        final DistributionSet distributionSet = findDistributionSetAndThrowExceptionIfNotFound(setId);

        specList.add(TargetFilterQuerySpecification.byAutoAssignDS(distributionSet));

        if (!StringUtils.isEmpty(rsqlFilter)) {
            specList.add(
                    RSQLUtility.parse(rsqlFilter, TargetFilterQueryFields.class, virtualPropertyReplacer, database));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findWithAutoAssignDS(final Pageable pageable) {
        final List<Specification<JpaTargetFilterQuery>> specList = Collections
                .singletonList(TargetFilterQuerySpecification.withAutoAssignDS());
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    private Page<JpaTargetFilterQuery> findTargetFilterQueryByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaTargetFilterQuery>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return targetFilterQueryRepository.findAll(pageable);
        }

        final Specification<JpaTargetFilterQuery> specs = SpecificationsBuilder.combineWithAnd(specList);
        return targetFilterQueryRepository.findAll(specs, pageable);
    }

    @Override
    public Optional<TargetFilterQuery> getByName(final String targetFilterQueryName) {
        return targetFilterQueryRepository.findByName(targetFilterQueryName);
    }

    @Override
    public Optional<TargetFilterQuery> get(final long targetFilterQueryId) {
        return targetFilterQueryRepository.findById(targetFilterQueryId).map(tfq -> (TargetFilterQuery) tfq);
    }

    @Override
    @Transactional
    public TargetFilterQuery update(final TargetFilterQueryUpdate u) {
        final GenericTargetFilterQueryUpdate update = (GenericTargetFilterQueryUpdate) u;

        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(update.getId());

        update.getName().ifPresent(targetFilterQuery::setName);
        update.getQuery().ifPresent(query -> {

            // enforce the 'max targets per auto assignment'-quota only if the
            // query is going to change
            if (targetFilterQuery.getAutoAssignDistributionSet() != null
                    && !query.equals(targetFilterQuery.getQuery())) {
                assertMaxTargetsQuota(query);
            }

            // set the new query
            targetFilterQuery.setQuery(query);
        });

        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    @Override
    @Transactional
    public TargetFilterQuery updateAutoAssignDS(final AutoAssignDistributionSetUpdate update) {
        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(
                update.getTargetFilterId());
        if (update.getDsId() == null) {
            targetFilterQuery.setAutoAssignDistributionSet(null);
            targetFilterQuery.setAutoAssignActionType(null);
            targetFilterQuery.setAutoAssignWeight(null);
            targetFilterQuery.setAutoAssignInitiatedBy(null);
        } else {
            WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).validate(update);
            // we cannot be sure that the quota was enforced at creation time
            // because the Target Filter Query REST API does not allow to
            // specify an
            // auto-assign distribution set when creating a target filter query
            assertMaxTargetsQuota(targetFilterQuery.getQuery());
            final JpaDistributionSet ds = findDistributionSetAndThrowExceptionIfNotFound(update.getDsId());
            verifyDistributionSetAndThrowExceptionIfNotValid(ds);
            targetFilterQuery.setAutoAssignDistributionSet(ds);
            targetFilterQuery.setAutoAssignInitiatedBy(tenantAware.getCurrentUsername());
            targetFilterQuery.setAutoAssignActionType(sanitizeAutoAssignActionType(update.getActionType()));
            targetFilterQuery.setAutoAssignWeight(update.getWeight());
        }
        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    private static void verifyDistributionSetAndThrowExceptionIfNotValid(final DistributionSet distributionSet) {
        if (!distributionSet.isComplete() || distributionSet.isDeleted()) {
            throw new InvalidAutoAssignDistributionSetException();
        }
    }

    private static ActionType sanitizeAutoAssignActionType(final ActionType actionType) {
        if (actionType == null) {
            return ActionType.FORCED;
        }

        if (!TargetFilterQuery.ALLOWED_AUTO_ASSIGN_ACTION_TYPES.contains(actionType)) {
            throw new InvalidAutoAssignActionTypeException();
        }

        return actionType;
    }

    private JpaDistributionSet findDistributionSetAndThrowExceptionIfNotFound(final Long setId) {
        return (JpaDistributionSet) distributionSetManagement.get(setId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, setId));
    }

    private JpaTargetFilterQuery findTargetFilterQueryOrThrowExceptionIfNotFound(final Long queryId) {
        return targetFilterQueryRepository.findById(queryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, queryId));
    }

    @Override
    public boolean verifyTargetFilterQuerySyntax(final String query) {
        RSQLUtility.parse(query, TargetFields.class, virtualPropertyReplacer, database);
        return true;
    }

    private void assertMaxTargetsQuota(final String query) {
        QuotaHelper.assertAssignmentQuota(targetManagement.countByRsql(query),
                quotaManagement.getMaxTargetsPerAutoAssignment(), Target.class, TargetFilterQuery.class);
    }
}
