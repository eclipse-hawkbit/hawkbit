/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling rollout CRUD operations.
 *
 */
@RestController
public class MgmtRolloutResource implements MgmtRolloutRestApi {

    private static final String DOES_NOT_EXIST = "} does not exist";

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Override
    public ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(final int pagingOffsetParam,
            final int pagingLimitParam, final String sortParam, final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<Rollout> findModulesAll;
        if (rsqlParam != null) {
            findModulesAll = this.rolloutManagement
                    .findAllWithDetailedStatusByPredicate(RSQLUtility.parse(rsqlParam, RolloutFields.class), pageable);
        } else {
            findModulesAll = this.rolloutManagement.findAll(pageable);
        }

        final List<MgmtRolloutResponseBody> rest = MgmtRolloutMapper.toResponseRollout(findModulesAll.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, findModulesAll.getTotalElements()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> getRollout(final Long rolloutId) {
        final Rollout findRolloutById = findRolloutOrThrowException(rolloutId);
        return new ResponseEntity<>(MgmtRolloutMapper.toResponseRollout(findRolloutById), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtRolloutResponseBody> create(@RequestBody final MgmtRolloutRestRequestBody rolloutRequestBody) {

        // first check the given RSQL query if it's well formed, otherwise and
        // exception is thrown
        RSQLUtility.parse(rolloutRequestBody.getTargetFilterQuery(), TargetFields.class);

        final DistributionSet distributionSet = findDistributionSetOrThrowException(rolloutRequestBody);

        // success condition
        RolloutGroupSuccessCondition successCondition = null;
        String successConditionExpr = null;
        // success action
        RolloutGroupSuccessAction successAction = null;
        String successActionExpr = null;
        // error condition
        RolloutGroupErrorCondition errorCondition = null;
        // error action
        String errorConditionExpr = null;
        RolloutGroupErrorAction errorAction = null;
        String errorActionExpr = null;
        if (rolloutRequestBody.getSuccessCondition() != null) {
            successCondition = MgmtRolloutMapper
                    .mapFinishCondition(rolloutRequestBody.getSuccessCondition().getCondition());
            successConditionExpr = rolloutRequestBody.getSuccessCondition().getExpression();
        }
        if (rolloutRequestBody.getSuccessAction() != null) {
            successAction = MgmtRolloutMapper.map(rolloutRequestBody.getSuccessAction().getAction());
            successActionExpr = rolloutRequestBody.getSuccessAction().getExpression();
        }
        if (rolloutRequestBody.getErrorCondition() != null) {
            errorCondition = MgmtRolloutMapper.mapErrorCondition(rolloutRequestBody.getErrorCondition().getCondition());
            errorConditionExpr = rolloutRequestBody.getErrorCondition().getExpression();
        }
        if (rolloutRequestBody.getErrorAction() != null) {
            errorAction = MgmtRolloutMapper.map(rolloutRequestBody.getErrorAction().getAction());
            errorActionExpr = rolloutRequestBody.getErrorAction().getExpression();
        }

        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroup.RolloutGroupConditionBuilder()
                .successCondition(successCondition, successConditionExpr)
                .successAction(successAction, successActionExpr).errorCondition(errorCondition, errorConditionExpr)
                .errorAction(errorAction, errorActionExpr).build();
        final Rollout rollout = this.rolloutManagement.createRollout(
                MgmtRolloutMapper.fromRequest(rolloutRequestBody, distributionSet,
                        rolloutRequestBody.getTargetFilterQuery()),
                rolloutRequestBody.getAmountGroups(), rolloutGroupConditions);

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtRolloutMapper.toResponseRollout(rollout));
    }

    @Override
    public ResponseEntity<Void> start(final Long rolloutId, final boolean startAsync) {
        final Rollout rollout = findRolloutOrThrowException(rolloutId);
        if (startAsync) {
            this.rolloutManagement.startRolloutAsync(rollout);
        } else {
            this.rolloutManagement.startRollout(rollout);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> pause(final Long rolloutId) {
        final Rollout rollout = findRolloutOrThrowException(rolloutId);
        this.rolloutManagement.pauseRollout(rollout);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> resume(final Long rolloutId) {
        final Rollout rollout = findRolloutOrThrowException(rolloutId);
        this.rolloutManagement.resumeRollout(rollout);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(final Long rolloutId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final Rollout rollout = findRolloutOrThrowException(rolloutId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutGroupSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<RolloutGroup> findRolloutGroupsAll;
        if (rsqlParam != null) {
            findRolloutGroupsAll = this.rolloutGroupManagement.findRolloutGroupsByPredicate(rollout,
                    RSQLUtility.parse(rsqlParam, RolloutGroupFields.class), pageable);
        } else {
            findRolloutGroupsAll = this.rolloutGroupManagement.findRolloutGroupsByRolloutId(rolloutId, pageable);
        }

        final List<MgmtRolloutGroupResponseBody> rest = MgmtRolloutMapper
                .toResponseRolloutGroup(findRolloutGroupsAll.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, findRolloutGroupsAll.getTotalElements()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(final Long rolloutId, final Long groupId) {
        findRolloutOrThrowException(rolloutId);
        final RolloutGroup rolloutGroup = findRolloutGroupOrThrowException(groupId);
        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRolloutGroup(rolloutGroup));
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(final Long rolloutId, final Long groupId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        findRolloutOrThrowException(rolloutId);
        final RolloutGroup rolloutGroup = findRolloutGroupOrThrowException(groupId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<Target> rolloutGroupTargets;
        if (rsqlParam != null) {
            final Specification<Target> rsqlSpecification = RSQLUtility.parse(rsqlParam, TargetFields.class);
            rolloutGroupTargets = this.rolloutGroupManagement.findRolloutGroupTargets(rolloutGroup, rsqlSpecification,
                    pageable);
        } else {
            final Page<Target> pageTargets = this.rolloutGroupManagement.findRolloutGroupTargets(rolloutGroup,
                    pageable);
            rolloutGroupTargets = pageTargets;
        }
        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(rolloutGroupTargets.getContent());
        return new ResponseEntity<>(new PagedList<MgmtTarget>(rest, rolloutGroupTargets.getTotalElements()),
                HttpStatus.OK);
    }

    private Rollout findRolloutOrThrowException(final Long rolloutId) {
        final Rollout rollout = this.rolloutManagement.findRolloutWithDetailedStatus(rolloutId);
        if (rollout == null) {
            throw new EntityNotFoundException("Rollout with Id {" + rolloutId + DOES_NOT_EXIST);
        }
        return rollout;
    }

    private RolloutGroup findRolloutGroupOrThrowException(final Long rolloutGroupId) {
        final RolloutGroup rolloutGroup = this.rolloutGroupManagement.findRolloutGroupById(rolloutGroupId);
        if (rolloutGroup == null) {
            throw new EntityNotFoundException("Group with Id {" + rolloutGroupId + DOES_NOT_EXIST);
        }
        return rolloutGroup;
    }

    private DistributionSet findDistributionSetOrThrowException(final MgmtRolloutRestRequestBody rolloutRequestBody) {
        final DistributionSet ds = this.distributionSetManagement
                .findDistributionSetById(rolloutRequestBody.getDistributionSetId());
        if (ds == null) {
            throw new EntityNotFoundException(
                    "DistributionSet with Id {" + rolloutRequestBody.getDistributionSetId() + DOES_NOT_EXIST);
        }
        return ds;
    }
}
