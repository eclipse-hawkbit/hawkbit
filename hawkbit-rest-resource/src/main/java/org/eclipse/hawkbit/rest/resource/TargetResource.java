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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.im.authentication.SpPermission;
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
import org.eclipse.hawkbit.rest.resource.model.ExceptionInfo;
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
import org.springframework.transaction.annotation.Transactional;
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
@Transactional(readOnly = true)
@RequestMapping(RestConstants.TARGET_V1_REQUEST_MAPPING)
@Api(value = "targets", description = "Provisioning Target Management API")
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
    @ApiOperation(response = TargetRest.class, value = "Get single Target", notes = "Handles the GET request of retrieving a single target within SP. Required Permission: "
            + SpPermission.READ_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
    public ResponseEntity<TargetRest> getTarget(@PathVariable final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        // to single response include poll status
        final TargetRest response = TargetMapper.toResponse(findTarget, true);
        // add links
        response.add(linkTo(methodOn(TargetResource.class).getAssignedDistributionSet(response.getControllerId()))
                .withRel(RestConstants.TARGET_V1_ASSIGNED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(TargetResource.class).getInstalledDistributionSet(response.getControllerId()))
                .withRel(RestConstants.TARGET_V1_INSTALLED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(TargetResource.class).getAttributes(response.getControllerId())).withRel(
                RestConstants.TARGET_V1_ATTRIBUTES));
        response.add(linkTo(
                methodOn(TargetResource.class).getActionHistory(response.getControllerId(), 0,
                        RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                        ActionFields.ID.getFieldName() + ":" + SortDirection.DESC, null)).withRel(
                RestConstants.TARGET_V1_ACTIONS));

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
    @ApiOperation(response = TargetPagedList.class, value = "Get paged list of Targets", notes = "Handles the GET request of retrieving all targets within SP. Required Permission: "
            + SpPermission.READ_TARGET)
    public ResponseEntity<TargetPagedList> getTargets(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @ApiParam(required = false, value = "FIQL syntax search query"
                    + "<table border=0>"
                    + "<tr><td>updatestatus==unknown,updatestatus==error</td><td>targets with status 'unknown' or 'error'</td></tr>"
                    + "<tr><td>installedAt=ge=1424699220</td><td>targets which installation date later than 1424699220</td></tr>"
                    + "<tr><td>name=li=%25ccu%25</td><td>targets which contain 'ccu' in their name ingore case</td></tr>"
                    + "<tr><td>controllerId==target0815</td><td>target which has the ID 'target0815'</td></tr>"
                    + "<tr><td>ipaddress=li=171.%25</td><td>target which starts with IP address '171.'</td></tr>"
                    + "</table>") @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<Target> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam != null) {
            final Page<Target> findTargetPage = targetManagement.findTargetsAll(
                    RSQLUtility.parse(rsqlParam, TargetFields.class, entityManager), pageable);
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
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE }, produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = TargetsRest.class, value = "Create list of Targets", notes = "Handles the POST request of creating new targets within SP. The request body must always be a list of targets.  Required Permission: "
            + SpPermission.CREATE_TARGET)
    @ApiResponses(@ApiResponse(code = 409, message = "Conflict", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<TargetsRest> createTargets(
            @RequestBody @ApiParam(value = "List of targets", required = true) final List<TargetRequestBody> targets) {
        LOG.debug("creating {} targets", targets.size());
        final Iterable<Target> createdTargets = targetManagement.createTargets(TargetMapper.fromRequest(targets));
        LOG.debug("{} targets created, return status {}", targets.size(), HttpStatus.CREATED);

        // we flush to ensure that entity is generated and we can return ID etc.
        entityManager.flush();

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
    @ApiOperation(response = TargetRest.class, value = "Updates a target", notes = "Handles the PUT request of updating a target within SP.  Required Permission: "
            + SpPermission.UPDATE_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<TargetRest> updateTarget(@PathVariable final String targetId,
            @RequestBody @ApiParam(value = "Target to be updated", required = true) final TargetRequestBody targetRest) {
        final Target existingTarget = findTargetWithExceptionIfNotFound(targetId);
        LOG.debug("updating target {}", existingTarget.getId());
        if (targetRest.getDescription() != null) {
            existingTarget.setDescription(targetRest.getDescription());
        }
        if (targetRest.getName() != null) {
            existingTarget.setName(targetRest.getName());
        }
        final Target updateTarget = targetManagement.updateTarget(existingTarget);
        LOG.debug("target {} updated, return status {}", updateTarget.getId(), HttpStatus.OK);

        // we flush to ensure that entity is generated and we can return ID etc.
        entityManager.flush();

        return new ResponseEntity<>(TargetMapper.toResponse(updateTarget, false), HttpStatus.OK);
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
    @ApiOperation(value = "Deletes a target", notes = "Handles the DELETE request of deleting a single target within SP. Required Permission: "
            + SpPermission.DELETE_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
    @Transactional
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
    @ApiOperation(response = TargetAttributes.class, value = "Get attributes of Target", notes = "Handles the GET request of retrieving the attributes of a specific target. Reponse is a key/value list. Required Permission: "
            + SpPermission.READ_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
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
    @ApiOperation(response = ActionPagedList.class, value = "List all actions of Target", notes = "Handles the GET request of retrieving the full action history of a specific target. Required Permission: "
            + SpPermission.READ_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
    public ResponseEntity<ActionPagedList> getActionHistory(
            @PathVariable final String targetId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @ApiParam(required = false, value = "FIQL syntax search query" + "<table border=0>"
                    + "<tr><td>status==finished</td><td>actions which are already 'finished'</td></tr>"
                    + "<tr><td>status==pending</td><td>actions which are currently in 'pending' state</td></tr>"
                    + "</table>") @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final Target foundTarget = findTargetWithExceptionIfNotFound(targetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> activeActions;
        final Long totalActionCount;
        if (rsqlParam != null) {
            final Specification<Action> parse = RSQLUtility.parse(rsqlParam, ActionFields.class, entityManager);
            activeActions = deploymentManagement.findActionsByTarget(parse, foundTarget, pageable);
            totalActionCount = deploymentManagement.countActionsByTarget(parse, foundTarget);
        } else {
            activeActions = deploymentManagement.findActionsByTarget(foundTarget, pageable);
            totalActionCount = deploymentManagement.countActionsByTarget(foundTarget);
        }

        return new ResponseEntity<ActionPagedList>(new ActionPagedList(TargetMapper.toResponse(targetId,
                activeActions.getContent()), totalActionCount), HttpStatus.OK);
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
    @ApiOperation(response = ActionRest.class, value = "Get assigned action of Target", notes = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: "
            + SpPermission.READ_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target or Action", response = ExceptionInfo.class))
    public ResponseEntity<ActionRest> getAction(@PathVariable final String targetId, @PathVariable final Long actionId) {
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
            result.add(linkTo(methodOn(TargetResource.class).getAction(targetId, action.getId())).withRel(
                    RestConstants.TARGET_V1_CANCELED_ACTION));
        }

        result.add(linkTo(
                methodOn(TargetResource.class).getActionStatusList(targetId, action.getId(), 0,
                        RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                        ActionStatusFields.ID.getFieldName() + ":" + SortDirection.DESC)).withRel(
                RestConstants.TARGET_V1_ACTION_STATUS));

        return new ResponseEntity<ActionRest>(result, HttpStatus.OK);
    }

    /**
     * Handles the DELETE request of canceling an specific {@link Action}s of a
     * specific {@link Target}.
     * 
     * @param targetId
     *            the ID of the target in the URL path parameter
     * @param actionId
     *            the ID of the action in the URL path parameter
     * @return status no content in case cancellation was successful
     * @throws CancelActionNotAllowedException
     *             if the given action is not active and cannot be canceled.
     * @throws EntityNotFoundException
     *             if the target or the action is not found
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{targetId}/actions/{actionId}")
    @ApiOperation(value = "Cancels an active action", notes = "Cancels an active action, only active actions can be deleted. Required Permission: "
            + SpPermission.UPDATE_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target or Action", response = ExceptionInfo.class))
    public ResponseEntity<Void> cancelAction(@PathVariable final String targetId, @PathVariable final Long actionId) {
        final Target target = findTargetWithExceptionIfNotFound(targetId);
        final Action action = findActionWithExceptionIfNotFound(actionId);

        // cancel action will throw an exception if action cannot be canceled
        // which is mapped by
        // response exception handler.
        deploymentManagement.cancelAction(action, target);

        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);

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
    @ApiOperation(response = ActionStatusPagedList.class, value = "Get assigned action of Target", notes = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: "
            + SpPermission.READ_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target or Action", response = ExceptionInfo.class))
    public ResponseEntity<ActionStatusPagedList> getActionStatusList(
            @PathVariable final String targetId,
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

        return new ResponseEntity<>(new ActionStatusPagedList(TargetMapper.toActionStatusRestResponse(action,
                statusList.getContent()), statusList.getTotalElements()), HttpStatus.OK);

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
    @ApiOperation(response = DistributionSetRest.class, value = "Get assigned Distribution Set of Target", notes = "Handles the GET request of retrieving the assigned distribution set of an specific target. Required Permission: "
            + SpPermission.READ_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
    public ResponseEntity<DistributionSetRest> getAssignedDistributionSet(@PathVariable final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        final DistributionSetRest distributionSetRest = DistributionSetMapper.toResponse(findTarget
                .getAssignedDistributionSet());
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
    @ApiOperation(response = Void.class, value = "Assigned Distribution Set to Target", notes = "Handles the POST request for assigning a distribution set to a specific target. Required Permission: "
            + SpPermission.READ_REPOSITORY + " and " + SpPermission.UPDATE_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<Void> postAssignedDistributionSet(
            @PathVariable final String targetId,
            @RequestBody @ApiParam(value = "Distribution Set ID", required = true) final DistributionSetAssigmentRest dsId) {

        findTargetWithExceptionIfNotFound(targetId);
        final ActionType type = (dsId.getType() != null) ? RestResourceConversionHelper.convertActionType(dsId
                .getType()) : ActionType.FORCED;
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
    @ApiOperation(response = DistributionSetRest.class, value = "Get installed Distribution Set of Target", notes = "Handles the GET request of retrieving the installed distribution set of an specific target. Required Permission: "
            + SpPermission.READ_TARGET)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Target", response = ExceptionInfo.class))
    public ResponseEntity<DistributionSetRest> getInstalledDistributionSet(@PathVariable final String targetId) {
        final Target findTarget = findTargetWithExceptionIfNotFound(targetId);
        final DistributionSetRest distributionSetRest = DistributionSetMapper.toResponse(findTarget.getTargetInfo()
                .getInstalledDistributionSet());
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
