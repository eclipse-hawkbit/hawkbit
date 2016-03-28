/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.Constants;
import org.eclipse.hawkbit.eventbus.event.TargetTagAssigmentResultEvent;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetInfo_;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssigmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Target_;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.specifications.TargetSpecifications;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * Business service facade for managing {@link Target}s.
 *
 *
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
public class TargetManagement {

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

    /**
     * Find {@link Target} based on given ID returns found Target without
     * details, i.e. NO {@link Target#getTags()} and
     * {@link Target#getActiveActions()} possible.
     *
     * @param controllerId
     *            to look for.
     * @return {@link Target} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Target findTargetByControllerID(@NotEmpty final String controllerId) {
        return targetRepository.findByControllerId(controllerId);
    }

    /**
     * Find {@link Target} based on given ID returns found Target with details,
     * i.e. {@link Target#getTags()} and {@link Target#getActiveActions()} are
     * possible.
     *
     * Note: try to use {@link #findTargetByControllerID(String)} as much as
     * possible.
     *
     * @param controllerId
     *            to look for.
     * @return {@link Target} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Target findTargetByControllerIDWithDetails(@NotEmpty final String controllerId) {
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

    /**
     * Find {@link Target} based on given ID returns found Target without
     * details, i.e. NO {@link Target#getTags()} and
     * {@link Target#getActiveActions()} possible.
     *
     * @param controllerIDs
     *            to look for.
     * @return List of found{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<Target> findTargetsByControllerID(@NotEmpty final Collection<String> controllerIDs) {
        return targetRepository.findAll(TargetSpecifications.byControllerIdWithStatusAndAssignedInJoin(controllerIDs));
    }

    /**
     * Counts all {@link Target}s in the repository.
     *
     * @return number of targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countTargetsAll() {
        return targetRepository.count();
    }

    /**
     * Retrieves all targets without details, i.e. NO {@link Target#getTags()}
     * and {@link Target#getActiveActions()} possible
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Target> findTargetsAll(@NotNull final Pageable pageable) {
        // workarround - no join fetch allowed that is why we need specification
        // instead of query for
        // count() of Pageable
        final Specification<Target> spec = (root, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                root.fetch(Target_.targetInfo);
            }
            return cb.conjunction();
        };
        return criteriaNoCountDao.findAll(spec, pageable, Target.class);
    }

    /**
     * Retrieves all targets without details, i.e. NO {@link Target#getTags()}
     * and {@link Target#getActiveActions()} possible based on
     * {@link TargetFilterQuery#getQuery()}
     *
     * @param targetFilterQuery
     * @param pageable
     * @return
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Target> findTargetsAll(@NotNull final TargetFilterQuery targetFilterQuery,
            @NotNull final Pageable pageable) {
        return findTargetsAll(RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class), pageable);
    }

    /**
     * Retrieves all targets without details, i.e. NO {@link Target#getTags()}
     * and {@link Target#getActiveActions()} possible based on
     * {@link TargetFilterQuery#getQuery()}
     *
     * @param targetFilterQuery
     * @param pageable
     * @return
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Target> findTargetsAll(@NotNull final String targetFilterQuery, @NotNull final Pageable pageable) {
        return findTargetsAll(RSQLUtility.parse(targetFilterQuery, TargetFields.class), pageable);
    }

    /**
     * Retrieves all targets based on the given specification.
     *
     * @param spec
     *            the specification for the query
     * @param pageable
     *            pagination parameter
     * @return the found {@link Target}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<Target> findTargetsAll(@NotNull final Specification<Target> spec, @NotNull final Pageable pageable) {
        return targetRepository.findAll(spec, pageable);
    }

    /**
     * retrieves a list of {@link Target}s by their controller ID with details,
     * i.e. {@link Target#getTags()} are possible.
     *
     * Note: try to use {@link #findTargetByControllerID(String)} as much as
     * possible.
     *
     * @param controllerIDs
     *            {@link Target}s Names parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<Target> findTargetsByControllerIDsWithTags(@NotNull final List<String> controllerIDs) {
        final List<List<String>> partition = Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT);
        return partition.stream()
                .map(ids -> targetRepository.findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(ids)))
                .flatMap(t -> t.stream()).collect(Collectors.toList());
    }

    /**
     * updates the {@link Target}.
     *
     * @param target
     *            to be updated
     * @return the updated {@link Target}
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    public Target updateTarget(@NotNull final Target target) {
        Assert.notNull(target.getId());
        target.setNew(false);
        return targetRepository.save(target);
    }

    /**
     * updates multiple {@link Target}s.
     *
     * @param targets
     *            to be updated
     * @return the updated {@link Target}s
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    public List<Target> updateTargets(@NotNull final List<Target> targets) {
        targets.forEach(target -> target.setNew(false));
        return targetRepository.save(targets);
    }

    /**
     * Deletes all targets with the given IDs.
     *
     * @param targetIDs
     *            the technical IDs of the targets to be deleted
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    public void deleteTargets(@NotEmpty final Long... targetIDs) {
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

    /**
     * retrieves {@link Target}s by the assigned {@link DistributionSet} without
     * details, i.e. NO {@link Target#getTags()} and
     * {@link Target#getActiveActions()} possible.
     *
     *
     * @param distributionSetID
     *            the ID of the {@link DistributionSet}
     * @param pageReq
     *            page parameter
     * @return the found {@link Target}s
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    public Page<Target> findTargetByAssignedDistributionSet(@NotNull final Long distributionSetID,
            @NotNull final Pageable pageReq) {
        return targetRepository.findByAssignedDistributionSetId(pageReq, distributionSetID);
    }

    /**
     * retrieves {@link Target}s by the assigned {@link DistributionSet} without
     * details, i.e. NO {@link Target#getTags()} and
     * {@link Target#getActiveActions()} possible including the filtering based
     * on the given {@code spec}.
     *
     * @param distributionSetID
     *            the ID of the {@link DistributionSet}
     * @param spec
     *            the specification to filter the result set
     * @param pageReq
     *            page parameter
     * @return the found {@link Target}s, never {@code null}
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    public Page<Target> findTargetByAssignedDistributionSet(@NotNull final Long distributionSetID,
            final Specification<Target> spec, @NotNull final Pageable pageReq) {
        return targetRepository.findAll((Specification<Target>) (root, query, cb) -> cb.and(
                TargetSpecifications.hasAssignedDistributionSet(distributionSetID).toPredicate(root, query, cb),
                spec.toPredicate(root, query, cb)), pageReq);
    }

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet}without
     * details, i.e. NO {@link Target#getTags()} and
     * {@link Target#getActiveActions()} possible.
     *
     * @param distributionSetID
     *            the ID of the {@link DistributionSet}
     * @param pageReq
     *            page parameter
     * @return the found {@link Target}s
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    public Page<Target> findTargetByInstalledDistributionSet(@NotNull final Long distributionSetID,
            @NotNull final Pageable pageReq) {
        return targetRepository.findByTargetInfoInstalledDistributionSetId(pageReq, distributionSetID);
    }

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet}without
     * details, i.e. NO {@link Target#getTags()} and
     * {@link Target#getActiveActions()} possible.
     *
     * @param distributionSetId
     *            the ID of the {@link DistributionSet}
     * @param spec
     *            the specification to filter the result
     * @param pageable
     *            page parameter
     * @return the found {@link Target}s, never {@code null}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    public Page<Target> findTargetByInstalledDistributionSet(final Long distributionSetId,
            final Specification<Target> spec, final Pageable pageable) {
        return targetRepository.findAll((Specification<Target>) (root, query, cb) -> cb.and(
                TargetSpecifications.hasInstalledDistributionSet(distributionSetId).toPredicate(root, query, cb),
                spec.toPredicate(root, query, cb)), pageable);
    }

    /**
     * Retrieves the {@link Target} which have a certain
     * {@link TargetUpdateStatus} without details, i.e. NO
     * {@link Target#getTags()} and {@link Target#getActiveActions()} possible.
     *
     * @param pageable
     *            page parameter
     * @param status
     *            the {@link TargetUpdateStatus} to be filtered on
     * @return the found {@link Target}s
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Page<Target> findTargetByUpdateStatus(@NotNull final Pageable pageable,
            @NotNull final TargetUpdateStatus status) {
        return targetRepository.findByTargetInfoUpdateStatus(pageable, status);
    }

    /**
     * Filter {@link Target}s for all the given parameters. If all parameters
     * except pageable are null, all available {@link Target}s are returned.
     *
     * @param pageable
     *            page parameters
     * @param status
     *            find targets having this {@link TargetUpdateStatus}s. Set to
     *            <code>null</code> in case this is not required.
     * @param searchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param installedOrAssignedDistributionSetId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param tagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     *
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Target> findTargetByFilters(@NotNull final Pageable pageable,
            final Collection<TargetUpdateStatus> status, final String searchText,
            final Long installedOrAssignedDistributionSetId, final Boolean selectTargetWithNoTag,
            final String... tagNames) {
        final List<Specification<Target>> specList = buildSpecificationList(status, searchText,
                installedOrAssignedDistributionSetId, selectTargetWithNoTag, true, tagNames);
        return findByCriteriaAPI(pageable, specList);
    }

    /**
     * Count {@link Target}s for all the given filter parameters.
     *
     * @param status
     *            find targets having on of these {@link TargetUpdateStatus}s.
     *            Set to <code>null</code> in case this is not required.
     * @param searchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param installedOrAssignedDistributionSetId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param tagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     *
     * @return the found number {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countTargetByFilters(final Collection<TargetUpdateStatus> status, final String searchText,
            final Long installedOrAssignedDistributionSetId, final Boolean selectTargetWithNoTag,
            final String... tagNames) {
        final List<Specification<Target>> specList = buildSpecificationList(status, searchText,
                installedOrAssignedDistributionSetId, selectTargetWithNoTag, true, tagNames);
        return countByCriteriaAPI(specList);
    }

    private static List<Specification<Target>> buildSpecificationList(final Collection<TargetUpdateStatus> status,
            final String searchText, final Long installedOrAssignedDistributionSetId,
            final Boolean selectTargetWithNoTag, final boolean fetch, final String... tagNames) {
        final List<Specification<Target>> specList = new ArrayList<>();
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
        if (selectTargetWithNoTag || (tagNames != null && tagNames.length > 0)) {
            specList.add(TargetSpecifications.hasTags(tagNames, selectTargetWithNoTag));
        }
        return specList;
    }

    /**
     * executes findAll with the given {@link Target} {@link Specification}s.
     *
     * @param pageable
     *            paging parameter
     * @param specList
     *            list of @link {@link Specification}
     * @return the page with the found {@link Target}
     */
    private Slice<Target> findByCriteriaAPI(final Pageable pageable, final List<Specification<Target>> specList) {
        if (specList == null || specList.isEmpty()) {
            return criteriaNoCountDao.findAll(pageable, Target.class);
        }
        return criteriaNoCountDao.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable, Target.class);
    }

    private Long countByCriteriaAPI(final List<Specification<Target>> specList) {
        if (specList == null || specList.isEmpty()) {
            return targetRepository.count();
        }

        return targetRepository.count(SpecificationsBuilder.combineWithAnd(specList));
    }

    /**
     * {@link Entity} based method call for
     * {@link #toggleTagAssignment(Collection, String)}.
     *
     * @param targets
     *            to toggle for
     * @param tag
     *            to toogle
     * @return TagAssigmentResult with all metadata of the assigment outcome.
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public TargetTagAssigmentResult toggleTagAssignment(@NotEmpty final List<Target> targets,
            @NotNull final TargetTag tag) {
        return toggleTagAssignment(
                targets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()), tag.getName());
    }

    /**
     * Toggles {@link TargetTag} assignment to given {@link Target}s by means
     * that if some (or all) of the targets in the list have the {@link Tag} not
     * yet assigned, they will be. If all of theme have the tag already assigned
     * they will be removed instead.
     *
     * @param targetIds
     *            to toggle for
     * @param tagName
     *            to toogle
     * @return TagAssigmentResult with all metadata of the assigment outcome.
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public TargetTagAssigmentResult toggleTagAssignment(@NotEmpty final Collection<String> targetIds,
            @NotNull final String tagName) {
        final TargetTag tag = targetTagRepository.findByNameEquals(tagName);
        final List<Target> alreadyAssignedTargets = targetRepository.findByTagNameAndControllerIdIn(tagName, targetIds);
        final List<Target> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(targetIds));

        // all are already assigned -> unassign
        if (alreadyAssignedTargets.size() == allTargets.size()) {
            alreadyAssignedTargets.forEach(target -> target.getTags().remove(tag));
            final TargetTagAssigmentResult result = new TargetTagAssigmentResult(0, 0, alreadyAssignedTargets.size(),
                    Collections.emptyList(), alreadyAssignedTargets, tag);

            afterCommit.afterCommit(() -> eventBus.post(new TargetTagAssigmentResultEvent(result)));
            return result;
        }

        allTargets.removeAll(alreadyAssignedTargets);
        // some or none are assigned -> assign
        allTargets.forEach(target -> target.getTags().add(tag));
        final TargetTagAssigmentResult result = new TargetTagAssigmentResult(alreadyAssignedTargets.size(),
                allTargets.size(), 0, targetRepository.save(allTargets), Collections.emptyList(), tag);

        afterCommit.afterCommit(() -> eventBus.post(new TargetTagAssigmentResultEvent(result)));

        // no reason to persist the tag
        entityManager.detach(tag);
        return result;
    }

    /**
     * Assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param targetIds
     *            to assign for
     * @param tagName
     *            to assign
     * @return list of assigned targets
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public List<Target> assignTag(@NotEmpty final Collection<String> targetIds, @NotNull final TargetTag tag) {
        final List<Target> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(targetIds));

        allTargets.forEach(target -> target.getTags().add(tag));
        final List<Target> save = targetRepository.save(allTargets);

        afterCommit.afterCommit(() -> {
            final TargetTagAssigmentResult assigmentResult = new TargetTagAssigmentResult(0, save.size(), 0, save,
                    Collections.emptyList(), tag);
            eventBus.post(new TargetTagAssigmentResultEvent(assigmentResult));
        });

        return save;
    }

    private List<Target> unAssignTag(@NotEmpty final Collection<Target> targets, @NotNull final TargetTag tag) {
        targets.forEach(target -> target.getTags().remove(tag));

        final List<Target> save = targetRepository.save(targets);
        afterCommit.afterCommit(() -> {
            final TargetTagAssigmentResult assigmentResult = new TargetTagAssigmentResult(0, 0, save.size(),
                    Collections.emptyList(), save, tag);
            eventBus.post(new TargetTagAssigmentResultEvent(assigmentResult));
        });
        return save;
    }

    /**
     * Unassign all {@link Target} from a given {@link TargetTag} .
     *
     * @param tag
     *            to unassign all targets
     * @return list of unassigned targets
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public List<Target> unAssignAllTargetsByTag(@NotNull final TargetTag tag) {
        return unAssignTag(tag.getAssignedToTargets(), tag);
    }

    /**
     * Unassign a {@link TargetTag} assignment to given {@link Target}.
     *
     * @param controllerID
     *            to unassign for
     * @param targetTag
     *            to unassign
     * @return the unassigned target or <null> if no target is unassigned
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public Target unAssignTag(@NotNull final String controllerID, @NotNull final TargetTag targetTag) {
        final List<Target> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithStatusAndTagsInJoin(Arrays.asList(controllerID)));
        final List<Target> unAssignTag = unAssignTag(allTargets, targetTag);
        return unAssignTag.isEmpty() ? null : unAssignTag.get(0);
    }

    /**
     * method retrieves all {@link Target}s from the repo in the following
     * order:
     * <p>
     * 1) {@link Target}s which have the given {@link DistributionSet} as
     * {@link Target#getTargetStatus()}
     * {@link TargetStatus#getInstalledDistributionSet()}
     * <p>
     * 2) {@link Target}s which have the given {@link DistributionSet} as
     * {@link Target#getAssignedDistributionSet()}
     * <p>
     * 3) {@link Target}s which have no connection to the given
     * {@link DistributionSet}.
     *
     * @param pageable
     *            the page request to page the result set
     * @param orderByDistributionId
     *            {@link DistributionSet#getId()} to be ordered by
     * @param filterByDistributionId
     *            {@link DistributionSet#getId()} to be filter the result. Set
     *            to <code>null</code> in case this is not required.
     * @param filterByStatus
     *            find targets having this {@link TargetUpdateStatus}s. Set to
     *            <code>null</code> in case this is not required.
     * @param filterBySearchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param installedOrAssignedDistributionSetId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param filterByTagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     * @return a paged result {@link Page} of the {@link Target}s in a defined
     *         order.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Slice<Target> findTargetsAllOrderByLinkedDistributionSet(@NotNull final Pageable pageable,
            @NotNull final Long orderByDistributionId, final Long filterByDistributionId,
            final Collection<TargetUpdateStatus> filterByStatus, final String filterBySearchText,
            final Boolean selectTargetWithNoTag, final String... filterByTagNames) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Target> query = cb.createQuery(Target.class);
        final Root<Target> targetRoot = query.from(Target.class);

        // necessary joins for the select
        final Join<Target, TargetInfo> targetInfo = (Join<Target, TargetInfo>) targetRoot.fetch(Target_.targetInfo,
                JoinType.LEFT);

        // select case expression to retrieve the case value as a column to be
        // able to order based on
        // this column, installed first,...
        final Expression<Object> selectCase = cb.selectCase()
                .when(cb.equal(targetInfo.get(TargetInfo_.installedDistributionSet).get(DistributionSet_.id),
                        orderByDistributionId), 1)
                .when(cb.equal(targetRoot.get(Target_.assignedDistributionSet).get(DistributionSet_.id),
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
        query.orderBy(cb.asc(selectCase), cb.desc(targetRoot.get(Target_.id)));
        // the result is a Object[] due the fact that the selectCase is an extra
        // column, so it cannot
        // be mapped directly to a Target entity because the selectCase is not a
        // attribute of the
        // Target entity, the the Object array contains the Target on the first
        // index (case of the
        // multiselect order) of the array and
        // the 2nd contains the selectCase int value.
        final int pageSize = pageable.getPageSize();
        final List<Target> resultList = entityManager.createQuery(query).setFirstResult(pageable.getOffset())
                .setMaxResults(pageSize + 1).getResultList();
        final boolean hasNext = resultList.size() > pageSize;
        return new SliceImpl<>(resultList, pageable, hasNext);
    }

    /**
     * @param specifications
     */
    private static Predicate[] specificationsToPredicate(final List<Specification<Target>> specifications,
            final Root<Target> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        final Predicate[] predicates = new Predicate[specifications.size()];
        for (int index = 0; index < predicates.length; index++) {
            predicates[index] = specifications.get(index).toPredicate(root, query, cb);
        }
        return predicates;
    }

    /**
     * Counts number of targets with given
     * {@link Target#getAssignedDistributionSet()}.
     *
     * @param distId
     *            to search for
     *
     * @return number of found {@link Target}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countTargetByAssignedDistributionSet(final Long distId) {
        return targetRepository.countByAssignedDistributionSetId(distId);
    }

    /**
     * Counts number of targets with given
     * {@link TargetStatus#getInstalledDistributionSet()}.
     *
     * @param distId
     *            to search for
     * @return number of found {@link Target}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countTargetByInstalledDistributionSet(final Long distId) {
        return targetRepository.countByTargetInfoInstalledDistributionSetId(distId);
    }

    /**
     * finds all {@link Target#getControllerId()} which are currently in the
     * database.
     *
     * @return all IDs of all {@link Target} in the system
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<TargetIdName> findAllTargetIds() {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<TargetIdName> query = cb.createQuery(TargetIdName.class);
        final Root<Target> targetRoot = query.from(Target.class);
        return entityManager.createQuery(query.multiselect(targetRoot.get(Target_.id),
                targetRoot.get(Target_.controllerId), targetRoot.get(Target_.name))).getResultList();

    }

    /**
     * finds all {@link Target#getControllerId()} for all the given parameters.
     *
     * @param pageRequest
     *            the pageRequest to enhance the query for paging and sorting
     * @param filterByDistributionId
     *            to find targets having the {@link DistributionSet} as
     *            installed or assigned. Set to <code>null</code> in case this
     *            is not required.
     * @param filterByStatus
     *            find targets having this {@link TargetUpdateStatus}s. Set to
     *            <code>null</code> in case this is not required.
     * @param filterBySearchText
     *            to find targets having the text anywhere in name or
     *            description. Set <code>null</code> in case this is not
     *            required.
     * @param filterByTagNames
     *            to find targets which are having any one in this tag names.
     *            Set <code>null</code> in case this is not required.
     * @param selectTargetWithNoTag
     *            flag to select targets with no tag assigned
     *
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<TargetIdName> findAllTargetIdsByFilters(final PageRequest pageRequest,
            final Long filterByDistributionId, final Collection<TargetUpdateStatus> filterByStatus,
            final String filterBySearchText, final Boolean selectTargetWithNoTag, final String... filterByTagNames) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<Target> targetRoot = query.from(Target.class);
        List<Object[]> resultList;
        final CriteriaQuery<Object[]> multiselect = query.multiselect(targetRoot.get(Target_.id),
                targetRoot.get(Target_.controllerId), targetRoot.get(Target_.name),
                targetRoot.get(pageRequest.getSort().iterator().next().getProperty()));

        final Predicate[] specificationsForMultiSelect = specificationsToPredicate(
                buildSpecificationList(filterByStatus, filterBySearchText, filterByDistributionId,
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

    /**
     * Finds all {@link Target#getControllerId()} for all the given parameter
     * {@link TargetFilterQuery}.
     *
     * @param pageRequest
     *            the pageRequest to enhance the query for paging and sorting
     * @param targetFilterQuery
     *            {@link TargetFilterQuery}
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<TargetIdName> findAllTargetIdsByTargetFilterQuery(final PageRequest pageRequest,
            @NotNull final TargetFilterQuery targetFilterQuery) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<Target> targetRoot = query.from(Target.class);
        final CriteriaQuery<Object[]> multiselect = query.multiselect(targetRoot.get(Target_.id),
                targetRoot.get(Target_.controllerId), targetRoot.get(Target_.name),
                targetRoot.get(pageRequest.getSort().iterator().next().getProperty()));

        final Specification<Target> spec = RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class);
        final List<Specification<Target>> specList = new ArrayList<>();
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

    /**
     * creating new {@link Target}s including poll status data. useful
     * especially in plug and play scenarios.
     *
     * @param target
     *            to be created *
     * @param status
     *            of the target
     * @param lastTargetQuery
     *            if a plug and play case
     * @param address
     *            if a plug and play case
     *
     * @throws EntityAlreadyExistsException
     *
     * @return
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    @CacheEvict(value = { "targetsCreatedOverPeriod" }, allEntries = true)
    public Target createTarget(@NotNull final Target target, @NotNull final TargetUpdateStatus status,
            final Long lastTargetQuery, final URI address) {

        if (targetRepository.findByControllerId(target.getControllerId()) != null) {
            throw new EntityAlreadyExistsException(target.getControllerId());
        }

        target.setNew(true);
        final Target savedTarget = targetRepository.save(target);
        final TargetInfo targetInfo = savedTarget.getTargetInfo();
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

    /**
     * creating a new {@link Target}.
     *
     * @param target
     *            to be created
     * @return the created {@link Target}
     *
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    @CacheEvict(value = { "targetsCreatedOverPeriod" }, allEntries = true)
    public Target createTarget(@NotNull final Target target) {
        return createTarget(target, TargetUpdateStatus.UNKNOWN, null, null);
    }

    /**
     * creates multiple {@link Target}s. If some of the given {@link Target}s
     * already exists in the DB a {@link EntityAlreadyExistsException} is
     * thrown. {@link Target}s contain all objects of the parameter targets,
     * including duplicates.
     *
     * @param targets
     *            to be created.
     * @return the created {@link Target}s
     *
     * @throws {@link
     *             EntityAlreadyExistsException} of one of the given targets
     *             already exist.
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    public List<Target> createTargets(@NotNull final List<Target> targets) {
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

    /**
     * creating a new {@link Target} including poll status data. useful
     * especially in plug and play scenarios.
     *
     * @param targets
     *            to be created *
     * @param status
     *            of the target
     * @param lastTargetQuery
     *            if a plug and play case
     * @param address
     *            if a plug and play case
     *
     * @return newly created target
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    public List<Target> createTargets(@NotNull final Collection<Target> targets,
            @NotNull final TargetUpdateStatus status, final long lastTargetQuery, final URI address) {
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

    /**
     * Find targets by tag name.
     *
     * @param tagName
     *            tag name
     * @return list of matching targets
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public List<Target> findTargetsByTag(@NotNull final String tagName) {
        final TargetTag tag = targetTagRepository.findByNameEquals(tagName);
        return targetRepository.findByTag(tag);
    }

    /**
     * Count {@link TargetFilterQuery}s for given filter parameter.
     *
     * @param targetFilterQuery
     *            {link TargetFilterQuery}
     * @return the found number {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countTargetByTargetFilterQuery(@NotNull final TargetFilterQuery targetFilterQuery) {
        final Specification<Target> specs = RSQLUtility.parse(targetFilterQuery.getQuery(), TargetFields.class);
        return targetRepository.count(specs);
    }

    /**
     * Count {@link TargetFilterQuery}s for given target filter query.
     *
     * @param targetFilterQuery
     *            {link TargetFilterQuery}
     * @return the found number {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    public Long countTargetByTargetFilterQuery(@NotNull final String targetFilterQuery) {
        final Specification<Target> specs = RSQLUtility.parse(targetFilterQuery, TargetFields.class);
        return targetRepository.count(specs);
    }

    private List<Object[]> getTargetIdNameResultSet(final PageRequest pageRequest, final CriteriaBuilder cb,
            final Root<Target> targetRoot, final CriteriaQuery<Object[]> multiselect) {
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
}
