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
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.TARGET_TAG_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostUpdateNoContentResponses;
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
 * REST Resource handling for TargetTag CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(
        name = "Target Tags", description = "REST API for Target Tag CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = TARGET_TAG_ORDER)))
public interface MgmtTargetTagRestApi {

    String TARGETTAGS_V1 = MgmtRestConstants.REST_V1 + "/targettags";
    String TARGET_TAG_ID_ASSIGNED = "/{targetTagId}/assigned";

    /**
     * Handles the GET request of retrieving all target tags.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of target tags for pagination, might not be present in the rest request then default value
     *         will be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all target tags for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all target tags", description = "Handles the GET request of retrieving all target tags.")
    @GetIfExistResponses
    @GetMapping(value = TARGETTAGS_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTag>> getTargetTags(
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
     * Handles the GET request of retrieving a single target tag.
     *
     * @param targetTagId the ID of the target tag to retrieve
     * @return a single target tag with status OK.
     */
    @Operation(summary = "Return target tag by id", description = "Handles the GET request of retrieving a single target tag.")
    @GetResponses
    @GetMapping(value = TARGETTAGS_V1 + "/{targetTagId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> getTargetTag(@PathVariable("targetTagId") Long targetTagId);

    /**
     * Handles the POST request of creating new target tag. The request body must always be a list of tags.
     *
     * @param tags the target tags to be created.
     * @return In case all modules could successful created the ResponseEntity with status code 201 - Created. The Response Body are the created
     *         target tags but without ResponseBody.
     */
    @Operation(summary = "Create target tag(s)", description = "Handles the POST request of creating new target tag. " +
            "The request body must always be a list of target tags.")
    @PostCreateResponses
    @PostMapping(value = TARGETTAGS_V1,
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTag>> createTargetTags(@RequestBody List<MgmtTagRequestBodyPut> tags);

    /**
     * Handles the PUT request of updating a single target tag.
     *
     * @param targetTagId the ID of the target tag
     * @param restTargetTagRest the request body to be updated
     * @return status OK if update is successful and the updated target tag.
     */
    @Operation(summary = "Update target tag by id", description = "Handles the PUT request of updating a target tag.")
    @PutResponses
    @PutMapping(value = TARGETTAGS_V1 + "/{targetTagId}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> updateTargetTag(
            @PathVariable("targetTagId") Long targetTagId,
            @RequestBody MgmtTagRequestBodyPut restTargetTagRest);

    /**
     * Handles the DELETE request for a single target tag.
     *
     * @param targetTagId the ID of the target tag
     * @return status OK if delete as successfully.
     */
    @Operation(summary = "Delete target tag by id", description = "Handles the DELETE request of deleting a single target tag.")
    @DeleteResponses
    @DeleteMapping(value = TARGETTAGS_V1 + "/{targetTagId}")
    ResponseEntity<Void> deleteTargetTag(@PathVariable("targetTagId") Long targetTagId);

    /**
     * Handles the GET request of retrieving all assigned targets by the given tag id.
     *
     * @param targetTagId the ID of the target tag to retrieve
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of target tags for pagination, might not be present in the rest request then default value
     *         will be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return the list of assigned targets.
     */
    @Operation(summary = "Return assigned targets for tag",
            description = "Handles the GET request of retrieving a list of assigned targets.")
    @GetResponses
    @GetMapping(value = TARGETTAGS_V1 + TARGET_TAG_ID_ASSIGNED, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            @PathVariable("targetTagId") Long targetTagId,
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
     * Handles the PUT request to assign targets to the given tag id.
     *
     * @param targetTagId the ID of the target tag to retrieve
     * @param controllerId stream of controller ids to be assigned
     * @return the list of assigned targets.
     */
    @Operation(summary = "Assign target(s) to given tagId",
            description = "Handles the POST request of target assignment. Already assigned target will be ignored.")
    @PostUpdateNoContentResponses
    @PostMapping(value = TARGETTAGS_V1 + TARGET_TAG_ID_ASSIGNED + "/{controllerId}")
    ResponseEntity<Void> assignTarget(
            @PathVariable("targetTagId") Long targetTagId,
            @PathVariable("controllerId") String controllerId);

    /**
     * Handles the PUT request to assign targets to the given tag id.
     *
     * @param targetTagId the ID of the target tag to retrieve
     * @param controllerIds stream of controller ids to be assigned
     * @return the list of assigned targets.
     */
    @Operation(summary = "Assign target(s) to given tagId",
            description = "Handles the POST request of target assignment. Already assigned target will be ignored.")
    @PostUpdateNoContentResponses
    @PostMapping(value = TARGETTAGS_V1 + TARGET_TAG_ID_ASSIGNED, consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignTargets(
            @PathVariable("targetTagId") Long targetTagId,
            @Schema(description = "List of controller ids to be assigned", example = "[\"controllerId1\", \"controllerId2\"]")
            @RequestBody List<String> controllerIds,
            @RequestParam(value = "onNotFoundPolicy", required = false, defaultValue = "FAIL") OnNotFoundPolicy onNotFoundPolicy);

    /**
     * Handles the DELETE request to unassign one target from the given tag id.
     *
     * @param targetTagId the ID of the target tag
     * @param controllerId the ID of the target to unassign
     * @return http status code
     */
    @Operation(summary = "Unassign target from a given tagId", description = "Handles the DELETE request to unassign the given target.")
    @DeleteResponses
    @DeleteMapping(value = TARGETTAGS_V1 + TARGET_TAG_ID_ASSIGNED + "/{controllerId}")
    ResponseEntity<Void> unassignTarget(
            @PathVariable("targetTagId") Long targetTagId,
            @PathVariable("controllerId") String controllerId);

    /**
     * Handles the DELETE request to unassign one target from the given tag id.
     *
     * @param targetTagId the ID of the target tag
     * @param controllerId the ID of the target to unassign
     * @return http status code
     */
    @Operation(summary = "Unassign targets from a given tagId", description = "Handles the DELETE request to unassign the given targets.")
    @DeleteResponses
    @DeleteMapping(value = TARGETTAGS_V1 + TARGET_TAG_ID_ASSIGNED, consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> unassignTargets(
            @PathVariable("targetTagId") Long targetTagId,
            @RequestParam(value = "onNotFoundPolicy", required = false, defaultValue = "FAIL") OnNotFoundPolicy onNotFoundPolicy,
            @Schema(description = "List of controller ids to be unassigned", example = "[\"controllerId1\", \"controllerId2\"]")
            @RequestBody List<String> controllerId);

    enum OnNotFoundPolicy {
        // if it has not found - operation fail
        FAIL, // default
        // if it has not found - do operation on found operation and fail indicating that not all are found
        ON_WHAT_FOUND_AND_FAIL,
        // if it has not found - do operation on found operation and success, silently
        ON_WHAT_FOUND_AND_SUCCESS
    }
}