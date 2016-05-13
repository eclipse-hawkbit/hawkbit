/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.api.DistributionSetRestApi;
import org.eclipse.hawkbit.rest.resource.api.TargetRestApi;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.action.ActionRest;
import org.eclipse.hawkbit.rest.resource.model.action.ActionStatusRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRest;
import org.eclipse.hawkbit.rest.resource.model.target.DistributionSetAssigmentRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetAttributes;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling target CRUD operations.
 */
@RestController
public class TargetResource implements TargetRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(TargetResource.class);

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private DeploymentManagement deploymentManagement;

    @Override
    public ResponseEntity<TargetRest> getTarget(final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        // to single response include poll status
        final TargetRest response = TargetMapper.toResponse(findTarget);
        TargetMapper.addPollStatus(findTarget, response);
        TargetMapper.addTargetLinks(response);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedList<TargetRest>> getTargets(final int pagingOffsetParam, final int pagingLimitParam,
            final String sortParam, final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<Target> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam != null) {
            final Page<Target> findTargetPage = this.targetManagement
                    .findTargetsAll(RSQLUtility.parse(rsqlParam, TargetFields.class), pageable);
            countTargetsAll = findTargetPage.getTotalElements();
            findTargetsAll = findTargetPage;
        } else {
            findTargetsAll = this.targetManagement.findTargetsAll(pageable);
            countTargetsAll = this.targetManagement.countTargetsAll();
        }

        final List<TargetRest> rest = TargetMapper.toResponse(findTargetsAll.getContent());
        return new ResponseEntity<>(new PagedList<TargetRest>(rest, countTargetsAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TargetRest>> createTargets(final List<TargetRequestBody> targets) {
        LOG.debug("creating {} targets", targets.size());
        final Iterable<Target> createdTargets = this.targetManagement.createTargets(TargetMapper.fromRequest(targets));
        LOG.debug("{} targets created, return status {}", targets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(TargetMapper.toResponse(createdTargets), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<TargetRest> updateTarget(final String targetId, final TargetRequestBody targetRest) {
        final Target existingTarget = findTargetWithExceptionIfNotFound(targetId);
        LOG.debug("updating target {}", existingTarget.getId());
        if (targetRest.getDescription() != null) {
            existingTarget.setDescription(targetRest.getDescription());
        }
        if (targetRest.getName() != null) {
            existingTarget.setName(targetRest.getName());
        }
        final Target updateTarget = this.targetManagement.updateTarget(existingTarget);

        return new ResponseEntity<>(TargetMapper.toResponse(updateTarget), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteTarget(final String targetId) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);
        this.targetManagement.deleteTargets(target.getId());
        LOG.debug("{} target deleted, return status {}", targetId, HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<TargetAttributes> getAttributes(final String targetId) {
        final Target foundTarget = findTargetWithExceptionIfNotFound(targetId);
        final Map<String, String> controllerAttributes = foundTarget.getTargetInfo().getControllerAttributes();
        if (controllerAttributes.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        final TargetAttributes result = new TargetAttributes();
        result.putAll(controllerAttributes);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedList<ActionRest>> getActionHistory(final String targetId, final int pagingOffsetParam,
            final int pagingLimitParam, final String sortParam, final String rsqlParam) {

        final Target foundTarget = findTargetWithExceptionIfNotFound(targetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> activeActions;
        final Long totalActionCount;
        if (rsqlParam != null) {
            final Specification<Action> parse = RSQLUtility.parse(rsqlParam, ActionFields.class);
            activeActions = this.deploymentManagement.findActionsByTarget(parse, foundTarget, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(parse, foundTarget);
        } else {
            activeActions = this.deploymentManagement.findActionsByTarget(foundTarget, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(foundTarget);
        }

        return new ResponseEntity<>(
                new PagedList<>(TargetMapper.toResponse(targetId, activeActions.getContent()), totalActionCount),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ActionRest> getAction(final String targetId, final Long actionId) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn("given action ({}) is not assigned to given target ({}).", action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final ActionRest result = TargetMapper.toResponse(targetId, action, action.isActive());

        if (!action.isCancelingOrCanceled()) {
            result.add(linkTo(
                    methodOn(DistributionSetRestApi.class).getDistributionSet(action.getDistributionSet().getId()))
                            .withRel("distributionset"));
        } else if (action.isCancelingOrCanceled()) {
            result.add(linkTo(methodOn(TargetRestApi.class).getAction(targetId, action.getId()))
                    .withRel(RestConstants.TARGET_V1_CANCELED_ACTION));
        }

        result.add(linkTo(methodOn(TargetRestApi.class).getActionStatusList(targetId, action.getId(), 0,
                RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionStatusFields.ID.getFieldName() + ":" + SortDirection.DESC))
                        .withRel(RestConstants.TARGET_V1_ACTION_STATUS));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> cancelAction(final String targetId, final Long actionId,
            @RequestParam(required = false, defaultValue = "false") final boolean force) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);
        final Action action = findActionWithExceptionIfNotFound(actionId);

        if (force) {
            this.deploymentManagement.forceQuitAction(action);
        } else {
            this.deploymentManagement.cancelAction(action, target);
        }
        // both functions will throw an exception, when action is in wrong
        // state, which is mapped by ResponseExceptionHandler.

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<PagedList<ActionStatusRest>> getActionStatusList(final String targetId, final Long actionId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {

        final Target target = findTargetWithExceptionIfNotFound(targetId);

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn("given action ({}) is not assigned to given target ({}).", action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionStatusSortParam(sortParam);

        final Page<ActionStatus> statusList = this.deploymentManagement.findActionStatusByAction(
                new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting), action, true);

        return new ResponseEntity<>(new PagedList<>(TargetMapper.toActionStatusRestResponse(statusList.getContent()),
                statusList.getTotalElements()), HttpStatus.OK);

    }

    @Override
    public ResponseEntity<DistributionSetRest> getAssignedDistributionSet(final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        final DistributionSetRest distributionSetRest = DistributionSetMapper
                .toResponse(findTarget.getAssignedDistributionSet());
        final HttpStatus retStatus;
        if (distributionSetRest == null) {
            retStatus = HttpStatus.NO_CONTENT;
        } else {
            retStatus = HttpStatus.OK;
        }
        return new ResponseEntity<>(distributionSetRest, retStatus);
    }

    @Override
    public ResponseEntity<Void> postAssignedDistributionSet(final String targetId,
            final DistributionSetAssigmentRest dsId) {

        findTargetWithExceptionIfNotFound(targetId);
        final ActionType type = (dsId.getType() != null)
                ? RestResourceConversionHelper.convertActionType(dsId.getType()) : ActionType.FORCED;
        final Iterator<Target> changed = this.deploymentManagement
                .assignDistributionSet(dsId.getId(), type, dsId.getForcetime(), targetId).getAssignedEntity()
                .iterator();
        if (changed.hasNext()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        LOG.error("Target update (ds {} assigment to target {}) failed! Returnd target list is empty.", dsId.getId(),
                targetId);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @Override
    public ResponseEntity<DistributionSetRest> getInstalledDistributionSet(final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        final DistributionSetRest distributionSetRest = DistributionSetMapper
                .toResponse(findTarget.getTargetInfo().getInstalledDistributionSet());
        final HttpStatus retStatus;
        if (distributionSetRest == null) {
            retStatus = HttpStatus.NO_CONTENT;
        } else {
            retStatus = HttpStatus.OK;
        }
        return new ResponseEntity<>(distributionSetRest, retStatus);
    }

    private Target findTargetWithExceptionIfNotFound(final String targetId) {
        final Target findTarget = this.targetManagement.findTargetByControllerID(targetId);
        if (findTarget == null) {
            throw new EntityNotFoundException("Target with Id {" + targetId + "} does not exist");
        }
        return findTarget;
    }

    private Action findActionWithExceptionIfNotFound(final Long actionId) {
        final Action findAction = this.deploymentManagement.findAction(actionId);
        if (findAction == null) {
            throw new EntityNotFoundException("Action with Id {" + actionId + "} does not exist");
        }
        return findAction;
    }
}
