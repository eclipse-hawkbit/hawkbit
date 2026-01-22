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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.TARGET_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GONE_410;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.METHOD_NOT_ALLOWED_405;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.NOT_FOUND_404;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutNoContentResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionConfirmationRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionStatus;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtDistributionSetAssignments;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAttributes;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirm;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAutoConfirmUpdate;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostUpdateNoContentResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostUpdateResponses;
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
 * API for handling target operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(
        name = "Targets", description = "REST API for Target CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = TARGET_ORDER)))
public interface MgmtTargetRestApi {

    String TARGETS_V1 = MgmtRestConstants.REST_V1 + "/targets";
    String TARGET_ID_TARGETTYPE = "/{targetId}/targettype";

    /**
     * Handles the GET request of retrieving a single target.
     *
     * @param targetId the ID of the target to retrieve
     * @return a single target with status OK.
     */
    @Operation(summary = "Return target by id", description = "Handles the GET request of retrieving a single target. " +
            "Required Permission: READ_TARGET.")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTarget> getTarget(@PathVariable("targetId") String targetId);

    /**
     * Handles the GET request of retrieving all targets.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all targets for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all targets", description = "Handles the GET request of retrieving all targets. Required permission: READ_TARGET")
    @GetIfExistResponses
    @GetMapping(value = TARGETS_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getTargets(
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
            String sortParam);

    /**
     * Handles the POST request of creating new targets. The request body must always be a list of targets.
     *
     * @param targets the targets to be created.
     * @return In case all targets could successful created the ResponseEntity with status code 201 with a list of successfully created
     *         entities. In any failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Create target(s)", description = "Handles the POST request of creating new targets. The request body must always be a list of targets. Required Permission: CREATE_TARGET")
    @PostCreateResponses
    @PostMapping(value = TARGETS_V1,
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTarget>> createTargets(@RequestBody List<MgmtTargetRequestBody> targets);

    /**
     * Handles the PUT request of updating a target. The ID is within the URL path of the request. A given ID in the request body is ignored.
     * It's not possible to set fields to {@code null} values.
     *
     * @param targetId the path parameter which contains the ID of the target
     * @param targetRest the request body which contains the fields which should be updated, fields which are not given are ignored for the
     *         update.
     * @return the updated target response which contains all fields also fields which have not updated
     */
    @Operation(summary = "Update target by id", description = "Handles the PUT request of updating a target. Required Permission: UPDATE_TARGET")
    @PutResponses
    @PutMapping(value = TARGETS_V1 + "/{targetId}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTarget> updateTarget(
            @PathVariable("targetId") String targetId,
            @RequestBody MgmtTargetRequestBody targetRest);

    /**
     * Handles the DELETE request of deleting a target.
     *
     * @param targetId the ID of the target to be deleted
     * @return If the given targetId could exists and could be deleted Http OK. In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Delete target by id", description = "Handles the DELETE request of deleting a single target. Required Permission: DELETE_TARGET")
    @DeleteResponses
    @DeleteMapping(value = TARGETS_V1 + "/{targetId}")
    ResponseEntity<Void> deleteTarget(@PathVariable("targetId") String targetId);

    /**
     * Handles the DELETE (unassign) request of a target type.
     *
     * @param targetId the ID of the target
     * @return If the given targetId could exists and could be unassign Http OK. In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Unassign target type from target.", description = "Remove the target type from a target. The target type will be set to null. Required permission: UPDATE_TARGET")
    @DeleteResponses
    @DeleteMapping(value = TARGETS_V1 + TARGET_ID_TARGETTYPE)
    ResponseEntity<Void> unassignTargetType(@PathVariable("targetId") String targetId);

    /**
     * Handles the POST (assign) request of a target type.
     *
     * @param targetId the ID of the target
     * @param targetTypeId the ID of the target type to be assigned
     * @return If the given targetId could exists and could be assign Http OK. In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Assign target type to a target", description = "Assign or update the target type of a target. Required permission: UPDATE_TARGET")
    @PostUpdateNoContentResponses
    @PostMapping(value = TARGETS_V1 + TARGET_ID_TARGETTYPE, consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignTargetType(@PathVariable("targetId") String targetId, @RequestBody MgmtId targetTypeId);

    /**
     * Handles the GET request of retrieving the attributes of a specific target.
     *
     * @param targetId the ID of the target to retrieve the attributes.
     * @return the target attributes as map response with status OK
     */
    @Operation(summary = "Return attributes of a specific target", description = "Handles the GET request of retrieving the attributes of a specific target. Reponse is a key/value list. Required Permission: READ_TARGET")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/attributes", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAttributes> getAttributes(@PathVariable("targetId") String targetId);

    /**
     * Handles the GET request of retrieving the Actions of a specific target.
     *
     * @param targetId to load actions for
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=status==pending}
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all Actions for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return actions for a specific target", description = "Handles the GET request of retrieving the full action history of a specific target. Required Permission: READ_TARGET")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/actions", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtAction>> getActionHistory(
            @PathVariable("targetId") String targetId,
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
            String sortParam);

    /**
     * Deletes all actions for the provided target by provided action IDs list
     * OR
     * Deletes all EXCEPT the latest N actions
     *
     * @param targetId - target id
     * @param keepLast - the number of last target actions to be left
     * @param actionIds - Specific action id list for actions to be deleted
     */
    @Operation(summary = "Deletes all actions for the provided target EXCEPT the latest N actions OR by provided action IDs list.", description = "Deletes/Purges the action history of the target except the last N actions OR deletes only the actions in the provided action ids list. Required Permission: DELETE_REPOSITORY")
    @DeleteResponses
    @DeleteMapping(value = TARGETS_V1 + "/{targetId}/actions", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deleteActionsForTarget(
            @PathVariable("targetId") String targetId,
            @RequestParam(name = "keepLast", required = false, defaultValue = "-1") int keepLast,
            @Schema(description = "List of action ids to be deleted", example = "[253, 255]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            @RequestBody(required = false) List<Long> actionIds);

    /**
     * Handles the GET request of retrieving a specific Actions of a specific Target.
     *
     * @param targetId to load the action for
     * @param actionId to load
     * @return the action
     */
    @Operation(summary = "Return action by id of a specific target",
            description = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/actions/{actionId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAction> getAction(
            @PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId);

    /**
     * Handles the DELETE request of canceling an specific Actions of a specific Target.
     *
     * @param targetId the ID of the target in the URL path parameter
     * @param actionId the ID of the action in the URL path parameter
     * @param force optional parameter, which indicates a force cancel
     * @return status no content in case cancellation was successful
     */
    @Operation(summary = "Cancel action for a specific target",
            description = "Cancels an active action, only active actions can be deleted. Required Permission: UPDATE_TARGET")
    @DeleteResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = METHOD_NOT_ALLOWED_405, description = "Software module is locked",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = TARGETS_V1 + "/{targetId}/actions/{actionId}")
    ResponseEntity<Void> cancelAction(
            @PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force);

    /**
     * Handles the PUT update request to switch an action from soft to forced.
     *
     * @param targetId the ID of the target in the URL path parameter
     * @param actionId the ID of the action in the URL path parameter
     * @param actionUpdate to update the action
     * @return status no content in case cancellation was successful
     */
    @Operation(summary = "Switch an action from soft to forced", description = "Handles the PUT request to switch an action from soft to forced. Required Permission: UPDATE_TARGET.")
    @PutResponses
    @PutMapping(value = TARGETS_V1 + "/{targetId}/actions/{actionId}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAction> updateAction(
            @PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId,
            @RequestBody MgmtActionRequestBodyPut actionUpdate);

    /**
     * Handles the PUT update request to either 'confirm' or 'deny' single action on a target.
     */
    @Operation(summary = "Controls (confirm/deny) actions waiting for confirmation", description = """
            Either confirm or deny an action which is waiting for confirmation.
            The action will be transferred into the RUNNING state in case confirming it.
            The action will remain in WAITING_FOR_CONFIRMATION state in case denying it. 
            Required Permission: READ_REPOSITORY AND UPDATE_TARGET
            """)
    @PutNoContentResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = GONE_410, description = "Action is not active anymore.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
    })
    @PutMapping(value = TARGETS_V1 + "/{targetId}/actions/{actionId}/confirmation",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE },
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> updateActionConfirmation(
            @PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId,
            @Valid @RequestBody MgmtActionConfirmationRequestBodyPut actionConfirmation);

    /**
     * Handles the GET request of retrieving the ActionStatus of a specific target and action.
     *
     * @param targetId of the action
     * @param actionId of the status we are intend to load
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all ActionStatus for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return status of a specific action on a specific target",
            description = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/actions/{actionId}/status",
            produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtActionStatus>> getActionStatusList(
            @PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId,
            @RequestParam(value = REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            int pagingOffsetParam,
            @RequestParam(value = REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false)
            String sortParam);

    /**
     * Handles the GET request of retrieving the assigned distribution set of a specific target.
     *
     * @param targetId the ID of the target to retrieve the assigned distribution
     * @return the assigned distribution set with status OK, if none is assigned than {@code null} content (e.g. "{}")
     */
    @Operation(summary = "Return the assigned distribution set of a specific target", description = "Handles the GET request of retrieving the assigned distribution set of an specific target. Required Permission: READ_TARGET")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/assignedDS", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(@PathVariable("targetId") String targetId);

    /**
     * Changes the assigned distribution set of a target.
     *
     * @param targetId of the target to change
     * @param dsAssignments the requested Assignments that shall be made
     * @param offline to <code>true</code> if update was executed offline, i.e. not managed by hawkBit.
     * @return status OK if the assignment of the targets was successful and a complex return body which contains information about the assigned
     *         targets and the already assigned targets counters
     */
    @Operation(summary = "Assigns a distribution set to a specific target", description = "Handles the POST request for assigning a distribution set to a specific target. Required Permission: READ_REPOSITORY and UPDATE_TARGET")
    @PostUpdateResponses
    @PostMapping(value = TARGETS_V1 + "/{targetId}/assignedDS",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAssignmentResponseBody> postAssignedDistributionSet(
            @PathVariable("targetId") String targetId,
            @RequestBody @Valid MgmtDistributionSetAssignments dsAssignments,
            @RequestParam(value = "offline", required = false)
            @Schema(description = """
                    Offline update (set param to true) that is only reported but not managed by the service, e.g.
                    defaults set in factory, manual updates or migrations from other update systems. A completed action
                    is added to the history of the target(s). Target is set to IN_SYNC state as both assigned and
                    installed DS are set. Note: only executed if the target has currently no running update""")
            Boolean offline);

    /**
     * Handles the GET request of retrieving the installed distribution set of a specific target.
     *
     * @param targetId the ID of the target to retrieve
     * @return the assigned installed set with status OK, if none is installed than {@code null} content (e.g. "{}")
     */
    @Operation(summary = "Return installed distribution set of a specific target", description = "Handles the GET request of retrieving the installed distribution set of an specific target. Required Permission: READ_TARGET")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/installedDS", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getInstalledDistributionSet(@PathVariable("targetId") String targetId);

    /**
     * Gets a paged list of metadata for a target.
     *
     * @param targetId the ID of the target for the metadata
     */
    @Operation(summary = "Return tags for specific target", description = "Get a paged list of tags for a target. Required permission: READ_REPOSITORY")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/tags", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTag>> getTags(@PathVariable("targetId") String targetId);

    /**
     * Creates a list of metadata for a specific target.
     *
     * @param targetId the ID of the targetId to create metadata for
     * @param metadataRest the list of metadata entries to create
     */
    @Operation(summary = "Create a list of metadata for a specific target", description = "Create a list of metadata entries Required permissions: READ_REPOSITORY and UPDATE_TARGET")
    @PostCreateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = NOT_FOUND_404, description = "Target not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = TARGETS_V1 + "/{targetId}/metadata",
            consumes = { APPLICATION_JSON_VALUE, HAL_JSON_VALUE })
    ResponseEntity<Void> createMetadata(@PathVariable("targetId") String targetId, @RequestBody List<MgmtMetadata> metadataRest);

    /**
     * Gets a paged list of metadata for a target.
     *
     * @param targetId the ID of the target for the metadata
     * @return status OK if get request is successful with the paged list of metadata
     */
    @Operation(summary = "Return metadata for specific target", description = "Get a paged list of metadata for a target. Required permission: READ_REPOSITORY")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/metadata", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtMetadata>> getMetadata(@PathVariable("targetId") String targetId);

    /**
     * Gets a single metadata value for a specific key of a target.
     *
     * @param targetId the ID of the target to get the metadata from
     * @param metadataKey the key of the metadata entry to retrieve the value from
     * @return status OK if get request is successful with the value of the metadata
     */
    @Operation(summary = "Return single metadata value for a specific key of a target",
            description = "Get a single metadata value for a metadata key. Required permission: READ_REPOSITORY")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/metadata/{metadataKey}", produces = { APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtMetadata> getMetadataValue(
            @PathVariable("targetId") String targetId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Updates a single metadata value of a target.
     *
     * @param targetId the ID of the target to update the metadata entry
     * @param metadataKey the key of the metadata to update the value
     * @param metadata update body
     */
    @Operation(summary = "Updates a single metadata value of a target",
            description = "Update a single metadata value for speficic key. Required permission: UPDATE_REPOSITORY")
    @PutNoContentResponses
    @PutMapping(value = TARGETS_V1 + "/{targetId}/metadata/{metadataKey}")
    ResponseEntity<Void> updateMetadata(
            @PathVariable("targetId") String targetId,
            @PathVariable("metadataKey") String metadataKey,
            @RequestBody MgmtMetadataBodyPut metadata);

    /**
     * Deletes a single metadata entry from the target.
     *
     * @param targetId the ID of the target to delete the metadata entry
     * @param metadataKey the key of the metadata to delete
     * @return status OK if the delete request is successful
     */
    @Operation(summary = "Deletes a single metadata entry from a target", description = "Delete a single metadata. Required permission: UPDATE_REPOSITORY")
    @DeleteResponses
    @DeleteMapping(value = TARGETS_V1 + "/{targetId}/metadata/{metadataKey}")
    ResponseEntity<Void> deleteMetadata(
            @PathVariable("targetId") String targetId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Get the current auto-confirm state for a specific target.
     *
     * @param targetId to check the state for
     * @return the current state as {@link MgmtTargetAutoConfirm}
     */
    @Operation(summary = "Return the current auto-confitm state for a specific target", description = "Handles the GET request to check the current auto-confirmation state of a target. Required Permission: READ_TARGET")
    @GetResponses
    @GetMapping(value = TARGETS_V1 + "/{targetId}/autoConfirm", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAutoConfirm> getAutoConfirmStatus(
            @PathVariable("targetId") String targetId);

    /**
     * Activate auto-confirm on a specific target.
     *
     * @param targetId to activate auto-confirm on
     * @param update properties to update
     * @return {@link org.springframework.http.HttpStatus#OK} in case of a success
     */
    @Operation(summary = "Activate auto-confirm on a specific target", description = "Handles the POST request to activate auto-confirmation for a specific target. As a result all current active as well as future actions will automatically be confirmed by mentioning the initiator as triggered person. Actions will be automatically confirmed, as long as auto-confirmation is active. Required Permission: UPDATE_TARGET")
    @PostUpdateNoContentResponses
    @PostMapping(value = TARGETS_V1 + "/{targetId}/autoConfirm/activate")
    ResponseEntity<Void> activateAutoConfirm(
            @PathVariable("targetId") String targetId,
            @RequestBody(required = false) MgmtTargetAutoConfirmUpdate update);

    /**
     * Deactivate auto-confirm on a specific target.
     *
     * @param targetId to deactivate auto-confirm on
     * @return {@link org.springframework.http.HttpStatus#OK} in case of a success
     */
    @Operation(summary = "Deactivate auto-confirm on a specific target", description = "Handles the POST request to deactivate auto-confirmation for a specific target. All active actions will remain unchanged while all future actions need to be confirmed, before processing with the deployment. Required Permission: UPDATE_TARGET")
    @PostUpdateNoContentResponses
    @PostMapping(value = TARGETS_V1 + "/{targetId}/autoConfirm/deactivate")
    ResponseEntity<Void> deactivateAutoConfirm(
            @PathVariable("targetId") String targetId);
}