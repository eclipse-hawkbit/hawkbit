/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import java.util.List;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
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
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutPagedList;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutResponseBody;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutRestRequestBody;
import org.eclipse.hawkbit.rest.resource.model.rolloutgroup.RolloutGroupPagedList;
import org.eclipse.hawkbit.rest.resource.model.rolloutgroup.RolloutGroupResponseBody;
import org.eclipse.hawkbit.rest.resource.model.target.TargetPagedList;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * REST Resource handling rollout CRUD operations.
 *
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping(RestConstants.ROLLOUT_V1_REQUEST_MAPPING)
public class RolloutResource {

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private TargetFilterQueryManagement targetFilterQueryManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private EntityManager entityManager;

    /**
     * Handles the GET request of retrieving all rollouts.
     *
     * @param pagingOffsetParam
     *            the offset of list of rollouts for pagination, might not be
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
     * @return a list of all rollouts for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<RolloutPagedList> getRollouts(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<Rollout> findModulesAll;
        if (rsqlParam != null) {
            findModulesAll = rolloutManagement.findAllByPredicate(
                    RSQLUtility.parse(rsqlParam, RolloutFields.class, entityManager), pageable);
        } else {
            findModulesAll = rolloutManagement.findAll(pageable);
        }

        final List<RolloutResponseBody> rest = RolloutMapper.toResponseRollout(findModulesAll.getContent());
        return new ResponseEntity<>(new RolloutPagedList(rest, findModulesAll.getTotalElements()), HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving a single rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to retrieve
     * @return a single rollout with status OK.
     * @throws EntityNotFoundException
     *             in case no rollout with the given {@code rolloutId} exists.
     */
    @RequestMapping(value = "/{rolloutId}", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE,
            "application/hal+json" })
    public ResponseEntity<RolloutResponseBody> getRollout(@PathVariable("rolloutId") final Long rolloutId) {
        final Rollout findRolloutById = findRolloutOrThrowException(rolloutId);
        return new ResponseEntity<>(RolloutMapper.toResponseRollout(findRolloutById), HttpStatus.OK);
    }

    /**
     * Handles the POST request for creating rollout.
     *
     * @param rollout
     *            the rollout body to be created.
     * @return In case rollout could successful created the ResponseEntity with
     *         status code 201 with the successfully created rollout. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE }, produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<RolloutResponseBody> create(@RequestBody final RolloutRestRequestBody rolloutRequestBody) {

        // first check the given RSQL query if it's well formed, otherwise and
        // exception is thrown
        RSQLUtility.parse(rolloutRequestBody.getTargetFilterQuery(), TargetFields.class, entityManager);

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
            successCondition = RolloutMapper
                    .mapFinishCondition(rolloutRequestBody.getSuccessCondition().getCondition());
            successConditionExpr = rolloutRequestBody.getSuccessCondition().getExpression();
        }
        if (rolloutRequestBody.getSuccessAction() != null) {
            successAction = RolloutMapper.map(rolloutRequestBody.getSuccessAction().getAction());
            successActionExpr = rolloutRequestBody.getSuccessAction().getExpression();
        }
        if (rolloutRequestBody.getErrorCondition() != null) {
            errorCondition = RolloutMapper.mapErrorCondition(rolloutRequestBody.getErrorCondition().getCondition());
            errorConditionExpr = rolloutRequestBody.getErrorCondition().getExpression();
        }
        if (rolloutRequestBody.getErrorAction() != null) {
            errorAction = RolloutMapper.map(rolloutRequestBody.getErrorAction().getAction());
            errorActionExpr = rolloutRequestBody.getErrorAction().getExpression();
        }

        final RolloutGroupConditions rolloutGroupConditions = new RolloutGroup.RolloutGroupConditionBuilder()
                .successCondition(successCondition, successConditionExpr)
                .successAction(successAction, successActionExpr).errorCondition(errorCondition, errorConditionExpr)
                .errorAction(errorAction, errorActionExpr).build();
        final Rollout rollout = rolloutManagement.createRollout(
                RolloutMapper.fromRequest(rolloutRequestBody, distributionSet,
                        rolloutRequestBody.getTargetFilterQuery()), rolloutRequestBody.getAmountGroups(),
                rolloutGroupConditions);
        return ResponseEntity.status(HttpStatus.CREATED).body(RolloutMapper.toResponseRollout(rollout));
    }

    /**
     * Handles the POST request for starting a rollout.
     * 
     * @param rolloutId
     *            the ID of the rollout to be started.
     * @return OK response (200) if rollout could be started. In case of any
     *         exception the corresponding errors occur.
     * @throws EntityNotFoundException
     * @see RolloutManagement#startRollout(Rollout)
     * @see ResponseExceptionHandler
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{rolloutId}/start", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> start(@PathVariable("rolloutId") final Long rolloutId) {
        final Rollout rollout = findRolloutOrThrowException(rolloutId);
        rolloutManagement.startRollout(rollout);
        return ResponseEntity.ok().build();
    }

    /**
     * Handles the POST request for pausing a rollout.
     * 
     * @param rolloutId
     *            the ID of the rollout to be paused.
     * @return OK response (200) if rollout could be paused. In case of any
     *         exception the corresponding errors occur.
     * @throws EntityNotFoundException
     * @see RolloutManagement#pauseRollout(Rollout)
     * @see ResponseExceptionHandler
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{rolloutId}/pause", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> pause(@PathVariable("rolloutId") final Long rolloutId) {
        final Rollout rollout = findRolloutOrThrowException(rolloutId);
        rolloutManagement.pauseRollout(rollout);
        return ResponseEntity.ok().build();
    }

    /**
     * Handles the POST request for resuming a rollout.
     * 
     * @param rolloutId
     *            the ID of the rollout to be resumed.
     * @return OK response (200) if rollout could be resumed. In case of any
     *         exception the corresponding errors occur.
     * @throws EntityNotFoundException
     * @see RolloutManagement#resumeRollout(Rollout)
     * @see ResponseExceptionHandler
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{rolloutId}/resume", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> resume(@PathVariable("rolloutId") final Long rolloutId) {
        final Rollout rollout = findRolloutOrThrowException(rolloutId);
        rolloutManagement.resumeRollout(rollout);
        return ResponseEntity.ok().build();
    }

    /**
     * Handles the GET request of retrieving all rollout groups referred to a
     * rollout.
     *
     * @param pagingOffsetParam
     *            the offset of list of rollout groups for pagination, might not
     *            be present in the rest request then default value will be
     *            applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return a list of all rollout groups referred to a rollout for a defined
     *         or default page request with status OK. The response is always
     *         paged. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{rolloutId}/deploygroups", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<RolloutGroupPagedList> getRolloutGroups(
            @PathVariable("rolloutId") final Long rolloutId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        findRolloutOrThrowException(rolloutId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutGroupSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<RolloutGroup> findRolloutGroupsAll;
        if (rsqlParam != null) {
            findRolloutGroupsAll = rolloutManagement.findRolloutGroupsByPredicate(rolloutId,
                    RSQLUtility.parse(rsqlParam, RolloutGroupFields.class, entityManager), pageable);
        } else {
            findRolloutGroupsAll = rolloutManagement.findRolloutGroupsByRollout(rolloutId, pageable);
        }

        final List<RolloutGroupResponseBody> rest = RolloutMapper.toResponseRolloutGroup(findRolloutGroupsAll
                .getContent());
        return new ResponseEntity<>(new RolloutGroupPagedList(rest, findRolloutGroupsAll.getTotalElements()),
                HttpStatus.OK);
    }

    /**
     * Handles the GET request for retrieving a single rollout group.
     * 
     * @param rolloutId
     *            the rolloutId to retrieve the group from
     * @param groupId
     *            the groupId to retrieve the rollout group
     * @return the OK response containing the {@link RolloutGroupResponseBody}
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{rolloutId}/deploygroups/{groupId}", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<RolloutGroupResponseBody> getRolloutGroup(@PathVariable("rolloutId") final Long rolloutId,
            @PathVariable("groupId") final Long groupId) {
        findRolloutOrThrowException(rolloutId);
        final RolloutGroup rolloutGroup = findRolloutGroupOrThrowException(groupId);
        return ResponseEntity.ok(RolloutMapper.toResponseRolloutGroup(rolloutGroup));
    }

    /**
     * Retrieves all targets related to a specific rollout group.
     * 
     * @param rolloutId
     *            the ID of the rollout
     * @param groupId
     *            the ID of the rollout group
     * @param pagingOffsetParam
     *            the offset of list of rollout groups for pagination, might not
     *            be present in the rest request then default value will be
     *            applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return a paged list of targets related to a specific rollout and rollout
     *         group.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{rolloutId}/deploygroups/{groupId}/targets", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    public ResponseEntity<TargetPagedList> getRolloutGroupTargets(
            @PathVariable("rolloutId") final Long rolloutId,
            @PathVariable("groupId") final Long groupId,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        findRolloutOrThrowException(rolloutId);
        final RolloutGroup rolloutGroup = findRolloutGroupOrThrowException(groupId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<Target> rolloutGroupTargets;
        if (rsqlParam != null) {
            final Specification<Target> rsqlSpecification = RSQLUtility.parse(rsqlParam, TargetFields.class,
                    entityManager);
            rolloutGroupTargets = rolloutManagement.findRolloutGroupTargets(rolloutGroup, rsqlSpecification, pageable);
        } else {
            final Page<Target> pageTargets = rolloutManagement.getRolloutGroupTargets(rolloutGroup, pageable);
            rolloutGroupTargets = pageTargets;
        }
        final List<TargetRest> rest = TargetMapper.toResponse(rolloutGroupTargets.getContent());
        return new ResponseEntity<>(new TargetPagedList(rest, rolloutGroupTargets.getTotalElements()), HttpStatus.OK);
    }

    private Rollout findRolloutOrThrowException(final Long rolloutId) {
        final Rollout rollout = rolloutManagement.findRolloutById(rolloutId);
        if (rollout == null) {
            throw new EntityNotFoundException("Rollout with Id {" + rolloutId + "} does not exist");
        }
        return rollout;
    }

    private RolloutGroup findRolloutGroupOrThrowException(final Long rolloutGroupId) {
        final RolloutGroup rolloutGroup = rolloutManagement.findRolloutGroupById(rolloutGroupId);
        if (rolloutGroup == null) {
            throw new EntityNotFoundException("Group with Id {" + rolloutGroupId + "} does not exist");
        }
        return rolloutGroup;
    }

    private DistributionSet findDistributionSetOrThrowException(final RolloutRestRequestBody rolloutRequestBody) {
        final DistributionSet ds = distributionSetManagement.findDistributionSetById(rolloutRequestBody
                .getDistributionSetId());
        if (ds == null) {
            throw new EntityNotFoundException("DistributionSet with Id {" + rolloutRequestBody.getDistributionSetId()
                    + "} does not exist");
        }
        return ds;
    }
}
