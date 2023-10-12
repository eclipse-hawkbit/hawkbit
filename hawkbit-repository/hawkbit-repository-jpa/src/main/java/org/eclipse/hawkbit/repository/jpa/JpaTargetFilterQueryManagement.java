/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericTargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetFilterQuerySpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;

import cz.jirutka.rsql.parser.RSQLParserException;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetFilterQueryManagement implements TargetFilterQueryManagement {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaTargetFilterQueryManagement.class);

    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final TargetManagement targetManagement;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final DistributionSetManagement distributionSetManagement;
    private final QuotaManagement quotaManagement;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final ContextAware contextAware;
    private final AccessController<JpaDistributionSet> distributionSetAccessController;

    private final Database database;

    JpaTargetFilterQueryManagement(final TargetFilterQueryRepository targetFilterQueryRepository,
            final TargetManagement targetManagement, final VirtualPropertyReplacer virtualPropertyReplacer,
            final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final Database database, final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext, final ContextAware contextAware,
            final AccessController<JpaDistributionSet> distributionSetAccessController) {
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.targetManagement = targetManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.database = database;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.contextAware = contextAware;
        this.distributionSetAccessController = distributionSetAccessController;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetFilterQuery create(final TargetFilterQueryCreate c) {
        final JpaTargetFilterQueryCreate create = (JpaTargetFilterQueryCreate) c;

        create.getQuery().ifPresent(query -> {
            // validate the RSQL query syntax
            RSQLUtility.validateRsqlFor(query, TargetFields.class);

            // enforce the 'max targets per auto assign' quota right here even
            // if the result of the filter query can vary over time
            create.getAutoAssignDistributionSetId().ifPresent(dsId -> {
                WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                        .validate(create);
                assertMaxTargetsQuota(query, create.getName().orElse(null), dsId);
            });
        });

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
    public Slice<TargetFilterQuery> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetFilterQueryRepository, pageable, null);
    }

    @Override
    public long count() {
        return targetFilterQueryRepository.count();
    }

    @Override
    public long countByAutoAssignDistributionSetId(final long autoAssignDistributionSetId) {
        return targetFilterQueryRepository.countByAutoAssignDistributionSetId(autoAssignDistributionSetId);
    }

    @Override
    public Slice<TargetFilterQuery> findByName(final Pageable pageable, final String name) {
        if (ObjectUtils.isEmpty(name)) {
            return findAll(pageable);
        }

        return JpaManagementHelper.findAllWithoutCountBySpec(targetFilterQueryRepository, pageable,
                Collections.singletonList(TargetFilterQuerySpecification.likeName(name)));
    }

    @Override
    public long countByName(final String name) {
        if (ObjectUtils.isEmpty(name)) {
            return count();
        }

        return JpaManagementHelper.countBySpec(targetFilterQueryRepository,
                Collections.singletonList(TargetFilterQuerySpecification.likeName(name)));
    }

    @Override
    public Page<TargetFilterQuery> findByRsql(final Pageable pageable, final String rsqlFilter) {
        final List<Specification<JpaTargetFilterQuery>> specList = !ObjectUtils.isEmpty(rsqlFilter)
                ? Collections.singletonList(RSQLUtility.buildRsqlSpecification(rsqlFilter,
                        TargetFilterQueryFields.class, virtualPropertyReplacer, database))
                : Collections.emptyList();

        return JpaManagementHelper.findAllWithCountBySpec(targetFilterQueryRepository, pageable, specList);
    }

    @Override
    public Slice<TargetFilterQuery> findByQuery(final Pageable pageable, final String query) {
        final List<Specification<JpaTargetFilterQuery>> specList = !ObjectUtils.isEmpty(query)
                ? Collections.singletonList(TargetFilterQuerySpecification.equalsQuery(query))
                : Collections.emptyList();

        return JpaManagementHelper.findAllWithoutCountBySpec(targetFilterQueryRepository, pageable, specList);
    }

    @Override
    public Slice<TargetFilterQuery> findByAutoAssignDistributionSetId(@NotNull final Pageable pageable,
            final long setId) {
        final DistributionSet distributionSet = distributionSetManagement.getOrElseThrowException(setId);

        return JpaManagementHelper.findAllWithoutCountBySpec(targetFilterQueryRepository, pageable,
                Collections.singletonList(TargetFilterQuerySpecification.byAutoAssignDS(distributionSet)));
    }

    @Override
    public Page<TargetFilterQuery> findByAutoAssignDSAndRsql(final Pageable pageable, final long setId,
            final String rsqlFilter) {
        final DistributionSet distributionSet = distributionSetManagement.getOrElseThrowException(setId);

        final List<Specification<JpaTargetFilterQuery>> specList = Lists.newArrayListWithExpectedSize(2);
        specList.add(TargetFilterQuerySpecification.byAutoAssignDS(distributionSet));
        if (!ObjectUtils.isEmpty(rsqlFilter)) {
            specList.add(RSQLUtility.buildRsqlSpecification(rsqlFilter, TargetFilterQueryFields.class,
                    virtualPropertyReplacer, database));
        }

        return JpaManagementHelper.findAllWithCountBySpec(targetFilterQueryRepository, pageable, specList);
    }

    @Override
    public Slice<TargetFilterQuery> findWithAutoAssignDS(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetFilterQueryRepository, pageable,
                Collections.singletonList(TargetFilterQuerySpecification.withAutoAssignDS()));
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
                assertMaxTargetsQuota(query, targetFilterQuery.getName(),
                        targetFilterQuery.getAutoAssignDistributionSet().getId());
            }

            // set the new query
            targetFilterQuery.setQuery(query);
        });
        update.getConfirmationRequired().ifPresent(targetFilterQuery::setConfirmationRequired);

        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    @Override
    @Transactional
    public TargetFilterQuery updateAutoAssignDS(final AutoAssignDistributionSetUpdate update) {
        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(
                update.getTargetFilterId());

        if (update.getDsId() == null) {
            // verify the current auto assignment can be deactivated
            if (targetFilterQuery.getAutoAssignDistributionSet() != null) {
                distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE,
                        (JpaDistributionSet) targetFilterQuery.getAutoAssignDistributionSet());
            }
            targetFilterQuery.setAccessControlContext(null);
            targetFilterQuery.setAutoAssignDistributionSet(null);
            targetFilterQuery.setAutoAssignActionType(null);
            targetFilterQuery.setAutoAssignWeight(null);
            targetFilterQuery.setAutoAssignInitiatedBy(null);
            targetFilterQuery.setConfirmationRequired(false);
        } else {
            WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).validate(update);
            assertMaxTargetsQuota(targetFilterQuery.getQuery(), targetFilterQuery.getName(), update.getDsId());
            final JpaDistributionSet ds = (JpaDistributionSet) distributionSetManagement
                    .getValidAndComplete(update.getDsId());
            verifyDistributionSetAndThrowExceptionIfDeleted(ds);
            distributionSetAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, ds);
            targetFilterQuery.setAutoAssignDistributionSet(ds);
            contextAware.getCurrentContext().ifPresent(targetFilterQuery::setAccessControlContext);
            targetFilterQuery.setAutoAssignInitiatedBy(contextAware.getCurrentUsername());
            targetFilterQuery.setAutoAssignActionType(sanitizeAutoAssignActionType(update.getActionType()));
            targetFilterQuery.setAutoAssignWeight(update.getWeight());
            final boolean confirmationRequired = update.isConfirmationRequired() == null ? isConfirmationFlowEnabled()
                    : update.isConfirmationRequired();
            targetFilterQuery.setConfirmationRequired(confirmationRequired);
        }
        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    private boolean isConfirmationFlowEnabled() {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                .isConfirmationFlowEnabled();
    }

    private static void verifyDistributionSetAndThrowExceptionIfDeleted(final DistributionSet distributionSet) {
        if (distributionSet.isDeleted()) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSet.getId());
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

    private JpaTargetFilterQuery findTargetFilterQueryOrThrowExceptionIfNotFound(final Long queryId) {
        return targetFilterQueryRepository.findById(queryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, queryId));
    }

    @Override
    public boolean verifyTargetFilterQuerySyntax(final String query) {
        try {
            RSQLUtility.validateRsqlFor(query, TargetFields.class);
            return true;
        } catch (final RSQLParserException | RSQLParameterUnsupportedFieldException e) {
            LOGGER.debug("The RSQL query '" + query + "' is invalid.", e);
            return false;
        }
    }

    private void assertMaxTargetsQuota(final String query, final String filterName, final long dsId) {
        QuotaHelper.assertAssignmentQuota(filterName,
                targetManagement.countByRsqlAndNonDSAndCompatibleAndUpdatable(dsId, query),
                quotaManagement.getMaxTargetsPerAutoAssignment(), Target.class, TargetFilterQuery.class, null);
    }

    @Override
    @Transactional
    public void cancelAutoAssignmentForDistributionSet(final long setId) {
        distributionSetManagement.get(setId).ifPresent(ds -> distributionSetAccessController
                .assertOperationAllowed(AccessController.Operation.UPDATE, (JpaDistributionSet) ds));

        targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(setId);
        LOGGER.debug("Auto assignments for distribution sets {} deactivated", setId);
    }
}
