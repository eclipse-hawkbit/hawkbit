/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling rollout CRUD operations.
 *
 */
@RequestMapping(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING)
public interface MgmtRolloutRestApi {

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
    ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request of retrieving a single rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to retrieve
     * @return a single rollout with status OK.
     */
    @RequestMapping(value = "/{rolloutId}", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE,
            "application/hal+json" })
    ResponseEntity<MgmtRolloutResponseBody> getRollout(@PathVariable("rolloutId") final Long rolloutId);

    /**
     * Handles the POST request for creating rollout.
     *
     * @param rollout
     *            the rollout body to be created.
     * @return In case rollout could successful created the ResponseEntity with
     *         status code 201 with the successfully created rollout. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutResponseBody> create(@RequestBody final MgmtRolloutRestRequestBody rolloutRequestBody);

    /**
     * Handles the POST request for starting a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be started.
     * @return OK response (200) if rollout could be started. In case of any
     *         exception the corresponding errors occur.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{rolloutId}/start", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> start(@PathVariable("rolloutId") final Long rolloutId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_ASYNC, defaultValue = "false") final boolean startAsync);

    /**
     * Handles the POST request for pausing a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be paused.
     * @return OK response (200) if rollout could be paused. In case of any
     *         exception the corresponding errors occur.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{rolloutId}/pause", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> pause(@PathVariable("rolloutId") final Long rolloutId);

    /**
     * Handles the POST request for resuming a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be resumed.
     * @return OK response (200) if rollout could be resumed. In case of any
     *         exception the corresponding errors occur.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{rolloutId}/resume", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> resume(@PathVariable("rolloutId") final Long rolloutId);

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
    ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(
            @PathVariable("rolloutId") final Long rolloutId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request for retrieving a single rollout group.
     *
     * @param rolloutId
     *            the rolloutId to retrieve the group from
     * @param groupId
     *            the groupId to retrieve the rollout group
     * @return the OK response containing the MgmtRolloutGroupResponseBody
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{rolloutId}/deploygroups/{groupId}", produces = {
            MediaType.APPLICATION_JSON_VALUE, "application/hal+json" })
    ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(@PathVariable("rolloutId") final Long rolloutId,
            @PathVariable("groupId") final Long groupId);

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
    ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(@PathVariable("rolloutId") final Long rolloutId,
            @PathVariable("groupId") final Long groupId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);
}
