/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionStatus;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtDistributionSetAssigment;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAttributes;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.data.SortDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

/**
 * REST Resource handling target CRUD operations.
 */
@RestController
public class MgmtTargetResource implements MgmtTargetRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtTargetResource.class);

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private DeploymentManagement deploymentManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ResponseEntity<MgmtTarget> getTarget(@PathVariable("controllerId") final String controllerId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(controllerId);
        // to single response include poll status
        final MgmtTarget response = MgmtTargetMapper.toResponse(findTarget);
        MgmtTargetMapper.addPollStatus(findTarget, response);
        MgmtTargetMapper.addTargetLinks(response);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getTargets(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<Target> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam != null) {
            final Page<Target> findTargetPage = this.targetManagement.findTargetsAll(rsqlParam, pageable);
            countTargetsAll = findTargetPage.getTotalElements();
            findTargetsAll = findTargetPage;
        } else {
            findTargetsAll = this.targetManagement.findTargetsAll(pageable);
            countTargetsAll = this.targetManagement.countTargetsAll();
        }

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(findTargetsAll.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, countTargetsAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtTarget>> createTargets(@RequestBody final List<MgmtTargetRequestBody> targets) {
        LOG.debug("creating {} targets", targets.size());
        final Collection<Target> createdTargets = this.targetManagement
                .createTargets(MgmtTargetMapper.fromRequest(entityFactory, targets));
        LOG.debug("{} targets created, return status {}", targets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(MgmtTargetMapper.toResponse(createdTargets), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTarget> updateTarget(@PathVariable("controllerId") final String controllerId,
            @RequestBody final MgmtTargetRequestBody targetRest) {

        final Target updateTarget = this.targetManagement.updateTarget(entityFactory.target().update(controllerId)
                .name(targetRest.getName()).description(targetRest.getDescription()).address(targetRest.getAddress())
                .securityToken(targetRest.getSecurityToken()));

        return new ResponseEntity<>(MgmtTargetMapper.toResponse(updateTarget), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteTarget(@PathVariable("controllerId") final String controllerId) {
        this.targetManagement.deleteTarget(controllerId);
        LOG.debug("{} target deleted, return status {}", controllerId, HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtTargetAttributes> getAttributes(@PathVariable("controllerId") final String controllerId) {
        final Map<String, String> controllerAttributes = targetManagement.getControllerAttributes(controllerId);
        if (controllerAttributes.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        final MgmtTargetAttributes result = new MgmtTargetAttributes();
        result.putAll(controllerAttributes);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedList<MgmtAction>> getActionHistory(
            @PathVariable("controllerId") final String controllerId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        findTargetWithExceptionIfNotFound(controllerId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> activeActions;
        final Long totalActionCount;
        if (rsqlParam != null) {
            activeActions = this.deploymentManagement.findActionsByTarget(rsqlParam, controllerId, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(rsqlParam, controllerId);
        } else {
            activeActions = this.deploymentManagement.findActionsByTarget(controllerId, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(controllerId);
        }

        return new ResponseEntity<>(
                new PagedList<>(MgmtTargetMapper.toResponse(controllerId, activeActions.getContent()),
                        totalActionCount),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtAction> getAction(@PathVariable("controllerId") final String controllerId,
            @PathVariable("actionId") final Long actionId) {

        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
        if (!action.getTarget().getControllerId().equals(controllerId)) {
            LOG.warn("given action ({}) is not assigned to given target ({}).", action.getId(), controllerId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final MgmtAction result = MgmtTargetMapper.toResponse(controllerId, action, action.isActive());

        if (!action.isCancelingOrCanceled()) {
            result.add(linkTo(
                    methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(action.getDistributionSet().getId()))
                            .withRel("distributionset"));
        } else if (action.isCancelingOrCanceled()) {
            result.add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(controllerId, action.getId()))
                    .withRel(MgmtRestConstants.TARGET_V1_CANCELED_ACTION));
        }

        result.add(linkTo(methodOn(MgmtTargetRestApi.class).getActionStatusList(controllerId, action.getId(), 0,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionStatusFields.ID.getFieldName() + ":" + SortDirection.DESC))
                        .withRel(MgmtRestConstants.TARGET_V1_ACTION_STATUS));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> cancelAction(@PathVariable("controllerId") final String controllerId,
            @PathVariable("actionId") final Long actionId,
            @RequestParam(value = "force", required = false, defaultValue = "false") final boolean force) {
        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.getTarget().getControllerId().equals(controllerId)) {
            LOG.warn("given action ({}) is not assigned to given target ({}).", actionId, controllerId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (force) {
            this.deploymentManagement.forceQuitAction(actionId);
        } else {
            this.deploymentManagement.cancelAction(actionId);
        }
        // both functions will throw an exception, when action is in wrong
        // state, which is mapped by MgmtResponseExceptionHandler.

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<PagedList<MgmtActionStatus>> getActionStatusList(
            @PathVariable("controllerId") final String controllerId, @PathVariable("actionId") final Long actionId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam) {

        final Target target = findTargetWithExceptionIfNotFound(controllerId);

        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn("given action ({}) is not assigned to given target ({}).", action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionStatusSortParam(sortParam);

        final Page<ActionStatus> statusList = this.deploymentManagement.findActionStatusByAction(
                new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting), action.getId());

        return new ResponseEntity<>(new PagedList<>(
                MgmtTargetMapper.toActionStatusRestResponse(statusList.getContent(), deploymentManagement),
                statusList.getTotalElements()), HttpStatus.OK);

    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(
            @PathVariable("controllerId") final String controllerId) {
        final MgmtDistributionSet distributionSetRest = deploymentManagement.getAssignedDistributionSet(controllerId)
                .map(MgmtDistributionSetMapper::toResponse).orElse(null);

        if (distributionSetRest == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(distributionSetRest, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> postAssignedDistributionSet(@PathVariable("controllerId") final String controllerId,
            @RequestBody final MgmtDistributionSetAssigment dsId) {

        findTargetWithExceptionIfNotFound(controllerId);
        final ActionType type = (dsId.getType() != null) ? MgmtRestModelMapper.convertActionType(dsId.getType())
                : ActionType.FORCED;
        this.deploymentManagement.assignDistributionSet(dsId.getId(), type, dsId.getForcetime(),
                Lists.newArrayList(controllerId));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getInstalledDistributionSet(
            @PathVariable("controllerId") final String controllerId) {
        final MgmtDistributionSet distributionSetRest = deploymentManagement.getInstalledDistributionSet(controllerId)
                .map(MgmtDistributionSetMapper::toResponse).orElse(null);

        if (distributionSetRest == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(distributionSetRest, HttpStatus.OK);
    }

    private Target findTargetWithExceptionIfNotFound(final String controllerId) {
        return targetManagement.findTargetByControllerID(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

}
