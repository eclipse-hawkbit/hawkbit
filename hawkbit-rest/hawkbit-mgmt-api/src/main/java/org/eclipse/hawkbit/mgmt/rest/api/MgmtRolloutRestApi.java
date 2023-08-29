/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling rollout CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
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
     * @param representationModeParam
     *            the representation mode parameter specifying whether a compact
     *            or a full representation shall be returned
     * @return a list of all rollouts for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Rollouts", description = "Handles the GET request of retrieving all rollouts. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT) String representationModeParam);

    /**
     * Handles the GET request of retrieving a single rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to retrieve
     * @return a single rollout with status OK.
     */
    @Operation(summary = "Return single Rollout", description = "Handles the GET request of retrieving a single rollout. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Rollout not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutResponseBody> getRollout(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for creating rollout.
     *
     * @param rolloutRequestBody
     *            the rollout body to be created.
     * @return In case rollout could successful created the ResponseEntity with
     *         status code 201 with the successfully created rollout. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Create a new Rollout", description = "Handles the POST request of creating new rollout. Required Permission: CREATE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutResponseBody> create(MgmtRolloutRestRequestBody rolloutRequestBody);

    /**
     * Handles the request for approving a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be approved.
     * @param remark
     *            an optional remark on the approval decision
     * @return OK response (200) if rollout is approved now. In case of any
     *         exception the corresponding errors occur.
     */
    @Operation(summary = "Approve a Rollout", description = "Handles the POST request of approving a created rollout. Only possible if approval workflow is enabled in system configuration and rollout is in state WAITING_FOR_APPROVAL. Required Permission: APPROVE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/approve", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> approve(@PathVariable("rolloutId") Long rolloutId,
            @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the request for denying the approval of a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be denied.
     * @param remark
     *            an optional remark on the denial decision
     * @return OK response (200) if rollout is denied now. In case of any
     *         exception the corresponding errors occur.
     */
    @Operation(summary = "Deny a Rollout", description = "Handles the POST request of denying a created rollout. Only possible if approval workflow is enabled in system configuration and rollout is in state WAITING_FOR_APPROVAL. Required Permission: APPROVE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deny", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deny(@PathVariable("rolloutId") Long rolloutId,
            @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the POST request for starting a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be started.
     * @return OK response (200) if rollout could be started. In case of any
     *         exception the corresponding errors occur.
     */
    @Operation(summary = "Start a Rollout", description = "Handles the POST request of starting a created rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/start", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> start(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for pausing a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be paused.
     * @return OK response (200) if rollout could be paused. In case of any
     *         exception the corresponding errors occur.
     */
    @Operation(summary = "Pause a Rollout", description = "Handles the POST request of pausing a running rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/pause", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> pause(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the DELETE request for deleting a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be deleted.
     * @return OK response (200) if rollout could be deleted. In case of any
     *         exception the corresponding errors occur.
     */
    @Operation(summary = "Delete a Rollout", description = "Handles the DELETE request of deleting a rollout. Required Permission: DELETE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Rollout not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @DeleteMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> delete(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for resuming a rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to be resumed.
     * @return OK response (200) if rollout could be resumed. In case of any
     *         exception the corresponding errors occur.
     */
    @Operation(summary = "Resume a Rollout", description = "Handles the POST request of resuming a paused rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/resume", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> resume(@PathVariable("rolloutId") Long rolloutId);

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
     * @param representationModeParam
     *            the representation mode parameter specifying whether a compact
     *            or a full representation shall be returned
     * 
     * @return a list of all rollout groups referred to a rollout for a defined
     *         or default page request with status OK. The response is always
     *         paged. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @Operation(summary = "Return all rollout groups referred to a Rollout", description = "Handles the GET request of retrieving all deploy groups of a specific rollout. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Rollout not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(@PathVariable("rolloutId") Long rolloutId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT) String representationModeParam);

    /**
     * Handles the GET request for retrieving a single rollout group.
     *
     * @param rolloutId
     *            the rolloutId to retrieve the group from
     * @param groupId
     *            the groupId to retrieve the rollout group
     * @return the OK response containing the MgmtRolloutGroupResponseBody
     */
    @Operation(summary = "Return single rollout group", description = "Handles the GET request of a single deploy group of a specific rollout. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Rollout not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING
            + "/{rolloutId}/deploygroups/{groupId}", produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(@PathVariable("rolloutId") Long rolloutId,
            @PathVariable("groupId") Long groupId);

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
    @Operation(summary = "Return all targets related to a specific rollout group", description = "Handles the GET request of retrieving all targets of a single deploy group of a specific rollout. Required Permissions: READ_ROLLOUT, READ_TARGET.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Rollout not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING
            + "/{rolloutId}/deploygroups/{groupId}/targets", produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(@PathVariable("rolloutId") Long rolloutId,
            @PathVariable("groupId") Long groupId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the POST request to force trigger processing next group of a
     * rollout even success threshold isn't yet met
     *
     * @param rolloutId
     *            the ID of the rollout to trigger next group.
     * @return OK response (200). In case of any exception the corresponding
     *         errors occur.
     */
    @Operation(summary = "Force trigger processing next group of a Rollout", description = "Handles the POST request of triggering the next group of a rollout. Required Permission: UPDATE_ROLLOUT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/triggerNextGroup", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> triggerNextGroup(@PathVariable("rolloutId") Long rolloutId);
}
