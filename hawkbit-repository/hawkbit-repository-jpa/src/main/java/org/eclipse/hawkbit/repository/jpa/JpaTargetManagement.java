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
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.eventbus.event.TargetTagAssigmentResultEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * JPA implementation of {@link TargetManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class JpaTargetManagement implements TargetManagement {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private TargetTagRepository targetTagRepository;

    @Autowired
    private TargetInfoRepository targetInfoRepository;

    @Autowired
    private NoCountPagingRepository criteriaNoCountDao;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Override
    public Target findTargetByControllerID(final String controllerId) {
        return targetRepository.findByControllerId(controllerId);
    }

    @Override
    public Target findTargetByControllerIDWithDetails(final String controllerId) {
        final Target result = targetRepository.findByControllerId(controllerId);
        // load lazy relations
        if (result != null) {
            result.getTargetInfo().getControllerAttributes().size();
            if (result.getTargetInfo() != null && result.getTargetInfo().getInstalledDistributionSet() != null) {
                result.getTargetInfo().getInstalledDistributionSet().getName();
                result.getTargetInfo().getInstalledDistributionSet().getModules().size();
            }
            if (result.getAssignedDistributionSet() != null) {
                result.getAssignedDistributionSet().getName();
                result.getAssignedDistributionSet().getModules().size();
            }
        }
        return result;
    }

    @Override
    public List<Target> findTargetByControllerID(final Collection<String> controllerIDs) {
        return new ArrayList<>(targetRepository
                .findAll(TargetSpecifications.byControllerIdWithStatusAndAssignedInJoin(controllerIDs)));
    }

    @Override
    public Long countTargetsAll() {
        return targetRepository.count();
    }

    @Override
    public Slice<Target> findTargetsAll(final Pageable pageable) {
        // workarround - no join fetch allowed that is why we need specification
        // instead of query for
        // count() of Pageable
        final Specification<JpaTarget> spec = (root, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                root.fetch(JpaTarget_.targetInfo);
            }
            return cb.conjunction();
        };
        return convertPage(criteriaNoCountDao.findAll(spec, pageable, JpaTarget.class), pageable);
    }

    @Override
    public Slice<Target> findTargetsAll(final TargetFilterQuery targetFilterQuery, final Pageable pageable) {
        return findTargetsBySpec(RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class), pageable);
    }

    @Override
    public Page<Target> findTargetsAll(final String targetFilterQuery, final Pageable pageable) {
        return findTargetsBySpec(RSQLUtility.parse(targetFilterQuery, TargetFields.class), pageable);
    }

    private Page<Target> findTargetsBySpec(final Specification<JpaTarget> spec, final Pageable pageable) {
        return convertPage(targetRepository.findAll(spec, pageable), pageable);
    }

    @Override
    public List<Target> findTargetsByControllerIDsWithTags(final List<String> controllerIDs) {
        final List<List<String>> partition = Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT);
        return partition.stream()
                .map(ids -> targetRepository.findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(ids)))
                .flatMap(t -> t.stream()).collect(Collectors.toList());
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Target updateTarget(final Target target) {
        Assert.notNull(target.getId());

        final JpaTarget toUpdate = (JpaTarget) target;
        toUpdate.setNew(false);
        return targetRepository.save(toUpdate);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<Target> updateTargets(final Collection<Target> targets) {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<JpaTarget> toUpdate = (Collection) targets;

        toUpdate.forEach(target -> target.setNew(false));

        return new ArrayList<>(targetRepository.save(toUpdate));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteTargets(final Long... targetIDs) {
        // we need to select the target IDs first to check the if the targetIDs
        // belonging to the
        // tenant! Delete statement are not automatically enhanced with the
        // @FilterDef of the
        // hibernate session.
        final List<Long> targetsForCurrentTenant = targetRepository.findAll(Lists.newArrayList(targetIDs)).stream()
                .map(Target::getId).collect(Collectors.toList());
        if (!targetsForCurrentTenant.isEmpty()) {
            targetInfoRepository.deleteByTargetIdIn(targetsForCurrentTenant);
            targetRepository.deleteByIdIn(targetsForCurrentTenant);
        }
    }

    @Override
    public Page<Target> findTargetByAssignedDistributionSet(final Long distributionSetID, final Pageable pageReq) {
        return targetRepository.findByAssignedDistributionSetId(pageReq, distributionSetID);
    }

    @Override
    public Page<Target> findTargetByAssignedDistributionSet(final Long distributionSetID, final String rsqlParam,
            final Pageable pageReq) {

        final Specification<JpaTarget> spec = RSQLUtility.parse(rsqlParam, TargetFields.class);

        return convertPage(
                targetRepository
                        .findAll((Specification<JpaTarget>) (root, query,
                                cb) -> cb.and(TargetSpecifications.hasAssignedDistributionSet(distributionSetID)
                                        .toPredicate(root, query, cb), spec.toPredicate(root, query, cb)),
                                pageReq),
                pageReq);
    }

    private static Page<Target> convertPage(final Page<JpaTarget> findAll, final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Slice<Target> convertPage(final Slice<JpaTarget> findAll, final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, 0);
    }

    @Override
    public Page<Target> findTargetByInstalledDistributionSet(final Long distributionSetID, final Pageable pageReq) {
        return targetRepository.findByTargetInfoInstalledDistributionSetId(pageReq, distributionSetID);
    }

    @Override
    public Page<Target> findTargetByInstalledDistributionSet(final Long distributionSetId, final String rsqlParam,
            final Pageable pageable) {

        final Specification<JpaTarget> spec = RSQLUtility.parse(rsqlParam, TargetFields.class);

        return convertPage(
                targetRepository
                        .findAll((Specification<JpaTarget>) (root, query,
                                cb) -> cb.and(TargetSpecifications.hasInstalledDistributionSet(distributionSetId)
                                        .toPredicate(root, query, cb), spec.toPredicate(root, query, cb)),
                                pageable),
                pageable);
    }

    @Override
    public Page<Target> findTargetByUpdateStatus(final Pageable pageable, final TargetUpdateStatus status) {
        return targetRepository.findByTargetInfoUpdateStatus(pageable, status);
    }

    @Override
    public Slice<Target> findTargetByFilters(final Pageable pageable, final Collection<TargetUpdateStatus> status,
            final String searchText, final Long installedOrAssignedDistributionSetId,
            final Boolean selectTargetWithNoTag, final String... tagNames) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(status, searchText,
                installedOrAssignedDistributionSetId, selectTargetWithNoTag, true, tagNames);
        return findByCriteriaAPI(pageable, specList);
    }

    @Override
    public Long countTargetByFilters(final Collection<TargetUpdateStatus> status, final String searchText,
            final Long installedOrAssignedDistributionSetId, final Boolean selectTargetWithNoTag,
            final String... tagNames) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(status, searchText,
                installedOrAssignedDistributionSetId, selectTargetWithNoTag, true, tagNames);
        return countByCriteriaAPI(specList);
    }

    private static List<Specification<JpaTarget>> buildSpecificationList(final Collection<TargetUpdateStatus> status,
            final String searchText, final Long installedOrAssignedDistributionSetId,
            final Boolean selectTargetWithNoTag, final boolean fetch, final String... tagNames) {
        final List<Specification<JpaTarget>> specList = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            specList.add(TargetSpecifications.hasTargetUpdateStatus(status, fetch));
        }
        if (installedOrAssignedDistributionSetId != null) {
            specList.add(
                    TargetSpecifications.hasInstalledOrAssignedDistributionSet(installedOrAssignedDistributionSetId));
        }
        if (!Strings.isNullOrEmpty(searchText)) {
            specList.add(TargetSpecifications.likeNameOrDescriptionOrIp(searchText));
        }
        if (selectTargetWithNoTag != null && (selectTargetWithNoTag || (tagNames != null && tagNames.length > 0))) {
            specList.add(TargetSpecifications.hasTags(tagNames, selectTargetWithNoTag));
        }
        return specList;
    }

    private Slice<Target> findByCriteriaAPI(final Pageable pageable, final List<Specification<JpaTarget>> specList) {
        if (specList == null || specList.isEmpty()) {
            return convertPage(criteriaNoCountDao.findAll(pageable, JpaTarget.class), pageable);
        }
        return convertPage(
                criteriaNoCountDao.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable, JpaTarget.class),
                pageable);
    }

    private Long countByCriteriaAPI(final List<Specification<JpaTarget>> specList) {
        if (specList == null || specList.isEmpty()) {
            return targetRepository.count();
        }

        return targetRepository.count(SpecificationsBuilder.combineWithAnd(specList));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetTagAssignmentResult toggleTagAssignment(final Collection<Target> targets, final TargetTag tag) {
        return toggleTagAssignment(
                targets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()), tag.getName());
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public TargetTagAssignmentResult toggleTagAssignment(final Collection<String> targetIds, final String tagName) {
        final TargetTag tag = targetTagRepository.findByNameEquals(tagName);
        final List<Target> alreadyAssignedTargets = targetRepository.findByTagNameAndControllerIdIn(tagName, targetIds);
        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(targetIds));

        // all are already assigned -> unassign
        if (alreadyAssignedTargets.size() == allTargets.size()) {
            alreadyAssignedTargets.forEach(target -> target.getTags().remove(tag));
            final TargetTagAssignmentResult result = new TargetTagAssignmentResult(0, 0, alreadyAssignedTargets.size(),
                    Collections.emptyList(), alreadyAssignedTargets, tag);

            afterCommit.afterCommit(() -> eventBus.post(new TargetTagAssigmentResultEvent(result)));
            return result;
        }

        allTargets.removeAll(alreadyAssignedTargets);
        // some or none are assigned -> assign
        allTargets.forEach(target -> target.getTags().add(tag));
        final TargetTagAssignmentResult result = new TargetTagAssignmentResult(alreadyAssignedTargets.size(),
                allTargets.size(), 0, new ArrayList<>(targetRepository.save(allTargets)), Collections.emptyList(), tag);

        afterCommit.afterCommit(() -> eventBus.post(new TargetTagAssigmentResultEvent(result)));

        // no reason to persist the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<Target> assignTag(final Collection<String> targetIds, final TargetTag tag) {
        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(targetIds));

        allTargets.forEach(target -> target.getTags().add(tag));
        final List<Target> save = new ArrayList<>(targetRepository.save(allTargets));

        afterCommit.afterCommit(() -> {
            final TargetTagAssignmentResult assigmentResult = new TargetTagAssignmentResult(0, save.size(), 0, save,
                    Collections.emptyList(), tag);
            eventBus.post(new TargetTagAssigmentResultEvent(assigmentResult));
        });

        return save;
    }

    private List<Target> unAssignTag(final Collection<Target> targets, final TargetTag tag) {
        final Collection<JpaTarget> toUnassign = (Collection) targets;

        toUnassign.forEach(target -> target.getTags().remove(tag));

        final List<Target> save = new ArrayList<>(targetRepository.save(toUnassign));
        afterCommit.afterCommit(() -> {
            final TargetTagAssignmentResult assigmentResult = new TargetTagAssignmentResult(0, 0, save.size(),
                    Collections.emptyList(), save, tag);
            eventBus.post(new TargetTagAssigmentResultEvent(assigmentResult));
        });
        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<Target> unAssignAllTargetsByTag(final TargetTag tag) {
        return unAssignTag(tag.getAssignedToTargets(), tag);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Target unAssignTag(final String controllerID, final TargetTag targetTag) {
        // TODO : optimize this, findone?
        final List<Target> allTargets = new ArrayList<>(targetRepository
                .findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(Arrays.asList(controllerID))));
        final List<Target> unAssignTag = unAssignTag(allTargets, targetTag);
        return unAssignTag.isEmpty() ? null : unAssignTag.get(0);
    }

    @Override
    public Slice<Target> findTargetsAllOrderByLinkedDistributionSet(final Pageable pageable,
            final Long orderByDistributionId, final Long filterByDistributionId,
            final Collection<TargetUpdateStatus> filterByStatus, final String filterBySearchText,
            final Boolean selectTargetWithNoTag, final String... filterByTagNames) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<JpaTarget> query = cb.createQuery(JpaTarget.class);
        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);

        // necessary joins for the select
        final Join<JpaTarget, JpaTargetInfo> targetInfo = (Join<JpaTarget, JpaTargetInfo>) targetRoot
                .fetch(JpaTarget_.targetInfo, JoinType.LEFT);

        // select case expression to retrieve the case value as a column to be
        // able to order based on
        // this column, installed first,...
        final Expression<Object> selectCase = cb.selectCase()
                .when(cb.equal(targetInfo.get(JpaTargetInfo_.installedDistributionSet).get(JpaDistributionSet_.id),
                        orderByDistributionId), 1)
                .when(cb.equal(targetRoot.get(JpaTarget_.assignedDistributionSet).get(JpaDistributionSet_.id),
                        orderByDistributionId), 2)
                .otherwise(100);
        // multiselect statement order by the select case and controllerId
        query.distinct(true);
        // build the specifications and then to predicates necessary by the
        // given filters
        final Predicate[] specificationsForMultiSelect = specificationsToPredicate(
                buildSpecificationList(filterByStatus, filterBySearchText, filterByDistributionId,
                        selectTargetWithNoTag, true, filterByTagNames),
                targetRoot, query, cb);

        // if we have some predicates then add it to the where clause of the
        // multiselect
        if (specificationsForMultiSelect.length > 0) {
            query.where(specificationsForMultiSelect);
        }
        // add the order to the multi select first based on the selectCase
        query.orderBy(cb.asc(selectCase), cb.desc(targetRoot.get(JpaTarget_.id)));
        // the result is a Object[] due the fact that the selectCase is an extra
        // column, so it cannot
        // be mapped directly to a Target entity because the selectCase is not a
        // attribute of the
        // Target entity, the the Object array contains the Target on the first
        // index (case of the
        // multiselect order) of the array and
        // the 2nd contains the selectCase int value.
        final int pageSize = pageable.getPageSize();
        final List<JpaTarget> resultList = entityManager.createQuery(query).setFirstResult(pageable.getOffset())
                .setMaxResults(pageSize + 1).getResultList();
        final boolean hasNext = resultList.size() > pageSize;
        return new SliceImpl<>(new ArrayList<>(resultList), pageable, hasNext);
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
    public Long countTargetByAssignedDistributionSet(final Long distId) {
        return targetRepository.countByAssignedDistributionSetId(distId);
    }

    @Override
    public Long countTargetByInstalledDistributionSet(final Long distId) {
        return targetRepository.countByTargetInfoInstalledDistributionSetId(distId);
    }

    @Override
    public List<TargetIdName> findAllTargetIds() {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<TargetIdName> query = cb.createQuery(TargetIdName.class);
        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        return entityManager.createQuery(query.multiselect(targetRoot.get(JpaTarget_.id),
                targetRoot.get(JpaTarget_.controllerId), targetRoot.get(JpaTarget_.name))).getResultList();

    }

    @Override
    public List<TargetIdName> findAllTargetIdsByFilters(final Pageable pageRequest,
            final Collection<TargetUpdateStatus> filterByStatus, final String filterBySearchText,
            final Long installedOrAssignedDistributionSetId, final Boolean selectTargetWithNoTag,
            final String... filterByTagNames) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        List<Object[]> resultList;

        String sortProperty = JpaTarget_.id.getName();
        if (pageRequest.getSort() != null && pageRequest.getSort().iterator().hasNext()) {
            sortProperty = pageRequest.getSort().iterator().next().getProperty();
        }

        final CriteriaQuery<Object[]> multiselect = query.multiselect(targetRoot.get(JpaTarget_.id),
                targetRoot.get(JpaTarget_.controllerId), targetRoot.get(JpaTarget_.name), targetRoot.get(sortProperty));

        final Predicate[] specificationsForMultiSelect = specificationsToPredicate(
                buildSpecificationList(filterByStatus, filterBySearchText, installedOrAssignedDistributionSetId,
                        selectTargetWithNoTag, false, filterByTagNames),
                targetRoot, multiselect, cb);

        // if we have some predicates then add it to the where clause of the
        // multiselect
        if (specificationsForMultiSelect.length > 0) {
            multiselect.where(specificationsForMultiSelect);
        }

        resultList = getTargetIdNameResultSet(pageRequest, cb, targetRoot, multiselect);
        return resultList.parallelStream().map(o -> new TargetIdName((long) o[0], o[1].toString(), o[2].toString()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TargetIdName> findAllTargetIdsByTargetFilterQuery(final Pageable pageRequest,
            final TargetFilterQuery targetFilterQuery) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);

        String sortProperty = JpaTarget_.id.getName();
        if (pageRequest.getSort() != null && pageRequest.getSort().iterator().hasNext()) {
            sortProperty = pageRequest.getSort().iterator().next().getProperty();
        }

        final CriteriaQuery<Object[]> multiselect = query.multiselect(targetRoot.get(JpaTarget_.id),
                targetRoot.get(JpaTarget_.controllerId), targetRoot.get(JpaTarget_.name), targetRoot.get(sortProperty));

        final Specification<JpaTarget> spec = RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class);
        final List<Specification<JpaTarget>> specList = new ArrayList<>();
        specList.add(spec);

        final Predicate[] specificationsForMultiSelect = specificationsToPredicate(specList, targetRoot, multiselect,
                cb);

        // if we have some predicates then add it to the where clause of the
        // multiselect
        if (specificationsForMultiSelect.length > 0) {
            multiselect.where(specificationsForMultiSelect);
        }
        final List<Object[]> resultList = getTargetIdNameResultSet(pageRequest, cb, targetRoot, multiselect);
        return resultList.parallelStream().map(o -> new TargetIdName((long) o[0], o[1].toString(), o[2].toString()))
                .collect(Collectors.toList());
    }

    @PreDestroy
    void destroy() {
        eventBus.unregister(this);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @CacheEvict(value = { "targetsCreatedOverPeriod" }, allEntries = true)
    public Target createTarget(final Target t, final TargetUpdateStatus status, final Long lastTargetQuery,
            final URI address) {
        final JpaTarget target = (JpaTarget) t;

        if (targetRepository.findByControllerId(target.getControllerId()) != null) {
            throw new EntityAlreadyExistsException(target.getControllerId());
        }

        target.setNew(true);
        final JpaTarget savedTarget = targetRepository.save(target);
        final JpaTargetInfo targetInfo = (JpaTargetInfo) savedTarget.getTargetInfo();
        targetInfo.setUpdateStatus(status);
        if (lastTargetQuery != null) {
            targetInfo.setLastTargetQuery(lastTargetQuery);
        }
        if (address != null) {
            targetInfo.setAddress(address.toString());
        }
        targetInfo.setNew(true);
        final Target targetToReturn = targetInfoRepository.save(targetInfo).getTarget();
        targetInfo.setNew(false);
        return targetToReturn;

    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @CacheEvict(value = { "targetsCreatedOverPeriod" }, allEntries = true)
    public Target createTarget(final Target target) {
        return createTarget(target, TargetUpdateStatus.UNKNOWN, null, null);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<Target> createTargets(final Collection<Target> targets) {
        if (!targets.isEmpty() && targetRepository.countByControllerIdIn(
                targets.stream().map(target -> target.getControllerId()).collect(Collectors.toList())) > 0) {
            throw new EntityAlreadyExistsException();
        }
        final List<Target> savedTargets = new ArrayList<>();
        for (final Target t : targets) {
            final Target myTarget = createTarget(t);
            savedTargets.add(myTarget);
        }
        return savedTargets;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<Target> createTargets(final Collection<Target> targets, final TargetUpdateStatus status,
            final Long lastTargetQuery, final URI address) {
        if (targetRepository.countByControllerIdIn(
                targets.stream().map(target -> target.getControllerId()).collect(Collectors.toList())) > 0) {
            throw new EntityAlreadyExistsException();
        }
        final List<Target> savedTargets = new ArrayList<>();
        for (final Target t : targets) {
            final Target myTarget = createTarget(t, status, lastTargetQuery, address);
            savedTargets.add(myTarget);
        }
        return savedTargets;
    }

    @Override
    public List<Target> findTargetsByTag(final String tagName) {
        final JpaTargetTag tag = targetTagRepository.findByNameEquals(tagName);
        return new ArrayList<>(targetRepository.findByTag(tag));
    }

    @Override
    public Long countTargetByTargetFilterQuery(final TargetFilterQuery targetFilterQuery) {
        final Specification<JpaTarget> specs = RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class);
        return targetRepository.count(specs);
    }

    @Override
    public Long countTargetByTargetFilterQuery(final String targetFilterQuery) {
        final Specification<JpaTarget> specs = RSQLUtility.parse(targetFilterQuery, TargetFields.class);
        return targetRepository.count(specs);
    }

    private List<Object[]> getTargetIdNameResultSet(final Pageable pageRequest, final CriteriaBuilder cb,
            final Root<JpaTarget> targetRoot, final CriteriaQuery<Object[]> multiselect) {
        List<Object[]> resultList;
        if (pageRequest.getSort() != null) {
            final List<Order> orders = new ArrayList<>();
            final Sort sort = pageRequest.getSort();
            for (final Sort.Order sortOrder : sort) {
                if (sortOrder.getDirection() == Direction.ASC) {
                    orders.add(cb.asc(targetRoot.get(sortOrder.getProperty())));
                } else {
                    orders.add(cb.desc(targetRoot.get(sortOrder.getProperty())));
                }
            }
            multiselect.orderBy(orders);
            resultList = entityManager.createQuery(multiselect).setFirstResult(pageRequest.getOffset())
                    .setMaxResults(pageRequest.getPageSize()).getResultList();
        } else {
            resultList = entityManager.createQuery(multiselect).getResultList();
        }
        return resultList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Target generateTarget(final String controllerId) {
        return new JpaTarget(controllerId);
    }
}
