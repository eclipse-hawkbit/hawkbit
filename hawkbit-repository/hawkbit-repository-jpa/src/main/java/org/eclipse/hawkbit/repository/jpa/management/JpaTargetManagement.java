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

import static org.eclipse.hawkbit.repository.jpa.JpaManagementHelper.combineWithAnd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.validation.constraints.NotEmpty;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TimestampCalculator;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetUpdate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
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

/**
 * JPA implementation of {@link TargetManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetManagement implements TargetManagement {

    private final EntityManager entityManager;
    private final DistributionSetManagement distributionSetManagement;
    private final QuotaManagement quotaManagement;
    private final TargetRepository targetRepository;
    private final TargetTypeRepository targetTypeRepository;
    private final RolloutGroupRepository rolloutGroupRepository;
    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final TargetTagRepository targetTagRepository;
    private final TenantAware tenantAware;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    @SuppressWarnings("java:S107")
    public JpaTargetManagement(final EntityManager entityManager,
            final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final TargetRepository targetRepository, final TargetTypeRepository targetTypeRepository,
            final RolloutGroupRepository rolloutGroupRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository,
            final TargetTagRepository targetTagRepository,
            final TenantAware tenantAware, final VirtualPropertyReplacer virtualPropertyReplacer,
            final Database database) {
        this.entityManager = entityManager;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.targetRepository = targetRepository;
        this.targetTypeRepository = targetTypeRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.targetTagRepository = targetTagRepository;
        this.tenantAware = tenantAware;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    @Override
    public long countByAssignedDistributionSet(final long distributionSetId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException((distributionSetId));
        return targetRepository.count(TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId()));
    }

    @Override
    public long countByFilters(final FilterParams filterParams) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public long countByInstalledDistributionSet(final long distributionSetId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException((distributionSetId));
        return targetRepository.count(TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId()));
    }

    @Override
    public boolean existsByInstalledOrAssignedDistributionSet(final long distributionSetId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException((distributionSetId));
        return targetRepository.exists(TargetSpecifications.hasInstalledOrAssignedDistributionSet(validDistSet.getId()));
    }

    @Override
    public long countByRsql(final String rsql) {
        return JpaManagementHelper.countBySpec(
                targetRepository,
                List.of(RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database)));
    }

    @Override
    public long countByRsqlAndUpdatable(String rsql) {
        final List<Specification<JpaTarget>> specList = List.of(
                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database));
        return targetRepository.count(
                AccessController.Operation.UPDATE,
                combineWithAnd(specList));
    }

    @Override
    public long countByRsqlAndCompatible(final String rsql, final Long distributionSetIdTypeId) {
        final List<Specification<JpaTarget>> specList = List.of(
                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetIdTypeId));
        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public long countByRsqlAndCompatibleAndUpdatable(String rsql, Long distributionSetIdTypeId) {
        final List<Specification<JpaTarget>> specList = List.of(
                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetIdTypeId));
        return targetRepository.count(AccessController.Operation.UPDATE, combineWithAnd(specList));
    }

    @Override
    public long countByFailedInRollout(final String rolloutId, final Long dsTypeId) {
        final List<Specification<JpaTarget>> specList = List.of(TargetSpecifications.failedActionsForRollout(rolloutId));
        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public long countByTargetFilterQuery(final long targetFilterQueryId) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository
                .findById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));
        return countByRsql(targetFilterQuery.getQuery());
    }

    @Override
    public long count() {
        return targetRepository.count();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target create(final TargetCreate c) {
        final JpaTargetCreate create = (JpaTargetCreate) c;
        return targetRepository.save(AccessController.Operation.CREATE, create.build());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> create(final Collection<TargetCreate> targets) {
        final List<JpaTarget> targetList = targets.stream().map(JpaTargetCreate.class::cast).map(JpaTargetCreate::build).toList();
        return Collections.unmodifiableList(targetRepository.saveAll(AccessController.Operation.CREATE, targetList));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaTarget> targets = targetRepository.findAllById(ids);
        if (targets.size() < ids.size()) {
            throw new EntityNotFoundException(Target.class, ids,
                    targets.stream().map(Target::getId).filter(id -> !ids.contains(id)).toList());
        }
        targetRepository.deleteAll(targets);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteByControllerID(final String controllerId) {
        targetRepository.delete(getByControllerIdAndThrowIfNotFound(controllerId));
    }

    @Override
    public Slice<Target> findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(final long distributionSetId, final String rsql,
            final Pageable pageable) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.getOrElseThrowException(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        return targetRepository
                .findAllWithoutCount(
                        AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId))),
                        pageable)
                .map(Target.class::cast);
    }

    @Override
    public long countByRsqlAndNonDSAndCompatibleAndUpdatable(final long distributionSetId, final String rsql) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.getOrElseThrowException(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        return targetRepository.count(
                AccessController.Operation.UPDATE,
                combineWithAnd(List.of(
                        RsqlUtility.buildRsqlSpecification(
                                rsql, TargetFields.class, virtualPropertyReplacer, database),
                        TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                        TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId))));
    }

    @Override
    public Slice<Target> findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
            final Collection<Long> groups, final String rsql, final DistributionSetType dsType, final Pageable pageable) {
        return targetRepository
                .findAllWithoutCount(AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                                TargetSpecifications.isNotInRolloutGroups(groups),
                                TargetSpecifications.isCompatibleWithDistributionSetType(dsType.getId()))),
                        pageable)
                .map(Target.class::cast);
    }

    @Override
    public long countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
            final String rsql, final Collection<Long> groups, final DistributionSetType dsType) {
        return targetRepository.count(AccessController.Operation.UPDATE,
                combineWithAnd(List.of(
                        RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                        TargetSpecifications.isNotInRolloutGroups(groups),
                        TargetSpecifications.isCompatibleWithDistributionSetType(dsType.getId()))));
    }

    @Override
    public Slice<Target> findByFailedRolloutAndNotInRolloutGroups(String rolloutId, Collection<Long> groups, Pageable pageable) {
        final List<Specification<JpaTarget>> specList = List.of(
                TargetSpecifications.failedActionsForRollout(rolloutId),
                TargetSpecifications.isNotInRolloutGroups(groups));
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, specList, pageable);
    }

    @Override
    public long countByFailedRolloutAndNotInRolloutGroups(String rolloutId, Collection<Long> groups) {
        final List<Specification<JpaTarget>> specList = List.of(
                TargetSpecifications.failedActionsForRollout(rolloutId),
                TargetSpecifications.isNotInRolloutGroups(groups));
        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public Slice<Target> findByRsqlAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable(
            final long rolloutId, final String rsql, final DistributionSetType distributionSetType, final Pageable pageable) {
        return targetRepository
                .findAllWithoutCount(AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                                TargetSpecifications.hasNoOverridingActionsAndNotInRollout(rolloutId),
                                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetType.getId()))),
                        pageable)
                .map(Target.class::cast);
    }

    @Override
    public long countByActionsInRolloutGroup(final long rolloutGroupId) {
        return targetRepository.count(TargetSpecifications.isInActionRolloutGroup(rolloutGroupId));
    }

    @Override
    public Slice<Target> findByInRolloutGroupWithoutAction(final long group, final Pageable pageable) {
        if (!rolloutGroupRepository.existsById(group)) {
            throw new EntityNotFoundException(RolloutGroup.class, group);
        }

        return JpaManagementHelper.findAllWithoutCountBySpec(
                targetRepository, List.of(TargetSpecifications.hasNoActionInRolloutGroup(group)), pageable);
    }

    @Override
    public Page<Target> findByAssignedDistributionSet(final long distributionSetId, final Pageable pageable) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(
                targetRepository,
                List.of(TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId())), pageable);
    }

    @Override
    public Page<Target> findByAssignedDistributionSetAndRsql(final long distributionSetId, final String rsql, final Pageable pageable) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        final List<Specification<JpaTarget>> specList = List.of(
                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, specList, pageable);
    }

    @Override
    public List<Target> getByControllerID(final Collection<String> controllerIDs) {
        return Collections.unmodifiableList(targetRepository.findAll(TargetSpecifications.byControllerIdWithAssignedDsInJoin(controllerIDs)));
    }

    @Override
    public Optional<Target> getByControllerID(final String controllerId) {
        return targetRepository.findByControllerId(controllerId).map(Target.class::cast);
    }

    @Override
    public Target getWithDetails(final String controllerId, final String detailsKey) {
        return targetRepository.getWithDetailsByControllerId(controllerId, "Target." + detailsKey);
    }

    @Override
    public Slice<Target> findByFilters(final FilterParams filterParams, final Pageable pageable) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, specList, pageable);
    }

    @Override
    public Page<Target> findByInstalledDistributionSet(final long distributionSetId, final Pageable pageReq) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(
                targetRepository, List.of(TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId())), pageReq);
    }

    @Override
    public Page<Target> findByInstalledDistributionSetAndRsql(final long distributionSetId, final String rsql, final Pageable pageable) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        final List<Specification<JpaTarget>> specList = List.of(
                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, specList, pageable);
    }

    @Override
    public Page<Target> findByUpdateStatus(final TargetUpdateStatus status, final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(
                targetRepository, List.of(TargetSpecifications.hasTargetUpdateStatus(status)), pageable);
    }

    @Override
    public Slice<Target> findAll(final Pageable pageable) {
        return targetRepository.findAllWithoutCount(pageable).map(Target.class::cast);
    }

    @Override
    public Slice<Target> findByRsql(final String rsql, final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(
                targetRepository,
                List.of(RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database)), pageable
        );
    }

    @Override
    public Slice<Target> findByTargetFilterQuery(final long targetFilterQueryId, final Pageable pageable) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository.findById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));

        return JpaManagementHelper.findAllWithoutCountBySpec(
                targetRepository, List.of(RsqlUtility.buildRsqlSpecification(
                        targetFilterQuery.getQuery(), TargetFields.class, virtualPropertyReplacer, database)), pageable
        );
    }

    @Override
    public Page<Target> findByTag(final long tagId, final Pageable pageable) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, List.of(TargetSpecifications.hasTag(tagId)), pageable);
    }

    @Override
    public Page<Target> findByRsqlAndTag(final String rsql, final long tagId, final Pageable pageable) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        final List<Specification<JpaTarget>> specList = List.of(
                RsqlUtility.buildRsqlSpecification(rsql, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasTag(tagId));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, specList, pageable);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTypeAssignmentResult assignType(final Collection<String> controllerIds, final Long typeId) {
        final JpaTargetType type = targetTypeRepository
                .findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, typeId));

        final List<JpaTarget> targetsWithSameType = findTargetsByInSpecification(controllerIds, TargetSpecifications.hasTargetType(typeId));
        final List<JpaTarget> targetsWithoutSameType =
                findTargetsByInSpecification(controllerIds, TargetSpecifications.hasTargetTypeNot(typeId));

        // set new target type to all targets without that type
        targetsWithoutSameType.forEach(target -> target.setTargetType(type));

        final TargetTypeAssignmentResult result = new TargetTypeAssignmentResult(
                targetsWithSameType.size(), targetRepository.saveAll(targetsWithoutSameType), Collections.emptyList(), type);

        // no reason to persist the type
        entityManager.detach(type);
        return result;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTypeAssignmentResult unassignType(final Collection<String> controllerIds) {
        final List<JpaTarget> allTargets = findTargetsByInSpecification(controllerIds, null);

        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds, allTargets.stream().map(Target::getControllerId).toList());
        }

        // set new target type to null for all targets
        allTargets.forEach(target -> target.setTargetType(null));

        return new TargetTypeAssignmentResult(0, Collections.emptyList(), targetRepository.saveAll(allTargets), null);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> assignTag(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return assignTag0(controllerIds, targetTagId, notFoundHandler);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> assignTag(final Collection<String> controllerIds, final long targetTagId) {
        return assignTag0(controllerIds, targetTagId, null);
    }

    private List<Target> assignTag0(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return updateTag(controllerIds, targetTagId, notFoundHandler, (tag, target) -> {
            if (target.getTags().contains(tag)) {
                return target;
            } else {
                target.addTag(tag);
                return targetRepository.save(target);
            }
        });
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> unassignTag(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return unassignTag0(controllerIds, targetTagId, notFoundHandler);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> unassignTag(final Collection<String> controllerIds, final long targetTagId) {
        return unassignTag0(controllerIds, targetTagId, null);
    }

    private List<Target> unassignTag0(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return updateTag(controllerIds, targetTagId, notFoundHandler, (tag, target) -> {
            if (target.getTags().contains(tag)) {
                target.removeTag(tag);
                return targetRepository.save(target);
            } else {
                return target;
            }
        });
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target unassignType(final String controllerId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);
        target.setTargetType(null);
        return targetRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target assignType(final String controllerId, final Long targetTypeId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        targetRepository.getAccessController().ifPresent(acm ->
                acm.assertOperationAllowed(AccessController.Operation.UPDATE, target));

        final JpaTargetType targetType = getTargetTypeByIdAndThrowIfNotFound(targetTypeId);
        target.setTargetType(targetType);
        return targetRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target update(final TargetUpdate u) {
        final JpaTargetUpdate update = (JpaTargetUpdate) u;

        final JpaTarget target = getByControllerIdAndThrowIfNotFound(update.getControllerId());

        update.getName().ifPresent(target::setName);
        update.getDescription().ifPresent(target::setDescription);
        update.getAddress().ifPresent(target::setAddress);
        update.getSecurityToken().ifPresent(target::setSecurityToken);
        if (update.getTargetTypeId() != null) {
            final TargetType targetType = getTargetTypeByIdAndThrowIfNotFound(update.getTargetTypeId());
            target.setTargetType(targetType);
        }

        return targetRepository.save(target);
    }

    @Override
    public Optional<Target> get(final long id) {
        return targetRepository.findById(id).map(Target.class::cast);
    }

    @Override
    public List<Target> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(targetRepository.findAll(TargetSpecifications.hasIdIn(ids)));
    }

    @Override
    public boolean existsByControllerId(final String controllerId) {
        return targetRepository.exists(TargetSpecifications.hasControllerId(controllerId));
    }

    @Override
    public boolean isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
            final String controllerId, final long distributionSetId, final String targetFilterQuery) {
        RsqlUtility.validateRsqlFor(targetFilterQuery, TargetFields.class, JpaTarget.class, virtualPropertyReplacer, entityManager);
        final DistributionSet ds = distributionSetManagement.get(distributionSetId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, distributionSetId));
        final Long distSetTypeId = ds.getType().getId();
        final List<Specification<JpaTarget>> specList = List.of(
                RsqlUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId),
                TargetSpecifications.hasControllerId(controllerId));

        final Specification<JpaTarget> combinedSpecification = Objects
                .requireNonNull(SpecificationsBuilder.combineWithAnd(specList));
        return targetRepository.exists(AccessController.Operation.UPDATE, combinedSpecification);
    }

    @Override
    public Set<TargetTag> getTags(@NotEmpty String controllerId) {
        // the method has PreAuthorized by itself
        return ((JpaTarget) getWithTags(controllerId)).getTags();
    }

    @Override
    public Map<String, String> getControllerAttributes(final String controllerId) {
        return getMap(controllerId, JpaTarget_.controllerAttributes);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void requestControllerAttributes(final String controllerId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);
        targetRepository.getAccessController()
                .ifPresent(acm -> acm.assertOperationAllowed(AccessController.Operation.UPDATE, target));
        target.setRequestControllerAttributes(true);
        AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(() ->
                EventPublisherHolder.getInstance().getEventPublisher()
                        .publishEvent(new TargetAttributesRequestedEvent(
                                tenantAware.getCurrentTenant(), target.getId(), JpaTarget.class, target.getControllerId(),
                                target.getAddress() != null ? target.getAddress().toString() : null
                        )));
    }

    @Override
    public boolean isControllerAttributesRequested(final String controllerId) {
        return getByControllerIdAndThrowIfNotFound(controllerId).isRequestControllerAttributes();
    }

    @Override
    public Page<Target> findByControllerAttributesRequested(final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(
                targetRepository, List.of(TargetSpecifications.hasRequestControllerAttributesTrue()), pageable);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void createMetadata(final String controllerId, final Map<String, String> md) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        // get the modifiable metadata map
        final Map<String, String> metadata = target.getMetadata();
        md.keySet().forEach(key -> {
            if (metadata.containsKey(key)) {
                throw new EntityAlreadyExistsException("Metadata entry with key '" + key + "' already exists");
            }
        });
        metadata.putAll(md);

        assertMetadataQuota(target.getId(), metadata.size());
        targetRepository.save(target);
    }

    @Override
    public Map<String, String> getMetadata(final String controllerId) {
        return getMap(controllerId, JpaTarget_.metadata);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void updateMetadata(final String controllerId, final String key, final String value) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        // get the modifiable metadata map
        final Map<String, String> metadata = target.getMetadata();
        if (!metadata.containsKey(key)) {
            throw new EntityNotFoundException("Target metadata", controllerId + ":" + key);
        }
        metadata.put(key, value);

        targetRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetadata(final String controllerId, final String key) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        // get the modifiable metadata map
        final Map<String, String> metadata = target.getMetadata();
        if (metadata.remove(key) == null) {
            throw new EntityNotFoundException("Target metadata", controllerId + ":" + key);
        }

        targetRepository.save(target);
    }

    private Map<String, String> getMap(final String controllerId, final MapAttribute<JpaTarget, String, String> mapAttribute) {
        getByControllerIdAndThrowIfNotFound(controllerId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        query.where(cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerId));

        final MapJoin<JpaTarget, String, String> mapJoin = targetRoot.join(mapAttribute);
        query.multiselect(mapJoin.key(), mapJoin.value());
        query.orderBy(cb.asc(mapJoin.key()));

        return entityManager
                .createQuery(query)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(entry -> (String) entry[0], entry -> (String) entry[1], (v1, v2) -> v1, LinkedHashMap::new));
    }

    private static boolean hasTagsFilterActive(final FilterParams filterParams) {
        final boolean isNoTagActive = Boolean.TRUE.equals(filterParams.getSelectTargetWithNoTag());
        final boolean isAtLeastOneTagActive = filterParams.getFilterByTagNames() != null
                && filterParams.getFilterByTagNames().length > 0;

        return isNoTagActive || isAtLeastOneTagActive;
    }

    private static boolean hasTypesFilterActive(final FilterParams filterParams) {
        return filterParams.getFilterByTargetType() != null;
    }

    private static boolean hasNoTypeFilterActive(final FilterParams filterParams) {
        return Boolean.TRUE.equals(filterParams.getSelectTargetWithNoTargetType());
    }

    private static Collection<String> notFound(final Collection<String> controllerIds, final List<JpaTarget> foundTargets) {
        final Map<String, JpaTarget> foundTargetMap = foundTargets.stream()
                .collect(Collectors.toMap(Target::getControllerId, Function.identity()));
        return controllerIds.stream().filter(id -> !foundTargetMap.containsKey(id)).toList();
    }

    private JpaTarget getByControllerIdAndThrowIfNotFound(final String controllerId) {
        return targetRepository.getByControllerId(controllerId);
    }

    private JpaTargetType getTargetTypeByIdAndThrowIfNotFound(final long id) {
        return targetTypeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(TargetType.class, id));
    }

    private void assertMetadataQuota(final Long targetId, final int requested) {
        final int limit = quotaManagement.getMaxMetaDataEntriesPerTarget();
        QuotaHelper.assertAssignmentQuota(targetId, requested, limit, "Metadata", Target.class.getSimpleName(), null);
    }

    private List<Specification<JpaTarget>> buildSpecificationList(final FilterParams filterParams) {
        final List<Specification<JpaTarget>> specList = new ArrayList<>();
        if ((filterParams.getFilterByStatus() != null) && !filterParams.getFilterByStatus().isEmpty()) {
            specList.add(TargetSpecifications.hasTargetUpdateStatus(filterParams.getFilterByStatus()));
        }
        if (filterParams.getOverdueState() != null && filterParams.getOverdueState()) {
            specList.add(TargetSpecifications.isOverdue(TimestampCalculator.calculateOverdueTimestamp()));
        }
        if (filterParams.getFilterByDistributionId() != null) {
            final DistributionSet validDistSet = distributionSetManagement
                    .getOrElseThrowException(filterParams.getFilterByDistributionId());

            specList.add(TargetSpecifications.hasInstalledOrAssignedDistributionSet(validDistSet.getId()));
        }
        if (!ObjectUtils.isEmpty(filterParams.getFilterBySearchText())) {
            specList.add(TargetSpecifications.likeControllerIdOrName(filterParams.getFilterBySearchText()));
        }
        if (hasTagsFilterActive(filterParams)) {
            specList.add(TargetSpecifications.hasTags(filterParams.getFilterByTagNames(),
                    filterParams.getSelectTargetWithNoTag()));
        }

        if (hasTypesFilterActive(filterParams)) {
            specList.add(TargetSpecifications.hasTargetType(filterParams.getFilterByTargetType()));
        } else if (hasNoTypeFilterActive(filterParams)) {
            specList.add(TargetSpecifications.hasNoTargetType());
        }

        return specList;
    }

    private List<JpaTarget> findTargetsByInSpecification(final Collection<String> controllerIds, final Specification<JpaTarget> specification) {
        return ListUtils.partition(new ArrayList<>(controllerIds), Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository.findAll(TargetSpecifications.hasControllerIdIn(ids).and(specification)))
                .flatMap(List::stream).toList();
    }

    private List<Target> updateTag(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler,
            final BiFunction<JpaTargetTag, JpaTarget, Target> updater) {
        final JpaTargetTag tag = targetTagRepository.findById(targetTagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, targetTagId));
        final List<JpaTarget> targets = controllerIds.size() == 1 ?
                targetRepository.findByControllerId(controllerIds.iterator().next())
                        .map(List::of)
                        .orElseGet(Collections::emptyList) :
                targetRepository
                        .findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));
        if (targets.size() < controllerIds.size()) {
            final Collection<String> notFound = notFound(controllerIds, targets);
            if (notFoundHandler == null) {
                throw new EntityNotFoundException(Target.class, notFound);
            } else {
                notFoundHandler.accept(notFound);
            }
        }

        targetRepository.getAccessController()
                .ifPresent(acm -> acm.assertOperationAllowed(AccessController.Operation.UPDATE, targets));

        try {
            return targets.stream().map(target -> updater.apply(tag, target)).toList();
        } finally {
            // No reason to save the tag
            entityManager.detach(tag);
        }
    }

    private void throwEntityNotFoundExceptionIfTagDoesNotExist(final Long tagId) {
        if (!targetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(TargetTag.class, tagId);
        }
    }
}