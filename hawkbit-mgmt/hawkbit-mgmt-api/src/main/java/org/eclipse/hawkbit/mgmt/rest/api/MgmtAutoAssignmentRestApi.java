/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mgmt.rest.api;

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REST_V1;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.TARGET_FILTER_ORDER;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentRestRequestBodyPut;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateNoContentResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST API for Auto Assignment CRUD operations
 */
@Tag(name = "Auto Assignments", description = "REST API for Auto Assignment CRUD operations",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = TARGET_FILTER_ORDER)))
public interface MgmtAutoAssignmentRestApi {

    String AUTO_ASSIGNMENTS_V1 = REST_V1 + "/autoassignments";

    @Operation(summary = "Return all auto assignments",
            description = "Handles the GET request of retrieving all auto assignments. Required Permission: READ_TARGET")
    @GetIfExistResponses
    @GetMapping(value = AUTO_ASSIGNMENTS_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtAutoAssignmentResponseBody>> getAutoAssignments(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = "Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for available fields.")
            String rsqlParam,
            @RequestParam(value = REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            @Schema(description = "The paging offset (default is 0)")
            int pagingOffsetParam,
            @RequestParam(value = REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            @Schema(description = "The maximum number of entries in a page (default is 50)")
            int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false)
            @Schema(description = "The query parameter sort allows to define the sort order for the result of a query. " +
                    "A sort criteria consists of the name of a field and the sort direction (ASC for ascending and DESC descending)." +
                    "The sequence of the sort criteria (multiple can be used) defines the sort order of the entities in the result.")
            String sortParam,
            @RequestParam(value = REQUEST_PARAMETER_REPRESENTATION_MODE, defaultValue = REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT)
            String representationModeParam
    );

    /**
     * Handles the GET request of retrieving a single auto assignment
     *
     * @param targetFilterQueryId the id of the target filter query, associated with the auto assignment
     * @return a single auto assignment with status OK
     */
    @Operation(summary = "Return a single auto assignment",
            description = "Handles the GET request of retrieving a single auto assignment. Required Permission: READ_TARGET")
    @GetIfExistResponses
    @GetMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAutoAssignmentResponseBody> getAutoAssignment(@PathVariable("targetFilterQueryId") Long targetFilterQueryId);

    /**
     * Handles the POST request for creating an auto assignment
     *
     * @param autoAssignmentRequestBody the auto assignment body to be created
     * @return In case the auto assignment could be successfully created, the {@code ResponseEntity} is returned with status code 201
     * and the newly created auto assignment. In case of a failure the {@code JsonResponseExceptionHandler} is handling the response
     */
    @Operation(summary = "Create a new auto assignment",
            description = "Handles the POST request for creating an auto assignment. Required Permission: UPDATE_TARGET")
    @PostCreateResponses
    @PostMapping(value = AUTO_ASSIGNMENTS_V1,
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAutoAssignmentResponseBody> create(
            @RequestBody MgmtAutoAssignmentRestRequestBodyPost autoAssignmentRequestBody);

    /**
     * Handles the PUT request for updating an auto assignment
     *
     * @param autoAssignmentRequestBody the auto assignment body to be updated
     * @return In case the auto assignment could be successfully updated, the {@code ResponseEntity} is returned with status code 200
     * and the updated auto assignment. In case of a failure the {@code JsonResponseExceptionHandler} is handling the response
     */
    @Operation(summary = "Update an auto assignment",
            description = "Handles the UPDATE request for a single auto assignment. Required Permission: UPDATE_TARGET")
    @PutResponses
    @PutMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAutoAssignmentResponseBody> update(
            @PathVariable ("targetFilterQueryId") Long targetFilterQueryId,
            @RequestBody MgmtAutoAssignmentRestRequestBodyPut autoAssignmentRequestBody);

    /**
     * Handles the request for approving an auto assignment
     *
     * @param targetFilterQueryId the id of the target filter query associated with the auto assignment
     * @param remark an optional remark on the approval decision
     * @return OK response (200) if the auto assignment is approved. In case of any exceptions, the corresponding errors occur
     */
    @Operation(summary = "Approve an auto assignment",
            description = "Handles the request for approving an auto assignment. Only possible if " +
                            "approval workflow is enabled in the system configuration and the auto assignment " +
                            "is in state WAITING_FOR_APPROVAL. Required Permission: APPROVE_AUTO_ASSIGNMENT")
    @PostCreateNoContentResponses
    @PostMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}/approve", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> approve(
            @PathVariable("targetFilterQueryId") Long targetFilterQueryId,
            @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the request for denying the approval of an auto assignment
     *
     * @param targetFilterQueryId the id of the target filter query associated with the auto assignment
     * @param remark an optional remark on the denial decision
     * @return OK response (200) if the auto assignment is denied. In case of any exceptions, the corresponding errors occur
     */
    @Operation(summary = "Deny an auto assignment",
            description = "Handles the request for denying the approval of an auto assignment. Only possible if " +
                    "approval workflow is enabled in the system configuration and the auto assignment " +
                    "is in state WAITING_FOR_APPROVAL. Required Permission: APPROVE_AUTO_ASSIGNMENT")
    @PostCreateNoContentResponses
    @PostMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}/deny", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deny(
            @PathVariable("targetFilterQueryId") Long targetFilterQueryId,
            @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the POST request for starting an auto assignment
     *
     * @param targetFilterQueryId the id of the target filter query associated with the auto assignment
     * @return OK response (200) if the auto assignment could be started. In case of any exceptions the corresponding errors occur
     */
    @Operation(summary = "Start an auto assignment",
            description = "Handles the POST request for starting an auto assignment. Required Permission: HANDLE_AUTO_ASSIGNMENT")
    @PostCreateNoContentResponses
    @PostMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}/start", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> start(@PathVariable("targetFilterQueryId") Long targetFilterQueryId);

    /**
     * Handles the POST request for pausing an auto assignment
     *
     * @param targetFilterQueryId the id of the target filter query associated with the auto assignment
     * @return OK response (200) if the auto assignment could be paused. In case of any exceptions the corresponding errors occur
     */
    @Operation(summary = "Pause an auto assignment",
            description = "Handles the POST request for pausing an auto assignment. Required Permission: HANDLE_AUTO_ASSIGNMENT")
    @PostCreateNoContentResponses
    @PostMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}/pause", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> pause(@PathVariable("targetFilterQueryId") Long targetFilterQueryId);

    /**
     * Handles the POST request for resuming an auto assignment
     *
     * @param targetFilterQueryId the id of the target filter query associated with the auto assignment
     * @return OK response (200) if the auto assignment could be resumed. In case of any exceptions the corresponding errors occur
     */
    @Operation(summary = "Resume an auto assignment",
            description = "Handles the POST request for resuming an auto assignment. Required Permission: HANDLE_AUTO_ASSIGNMENT")
    @PostCreateNoContentResponses
    @PostMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}/resume", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> resume(@PathVariable("targetFilterQueryId") Long targetFilterQueryId);

    /**
     * Handles the DELETE request for deleting an auto assignment
     *
     * @param targetFilterQueryId the id of the target filter query associated with the auto assignment
     * @return OK response (200) if the auto assignment could be deleted. In case of any exceptions the corresponding errors occur
     */
    @Operation(summary = "Delete an auto assignment",
            description = "Handles the DELETE request for deleting an auto assignment. Required Permission: UPDATE_TARGET")
    @DeleteResponses
    @DeleteMapping(value = AUTO_ASSIGNMENTS_V1 + "/{targetFilterQueryId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> delete(@PathVariable("targetFilterQueryId") Long targetFilterQueryId);

}
