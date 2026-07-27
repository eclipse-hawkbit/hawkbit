/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.eclipse.hawkbit.repository.model.Action.ActionType.FORCED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.ALLOWED_ACTION_TYPES;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignApprovalDecision.APPROVED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.APPROVAL_DENIED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.PAUSED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.RUNNING;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.WAITING_FOR_APPROVAL;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTO_ASSIGNMENT_APPROVAL_ENABLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.AutoAssignmentIllegalStateException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaAutoAssignment;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.AutoAssignmentRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.AutoAssignmentSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.qfields.AutoAssignmentFields;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Transactional(readOnly = true)
@Validated
@Service
public class JpaAutoAssignmentManagement extends
        AbstractJpaRepositoryManagement<JpaAutoAssignment, AutoAssignmentManagement.Create, AutoAssignmentManagement.Update, AutoAssignmentRepository, AutoAssignmentFields>
        implements AutoAssignmentManagement<JpaAutoAssignment> {

    private final TargetManagement<? extends Target> targetManagement;
    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final QuotaManagement quotaManagement;
    private final RepositoryProperties repositoryProperties;

    protected JpaAutoAssignmentManagement(final AutoAssignmentRepository jpaRepository, final EntityManager entityManager,
            final TargetManagement<? extends Target> targetManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final QuotaManagement quotaManagement,
            final RepositoryProperties repositoryProperties) {
        super(jpaRepository, entityManager);
        this.targetManagement = targetManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.repositoryProperties = repositoryProperties;
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public JpaAutoAssignment create(final Create create) {
        validate(create);
        return super.create(create);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public List<JpaAutoAssignment> create(final Collection<Create> create) {
        create.forEach(this::validate);
        return super.create(create);
    }

    @Override
    protected JpaAutoAssignment jpaEntity(final Object create) {
        final JpaAutoAssignment jpaEntity = super.jpaEntity(create);
        AccessContext.securityContext().ifPresent(jpaEntity::setAccessControlContext);
        jpaEntity.setActionType(sanitizeAutoAssignActionType(jpaEntity.getActionType()));
        jpaEntity.setStatus(resolveInitialAutoAssignStatus());
        if (jpaEntity.getWeight().isEmpty()) {
            jpaEntity.setWeight(repositoryProperties.getActionWeightIfAbsent());
        }
        return jpaEntity;
    }

    @Override
    public Page<AutoAssignment> findByDSAndRsql(final long setId, final String rsql, final Pageable pageable) {
        final DistributionSet distributionSet = distributionSetManagement.get(setId);

        final List<Specification<JpaAutoAssignment>> specList = new ArrayList<>(2);
        specList.add(AutoAssignmentSpecification.byDistributionSet(distributionSet));
        if (!ObjectUtils.isEmpty(rsql)) {
            specList.add(QLSupport.getInstance().buildSpec(rsql, AutoAssignmentFields.class));
        }

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public Slice<AutoAssignment> getActiveAutoAssignments(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(
                jpaRepository, List.of(AutoAssignmentSpecification.activeAutoAssignment()), pageable);
    }

    @Override
    public Page<AutoAssignment> findAutoAssignmentByRsql(final String rsql, final Pageable pageable) {
        final List<Specification<JpaAutoAssignment>> specList = new ArrayList<>(2);
        specList.add(AutoAssignmentSpecification.getAll());
        if (!ObjectUtils.isEmpty(rsql)) {
            specList.add(QLSupport.getInstance().buildSpec(rsql, AutoAssignmentFields.class));
        }

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public Optional<AutoAssignment> findByName(final String name) {
        return JpaManagementHelper.<AutoAssignment, JpaAutoAssignment> findAllWithCountBySpec(jpaRepository,
                List.of(AutoAssignmentSpecification.getAll(), AutoAssignmentSpecification.byName(name)), Pageable.ofSize(1))
                .stream().findFirst();
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void cancelAutoAssignmentForDistributionSet(final long distributionSetId) {
        final List<JpaAutoAssignment> affected = jpaRepository.findByDistributionSet(distributionSetId);
        jpaRepository.deleteAll(affected);
        log.debug("Auto assignments for distribution sets {} deactivated", distributionSetId);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public AutoAssignment approveOrDeny(final long autoAssignmentId, final AutoAssignment.AutoAssignApprovalDecision decision) {
        return approveOrDeny0(autoAssignmentId, decision, null);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public AutoAssignment approveOrDeny(final long autoAssignmentId, final AutoAssignment.AutoAssignApprovalDecision decision,
            final String remark) {
        return approveOrDeny0(autoAssignmentId, decision, remark);
    }

    private AutoAssignment approveOrDeny0(final long autoAssignmentId, final AutoAssignment.AutoAssignApprovalDecision decision,
            final String remark) {
        final JpaAutoAssignment autoAssignment = jpaRepository.getById(autoAssignmentId);
        if (autoAssignment.getStatus() != WAITING_FOR_APPROVAL) {
            throw new AutoAssignmentIllegalStateException("Auto assignment not waiting for approval");
        }
        autoAssignment.setStatus(decision == APPROVED ? READY : APPROVAL_DENIED);
        autoAssignment.setApprovalDecidedBy(AccessContext.actor());
        autoAssignment.setApprovalRemark(remark);
        return jpaRepository.save(autoAssignment);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public AutoAssignment start(final long autoAssignmentId) {
        final JpaAutoAssignment autoAssignment = jpaRepository.getById(autoAssignmentId);
        if (autoAssignment.getStatus() != READY) {
            throw new AutoAssignmentIllegalStateException("Auto assignment not ready");
        }
        autoAssignment.setStatus(RUNNING);
        return jpaRepository.save(autoAssignment);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public AutoAssignment pause(final long autoAssignmentId) {
        final JpaAutoAssignment autoAssignment = jpaRepository.getById(autoAssignmentId);
        if (autoAssignment.getStatus() != RUNNING) {
            throw new AutoAssignmentIllegalStateException("Auto assignment not running");
        }
        autoAssignment.setStatus(PAUSED);
        return jpaRepository.save(autoAssignment);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public AutoAssignment resume(final long autoAssignmentId) {
        final JpaAutoAssignment autoAssignment = jpaRepository.getById(autoAssignmentId);
        if (autoAssignment.getStatus() != PAUSED) {
            throw new AutoAssignmentIllegalStateException("Auto assignment not paused");
        }
        autoAssignment.setStatus(RUNNING);
        return jpaRepository.save(autoAssignment);
    }

    private AutoAssignStatus resolveInitialAutoAssignStatus() {
        return TenantConfigHelper.getAsSystem(AUTO_ASSIGNMENT_APPROVAL_ENABLED, Boolean.class)
                ? WAITING_FOR_APPROVAL
                : READY;
    }

    private static Action.ActionType sanitizeAutoAssignActionType(final Action.ActionType actionType) {
        if (actionType == null) {
            return FORCED;
        }

        if (!ALLOWED_ACTION_TYPES.contains(actionType)) {
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
        Optional.ofNullable(create.getDistributionSet()).ifPresent(distributionSet -> {
            if (!distributionSet.isValid()) {
                throw new InvalidDistributionSetException();
            }
            if (!distributionSet.isComplete()) {
                throw new IncompleteDistributionSetException();
            }
        });
        Optional.ofNullable(create.getActionType()).ifPresent(actionType -> {
            if (!ALLOWED_ACTION_TYPES.contains(actionType)) {
                throw new InvalidAutoAssignActionTypeException();
            }
        });
        // validate the RSQL query syntax
        QLSupport.getInstance().validate(create.getTargetFilterQuery(), TargetFields.class, JpaTarget.class);

        // enforce the 'max targets per auto assign' quota right here even if the result of the filter query can vary over time
        create.setDistributionSet(distributionSetManagement.getValidAndComplete(create.getDistributionSet().getId()));
        DistributionSet distributionSet = create.getDistributionSet();

        assertMaxTargetsQuota(create.getTargetFilterQuery(), create.getName(), distributionSet.getId());

        if (distributionSetManagement.shouldLockImplicitly(distributionSet)) {
            distributionSetManagement.lock(distributionSet);
        }
    }
}