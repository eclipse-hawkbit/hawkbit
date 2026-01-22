/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.ROLLOUT_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.NOT_FOUND_404;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateNoContentResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostUpdateNoContentResponses;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling rollout CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(
        name = "Rollouts", description = "REST API for Rollout CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = ROLLOUT_ORDER)))
public interface MgmtRolloutRestApi {

    /**
     * Handles the GET request of retrieving all rollouts.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of rollouts for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @param representationModeParam the representation mode parameter specifying whether a compact or a full representation shall be returned
     * @return a list of all rollouts for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Rollouts", description = "Handles the GET request of retrieving all rollouts. " +
            "Required Permission: READ_ROLLOUT")
    @GetIfExistResponses
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam,
            @RequestParam(
                    value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            @Schema(description = "The paging offset (default is 0)")
            int pagingOffsetParam,
            @RequestParam(
                    value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            @Schema(description = "The maximum number of entries in a page (default is 50)")
            int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false)
            @Schema(description = """
                    The query parameter sort allows to define the sort order for the result of a query. A sort criteria
                    consists of the name of a field and the sort direction (ASC for ascending and DESC descending).
                    The sequence of the sort criteria (multiple can be used) defines the sort order of the entities
                    in the result.""")
            String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT)
            String representationModeParam);

    /**
     * Handles the GET request of retrieving a single rollout.
     *
     * @param rolloutId the ID of the rollout to retrieve
     * @return a single rollout with status OK.
     */
    @Operation(summary = "Return single Rollout", description = "Handles the GET request of retrieving a single " +
            "rollout. Required Permission: READ_ROLLOUT")
    @GetResponses
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutResponseBody> getRollout(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for creating rollout.
     *
     * @param rolloutRequestBody the rollout body to be created.
     * @return In case rollout could successful created the ResponseEntity with status code 201 with the successfully created rollout. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Create a new Rollout",
            description = "Handles the POST request of creating new rollout. Required Permission: CREATE_ROLLOUT")
    @PostCreateResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutResponseBody> create(
            @RequestBody MgmtRolloutRestRequestBodyPost rolloutRequestBody);

    /**
     * Handles the POST request for creating rollout.
     *
     * @param rolloutUpdateBody the rollout body with details for update.
     * @return In case rollout could successful updated the ResponseEntity with status code 200 with the successfully created rollout. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Update Rollout", description = "Handles the UPDATE request for a single " +
            "Rollout. Required permission: UPDATE_ROLLOUT")
    @PutResponses
    @PutMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}",
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
    ResponseEntity<MgmtRolloutResponseBody> update(
            @PathVariable("rolloutId") Long rolloutId,
            @RequestBody MgmtRolloutRestRequestBodyPut rolloutUpdateBody);

    /**
     * Handles the request for approving a rollout.
     *
     * @param rolloutId the ID of the rollout to be approved.
     * @param remark an optional remark on the approval decision
     * @return OK response (200) if rollout is approved now. In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Approve a Rollout",
            description = "Handles the POST request of approving a created rollout. Only possible if approval " +
                    "workflow is enabled in system configuration and rollout is in state WAITING_FOR_APPROVAL. " +
                    "Required Permission: APPROVE_ROLLOUT")
    @PostCreateNoContentResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/approve",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> approve(
            @PathVariable("rolloutId") Long rolloutId,
            @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the request for denying the approval of a rollout.
     *
     * @param rolloutId the ID of the rollout to be denied.
     * @param remark an optional remark on the denial decision
     * @return OK response (200) if rollout is denied now. In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Deny a Rollout", description = "Handles the POST request of denying a created rollout. " +
            "Only possible if approval workflow is enabled in system configuration and rollout is in state " +
            "WAITING_FOR_APPROVAL. Required Permission: APPROVE_ROLLOUT")
    @PostCreateNoContentResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deny",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deny(
            @PathVariable("rolloutId") Long rolloutId,
            @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the POST request for starting a rollout.
     *
     * @param rolloutId the ID of the rollout to be started.
     * @return OK response (200) if rollout could be started. In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Start a Rollout", description = "Handles the POST request of starting a created rollout. " +
            "Required Permission: HANDLE_ROLLOUT")
    @PostCreateNoContentResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/start",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> start(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for pausing a rollout.
     *
     * @param rolloutId the ID of the rollout to be paused.
     * @return OK response (200) if rollout could be paused. In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Pause a Rollout", description = "Handles the POST request of pausing a running rollout. " +
            "Required Permission: HANDLE_ROLLOUT")
    @PostCreateNoContentResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/pause",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> pause(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for stopping a rollout.
     *
     * @param rolloutId the ID of the rollout to be paused.
     * @return OK response (200) if rollout could be stopped. In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Stop a Rollout", description = "Handles the POST request of stopping a running rollout. " +
            "Required Permission: HANDLE_ROLLOUT")
    @PostUpdateNoContentResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/stop",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> stop(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the DELETE request for deleting a rollout.
     *
     * @param rolloutId the ID of the rollout to be deleted.
     * @return OK response (200) if rollout could be deleted. In case of any
     *         exception the corresponding errors occur.
     */
    @Operation(summary = "Delete a Rollout", description = "Handles the DELETE request of deleting a rollout. " +
            "Required Permission: DELETE_ROLLOUT")
    @DeleteResponses
    @DeleteMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> delete(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for resuming a rollout.
     *
     * @param rolloutId the ID of the rollout to be resumed.
     * @return OK response (200) if rollout could be resumed. In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Resume a Rollout", description = "Handles the POST request of resuming a paused rollout. " +
            "Required Permission: HANDLE_ROLLOUT")
    @PostCreateNoContentResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/resume",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> resume(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the GET request of retrieving all rollout groups referred to a rollout.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of rollout groups for pagination, might not be present in the rest request then default value
     *         will be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @param representationModeParam the representation mode parameter specifying whether a compact or a full representation shall be returned
     * @return a list of all rollout groups referred to a rollout for a defined or default page request with status OK. The response is always
     *         paged. In any failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all rollout groups referred to a Rollout", description = "Handles the GET request of " +
            "retrieving all deploy groups of a specific rollout. Required Permission: READ_ROLLOUT")
    @GetResponses
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(
            @PathVariable("rolloutId") Long rolloutId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam,
            @RequestParam(
                    value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            @Schema(description = "The paging offset (default is 0)")
            int pagingOffsetParam,
            @RequestParam(
                    value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            @Schema(description = "The maximum number of entries in a page (default is 50)")
            int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false)
            @Schema(description = """
                    The query parameter sort allows to define the sort order for the result of a query. A sort criteria
                    consists of the name of a field and the sort direction (ASC for ascending and DESC descending).
                    The sequence of the sort criteria (multiple can be used) defines the sort order of the entities
                    in the result.""")
            String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT)
            String representationModeParam);

    /**
     * Handles the GET request for retrieving a single rollout group.
     *
     * @param rolloutId the rolloutId to retrieve the group from
     * @param groupId the groupId to retrieve the rollout group
     * @return the OK response containing the MgmtRolloutGroupResponseBody
     */
    @Operation(summary = "Return single rollout group", description = "Handles the GET request of a single deploy " +
            "group of a specific rollout. Required Permission: READ_ROLLOUT")
    @GetResponses
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups/{groupId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(
            @PathVariable("rolloutId") Long rolloutId,
            @PathVariable("groupId") Long groupId);

    /**
     * Retrieves all targets related to a specific rollout group.
     *
     * @param rolloutId the ID of the rollout
     * @param groupId the ID of the rollout group
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of rollout groups for pagination, might not be present in the rest request then default value
     *         will be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a paged list of targets related to a specific rollout and rollout group.
     */
    @Operation(summary = "Return all targets related to a specific rollout group",
            description = "Handles the GET request of retrieving all targets of a single deploy group of a specific " +
                    "rollout. Required Permissions: READ_ROLLOUT, READ_TARGET.")
    @GetResponses
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/deploygroups/{groupId}/targets",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(
            @PathVariable("rolloutId") Long rolloutId,
            @PathVariable("groupId") Long groupId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam,
            @RequestParam(
                    value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            @Schema(description = "The paging offset (default is 0)")
            int pagingOffsetParam,
            @RequestParam(
                    value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                    defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            @Schema(description = "The maximum number of entries in a page (default is 50)")
            int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false)
            @Schema(description = """
                    The query parameter sort allows to define the sort order for the result of a query. A sort criteria
                    consists of the name of a field and the sort direction (ASC for ascending and DESC descending).
                    The sequence of the sort criteria (multiple can be used) defines the sort order of the entities
                    in the result.""")
            String sortParam);

    /**
     * Handles the POST request to force trigger processing next group of a rollout even success threshold isn't yet met
     *
     * @param rolloutId the ID of the rollout to trigger next group.
     * @return OK response (200). In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Force trigger processing next group of a Rollout", description = "Handles the POST request " +
            "of triggering the next group of a rollout. Required Permission: UPDATE_ROLLOUT")
    @PostCreateNoContentResponses
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/triggerNextGroup",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> triggerNextGroup(@PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request to retry a rollout
     *
     * @param rolloutId the ID of the rollout to be retried.
     * @return OK response (200). In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Retry a rollout", description = "Handles the POST request of retrying a rollout. " +
            "Required Permission: CREATE_ROLLOUT")
    @PostCreateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = NOT_FOUND_404, description = "Rollout not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING + "/{rolloutId}/retry",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtRolloutResponseBody> retryRollout(@PathVariable("rolloutId") Long rolloutId);
}