/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.rest.OpenApi;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.TARGET_GROUP_ORDER;

@Tag(
        name = "Target Groups", description = "REST API for Target Groups operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = TARGET_GROUP_ORDER)))
public interface MgmtTargetGroupRestApi {


    /**
     * Handles the GET request of retrieving a list of assigned targets for a specific group. Complex grouping (subgroups) not supported here.
     * For complex grouping use the analogical resource with query parameter for target group.
     *
     * @param group - target group
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of targets matching the provided group for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return assigned targets for group",
            description = "Handles the GET request of retrieving a list of assigned targets for a specific group. Complex grouping (subgroups) not supported here." +
                    "For complex grouping use the analogical resource with query parameter for target group.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + MgmtRestConstants.TARGET_GROUP_TARGETS_REQUEST_MAPPING,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            @PathVariable
            @Schema(description = "The target group of the targets")
            String group,
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
            String sortParam
    );

    /**
     * Handles the GET request of retrieving a list of assigned targets for a specific group. Complex grouping (subgroups) is supported here.
     * Search could be for specific group, complex group e.g Parent/Child or also for groups including its subgroups
     *
     * @param groupFilter - An Actual group - Parent/Child or Parent
     * @param subgroups - If set to {@code true} enables the search in subgroups
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of targets matching the provided group for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return assigned targets for group",
            description = "Handles the GET request of retrieving a list of assigned targets for a specific group. Complex grouping (subgroups) is supported here." +
                    "Search could be for specific group, complex group e.g Parent/Child or also for groups including its subgroups")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargetsWithSubgroups(
            @RequestParam(value = "group")
            @Schema(description = "Target Group or Filter based on target groups. ")
            String groupFilter,
            @RequestParam(value = "subgroups", defaultValue = "false")
            @Schema(description = " Possibility to search for subgroups with wildcard or not - e.g. ParentGroup/*")
            boolean subgroups,
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
            String sortParam
    );

    /**
     * Assigns targets to a given group.
     * For complex groups use analogical method with query parameters.
     *
     * @param group - target group to be assigned
     * @param controllerIds - list of controllerIds for targets to be assigned
     *
     */
    @Operation(summary = "Assign target(s) to given group",
            description = "Handles the POST request of target assignment. Already assigned target will be ignored. " +
                    "For complex groups use analogical method with query parameters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request."),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @PutMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + MgmtRestConstants.TARGET_GROUP_TARGETS_REQUEST_MAPPING)
    ResponseEntity<Void> assignTargetsToGroup(
            @PathVariable(value = "group")
            @Schema(description = "The target group to be set. Sub-grouping not allowed here, for sub-grouping use the analogical method with query parameter.")
            String group,
            @Schema(description = "List of controller ids to be assigned", example = "[\"controllerId1\", \"controllerId2\"]")
            @RequestBody List<String> controllerIds
    );

    /**
     * Assigns targets to a given group.
     * Complex (subgroups) are allowed - e.g. Parent/Child
     *
     * @param group - target group to be assigned
     * @param controllerIds - list of controllerIds for targets to be assigned
     *
     */
    @Operation(summary = "Assign target(s) to given group",
            description = "Handles the PUT request of assign target group." +
                    "Subgroups are allowed - e.g. Parent/Child")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request."),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @PutMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
    ResponseEntity<Void> assignTargetsToGroupWithSubgroups(
            @RequestParam("group")
            @Schema(description = "The target group to be set. Sub-grouping is allowed here - '/' could be used for subgroups")
            final String group,
            @Schema(description = "List of controller ids to be assigned", example = "[\"controllerId1\", \"controllerId2\"]")
            @RequestBody List<String> controllerIds
    );

    /**
     * Assigns targets to a given group.
     * Complex (subgroups) are allowed - e.g. Parent/Child
     *
     * @param group - target group to be assigned
     * @param rsql - filter to match desired targets
     *
     */
    @Operation(summary = "Assign target(s) to given group by rsql",
            description = "Handles the PUT request of target group assignment." +
                    "Subgroups are NOT allowed here - e.g. Parent/Child")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request."),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @PutMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/{group}")
    ResponseEntity<Void> assignTargetsToGroupWithRsql(
            @PathVariable final String group,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            final String rsql
    );

    /**
     * Unassigns targets from their groups
     *
     * @param controllerIds - list of targets to be unassigned.
     */
    @Operation(summary = "Unassign targets from their target groups",
            description = "Handles the DELETE request to unassign the given target(s).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully unassigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING + "/assigned")
    ResponseEntity<Void> unassignTargetsFromGroup(
            @RequestBody(required = false)
            @Schema(description = "List of controller ids to be unassigned from their groups", example = "[\"controllerId1\", \"controllerId2\"]")
            List<String> controllerIds

    );

    /**
     * Unassigns targets from their groups
     *
     * @param rsql - filter for the matching targets to be unassigned
     */
    @Operation(summary = "Unassign targets from their target groups",
            description = "Handles the DELETE request to unassign the given target(s).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully unassigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING)
    ResponseEntity<Void> unassignTargetsFromGroupByRsql(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            final String rsql
    );

    /**
     *
     * @return list of all assigned target groups
     */
    @Operation(summary = "Return all assigned target groups",
            description = "Handles the GET request of retrieving a list of all target groups.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<String>> getTargetGroups();


    /**
     * Assign targets matching a rsql filter to a provided target group
     * @param group - target group to be assigned
     * @param rsqlParam - rsql filter based on Target fields
     */
    @Operation(summary = "Assign targets matching a rsql filter to provided target group",
            description = "Assign targets matching a rsql filter to a provided target group" +
                    "Subgroups are allowed - e.g. Parent/Child")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies."),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another " +
                    "user in another request at the same time. You may retry your modification request."),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not " +
                    "supported by the server for this resource."),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.")
    })
    @PutMapping(value = MgmtRestConstants.TARGET_GROUP_V1_REQUEST_MAPPING)
    ResponseEntity<Void> assignTargetsToGroup(
            @RequestParam(name = "group")
            @Schema(description = "The target group to be set. Sub-grouping is allowed here - '/' could be used for subgroups")
            final String group,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam);
}
