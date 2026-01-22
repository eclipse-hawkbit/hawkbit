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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.TARGET_GROUP_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutNoContentResponses;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Target Groups", description = "REST API for Target Groups operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = TARGET_GROUP_ORDER)))
public interface MgmtTargetGroupRestApi {

    String TARGETGROUPS_V1 = MgmtRestConstants.REST_V1 + "/targetgroups";

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
    @GetIfExistResponses
    @GetMapping(value = TARGETGROUPS_V1 + "/{group}/assigned", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            @PathVariable
            @Schema(description = "The target group of the targets")
            String group,
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
    @GetIfExistResponses
    @GetMapping(value = TARGETGROUPS_V1 + "/assigned")
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargetsWithSubgroups(
            @RequestParam(value = "group")
            @Schema(description = "Target Group or Filter based on target groups. ")
            String groupFilter,
            @RequestParam(value = "subgroups", defaultValue = "false")
            @Schema(description = " Possibility to search for subgroups with wildcard or not - e.g. ParentGroup/*")
            boolean subgroups,
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
    @PutNoContentResponses
    @PutMapping(value = TARGETGROUPS_V1 + "/assigned")
    ResponseEntity<Void> assignTargetsToGroupWithSubgroups(
            @RequestParam("group")
            @Schema(description = "The target group to be set. Sub-grouping is allowed here - '/' could be used for subgroups") String group,
            @Schema(description = "List of controller ids to be assigned", example = "[\"controllerId1\", \"controllerId2\"]")
            @RequestBody List<String> controllerIds
    );

    /**
     * Assigns targets to a given group.
     *
     * @param group - target group to be assigned
     * @param controllerIds - list of controllerIds for targets to be assigned
     */
    @Operation(summary = "Assign target(s) to given group",
            description = "Handles the PUT request of target assignment." +
                    "Subgroups are NOT allowed here - e.g. Parent/Child")
    @PutNoContentResponses
    @PutMapping(value = TARGETGROUPS_V1 + "/{group}/assigned")
    ResponseEntity<Void> assignTargetsToGroup(
            @PathVariable(value = "group")
            @Schema(description = "The target group to be set. Sub-grouping not allowed here, for sub-grouping use the analogical method with query parameter.")
            String group,
            @Schema(description = "List of controller ids to be assigned", example = "[\"controllerId1\", \"controllerId2\"]")
            @RequestBody
            List<String> controllerIds
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
            description = "Handles the PUT request of target group assignment. Subgroups are NOT allowed here - e.g. Parent/Child")
    @PutNoContentResponses
    @PutMapping(value = TARGETGROUPS_V1 + "/{group}")
    ResponseEntity<Void> assignTargetsToGroupWithRsql(
            @PathVariable
            String group,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH)
            @Schema(description = "Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for available fields.")
            String rsql
    );

    /**
     * Unassigns targets from their groups
     *
     * @param controllerIds - list of targets to be unassigned.
     */
    @Operation(summary = "Unassign targets from their target groups",
            description = "Handles the DELETE request to unassign the given target(s).")
    @DeleteResponses
    @DeleteMapping(value = TARGETGROUPS_V1 + "/assigned")
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
    @Operation(summary = "Unassign targets from their target groups by filter",
            description = "Handles the DELETE request to unassign targets by RSQL filter.")
    @DeleteResponses
    @DeleteMapping(value = TARGETGROUPS_V1)
    ResponseEntity<Void> unassignTargetsFromGroupByRsql(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = "Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for available fields.")
            String rsql
    );

    /**
     *
     * @return list of all assigned target groups
     */
    @Operation(summary = "Return all assigned target groups",
            description = "Handles the GET request of retrieving a list of all target groups.")
    @GetIfExistResponses
    @GetMapping(value = TARGETGROUPS_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<List<String>> getTargetGroups();

    /**
     * Assign targets matching a rsql filter to a provided target group
     *
     * @param group - target group to be assigned
     * @param rsqlParam - rsql filter based on Target fields
     */
    @Operation(summary = "Assign targets matching a rsql filter to provided target group",
            description = "Assign targets matching a rsql filter to a provided target group" +
                    "Subgroups are allowed - e.g. Parent/Child")
    @PutNoContentResponses
    @PutMapping(value = TARGETGROUPS_V1)
    ResponseEntity<Void> assignTargetsToGroup(
            @RequestParam(name = "group")
            @Schema(description = "The target group to be set. Sub-grouping is allowed here - '/' could be used for subgroups")
            String group,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH)
            @Schema(description = "Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for available fields.")
            String rsqlParam);
}