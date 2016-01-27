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

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.rest.resource.model.action.ActionPagedList;
import org.eclipse.hawkbit.rest.resource.model.action.ActionRest;
import org.eclipse.hawkbit.rest.resource.model.action.ActionStatusPagedList;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRest;
import org.eclipse.hawkbit.rest.resource.model.target.DistributionSetAssigmentRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetAttributes;
import org.eclipse.hawkbit.rest.resource.model.target.TargetPagedList;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling target CRUD operations.
 *
 *
 *
 *
 */
@RestController
@RequestMapping(RestConstants.TARGET_V1_REQUEST_MAPPING)
public class TargetResource {
    private static final Logger LOG = LoggerFactory.getLogger(TargetResource.class);

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private DeploymentManagement deploymentManagement;

    @Autowired
    private EntityManager entityManager;

    /**
     * Handles the GET request of retrieving a single target within SP.
     *
     * @param targetId
     *            the ID of the target to retrieve
     * @return a single target with status OK.
     * @throws EntityNotFoundException
     *             in case no target with the given {@code targetId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TargetRest> getTarget(@PathVariable final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        // to single response include poll status
        final TargetRest response = TargetMapper.toResponse(findTarget);
        TargetMapper.addPollStatus(findTarget, response);
        TargetMapper.addTargetLinks(response);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving all targets within SP.
     *
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return a list of all targets for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TargetPagedList> getTargets(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<Target> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam != null) {
            final Page<Target> findTargetPage = targetManagement
                    .findTargetsAll(RSQLUtility.parse(rsqlParam, TargetFields.class), pageable);
            countTargetsAll = findTargetPage.getTotalElements();
            findTargetsAll = findTargetPage;
        } else {
            findTargetsAll = targetManagement.findTargetsAll(pageable);
            countTargetsAll = targetManagement.countTargetsAll();
        }

        final List<TargetRest> rest = TargetMapper.toResponse(findTargetsAll.getContent());
        return new ResponseEntity<>(new TargetPagedList(rest, countTargetsAll), HttpStatus.OK);
    }

    /**
     * Handles the POST request of creating new targets within SP. The request
     * body must always be a list of targets. The requests is delegating to the
     * {@link TargetManagement#createTarget(Iterable)}.
     *
     * @param targets
     *            the targets to be created.
     * @return In case all targets could successful created the ResponseEntity
     *         with status code 201 with a list of successfully created
     *         entities. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TargetsRest> createTargets(@RequestBody final List<TargetRequestBody> targets) {
        LOG.debug("creating {} targets", targets.size());
        final Iterable<Target> createdTargets = targetManagement.createTargets(TargetMapper.fromRequest(targets));
        LOG.debug("{} targets created, return status {}", targets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(TargetMapper.toResponse(createdTargets), HttpStatus.CREATED);
    }

    /**
     * Handles the PUT request of updating a target within SP. The ID is within
     * the URL path of the request. A given ID in the request body is ignored.
     * It's not possible to set fields to {@code null} values.
     *
     * @param targetId
     *            the path parameter which contains the ID of the target
     * @param targetRest
     *            the request body which contains the fields which should be
     *            updated, fields which are not given are ignored for the
     *            udpate.
     * @return the updated target response which contains all fields also fields
     *         which have not updated
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{targetId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TargetRest> updateTarget(@PathVariable final String targetId,
            @RequestBody final TargetRequestBody targetRest) {
        final Target existingTarget = findTargetWithExceptionIfNotFound(targetId);
        LOG.debug("updating target {}", existingTarget.getId());
        if (targetRest.getDescription() != null) {
            existingTarget.setDescription(targetRest.getDescription());
        }
        if (targetRest.getName() != null) {
            existingTarget.setName(targetRest.getName());
        }
        final Target updateTarget = targetManagement.updateTarget(existingTarget);

        return new ResponseEntity<>(TargetMapper.toResponse(updateTarget), HttpStatus.OK);
    }

    /**
     * Handles the DELETE request of deleting a target within SP.
     *
     * @param targetId
     *            the ID of the target to be deleted
     * @return If the given targetId could exists and could be deleted Http OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{targetId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> deleteTarget(@PathVariable final String targetId) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);
        targetManagement.deleteTargets(target.getId());
        LOG.debug("{} target deleted, return status {}", targetId, HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving the attributes of a specific
     * target.
     *
     * @param targetId
     *            the ID of the target to retrieve the attributes.
     * @return the target attributes as map response with status OK
     * @throws EntityNotFoundException
     *             in case no target with the given {@code targetId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetId}/attributes", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TargetAttributes> getAttributes(@PathVariable final String targetId) {
        final Target foundTarget = findTargetWithExceptionIfNotFound(targetId);
        final Map<String, String> controllerAttributes = foundTarget.getTargetInfo().getControllerAttributes();
        if (controllerAttributes.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        final TargetAttributes result = new TargetAttributes();
        result.putAll(controllerAttributes);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving the {@link Action}s of a specific
     * target.
     *
     * @param targetId
     *            to load actions for
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=status==pending}
     * @return a list of all {@link Action}s for a defined or default page
     *         request with status OK. The response is always paged. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetId}/actions", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ActionPagedList> getActionHistory(@PathVariable final String targetId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final Target foundTarget = findTargetWithExceptionIfNotFound(targetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> activeActions;
        final Long totalActionCount;
        if (rsqlParam != null) {
            final Specification<Action> parse = RSQLUtility.parse(rsqlParam, ActionFields.class);
            activeActions = deploymentManagement.findActionsByTarget(parse, foundTarget, pageable);
            totalActionCount = deploymentManagement.countActionsByTarget(parse, foundTarget);
        } else {
            activeActions = deploymentManagement.findActionsByTarget(foundTarget, pageable);
            totalActionCount = deploymentManagement.countActionsByTarget(foundTarget);
        }

        return new ResponseEntity<>(
                new ActionPagedList(TargetMapper.toResponse(targetId, activeActions.getContent()), totalActionCount),
                HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving a specific {@link Action}s of a
     * specific {@link Target}.
     *
     * @param targetId
     *            to load the action for
     * @param actionId
     *            to load
     * @return the action
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetId}/actions/{actionId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ActionRest> getAction(@PathVariable final String targetId,
            @PathVariable final Long actionId) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn("given action ({}) is not assigned to given target ({}).", action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final ActionRest result = TargetMapper.toResponse(targetId, action, action.isActive());

        if (!action.isCancelingOrCanceled()) {
            result.add(linkTo(
                    methodOn(DistributionSetResource.class).getDistributionSet(action.getDistributionSet().getId()))
                            .withRel("distributionset"));
        } else if (action.isCancelingOrCanceled()) {
            result.add(linkTo(methodOn(TargetResource.class).getAction(targetId, action.getId()))
                    .withRel(RestConstants.TARGET_V1_CANCELED_ACTION));
        }

        result.add(linkTo(methodOn(TargetResource.class).getActionStatusList(targetId, action.getId(), 0,
                RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionStatusFields.ID.getFieldName() + ":" + SortDirection.DESC))
                        .withRel(RestConstants.TARGET_V1_ACTION_STATUS));

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Handles the DELETE request of canceling an specific {@link Action}s of a
     * specific {@link Target}.
     *
     * @param targetId
     *            the ID of the target in the URL path parameter
     * @param actionId
     *            the ID of the action in the URL path parameter
     * @param force
     *            optional parameter, which indicates a force cancel
     * @return status no content in case cancellation was successful
     * @throws CancelActionNotAllowedException
     *             if the given action is not active and cannot be canceled.
     * @throws EntityNotFoundException
     *             if the target or the action is not found
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{targetId}/actions/{actionId}")
    public ResponseEntity<Void> cancelAction(@PathVariable final String targetId, @PathVariable final Long actionId,
            @RequestParam(required = false, defaultValue = "false") final boolean force) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);
        final Action action = findActionWithExceptionIfNotFound(actionId);

        if (force) {
            deploymentManagement.forceQuitAction(action, target);
        } else {
            deploymentManagement.cancelAction(action, target);
        }
        // both functions will throw an exception, when action is in wrong
        // state, which is mapped by ResponseExceptionHandler.

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Handles the GET request of retrieving the {@link ActionStatus}s of a
     * specific target and action.
     *
     * @param targetId
     *            of the the action
     * @param actionId
     *            of the status we are intend to load
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @return a list of all {@link ActionStatus}s for a defined or default page
     *         request with status OK. The response is always paged. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetId}/actions/{actionId}/status", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ActionStatusPagedList> getActionStatusList(@PathVariable final String targetId,
            @PathVariable final Long actionId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam) {

        final Target target = findTargetWithExceptionIfNotFound(targetId);

        final Action action = findActionWithExceptionIfNotFound(actionId);
        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn("given action ({}) is not assigned to given target ({}).", action.getId(), target.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionStatusSortParam(sortParam);

        final Page<ActionStatus> statusList = deploymentManagement.findActionStatusMessagesByActionInDescOrder(
                new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting), action, true);

        return new ResponseEntity<>(
                new ActionStatusPagedList(TargetMapper.toActionStatusRestResponse(action, statusList.getContent()),
                        statusList.getTotalElements()),
                HttpStatus.OK);

    }

    /**
     * Handles the GET request of retrieving the assigned distribution set of an
     * specific target.
     *
     * @param targetId
     *            the ID of the target to retrieve the assigned distribution
     * @return the assigned distribution set with status OK, if none is assigned
     *         than {@code null} content (e.g. "{}")
     * @throws EntityNotFoundException
     *             in case no target with the given {@code targetId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetId}/assignedDS", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DistributionSetRest> getAssignedDistributionSet(@PathVariable final String targetId) {
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

    /**
     * Changes the assigned distribution set of a target.
     *
     * @param targetId
     *            of the target to change
     * @param dsId
     *            of the distributionset that is to be assigned
     * @return {@link HttpStatus#OK}
     *
     * @throws EntityNotFoundException
     *             in case no target with the given {@code targetId} exists.
     *
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{targetId}/assignedDS", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> postAssignedDistributionSet(@PathVariable final String targetId,
            @RequestBody final DistributionSetAssigmentRest dsId) {

        findTargetWithExceptionIfNotFound(targetId);
        final ActionType type = (dsId.getType() != null)
                ? RestResourceConversionHelper.convertActionType(dsId.getType()) : ActionType.FORCED;
        final Iterator<Target> changed = deploymentManagement
                .assignDistributionSet(dsId.getId(), type, dsId.getForcetime(), targetId).getAssignedTargets()
                .iterator();
        if (changed.hasNext()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        LOG.error("Target update (ds {} assigment to target {}) failed! Returnd target list is empty.", dsId.getId(),
                targetId);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

    }

    /**
     * Handles the GET request of retrieving the installed distribution set of
     * an specific target.
     *
     * @param targetId
     *            the ID of the target to retrieve
     * @return the assigned installed set with status OK, if none is installed
     *         than {@code null} content (e.g. "{}")
     * @throws EntityNotFoundException
     *             in case no target with the given {@code targetId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetId}/installedDS", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DistributionSetRest> getInstalledDistributionSet(@PathVariable final String targetId) {
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
        final Target findTarget = targetManagement.findTargetByControllerID(targetId);
        if (findTarget == null) {
            throw new EntityNotFoundException("Target with Id {" + targetId + "} does not exist");
        }
        return findTarget;
    }

    private Action findActionWithExceptionIfNotFound(final Long actionId) {
        final Action findAction = deploymentManagement.findAction(actionId);
        if (findAction == null) {
            throw new EntityNotFoundException("Action with Id {" + actionId + "} does not exist");
        }
        return findAction;
    }
}
