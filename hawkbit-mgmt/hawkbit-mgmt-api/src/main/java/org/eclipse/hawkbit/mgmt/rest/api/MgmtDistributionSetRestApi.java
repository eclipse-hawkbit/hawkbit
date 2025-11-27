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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.DISTRIBUTION_SET_ORDER;

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
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadataBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetStatistics;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtInvalidateDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssignment;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.rest.OpenApi;
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
 * REST Resource handling for DistributionSet CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(
        name = "Distribution Sets", description = "REST Resource handling for DistributionSet CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = DISTRIBUTION_SET_ORDER)))
public interface MgmtDistributionSetRestApi {

    /**
     * Handles the GET request of retrieving all DistributionSets .
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of sets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all set for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Distribution Sets", description = "Handles the GET request of retrieving all " +
            "distribution sets. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further " +
                    "attempts and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtDistributionSet>> getDistributionSets(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam, @RequestParam(
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
     * Handles the GET request of retrieving a single DistributionSet .
     *
     * @param distributionSetId the ID of the set to retrieve
     * @return a single DistributionSet with status OK.
     */
    @Operation(summary = "Return single Distribution Set", description = "Handles the GET request of retrieving a " +
            "single distribution set. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getDistributionSet(@PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Handles the POST request of creating new distribution sets . The request body must always be a list of sets.
     *
     * @param sets the DistributionSets to be created.
     * @return In case all sets could successful created the ResponseEntity with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Creates new Distribution Sets", description = "Handles the POST request of creating new " +
            "distribution sets within Hawkbit. The request body must always be a list of sets. " +
            "Required permission: CREATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtDistributionSet>> createDistributionSets(@RequestBody List<MgmtDistributionSetRequestBodyPost> sets);

    /**
     * Handles the DELETE request for a single DistributionSet .
     *
     * @param distributionSetId the ID of the DistributionSet to delete
     * @return status OK if delete as successful.
     */
    @Operation(summary = "Delete Distribution Set by Id", description = "Handles the DELETE request for a single " +
            "Distribution Set. Required permission: DELETE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}")
    ResponseEntity<Void> deleteDistributionSet(@PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Handles the UPDATE request for a single DistributionSet .
     *
     * @param distributionSetId the ID of the DistributionSet to delete
     * @param toUpdate with the data that needs updating
     * @return status OK if update as successful with updated content.
     */
    @Operation(summary = "Update Distribution Set", description = "Handles the UPDATE request for a single " +
            "Distribution Set. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
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
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further " +
                    "attempts and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}",
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> updateDistributionSet(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestBody MgmtDistributionSetRequestBodyPut toUpdate);

    /**
     * Handles the GET request of retrieving assigned targets to a specific distribution set.
     *
     * @param distributionSetId the ID of the distribution set to retrieve the assigned targets
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return status OK if get request is successful with the paged list of targets
     */
    @Operation(summary = "Return assigned targets to a specific distribution set", description = "Handles the GET " +
            "request for retrieving assigned targets of a single distribution set. " +
            "Required permissions: READ_REPOSITORY and READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedTargets",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam, @RequestParam(
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
     * Handles the GET request of retrieving installed targets to a specific distribution set.
     *
     * @param distributionSetId the ID of the distribution set to retrieve the assigned targets
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return status OK if get request is successful with the paged list of targets
     */
    @Operation(summary = "Return installed targets to a specific distribution set", description = "Handles the GET " +
            "request for retrieving installed targets of a single distribution set. " +
            "Required permissions: READ_REPOSITORY and READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/installedTargets",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getInstalledTargets(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam, @RequestParam(
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
     * Handles the GET request to retrieve target filter queries that have the given distribution set as auto assign DS.
     *
     * @param distributionSetId the ID of the distribution set to retrieve the assigned targets
     * @param rsqlParam the search name parameter in the request URL, syntax {@code q=myFilter}
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return status OK if get request is successful with the paged list of targets
     */
    @Operation(summary = "Return target filter queries that have the given distribution set as auto assign DS",
            description = "Handles the GET request for retrieving assigned target filter queries of a single " +
                    "distribution set. Required permissions: READ_REPOSITORY and READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/autoAssignTargetFilters",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTargetFilterQuery>> getAutoAssignTargetFilterQueries(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            @Schema(description = """
                    Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for
                    available fields.""")
            String rsqlParam, @RequestParam(
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
     * Handles the POST request of assigning multiple targets to a single distribution set.
     *
     * @param distributionSetId the ID of the distribution set within the URL path parameter
     * @param assignments the IDs of the target which should get assigned to the distribution set given in the response body
     * @param offline to <code>true</code> if update was executed offline, i.e. not managed by hawkBit.
     * @return status OK if the assignment of the targets was successful and a complex return body which contains information about the assigned
     *         targets and the already assigned targets counters
     */
    @Operation(summary = "Assigning multiple targets to a single distribution set", description = "Handles the POST " +
            "request for assigning multiple targets to a distribution set.The request body must always be a list of " +
            "target IDs. Non-existing targets are silently ignored resulting in a valid response. " +
            "Required permissions: READ_REPOSITORY and UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING
            + "/{distributionSetId}/assignedTargets", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAssignmentResponseBody> createAssignedTarget(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestBody List<MgmtTargetAssignmentRequestBody> assignments,
            @RequestParam(value = "offline", required = false) Boolean offline);

    /**
     * Creates a list of meta-data for a specific distribution set.
     *
     * @param distributionSetId the ID of the distribution set to create meta data for
     * @param metadataRest the list of meta-data entries to create
     */
    @Operation(summary = "Create a list of meta data for a specific distribution set",
            description = "Create a list of meta data entries Required permissions: READ_REPOSITORY and UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
    ResponseEntity<Void> createMetadata(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestBody List<MgmtMetadata> metadataRest);

    /**
     * Gets a paged list of meta-data for a distribution set.
     *
     * @param distributionSetId the ID of the distribution set for the meta-data
     * @return status OK if get request is successful with the paged list of meta data
     */
    @Operation(summary = "Return meta data for Distribution Set", description = "Get a paged list of meta data for a " +
            "distribution set. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtMetadata>> getMetadata(@PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Gets a single meta-data value for a specific key of a distribution set.
     *
     * @param distributionSetId the ID of the distribution set to get the meta-data from
     * @param metadataKey the key of the meta-data entry to retrieve the value from
     * @return status OK with the value of the meta-data, if the request is successful
     */
    @Operation(summary = "Return single meta data value for a specific key of a Distribution Set",
            description = "Get a single meta data value for a meta data key. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata/{metadataKey}",
            produces = { MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtMetadata> getMetadataValue(
            @PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Updates a single meta-data value of a distribution set.
     *
     * @param distributionSetId the ID of the distribution set to update the meta data entry
     * @param metadataKey the key of the meta-data to update the value
     * @param metadata update body
     */
    @Operation(summary = "Update single meta data value of a distribution set", description = "Update a single meta " +
            "data value for speficic key. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
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
    @PutMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata/{metadataKey}",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
    ResponseEntity<Void> updateMetadata(
            @PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("metadataKey") String metadataKey,
            @RequestBody MgmtMetadataBodyPut metadata);

    /**
     * Deletes a single meta data entry from the distribution set.
     *
     * @param distributionSetId the ID of the distribution set to delete the meta data entry
     * @param metadataKey the key of the meta data to delete
     */
    @Operation(summary = "Delete a single meta data entry from the distribution set", description = "Delete a single " +
            "meta data. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/metadata/{metadataKey}")
    ResponseEntity<Void> deleteMetadata(
            @PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("metadataKey") String metadataKey);

    /**
     * Assigns a list of software modules to a distribution set.
     *
     * @param distributionSetId the ID of the distribution set to assign software modules for
     * @param softwareModuleIDs the list of software modules ids to assign
     * @return http status
     */
    @Operation(summary = "Assign a list of software modules to a distribution set", description = """
            Handles the POST request for assigning multiple software modules to a distribution set.The request body must
            always be a list of software module IDs. Required permissions: READ_REPOSITORY and UPDATE_REPOSITORY
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedSM",
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignSoftwareModules(
            @PathVariable("distributionSetId") Long distributionSetId,
            @RequestBody List<MgmtSoftwareModuleAssignment> softwareModuleIDs);

    /**
     * Deletes the assignment of the software module form the distribution set.
     *
     * @param distributionSetId the ID of the distribution set to reject the software module for
     * @param softwareModuleId the software module id to get rejected form the distribution set
     * @return status OK if rejection was successful.
     */
    @Operation(summary = "Delete the assignment of the software module from the distribution set",
            description = "Delete an assignment. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedSM/{softwareModuleId}")
    ResponseEntity<Void> deleteAssignSoftwareModules(
            @PathVariable("distributionSetId") Long distributionSetId,
            @PathVariable("softwareModuleId") Long softwareModuleId);

    /**
     * Handles the GET request for retrieving the assigned software modules of a
     * specific distribution set.
     *
     * @param distributionSetId the ID of the distribution to retrieve
     * @param pagingOffsetParam the offset of list of sets for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of the assigned software modules of a distribution set with status OK, if none is assigned than {@code null}
     */
    @Operation(summary = "Return the assigned software modules of a specific distribution set",
            description = "Handles the GET request of retrieving a single distribution set. " +
                    "Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/assignedSM",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModule>> getAssignedSoftwareModules(
            @PathVariable("distributionSetId") Long distributionSetId,
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
     * Handles the GET request of retrieving Rollouts count by Status for Distribution Set.
     *
     * @param distributionSetId the ID of the set to retrieve
     * @return a DistributionSetStatistics with status OK.
     */
    @Operation(summary = "Return Rollouts count by status for Distribution Set", description = "Handles the GET " +
            "request of retrieving Rollouts count by Status for Distribution Set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/statistics/rollouts",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetStatistics> getRolloutsCountByStatusForDistributionSet(
            @PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Handles the GET request of retrieving Actions count by Status
     * for Distribution Set.
     *
     * @param distributionSetId the ID of the set to retrieve
     * @return a DistributionSetStatistics with status OK.
     */
    @Operation(summary = "Return Actions count by status for Distribution Set", description = "Handles the GET " +
            "request of retrieving Actions count by Status for Distribution Set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/statistics/actions",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetStatistics> getActionsCountByStatusForDistributionSet(
            @PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Handles the GET request of retrieving Auto Assignments count for Distribution Set.
     *
     * @param distributionSetId the ID of the set to retrieve
     * @return a DistributionSetStatistics with status OK.
     */
    @Operation(summary = "Return Auto Assignments count for Distribution Set", description = "Handles the GET " +
            "request of retrieving Auto Assignments count for Distribution Set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/statistics/autoassignments",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetStatistics> getAutoAssignmentsCountForDistributionSet(
            @PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Handles the GET request of retrieving Rollouts, Actions and Auto Assignments counts by Status for Distribution Set.
     *
     * @param distributionSetId the ID of the set to retrieve
     * @return a DistributionSetStatistics with status OK.
     */
    @Operation(summary = "Return Rollouts, Actions and Auto Assignments counts by Status for Distribution Set",
            description = "Handles the GET request of retrieving Rollouts, Actions and Auto Assignments counts by " +
                    "Status for Distribution Set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/statistics",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetStatistics> getStatisticsForDistributionSet(@PathVariable("distributionSetId") Long distributionSetId);

    /**
     * Invalidates a distribution set
     *
     * @param distributionSetId the ID of the distribution set to invalidate
     * @param invalidateRequestBody the definition if rollouts and actions should be canceled
     * @return status OK if the invalidation was successful
     */
    @Operation(summary = "Invalidate a distribution set", description = """
            Invalidate a distribution set. Once a distribution set is invalidated, it can not be valid again. An invalidated
            distribution set cannot be assigned to targets anymore. The distribution set that is going to be invalidated
            will be removed from all auto assignments. Furthermore, the user can choose to cancel all rollouts and (force)
            cancel all actions connected to this distribution set. Required permission: UPDATE_REPOSITORY, UPDATE_TARGET
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully invalidated distribution set"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be " +
                    "changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set not found",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{distributionSetId}/invalidate",
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> invalidateDistributionSet(
            @PathVariable("distributionSetId") Long distributionSetId,
            @Valid @RequestBody MgmtInvalidateDistributionSetRequestBody invalidateRequestBody);
}
