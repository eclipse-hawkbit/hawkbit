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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
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
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
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
 * API for handling target operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Targets", description = "REST API for Target CRUD operations.")
public interface MgmtTargetRestApi {

    /**
     * Handles the GET request of retrieving a single target.
     *
     * @param targetId
     *            the ID of the target to retrieve
     * @return a single target with status OK.
     */
    @Operation(summary = "Return target by id", description = "Handles the GET request of retrieving a single target. " +
            "Required Permission: READ_TARGET.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.",
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTarget> getTarget(@PathVariable("targetId") String targetId);

    /**
     * Handles the GET request of retrieving all targets.
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
    @Operation(summary = "Return all targets", description = "Handles the GET request of retrieving all targets. Required permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getTargets(
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
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam);

    /**
     * Handles the POST request of creating new targets. The request body must
     * always be a list of targets.
     *
     * @param targets
     *            the targets to be created.
     * @return In case all targets could successful created the ResponseEntity
     *         with status code 201 with a list of successfully created
     *         entities. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @Operation(summary = "Create target(s)", description = "Handles the POST request of creating new targets. The request body must always be a list of targets. Required Permission: CREATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTarget>> createTargets(List<MgmtTargetRequestBody> targets);

    /**
     * Handles the PUT request of updating a target. The ID is within the URL
     * path of the request. A given ID in the request body is ignored. It's not
     * possible to set fields to {@code null} values.
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
    @Operation(summary = "Update target by id", description = "Handles the PUT request of updating a target. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTarget> updateTarget(@PathVariable("targetId") String targetId,
            MgmtTargetRequestBody targetRest);

    /**
     * Handles the DELETE request of deleting a target.
     *
     * @param targetId
     *            the ID of the target to be deleted
     * @return If the given targetId could exists and could be deleted Http OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Delete target by id", description = "Handles the DELETE request of deleting a single target. Required Permission: DELETE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}")
    ResponseEntity<Void> deleteTarget(@PathVariable("targetId") String targetId);

    /**
     * Handles the DELETE (unassign) request of a target type.
     *
     * @param targetId
     *            the ID of the target
     * @return If the given targetId could exists and could be unassign Http OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Unassign target type from target.", description = "Remove the target type from a target. The target type will be set to null. Required permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING
            + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING)
    ResponseEntity<Void> unassignTargetType(@PathVariable("targetId") String targetId);

    /**
     * Handles the POST (assign) request of a target type.
     *
     * @param targetId
     *            the ID of the target
     * @return If the given targetId could exists and could be assign Http OK.
     *         In any failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Assign target type to a target", description = "Assign or update the target type of a target. Required permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING
            + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignTargetType(@PathVariable("targetId") String targetId, MgmtId targetTypeId);

    /**
     * Handles the GET request of retrieving the attributes of a specific
     * target.
     *
     * @param targetId
     *            the ID of the target to retrieve the attributes.
     * @return the target attributes as map response with status OK
     */
    @Operation(summary = "Return attributes of a specific target", description = "Handles the GET request of retrieving the attributes of a specific target. Reponse is a key/value list. Required Permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/attributes", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAttributes> getAttributes(@PathVariable("targetId") String targetId);

    /**
     * Handles the GET request of retrieving the Actions of a specific target.
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
     * @return a list of all Actions for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return actions for a specific target", description = "Handles the GET request of retrieving the full action history of a specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtAction>> getActionHistory(@PathVariable("targetId") String targetId,
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
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam);

    /**
     * Handles the GET request of retrieving a specific Actions of a specific
     * Target.
     *
     * @param targetId
     *            to load the action for
     * @param actionId
     *            to load
     * @return the action
     */
    @Operation(summary = "Return action by id of a specific target", description = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAction> getAction(@PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId);

    /**
     * Handles the DELETE request of canceling an specific Actions of a specific
     * Target.
     *
     * @param targetId
     *            the ID of the target in the URL path parameter
     * @param actionId
     *            the ID of the action in the URL path parameter
     * @param force
     *            optional parameter, which indicates a force cancel
     * @return status no content in case cancellation was successful
     */
    @Operation(summary = "Cancel action for a specific target", description = "Cancels an active action, only active actions can be deleted. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}")
    ResponseEntity<Void> cancelAction(@PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force);

    /**
     * Handles the PUT update request to switch an action from soft to forced.
     *
     * @param targetId
     *            the ID of the target in the URL path parameter
     * @param actionId
     *            the ID of the action in the URL path parameter
     * @param actionUpdate
     *            to update the action
     * @return status no content in case cancellation was successful
     */
    @Operation(summary = "Switch an action from soft to forced", description = "Handles the PUT request to switch an action from soft to forced. Required Permission: UPDATE_TARGET.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/actions/{actionId}", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAction> updateAction(@PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId, MgmtActionRequestBodyPut actionUpdate);

    /**
     * Handles the GET request of retrieving the ActionStatus of a specific
     * target and action.
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
     * @return a list of all ActionStatus for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return status of a specific action on a specific target", description = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING
            + "/{targetId}/actions/{actionId}/status", produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtActionStatus>> getActionStatusList(@PathVariable("targetId") String targetId,
            @PathVariable("actionId") Long actionId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam);

    /**
     * Handles the GET request of retrieving the assigned distribution set of a
     * specific target.
     *
     * @param targetId
     *            the ID of the target to retrieve the assigned distribution
     * 
     * @return the assigned distribution set with status OK, if none is assigned
     *         than {@code null} content (e.g. "{}")
     */
    @Operation(summary = "Return the assigned distribution set of a specific target", description = "Handles the GET request of retrieving the assigned distribution set of an specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/assignedDS", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(@PathVariable("targetId") String targetId);

    /**
     * Changes the assigned distribution set of a target.
     *
     * @param targetId
     *            of the target to change
     * @param dsAssignments
     *            the requested Assignments that shall be made
     * @param offline
     *            to <code>true</code> if update was executed offline, i.e. not
     *            managed by hawkBit.
     * 
     * @return status OK if the assignment of the targets was successful and a
     *         complex return body which contains information about the assigned
     *         targets and the already assigned targets counters
     */
    @Operation(summary = "Assigns a distribution set to a specific target", description = "Handles the POST request for assigning a distribution set to a specific target. Required Permission: READ_REPOSITORY and UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/assignedDS", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAssignmentResponseBody> postAssignedDistributionSet(
            @PathVariable("targetId") String targetId,
            @Valid MgmtDistributionSetAssignments dsAssignments,
            @RequestParam(value = "offline", required = false)
            @Schema(description = """
                    Offline update (set param to true) that is only reported but not managed by the service, e.g.
                    defaults set in factory, manual updates or migrations from other update systems. A completed action
                    is added to the history of the target(s). Target is set to IN_SYNC state as both assigned and
                    installed DS are set. Note: only executed if the target has currently no running update""")
            boolean offline);

    /**
     * Handles the GET request of retrieving the installed distribution set of
     * a specific target.
     *
     * @param targetId
     *            the ID of the target to retrieve
     * @return the assigned installed set with status OK, if none is installed
     *         than {@code null} content (e.g. "{}")
     */
    @Operation(summary = "Return installed distribution set of a specific target", description = "Handles the GET request of retrieving the installed distribution set of an specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/installedDS", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getInstalledDistributionSet(@PathVariable("targetId") String targetId);

    /**
     * Gets a paged list of meta data for a target.
     *
     * @param targetId the ID of the target for the meta data
     */
    @Operation(summary = "Return tags for specific target", description = "Get a paged list of tags for a target. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "204", description = "No tags"),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/tags", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTag>> getTags(@PathVariable("targetId") String targetId);

    /**
     * Gets a paged list of meta data for a target.
     *
     * @param targetId
     *            the ID of the target for the meta data
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
     *            {@code q=key==abc}
     * @return status OK if get request is successful with the paged list of
     *         meta data
     */
    @Operation(summary = "Return metadata for specific target", description = "Get a paged list of meta data for a target. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtMetadata>> getMetadata(@PathVariable("targetId") String targetId,
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
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam);

    /**
     * Gets a single meta data value for a specific key of a target.
     *
     * @param targetId
     *            the ID of the target to get the meta data from
     * @param metadataKey
     *            the key of the meta data entry to retrieve the value from
     * @return status OK if get request is successful with the value of the meta
     *         data
     */
    @Operation(summary = "Return single metadata value for a specific key of a target", description = "Get a single meta data value for a meta data key. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata/{metadataKey}", produces = {
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtMetadata> getMetadataValue(@PathVariable("targetId") String targetId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Updates a single meta data value of a target.
     *
     * @param targetId
     *            the ID of the target to update the meta data entry
     * @param metadataKey
     *            the key of the meta data to update the value
     * @param metadata
     *            update body
     * @return status OK if the update request is successful and the updated
     *         meta data result
     */
    @Operation(summary = "Updates a single meta data value of a target", description = "Update a single meta data value for speficic key. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata/{metadataKey}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtMetadata> updateMetadata(@PathVariable("targetId") String targetId,
            @PathVariable("metadataKey") String metadataKey, MgmtMetadataBodyPut metadata);

    /**
     * Deletes a single meta data entry from the target.
     *
     * @param targetId
     *            the ID of the target to delete the meta data entry
     * @param metadataKey
     *            the key of the meta data to delete
     * @return status OK if the delete request is successful
     */
    @Operation(summary = "Deletes a single meta data entry from a target", description = "Delete a single meta data. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata/{metadataKey}")
    ResponseEntity<Void> deleteMetadata(@PathVariable("targetId") String targetId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Creates a list of meta data for a specific target.
     *
     * @param targetId
     *            the ID of the targetId to create meta data for
     * @param metadataRest
     *            the list of meta data entries to create
     * @return status created if post request is successful with the value of
     *         the created meta data
     */
    @Operation(summary = "Create a list of meta data for a specific target", description = "Create a list of meta data entries Required permissions: READ_REPOSITORY and UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/metadata", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypes.HAL_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtMetadata>> createMetadata(@PathVariable("targetId") String targetId,
            List<MgmtMetadata> metadataRest);

    /**
     * Get the current auto-confirm state for a specific target.
     *
     * @param targetId
     *            to check the state for
     * @return the current state as {@link MgmtTargetAutoConfirm}
     */
    @Operation(summary = "Return the current auto-confitm state for a specific target", description = "Handles the GET request to check the current auto-confirmation state of a target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/autoConfirm", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAutoConfirm> getAutoConfirmStatus(@PathVariable("targetId") String targetId);

    /**
     * Activate auto-confirm on a specific target.
     *
     * @param targetId
     *            to activate auto-confirm on
     * @param update
     *            properties to update
     * @return {@link org.springframework.http.HttpStatus#OK} in case of a
     *         success
     */
    @Operation(summary = "Activate auto-confirm on a specific target", description = "Handles the POST request to activate auto-confirmation for a specific target. As a result all current active as well as future actions will automatically be confirmed by mentioning the initiator as triggered person. Actions will be automatically confirmed, as long as auto-confirmation is active. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/autoConfirm/activate")
    ResponseEntity<Void> activateAutoConfirm(@PathVariable("targetId") String targetId,
            @RequestBody(required = false) MgmtTargetAutoConfirmUpdate update);

    /**
     * Deactivate auto-confirm on a specific target.
     *
     * @param targetId
     *            to deactivate auto-confirm on
     * 
     * @return {@link org.springframework.http.HttpStatus#OK} in case of a
     *         success
     */
    @Operation(summary = "Deactivate auto-confirm on a specific target", description = "Handles the POST request to deactivate auto-confirmation for a specific target. All active actions will remain unchanged while all future actions need to be confirmed, before processing with the deployment. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", 
                description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                        "data volume restriction applies.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                "user in another request at the same time. You may retry your modification request.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                "supported by the server for this resource.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                "and the client has to wait another second.", 
                content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{targetId}/autoConfirm/deactivate")
    ResponseEntity<Void> deactivateAutoConfirm(@PathVariable("targetId") String targetId);

}
