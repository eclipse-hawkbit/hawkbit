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
import java.util.Arrays;
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
import jakarta.validation.constraints.NotEmpty;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetMetadataFields;
import org.eclipse.hawkbit.repository.TimestampCalculator;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetUpdate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.TargetMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetMetadataRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
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

    private final TargetMetadataRepository targetMetadataRepository;

    private final RolloutGroupRepository rolloutGroupRepository;

    private final TargetFilterQueryRepository targetFilterQueryRepository;

    private final TargetTagRepository targetTagRepository;

    private final EventPublisherHolder eventPublisherHolder;

    private final TenantAware tenantAware;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;

    public JpaTargetManagement(final EntityManager entityManager,
            final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final TargetRepository targetRepository, final TargetTypeRepository targetTypeRepository,
            final TargetMetadataRepository targetMetadataRepository,
            final RolloutGroupRepository rolloutGroupRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository,
            final TargetTagRepository targetTagRepository, final EventPublisherHolder eventPublisherHolder,
            final TenantAware tenantAware, final VirtualPropertyReplacer virtualPropertyReplacer,
            final Database database) {
        this.entityManager = entityManager;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.targetRepository = targetRepository;
        this.targetTypeRepository = targetTypeRepository;
        this.targetMetadataRepository = targetMetadataRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.targetTagRepository = targetTagRepository;
        this.eventPublisherHolder = eventPublisherHolder;
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

        return targetRepository
                .exists(TargetSpecifications.hasInstalledOrAssignedDistributionSet(validDistSet.getId()));
    }

    @Override
    public long countByRsql(final String targetFilterQuery) {
        return JpaManagementHelper.countBySpec(targetRepository, List.of(RSQLUtility
                .buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database)));
    }

    @Override
    public long countByRsqlAndUpdatable(String targetFilterQuery) {
        final List<Specification<JpaTarget>> specList = List.of(RSQLUtility.buildRsqlSpecification(targetFilterQuery,
                TargetFields.class, virtualPropertyReplacer, database));
        return targetRepository.count(AccessController.Operation.UPDATE, combineWithAnd(specList));
    }

    @Override
    public long countByRsqlAndCompatible(final String targetFilterQuery, final Long distributionSetIdTypeId) {
        final List<Specification<JpaTarget>> specList = List.of(RSQLUtility.buildRsqlSpecification(
                        targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetIdTypeId));

        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public long countByRsqlAndCompatibleAndUpdatable(String targetFilterQuery, Long distributionSetIdTypeId) {
        final List<Specification<JpaTarget>> specList = List.of(RSQLUtility.buildRsqlSpecification(
                        targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetIdTypeId));
        return targetRepository.count(AccessController.Operation.UPDATE, combineWithAnd(specList));
    }

    @Override
    public long countByFailedInRollout(final String rolloutId, final Long dsTypeId) {
        final List<Specification<JpaTarget>> specList = List
                .of(TargetSpecifications.failedActionsForRollout(rolloutId));

        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public long countByTargetFilterQuery(final long targetFilterQueryId) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository.findById(targetFilterQueryId)
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
        final List<JpaTarget> targetList = targets.stream().map(JpaTargetCreate.class::cast).map(JpaTargetCreate::build)
                .toList();
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
    public Slice<Target> findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(final Pageable pageRequest,
            final long distributionSetId, final String targetFilterQuery) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.getOrElseThrowException(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        return targetRepository
                .findAllWithoutCount(AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class,
                                        virtualPropertyReplacer, database),
                                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId))),
                        pageRequest)
                .map(Target.class::cast);
    }

    @Override
    public long countByRsqlAndNonDSAndCompatibleAndUpdatable(final long distributionSetId,
            final String targetFilterQuery) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.getOrElseThrowException(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        return targetRepository.count(
                AccessController.Operation.UPDATE,
                combineWithAnd(List.of(
                        RSQLUtility.buildRsqlSpecification(
                                targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database),
                        TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                        TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId))));
    }

    @Override
    public Slice<Target> findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatable(
            final Pageable pageRequest, final Collection<Long> groups, final String targetFilterQuery,
            final DistributionSetType dsType) {
        return targetRepository
                .findAllWithoutCount(AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class,
                                        virtualPropertyReplacer, database),
                                TargetSpecifications.isNotInRolloutGroups(groups),
                                TargetSpecifications.isCompatibleWithDistributionSetType(dsType.getId()))),
                        pageRequest)
                .map(Target.class::cast);
    }

    @Override
    public Slice<Target> findByNotInGEGroupAndNotInActiveActionGEWeightOrInRolloutAndTargetFilterQueryAndCompatibleAndUpdatable(
            final Pageable pageRequest, final long rolloutId, final int weight, final long firstGroupId, final String targetFilterQuery,
            final DistributionSetType distributionSetType) {
        return targetRepository
                .findAllWithoutCount(AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class,
                                        virtualPropertyReplacer, database),
                                TargetSpecifications.isNotInGERolloutGroup(firstGroupId),
                                TargetSpecifications.hasNoActiveActionWithGEWeightOrInRollout(weight, rolloutId),
                                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetType.getId()))),
                        pageRequest)
                .map(Target.class::cast);
    }

    @Override
    public long countByActionsInRolloutGroup(final long rolloutGroupId) {
        return targetRepository.count(TargetSpecifications.isInActionRolloutGroup(rolloutGroupId));
    }

    @Override
    public Slice<Target> findByFailedRolloutAndNotInRolloutGroups(Pageable pageRequest, Collection<Long> groups,
            String rolloutId) {
        final List<Specification<JpaTarget>> specList = Arrays.asList(
                TargetSpecifications.failedActionsForRollout(rolloutId),
                TargetSpecifications.isNotInRolloutGroups(groups));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageRequest, specList);
    }

    @Override
    public long countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(final Collection<Long> groups,
            final String targetFilterQuery, final DistributionSetType dsType) {
        return targetRepository.count(AccessController.Operation.UPDATE,
                combineWithAnd(List.of(
                        RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class,
                                virtualPropertyReplacer, database),
                        TargetSpecifications.isNotInRolloutGroups(groups),
                        TargetSpecifications.isCompatibleWithDistributionSetType(dsType.getId()))));
    }

    @Override
    public long countByFailedRolloutAndNotInRolloutGroups(Collection<Long> groups, String rolloutId) {
        final List<Specification<JpaTarget>> specList = Arrays.asList(
                TargetSpecifications.failedActionsForRollout(rolloutId),
                TargetSpecifications.isNotInRolloutGroups(groups));

        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public Slice<Target> findByInRolloutGroupWithoutAction(final Pageable pageRequest, final long group) {
        if (!rolloutGroupRepository.existsById(group)) {
            throw new EntityNotFoundException(RolloutGroup.class, group);
        }

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageRequest,
                List.of(TargetSpecifications.hasNoActionInRolloutGroup(group)));
    }

    @Override
    public Page<Target> findByAssignedDistributionSet(final Pageable pageReq, final long distributionSetId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq,
                List.of(TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId())));
    }

    @Override
    public Page<Target> findByAssignedDistributionSetAndRsql(final Pageable pageReq, final long distributionSetId,
            final String rsqlParam) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        final List<Specification<JpaTarget>> specList = List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq, specList);
    }

    @Override
    public List<Target> getByControllerID(final Collection<String> controllerIDs) {
        return Collections.unmodifiableList(
                targetRepository.findAll(TargetSpecifications.byControllerIdWithAssignedDsInJoin(controllerIDs)));
    }

    @Override
    public Optional<Target> getByControllerID(final String controllerId) {
        return targetRepository.findByControllerId(controllerId).map(Target.class::cast);
    }

    @Override
    public Slice<Target> findByFilters(final Pageable pageable, final FilterParams filterParams) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    public Page<Target> findByInstalledDistributionSet(final Pageable pageReq, final long distributionSetId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq,
                List.of(TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId())));
    }

    @Override
    public Page<Target> findByInstalledDistributionSetAndRsql(final Pageable pageable, final long distributionSetId,
            final String rsqlParam) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        final List<Specification<JpaTarget>> specList = List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    public Page<Target> findByUpdateStatus(final Pageable pageable, final TargetUpdateStatus status) {
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable,
                List.of(TargetSpecifications.hasTargetUpdateStatus(status)));
    }

    @Override
    public Slice<Target> findAll(final Pageable pageable) {
        return targetRepository.findAllWithoutCount(pageable).map(Target.class::cast);
    }

    @Override
    public Slice<Target> findByRsql(final Pageable pageable, final String targetFilterQuery) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageable, List.of(RSQLUtility
                .buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database)));
    }

    @Override
    public Slice<Target> findByTargetFilterQuery(final Pageable pageable, final long targetFilterQueryId) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository.findById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageable,
                List.of(RSQLUtility.buildRsqlSpecification(targetFilterQuery.getQuery(), TargetFields.class,
                        virtualPropertyReplacer, database)));
    }

    @Override
    public Slice<Target> findByFilterOrderByLinkedDistributionSet(final Pageable pageable,
            final long orderByDistributionSetId, final FilterParams filterParams) {
        // remove default sort from pageable to not overwrite sorted spec
        final OffsetBasedPageRequest unsortedPage = new OffsetBasedPageRequest(pageable.getOffset(),
                pageable.getPageSize(), Sort.unsorted());

        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        specList.add(TargetSpecifications.orderedByLinkedDistributionSet(orderByDistributionSetId, pageable.getSort()));

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, unsortedPage, specList);
    }

    @Override
    public Page<Target> findByTag(final Pageable pageable, final long tagId) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable,
                List.of(TargetSpecifications.hasTag(tagId)));
    }

    @Override
    public Page<Target> findByRsqlAndTag(final Pageable pageable, final String rsqlParam, final long tagId) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasTag(tagId));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTypeAssignmentResult assignType(final Collection<String> controllerIds, final Long typeId) {
        final JpaTargetType type = targetTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, typeId));

        final List<JpaTarget> targetsWithSameType = findTargetsByInSpecification(controllerIds,
                TargetSpecifications.hasTargetType(typeId));

        final List<JpaTarget> targetsWithoutSameType = findTargetsByInSpecification(controllerIds,
                TargetSpecifications.hasTargetTypeNot(typeId));

        // set new target type to all targets without that type
        targetsWithoutSameType.forEach(target -> target.setTargetType(type));

        final TargetTypeAssignmentResult result = new TargetTypeAssignmentResult(targetsWithSameType.size(),
                targetRepository.saveAll(targetsWithoutSameType), Collections.emptyList(), type);

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
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).toList());
        }

        // set new target type to null for all targets
        allTargets.forEach(target -> target.setTargetType(null));

        return new TargetTypeAssignmentResult(0, Collections.emptyList(), targetRepository.saveAll(allTargets), null);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> assignTag(final Collection<String> controllerIds, final long targetTagId,
            final Consumer<Collection<String>> notFoundHandler) {
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
    public List<Target> unassignTag(final Collection<String> controllerIds, final long targetTagId,
            final Consumer<Collection<String>> notFoundHandler) {
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

        targetRepository.getAccessController().ifPresent(acm -> {
            acm.assertOperationAllowed(AccessController.Operation.UPDATE, target);
        });

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
    public Map<String, String> getControllerAttributes(final String controllerId) {
        getByControllerIdAndThrowIfNotFound(controllerId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        query.where(cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerId));

        final MapJoin<JpaTarget, String, String> attributes = targetRoot.join(JpaTarget_.controllerAttributes);
        query.multiselect(attributes.key(), attributes.value());
        query.orderBy(cb.asc(attributes.key()));

        final List<Object[]> attr = entityManager.createQuery(query).getResultList();

        return attr.stream().collect(Collectors.toMap(entry -> (String) entry[0], entry -> (String) entry[1],
                (v1, v2) -> v1, LinkedHashMap::new));
    }

    @Override
    public void requestControllerAttributes(final String controllerId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);
        targetRepository.getAccessController()
                .ifPresent(acm -> acm.assertOperationAllowed(AccessController.Operation.UPDATE, target));
        target.setRequestControllerAttributes(true);
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetAttributesRequestedEvent(tenantAware.getCurrentTenant(), target.getId(),
                        target.getControllerId(), target.getAddress() != null ? target.getAddress().toString() : null,
                        JpaTarget.class, eventPublisherHolder.getApplicationId()));
    }

    @Override
    public boolean isControllerAttributesRequested(final String controllerId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        return target.isRequestControllerAttributes();
    }

    @Override
    public Page<Target> findByControllerAttributesRequested(final Pageable pageReq) {
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq,
                List.of(TargetSpecifications.hasRequestControllerAttributesTrue()));
    }

    @Override
    public boolean existsByControllerId(final String controllerId) {
        return targetRepository.exists(TargetSpecifications.hasControllerId(controllerId));
    }

    @Override
    public boolean isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(final String controllerId,
            final long distributionSetId, final String targetFilterQuery) {
        RSQLUtility.validateRsqlFor(targetFilterQuery, TargetFields.class);
        final DistributionSet ds = distributionSetManagement.get(distributionSetId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, distributionSetId));
        final Long distSetTypeId = ds.getType().getId();
        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer,
                        database),
                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId),
                TargetSpecifications.hasControllerId(controllerId));

        final Specification<JpaTarget> combinedSpecification = Objects
                .requireNonNull(SpecificationsBuilder.combineWithAnd(specList));
        return targetRepository.exists(AccessController.Operation.UPDATE, combinedSpecification);
    }

    @Override
    public Set<TargetTag> getTagsByControllerId(@NotEmpty String controllerId) {
        // the method has PreAuthorized by itself
        return getByControllerID(controllerId).map(JpaTarget.class::cast).map(JpaTarget::getTags)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<TargetMetadata> createMetaData(final String controllerId, final Collection<MetaData> md) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        md.forEach(meta -> checkAndThrowIfTargetMetadataAlreadyExists(
                new TargetMetadataCompositeKey(target.getId(), meta.getKey())));

        assertMetaDataQuota(target.getId(), md.size());

        final JpaTarget updatedTarget = JpaManagementHelper.touch(entityManager, targetRepository, target);

        final List<TargetMetadata> createdMetadata = md.stream()
                .map(meta -> targetMetadataRepository
                        .save(new JpaTargetMetadata(meta.getKey(), meta.getValue(), updatedTarget)))
                .collect(Collectors.toUnmodifiableList());

        // TargetUpdatedEvent is not sent within the touch() method due to the
        // "lastModifiedAt" field being ignored in JpaTarget
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(updatedTarget, eventPublisherHolder.getApplicationId()));

        return createdMetadata;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final String controllerId, final String key) {
        final JpaTargetMetadata metadata = (JpaTargetMetadata) getMetaDataByControllerId(controllerId, key)
                .orElseThrow(() -> new EntityNotFoundException(TargetMetadata.class, controllerId, key));

        final JpaTarget target = JpaManagementHelper.touch(entityManager, targetRepository,
                getByControllerIdAndThrowIfNotFound(controllerId));

        targetRepository.getAccessController()
                .ifPresent(acm -> acm.assertOperationAllowed(AccessController.Operation.UPDATE, target));

        targetMetadataRepository.deleteById(metadata.getId());
        // target update event is set to ignore "lastModifiedAt" field, so it is
        // not send automatically within the touch() method
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(target, eventPublisherHolder.getApplicationId()));
    }

    @Override
    public Page<TargetMetadata> findMetaDataByControllerId(final Pageable pageable, final String controllerId) {
        final Long id = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return JpaManagementHelper.findAllWithCountBySpec(targetMetadataRepository, pageable,
                Collections.singletonList(metadataByTargetIdSpec(id)));
    }

    @Override
    public long countMetaDataByControllerId(@NotEmpty final String controllerId) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return JpaManagementHelper.countBySpec(targetMetadataRepository,
                Collections.singletonList(metadataByTargetIdSpec(targetId)));
    }

    @Override
    public Page<TargetMetadata> findMetaDataByControllerIdAndRsql(final Pageable pageable, final String controllerId,
            final String rsqlParam) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        final List<Specification<JpaTargetMetadata>> specList = Arrays.asList(RSQLUtility
                        .buildRsqlSpecification(rsqlParam, TargetMetadataFields.class, virtualPropertyReplacer, database),
                metadataByTargetIdSpec(targetId));

        return JpaManagementHelper.findAllWithCountBySpec(targetMetadataRepository, pageable, specList);
    }

    @Override
    public Optional<TargetMetadata> getMetaDataByControllerId(final String controllerId, final String key) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return targetMetadataRepository.findById(new TargetMetadataCompositeKey(targetId, key)).map(t -> t);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetMetadata updateMetadata(final String controllerId, final MetaData md) {

        // check if exists otherwise throw entity not found exception
        final JpaTargetMetadata updatedMetadata = (JpaTargetMetadata) getMetaDataByControllerId(controllerId,
                md.getKey())
                .orElseThrow(() -> new EntityNotFoundException(TargetMetadata.class, controllerId, md.getKey()));
        updatedMetadata.setValue(md.getValue());
        // touch it to update the lock revision because we are modifying the
        // target indirectly
        final JpaTarget target = JpaManagementHelper.touch(entityManager, targetRepository,
                getByControllerIdAndThrowIfNotFound(controllerId));

        final JpaTargetMetadata metadata = targetMetadataRepository.save(updatedMetadata);
        // target update event is set to ignore "lastModifiedAt" field, so it is
        // not send automatically within the touch() method
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(target, eventPublisherHolder.getApplicationId()));
        return metadata;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTagAssignmentResult toggleTagAssignment(final Collection<String> controllerIds, final String tagName) {
        final TargetTag tag = targetTagRepository
                .findByNameEquals(tagName)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagName));
        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));
        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).toList());
        }

        final List<JpaTarget> alreadyAssignedTargets = targetRepository.findAll(
                TargetSpecifications.hasTagName(tagName).and(TargetSpecifications.hasControllerIdIn(controllerIds)));

        // all are already assigned -> unassign
        if (alreadyAssignedTargets.size() == allTargets.size()) {

            alreadyAssignedTargets.forEach(target -> target.removeTag(tag));
            return new TargetTagAssignmentResult(0, Collections.emptyList(),
                    Collections.unmodifiableList(alreadyAssignedTargets), tag);
        }

        allTargets.removeAll(alreadyAssignedTargets);
        // some or none are assigned -> assign
        allTargets.forEach(target -> target.addTag(tag));
        final TargetTagAssignmentResult result = new TargetTagAssignmentResult(alreadyAssignedTargets.size(),
                targetRepository.saveAll(allTargets), Collections.emptyList(), tag);

        // no reason to persist the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target unassignTag(final String controllerId, final long targetTagId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        final TargetTag tag = targetTagRepository.findById(targetTagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, targetTagId));

        target.removeTag(tag);

        final Target result = targetRepository.save(target);

        // No reason to save the tag
        entityManager.detach(tag);
        return result;
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

    private void checkAndThrowIfTargetMetadataAlreadyExists(final TargetMetadataCompositeKey metadataId) {
        if (targetMetadataRepository.existsById(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    private void assertMetaDataQuota(final Long targetId, final int requested) {
        QuotaHelper.assertAssignmentQuota(targetId, requested, quotaManagement.getMaxMetaDataEntriesPerTarget(),
                TargetMetadata.class, Target.class, targetMetadataRepository::countByTargetId);
    }

    private Specification<JpaTargetMetadata> metadataByTargetIdSpec(final Long targetId) {
        return (root, query, cb) -> cb.equal(root.get(JpaTargetMetadata_.target).get(JpaTarget_.id), targetId);
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

    private List<JpaTarget> findTargetsByInSpecification(final Collection<String> controllerIds,
            final Specification<JpaTarget> specification) {
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
