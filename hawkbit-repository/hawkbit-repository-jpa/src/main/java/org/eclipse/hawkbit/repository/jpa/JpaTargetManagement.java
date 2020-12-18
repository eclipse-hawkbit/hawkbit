/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetMetadataFields;
import org.eclipse.hawkbit.repository.TimestampCalculator;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetUpdate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.TargetMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
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
 * JPA implementation of {@link TargetManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetManagement implements TargetManagement {

    private final EntityManager entityManager;

    private final QuotaManagement quotaManagement;

    private final TargetRepository targetRepository;

    private final TargetMetadataRepository targetMetadataRepository;

    private final RolloutGroupRepository rolloutGroupRepository;

    private final DistributionSetRepository distributionSetRepository;

    private final TargetFilterQueryRepository targetFilterQueryRepository;

    private final TargetTagRepository targetTagRepository;

    private final NoCountPagingRepository criteriaNoCountDao;

    private final EventPublisherHolder eventPublisherHolder;

    private final TenantAware tenantAware;

    private final AfterTransactionCommitExecutor afterCommit;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;

    public JpaTargetManagement(final EntityManager entityManager, final QuotaManagement quotaManagement,
            final TargetRepository targetRepository, final TargetMetadataRepository targetMetadataRepository,
            final RolloutGroupRepository rolloutGroupRepository,
            final DistributionSetRepository distributionSetRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository,
            final TargetTagRepository targetTagRepository, final NoCountPagingRepository criteriaNoCountDao,
            final EventPublisherHolder eventPublisherHolder, final TenantAware tenantAware,
            final AfterTransactionCommitExecutor afterCommit, final VirtualPropertyReplacer virtualPropertyReplacer,
            final Database database) {
        this.entityManager = entityManager;
        this.quotaManagement = quotaManagement;
        this.targetRepository = targetRepository;
        this.targetMetadataRepository = targetMetadataRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.targetTagRepository = targetTagRepository;
        this.criteriaNoCountDao = criteriaNoCountDao;
        this.eventPublisherHolder = eventPublisherHolder;
        this.tenantAware = tenantAware;
        this.afterCommit = afterCommit;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    @Override
    public Optional<Target> getByControllerID(final String controllerId) {
        return targetRepository.findByControllerId(controllerId);
    }

    private JpaTarget getByControllerIdAndThrowIfNotFound(final String controllerId) {
        return targetRepository.findByControllerId(controllerId).map(JpaTarget.class::cast)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    @Override
    public List<Target> getByControllerID(final Collection<String> controllerIDs) {
        return Collections.unmodifiableList(
                targetRepository.findAll(TargetSpecifications.byControllerIdWithAssignedDsInJoin(controllerIDs)));
    }

    @Override
    public long count() {
        return targetRepository.count();
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<TargetMetadata> createMetaData(final String controllerId, final Collection<MetaData> md) {

        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        md.forEach(meta -> checkAndThrowIfTargetMetadataAlreadyExists(
                new TargetMetadataCompositeKey(target.getId(), meta.getKey())));

        assertMetaDataQuota(target.getId(), md.size());

        final JpaTarget updatedTarget = touch(target);

        final List<TargetMetadata> createdMetadata = Collections.unmodifiableList(md.stream()
                .map(meta -> targetMetadataRepository
                        .save(new JpaTargetMetadata(meta.getKey(), meta.getValue(), updatedTarget)))
                .collect(Collectors.toList()));

        // TargetUpdatedEvent is not sent within the touch() method due to the
        // "lastModifiedAt" field being ignored in JpaTarget
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(updatedTarget, eventPublisherHolder.getApplicationId()));

        return createdMetadata;
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

    private JpaTarget touch(final JpaTarget target) {

        // merge base target so optLockRevision gets updated and audit
        // log written because modifying metadata is modifying the base
        // target itself for auditing purposes.
        final JpaTarget result = entityManager.merge(target);
        result.setLastModifiedAt(0L);

        return targetRepository.save(result);
    }

    private JpaTarget touch(final String controllerId) {
        return touch(getByControllerIdAndThrowIfNotFound(controllerId));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetMetadata updateMetadata(final String controllerId, final MetaData md) {

        // check if exists otherwise throw entity not found exception
        final JpaTargetMetadata updatedMetadata = (JpaTargetMetadata) getMetaDataByControllerId(controllerId,
                md.getKey()).orElseThrow(
                        () -> new EntityNotFoundException(TargetMetadata.class, controllerId, md.getKey()));
        updatedMetadata.setValue(md.getValue());
        // touch it to update the lock revision because we are modifying the
        // target indirectly
        final JpaTarget target = touch(controllerId);
        final JpaTargetMetadata matadata = targetMetadataRepository.save(updatedMetadata);
        // target update event is set to ignore "lastModifiedAt" field so it is
        // not send automatically within the touch() method
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(target, eventPublisherHolder.getApplicationId()));
        return matadata;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final String controllerId, final String key) {
        final JpaTargetMetadata metadata = (JpaTargetMetadata) getMetaDataByControllerId(controllerId, key)
                .orElseThrow(() -> new EntityNotFoundException(TargetMetadata.class, controllerId, key));

        final JpaTarget target = touch(controllerId);
        targetMetadataRepository.deleteById(metadata.getId());
        // target update event is set to ignore "lastModifiedAt" field so it is
        // not send automatically within the touch() method
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(target, eventPublisherHolder.getApplicationId()));
    }

    @Override
    public Page<TargetMetadata> findMetaDataByControllerId(final Pageable pageable, final String controllerId) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return convertMdPage(
                targetMetadataRepository
                        .findAll(
                                (Specification<JpaTargetMetadata>) (root, query, cb) -> cb
                                        .equal(root.get(JpaTargetMetadata_.target).get(JpaTarget_.id), targetId),
                                pageable),
                pageable);
    }

    private static Page<TargetMetadata> convertMdPage(final Page<JpaTargetMetadata> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<TargetMetadata> findMetaDataByControllerIdAndRsql(final Pageable pageable, final String controllerId,
            final String rsqlParam) {

        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        final Specification<JpaTargetMetadata> spec = RSQLUtility.parse(rsqlParam, TargetMetadataFields.class,
                virtualPropertyReplacer, database);

        return convertMdPage(targetMetadataRepository.findAll((Specification<JpaTargetMetadata>) (root, query, cb) -> cb
                .and(cb.equal(root.get(JpaTargetMetadata_.target).get(JpaTarget_.id), targetId),
                        spec.toPredicate(root, query, cb)),
                pageable), pageable);
    }

    @Override
    public Optional<TargetMetadata> getMetaDataByControllerId(final String controllerId, final String key) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return targetMetadataRepository.findById(new TargetMetadataCompositeKey(targetId, key)).map(t -> t);
    }

    @Override
    public Slice<Target> findAll(final Pageable pageable) {
        return convertPage(criteriaNoCountDao.findAll(pageable, JpaTarget.class), pageable);
    }

    @Override
    public Slice<Target> findByTargetFilterQuery(final Pageable pageable, final long targetFilterQueryId) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository.findById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));

        return findTargetsBySpec(
                RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class, virtualPropertyReplacer, database),
                pageable);
    }

    @Override
    public Page<Target> findByRsql(final Pageable pageable, final String targetFilterQuery) {
        return findTargetsBySpec(
                RSQLUtility.parse(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database), pageable);
    }

    private Page<Target> findTargetsBySpec(final Specification<JpaTarget> spec, final Pageable pageable) {
        return convertPage(targetRepository.findAll(spec, pageable), pageable);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target update(final TargetUpdate u) {
        final JpaTargetUpdate update = (JpaTargetUpdate) u;

        final JpaTarget target = getByControllerIdAndThrowIfNotFound(update.getControllerId());

        update.getName().ifPresent(target::setName);
        update.getDescription().ifPresent(target::setDescription);
        update.getAddress().ifPresent(target::setAddress);
        update.getSecurityToken().ifPresent(target::setSecurityToken);

        return targetRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> targetIDs) {
        final List<JpaTarget> targets = targetRepository.findAllById(targetIDs);

        if (targets.size() < targetIDs.size()) {
            throw new EntityNotFoundException(Target.class, targetIDs,
                    targets.stream().map(Target::getId).collect(Collectors.toList()));
        }

        targetRepository.deleteByIdIn(targetIDs);

        afterCommit
                .afterCommit(() -> targets.forEach(target -> eventPublisherHolder.getEventPublisher()
                        .publishEvent(new TargetDeletedEvent(tenantAware.getCurrentTenant(), target.getId(),
                                target.getControllerId(),
                                Optional.ofNullable(target.getAddress()).map(URI::toString).orElse(null),
                                JpaTarget.class.getName(), eventPublisherHolder.getApplicationId()))));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteByControllerID(final String controllerID) {
        final Target target = getByControllerIdAndThrowIfNotFound(controllerID);

        targetRepository.deleteById(target.getId());
    }

    @Override
    public Page<Target> findByAssignedDistributionSet(final Pageable pageReq, final long distributionSetID) {
        throwEntityNotFoundIfDsDoesNotExist(distributionSetID);

        return targetRepository.findByAssignedDistributionSetId(pageReq, distributionSetID);
    }

    @Override
    public Page<Target> findByAssignedDistributionSetAndRsql(final Pageable pageReq, final long distributionSetID,
            final String rsqlParam) {
        throwEntityNotFoundIfDsDoesNotExist(distributionSetID);

        final Specification<JpaTarget> spec = RSQLUtility.parse(rsqlParam, TargetFields.class, virtualPropertyReplacer,
                database);

        return convertPage(
                targetRepository
                        .findAll((Specification<JpaTarget>) (root, query,
                                cb) -> cb.and(TargetSpecifications.hasAssignedDistributionSet(distributionSetID)
                                        .toPredicate(root, query, cb), spec.toPredicate(root, query, cb)),
                                pageReq),
                pageReq);
    }

    private void throwEntityNotFoundIfDsDoesNotExist(final Long distributionSetID) {
        if (!distributionSetRepository.existsById(distributionSetID)) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetID);
        }
    }

    private static Page<Target> convertPage(final Page<JpaTarget> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Slice<Target> convertPage(final Slice<JpaTarget> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, 0);
    }

    @Override
    public Page<Target> findByInstalledDistributionSet(final Pageable pageReq, final long distributionSetID) {
        throwEntityNotFoundIfDsDoesNotExist(distributionSetID);
        return targetRepository.findByInstalledDistributionSetId(pageReq, distributionSetID);
    }

    @Override
    public Page<Target> findByInstalledDistributionSetAndRsql(final Pageable pageable, final long distributionSetId,
            final String rsqlParam) {
        throwEntityNotFoundIfDsDoesNotExist(distributionSetId);

        final Specification<JpaTarget> spec = RSQLUtility.parse(rsqlParam, TargetFields.class, virtualPropertyReplacer,
                database);

        return convertPage(
                targetRepository
                        .findAll((Specification<JpaTarget>) (root, query,
                                cb) -> cb.and(TargetSpecifications.hasInstalledDistributionSet(distributionSetId)
                                        .toPredicate(root, query, cb), spec.toPredicate(root, query, cb)),
                                pageable),
                pageable);
    }

    @Override
    public Page<Target> findByUpdateStatus(final Pageable pageable, final TargetUpdateStatus status) {
        return targetRepository.findByUpdateStatus(pageable, status);
    }

    @Override
    public Slice<Target> findByFilters(final Pageable pageable, final FilterParams filterParams) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        return findByCriteriaAPI(pageable, specList);
    }

    @Override
    public long countByFilters(final Collection<TargetUpdateStatus> status, final Boolean overdueState,
            final String searchText, final Long installedOrAssignedDistributionSetId,
            final Boolean selectTargetWithNoTag, final String... tagNames) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(new FilterParams(status, overdueState,
                searchText, installedOrAssignedDistributionSetId, selectTargetWithNoTag, tagNames));
        return countByCriteriaAPI(specList);
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
            throwEntityNotFoundIfDsDoesNotExist(filterParams.getFilterByDistributionId());

            specList.add(TargetSpecifications
                    .hasInstalledOrAssignedDistributionSet(filterParams.getFilterByDistributionId()));
        }
        if (!StringUtils.isEmpty(filterParams.getFilterBySearchText())) {
            specList.add(TargetSpecifications
                    .likeIdOrNameOrDescriptionOrAttributeValue(filterParams.getFilterBySearchText()));
        }
        if (hasTagsFilterActive(filterParams)) {
            specList.add(TargetSpecifications.hasTags(filterParams.getFilterByTagNames(),
                    filterParams.getSelectTargetWithNoTag()));
        }
        return specList;
    }

    private static boolean hasTagsFilterActive(final FilterParams filterParams) {
        final boolean isNoTagActive = Boolean.TRUE.equals(filterParams.getSelectTargetWithNoTag());
        final boolean isAtLeastOneTagActive = filterParams.getFilterByTagNames() != null
                && filterParams.getFilterByTagNames().length > 0;

        return isNoTagActive || isAtLeastOneTagActive;
    }

    private Slice<Target> findByCriteriaAPI(final Pageable pageable, final List<Specification<JpaTarget>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return convertPage(criteriaNoCountDao.findAll(pageable, JpaTarget.class), pageable);
        }
        return convertPage(
                criteriaNoCountDao.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable, JpaTarget.class),
                pageable);
    }

    private Long countByCriteriaAPI(final List<Specification<JpaTarget>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return targetRepository.count();
        }

        return targetRepository.count(SpecificationsBuilder.combineWithAnd(specList));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTagAssignmentResult toggleTagAssignment(final Collection<String> controllerIds, final String tagName) {
        final TargetTag tag = targetTagRepository.findByNameEquals(tagName)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagName));
        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));

        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).collect(Collectors.toList()));
        }

        final List<JpaTarget> alreadyAssignedTargets = targetRepository.findByTagNameAndControllerIdIn(tagName,
                controllerIds);

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
                Collections
                        .unmodifiableList(allTargets.stream().map(targetRepository::save).collect(Collectors.toList())),
                Collections.emptyList(), tag);

        // no reason to persist the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> assignTag(final Collection<String> controllerIds, final long tagId) {

        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));

        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).collect(Collectors.toList()));
        }

        final JpaTargetTag tag = targetTagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagId));

        allTargets.forEach(target -> target.addTag(tag));

        final List<Target> result = Collections
                .unmodifiableList(allTargets.stream().map(targetRepository::save).collect(Collectors.toList()));

        // No reason to save the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target unAssignTag(final String controllerID, final long targetTagId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerID);

        final TargetTag tag = targetTagRepository.findById(targetTagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, targetTagId));

        target.removeTag(tag);

        final Target result = targetRepository.save(target);

        // No reason to save the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    public Slice<Target> findByFilterOrderByLinkedDistributionSet(final Pageable pageable,
            final long orderByDistributionId, final FilterParams filterParams) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<JpaTarget> query = cb.createQuery(JpaTarget.class);
        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);

        // select case expression to retrieve the case value as a column to be
        // able to order based on
        // this column, installed first,...
        final Expression<Object> selectCase = cb.selectCase()
                .when(cb.equal(targetRoot.get(JpaTarget_.installedDistributionSet).get(JpaDistributionSet_.id),
                        orderByDistributionId), 1)
                .when(cb.equal(targetRoot.get(JpaTarget_.assignedDistributionSet).get(JpaDistributionSet_.id),
                        orderByDistributionId), 2)
                .otherwise(100);
        // build the specifications and then to predicates necessary by the
        // given filters
        final Predicate[] specificationsForMultiSelect = specificationsToPredicate(buildSpecificationList(filterParams),
                targetRoot, query, cb);

        // if we have some predicates then add it to the where clause of the
        // multiselect
        if (specificationsForMultiSelect.length > 0) {
            query.where(specificationsForMultiSelect);
        }
        // add the order to the multi select first based on the selectCase
        query.orderBy(cb.asc(selectCase), cb.desc(targetRoot.get(JpaTarget_.id)));

        final int pageSize = pageable.getPageSize();
        final List<JpaTarget> resultList = entityManager.createQuery(query).setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageSize).getResultList();
        final boolean hasNext = resultList.size() > pageSize;
        return new SliceImpl<>(Collections.unmodifiableList(resultList), pageable, hasNext);
    }

    private static Predicate[] specificationsToPredicate(final List<Specification<JpaTarget>> specifications,
            final Root<JpaTarget> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        final Predicate[] predicates = new Predicate[specifications.size()];
        for (int index = 0; index < predicates.length; index++) {
            predicates[index] = specifications.get(index).toPredicate(root, query, cb);
        }
        return predicates;
    }

    @Override
    public long countByAssignedDistributionSet(final long distId) {
        throwEntityNotFoundIfDsDoesNotExist(distId);

        return targetRepository.countByAssignedDistributionSetId(distId);
    }

    @Override
    public long countByInstalledDistributionSet(final long distId) {
        throwEntityNotFoundIfDsDoesNotExist(distId);

        return targetRepository.countByInstalledDistributionSetId(distId);
    }

    @Override
    public boolean existsByInstalledOrAssignedDistributionSet(final long distId) {
        throwEntityNotFoundIfDsDoesNotExist(distId);

        return targetRepository.existsByInstalledOrAssignedDistributionSet(distId);
    }

    @Override
    public Page<Target> findByTargetFilterQueryAndNonDS(final Pageable pageRequest, final long distributionSetId,
            final String targetFilterQuery) {
        throwEntityNotFoundIfDsDoesNotExist(distributionSetId);

        final Specification<JpaTarget> spec = RSQLUtility.parse(targetFilterQuery, TargetFields.class,
                virtualPropertyReplacer, database);

        return findTargetsBySpec(
                (root, cq,
                        cb) -> cb.and(spec.toPredicate(root, cq, cb), TargetSpecifications
                                .hasNotDistributionSetInActions(distributionSetId).toPredicate(root, cq, cb)),
                pageRequest);

    }

    @Override
    public Page<Target> findByTargetFilterQueryAndNotInRolloutGroups(final Pageable pageRequest,
            final Collection<Long> groups, final String targetFilterQuery) {

        final Specification<JpaTarget> spec = RSQLUtility.parse(targetFilterQuery, TargetFields.class,
                virtualPropertyReplacer, database);

        return findTargetsBySpec((root, cq, cb) -> cb.and(spec.toPredicate(root, cq, cb),
                TargetSpecifications.isNotInRolloutGroups(groups).toPredicate(root, cq, cb)), pageRequest);

    }

    @Override
    public Page<Target> findByInRolloutGroupWithoutAction(final Pageable pageRequest, final long group) {
        if (!rolloutGroupRepository.existsById(group)) {
            throw new EntityNotFoundException(RolloutGroup.class, group);
        }

        return findTargetsBySpec(
                (root, cq, cb) -> TargetSpecifications.hasNoActionInRolloutGroup(group).toPredicate(root, cq, cb),
                pageRequest);
    }

    @Override
    public long countByRsqlAndNotInRolloutGroups(final Collection<Long> groups, final String targetFilterQuery) {
        final Specification<JpaTarget> spec = RSQLUtility.parse(targetFilterQuery, TargetFields.class,
                virtualPropertyReplacer, database);
        final List<Specification<JpaTarget>> specList = Arrays.asList(spec,
                TargetSpecifications.isNotInRolloutGroups(groups));

        return countByCriteriaAPI(specList);
    }

    @Override
    public long countByRsqlAndNonDS(final long distributionSetId, final String targetFilterQuery) {
        throwEntityNotFoundIfDsDoesNotExist(distributionSetId);

        final Specification<JpaTarget> spec = RSQLUtility.parse(targetFilterQuery, TargetFields.class,
                virtualPropertyReplacer, database);
        final List<Specification<JpaTarget>> specList = Lists.newArrayListWithExpectedSize(2);
        specList.add(spec);
        specList.add(TargetSpecifications.hasNotDistributionSetInActions(distributionSetId));

        return countByCriteriaAPI(specList);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target create(final TargetCreate c) {
        final JpaTargetCreate create = (JpaTargetCreate) c;
        return targetRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> create(final Collection<TargetCreate> targets) {
        return targets.stream().map(this::create).collect(Collectors.toList());
    }

    @Override
    public Page<Target> findByTag(final Pageable pageable, final long tagId) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        return convertPage(targetRepository.findByTag(pageable, tagId), pageable);
    }

    private void throwEntityNotFoundExceptionIfTagDoesNotExist(final Long tagId) {
        if (!targetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(TargetTag.class, tagId);
        }
    }

    @Override
    public Page<Target> findByRsqlAndTag(final Pageable pageable, final String rsqlParam, final long tagId) {

        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        final Specification<JpaTarget> spec = RSQLUtility.parse(rsqlParam, TargetFields.class, virtualPropertyReplacer,
                database);

        return convertPage(targetRepository.findAll((Specification<JpaTarget>) (root, query, cb) -> cb.and(
                TargetSpecifications.hasTag(tagId).toPredicate(root, query, cb), spec.toPredicate(root, query, cb)),
                pageable), pageable);
    }

    @Override
    public long countByTargetFilterQuery(final long targetFilterQueryId) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository.findById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));

        final Specification<JpaTarget> specs = RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class,
                virtualPropertyReplacer, database);
        return targetRepository.count(specs);
    }

    @Override
    public long countByRsql(final String targetFilterQuery) {
        final Specification<JpaTarget> specs = RSQLUtility.parse(targetFilterQuery, TargetFields.class,
                virtualPropertyReplacer, database);
        return targetRepository.count((root, query, cb) -> {
            query.distinct(true);
            return specs.toPredicate(root, query, cb);
        });
    }

    @Override
    public Optional<Target> get(final long id) {
        return targetRepository.findById(id).map(t -> t);
    }

    @Override
    public List<Target> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(targetRepository.findAllById(ids));
    }

    @Override
    public Map<String, String> getControllerAttributes(final String controllerId) {
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

        target.setRequestControllerAttributes(true);

        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetAttributesRequestedEvent(tenantAware.getCurrentTenant(), target.getId(),
                        target.getControllerId(), target.getAddress() != null ? target.getAddress().toString() : null,
                        JpaTarget.class.getName(), eventPublisherHolder.getApplicationId()));
    }

    @Override
    public boolean isControllerAttributesRequested(final String controllerId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        return target.isRequestControllerAttributes();
    }

    @Override
    public boolean existsByControllerId(final String controllerId) {
        return targetRepository.existsByControllerId(controllerId);
    }

    @Override
    public Page<Target> findByControllerAttributesRequested(final Pageable pageReq) {
        return targetRepository.findByRequestControllerAttributesIsTrue(pageReq);
    }

}
