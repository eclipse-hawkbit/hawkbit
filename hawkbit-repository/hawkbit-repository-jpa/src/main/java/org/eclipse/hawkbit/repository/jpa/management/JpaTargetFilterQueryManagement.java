/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import cz.jirutka.rsql.parser.RSQLParserException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetFilterQuerySpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.eclipse.hawkbit.repository.qfields.TargetFilterQueryFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 */
@Slf4j
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "target-filter-management" }, matchIfMissing = true)
class JpaTargetFilterQueryManagement
        extends
        AbstractJpaRepositoryManagement<JpaTargetFilterQuery, TargetFilterQueryManagement.Create, TargetFilterQueryManagement.Update, TargetFilterQueryRepository, TargetFilterQueryFields>
        implements TargetFilterQueryManagement<JpaTargetFilterQuery> {

    private final TargetManagement<? extends Target> targetManagement;
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final QuotaManagement quotaManagement;
    private final RepositoryProperties repositoryProperties;

    protected JpaTargetFilterQueryManagement(
            final TargetFilterQueryRepository targetFilterQueryRepository, final EntityManager entityManager,
            final TargetManagement<? extends Target> targetManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final QuotaManagement quotaManagement,
            final RepositoryProperties repositoryProperties) {
        super(targetFilterQueryRepository, entityManager);
        this.targetManagement = targetManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.repositoryProperties = repositoryProperties;
    }

    @Override
    public JpaTargetFilterQuery create(final Create create) {
        validate(create);
        return super.create(create);
    }

    @Override
    public List<JpaTargetFilterQuery> create(final Collection<Create> create) {
        create.forEach(this::validate);
        return super.create(create);
    }

    @Override
    @Transactional
    public JpaTargetFilterQuery update(final Update update) {
        validate(update);
        return super.update(update);
    }

    @Override
    public void verifyTargetFilterQuerySyntax(final String query) {
        try {
            QLSupport.getInstance().validate(query, TargetFields.class, JpaTarget.class);
        } catch (final RSQLParserException | RSQLParameterUnsupportedFieldException e) {
            log.debug("The RSQL query '{}}' is invalid.", query, e);
            throw new RSQLParameterSyntaxException("Cannot create a Rollout with an empty target query filter!");
        }
    }

    @Override
    public long countByAutoAssignDistributionSetId(final long autoAssignDistributionSetId) {
        return jpaRepository.countByAutoAssignDistributionSetId(autoAssignDistributionSetId);
    }

    @Override
    public Page<TargetFilterQuery> findByAutoAssignDSAndRsql(final long setId, final String rsql, final Pageable pageable) {
        final DistributionSet distributionSet = distributionSetManagement.get(setId);

        final List<Specification<JpaTargetFilterQuery>> specList = new ArrayList<>(2);
        specList.add(TargetFilterQuerySpecification.byAutoAssignDS(distributionSet));
        if (!ObjectUtils.isEmpty(rsql)) {
            specList.add(QLSupport.getInstance().buildSpec(rsql, TargetFilterQueryFields.class));
        }

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public Slice<TargetFilterQuery> findWithAutoAssignDS(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(
                jpaRepository, List.of(TargetFilterQuerySpecification.withAutoAssignDS()), pageable);
    }

    @Override
    @Transactional
    public TargetFilterQuery updateAutoAssignDS(final AutoAssignDistributionSetUpdate update) {
        final JpaTargetFilterQuery targetFilterQuery = jpaRepository.getById(update.targetFilterId());
        if (update.dsId() == null) {
            targetFilterQuery.setAccessControlContext(null);
            targetFilterQuery.setAutoAssignDistributionSet(null);
            targetFilterQuery.setAutoAssignActionType(null);
            targetFilterQuery.setAutoAssignWeight(0);
            targetFilterQuery.setAutoAssignInitiatedBy(null);
            targetFilterQuery.setConfirmationRequired(false);
        } else {
            WeightValidationHelper.validate(update);
            assertMaxTargetsQuota(targetFilterQuery.getQuery(), targetFilterQuery.getName(), update.dsId());

            DistributionSet distributionSet = distributionSetManagement.getValidAndComplete(update.dsId());
            if (distributionSetManagement.shouldLockImplicitly(distributionSet)) {
                distributionSet = distributionSetManagement.lock(distributionSet);
            }

            targetFilterQuery.setAutoAssignDistributionSet(distributionSet);
            AccessContext.securityContext().ifPresent(targetFilterQuery::setAccessControlContext);
            targetFilterQuery.setAutoAssignInitiatedBy(
                    Optional.ofNullable(AccessContext.actor()).orElse(targetFilterQuery.getCreatedBy()));
            targetFilterQuery.setAutoAssignActionType(sanitizeAutoAssignActionType(update.actionType()));
            targetFilterQuery.setAutoAssignWeight(update.weight() == null ? repositoryProperties.getActionWeightIfAbsent() : update.weight());
            final boolean confirmationRequired = update.confirmationRequired() == null
                    ? TenantConfigHelper.isConfirmationFlowEnabled() :
                    update.confirmationRequired();
            targetFilterQuery.setConfirmationRequired(confirmationRequired);
        }
        return jpaRepository.save(targetFilterQuery);
    }

    @Override
    @Transactional
    public void cancelAutoAssignmentForDistributionSet(final long distributionSetId) {
        jpaRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(distributionSetId);
        log.debug("Auto assignments for distribution sets {} deactivated", distributionSetId);
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

    private void assertMaxTargetsQuota(final String query, final String filterName, final long dsId) {
        QuotaHelper.assertAssignmentQuota(filterName,
                targetManagement.countByRsqlAndNonDsAndCompatibleAndUpdatable(dsId, query),
                quotaManagement.getMaxTargetsPerAutoAssignment(), Target.class, TargetFilterQuery.class, null);
    }

    private void validate(final Create create) {
        Optional.ofNullable(create.getAutoAssignDistributionSet()).ifPresent(distributionSet -> {
            if (!distributionSet.isValid()) {
                throw new InvalidDistributionSetException();
            }
            if (!distributionSet.isComplete()) {
                throw new IncompleteDistributionSetException();
            }
        });
        Optional.ofNullable(create.getAutoAssignActionType()).ifPresent(actionType -> {
            if (!TargetFilterQuery.ALLOWED_AUTO_ASSIGN_ACTION_TYPES.contains(actionType)) {
                throw new InvalidAutoAssignActionTypeException();
            }
        });
        Optional.ofNullable(create.getQuery()).ifPresent(query -> {
            // validate the RSQL query syntax
            QLSupport.getInstance().validate(query, TargetFields.class, JpaTarget.class);

            // enforce the 'max targets per auto assign' quota right here even if the result of the filter query can vary over time
            Optional.ofNullable(create.getAutoAssignDistributionSet()).ifPresent(dsId -> {
                WeightValidationHelper.validate(create);
                assertMaxTargetsQuota(query, create.getName(), dsId.getId());
            });
        });
        if (create.getAutoAssignWeight() == null) {
            create.setAutoAssignWeight(create.getAutoAssignDistributionSet() == null ? 0 : repositoryProperties.getActionWeightIfAbsent());
        }
    }

    private void validate(final Update update) {
        final JpaTargetFilterQuery targetFilterQuery = jpaRepository.getById(update.getId());
        Optional.ofNullable(update.getQuery()).ifPresent(query -> {
            // validate the RSQL query syntax
            QLSupport.getInstance().validate(query, TargetFields.class, JpaTarget.class);

            Optional.ofNullable(targetFilterQuery.getAutoAssignDistributionSet()).ifPresent(autoAssignDs -> {
                // enforce the 'max targets per auto assignment'-quota only if the query is going to change
                if (!query.equals(targetFilterQuery.getQuery())) {
                    assertMaxTargetsQuota(query, targetFilterQuery.getName(), autoAssignDs.getId());
                }
            });

            // set the new query
            targetFilterQuery.setQuery(query);
        });
    }
}