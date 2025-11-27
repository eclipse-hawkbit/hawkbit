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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.DISTRIBUTION_SET_TYPE_ORDER;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
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
 * REST Resource handling for DistributionSetType CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(
        name = "Distribution Set Types", description = "REST Resource handling for DistributionSetType CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = DISTRIBUTION_SET_TYPE_ORDER)))
public interface MgmtDistributionSetTypeRestApi {

    /**
     * Handles the GET request of retrieving all DistributionSetTypes.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of modules for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all DistributionSetType for a defined or default page request with status OK. The response is always paged. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Distribution Set Types", description = "Handles the GET request of " +
            "retrieving all distribution set types. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
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
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtDistributionSetType>> getDistributionSetTypes(
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
     * Handles the GET request of retrieving a single DistributionSetType within.
     *
     * @param distributionSetTypeId the ID of the DS type to retrieve
     * @return a single DS type with status OK.
     */
    @Operation(summary = "Return single Distribution Set Type", description = "Handles the GET request of retrieving a " +
            "single distribution set type. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetType> getDistributionSetType(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the DELETE request for a single Distribution Set Type.
     *
     * @param distributionSetTypeId the ID of the module to retrieve
     * @return status OK if delete is successful.
     */
    @Operation(summary = "Delete Distribution Set Type by Id", description = "Handles the DELETE request for a single" +
            " distribution set type. Required Permission: DELETE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}")
    ResponseEntity<Void> deleteDistributionSetType(@PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the PUT request of updating a Distribution Set Type.
     *
     * @param distributionSetTypeId the ID of the DS type in the URL
     * @param restDistributionSetType the DS type to be updated.
     * @return status OK if update is successful
     */
    @Operation(summary = "Update Distribution Set Type", description = "Handles the PUT request for a single " +
            "distribution set type. Required Permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
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
    @PutMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}",
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetType> updateDistributionSetType(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @RequestBody MgmtDistributionSetTypeRequestBodyPut restDistributionSetType);

    /**
     * Handles the POST request of creating new DistributionSetTypes. The request body must always be a list of types.
     *
     * @param distributionSetTypes the modules to be created.
     * @return In case all modules could successful created the ResponseEntity with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Create new distribution set types", description = "Handles the POST request for creating " +
            "new distribution set types. The request body must always be a list of types. " +
            "Required Permission: CREATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtDistributionSetType>> createDistributionSetTypes(
            @RequestBody List<MgmtDistributionSetTypeRequestBodyPost> distributionSetTypes);

    /**
     * Handles the GET request of retrieving the list of mandatory software module types in that distribution set type.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @Operation(summary = "Return mandatory Software Module Types in a Distribution Set Type",
            description = "Handles the GET request of retrieving the list of mandatory software module types in that " +
                    "distribution set type. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleType>> getMandatoryModules(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the GET request of retrieving the single mandatory software
     * module type in that distribution set type.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @param softwareModuleTypeId of SoftwareModuleType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @Operation(summary = "Return single mandatory Software Module Type in a Distribution Set Type",
            description = "Handles the GET request of retrieving the single mandatory software module type in that " +
                    "distribution set type. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES + "/{softwareModuleTypeId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> getMandatoryModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the GET request of retrieving the single optional software module type in that distribution set type.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @param softwareModuleTypeId of SoftwareModuleType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @Operation(summary = "Return single optional Software Module Type in a Distribution Set Type",
            description = "Handles the GET request of retrieving the single optional software module type in that " +
                    "distribution set type. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES + "/{softwareModuleTypeId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> getOptionalModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the GET request of retrieving the list of optional software module types in that distribution set type.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @Operation(summary = "Return optional Software Module Types in a Distribution Set Type",
            description = "Handles the GET request of retrieving the list of optional software module types in that " +
                    "distribution set type. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleType>> getOptionalModules(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles DELETE request for removing a mandatory module from the DistributionSetType.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @param softwareModuleTypeId of the SoftwareModuleType to remove
     * @return OK if the request was successful
     */
    @Operation(summary = "Delete a mandatory module from a Distribution Set Type",
            description = "Handles the DELETE request for removing a software module type from a single " +
                    "distribution set type. Required Permission: DELETE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully removed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES + "/{softwareModuleTypeId}")
    ResponseEntity<Void> removeMandatoryModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles DELETE request for removing an optional module from the DistributionSetType.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @param softwareModuleTypeId of the SoftwareModuleType to remove
     * @return OK if the request was successful
     */
    @Operation(summary = "Delete an optional module from a Distribution Set Type",
            description = "Handles DELETE request for removing an optional module from the distribution set type. " +
                    "Note that a DS type cannot be changed after it has been used by a DS. " +
                    "Required Permission: UPDATE_REPOSITORY and READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully removed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts " +
                    "and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES + "/{softwareModuleTypeId}")
    ResponseEntity<Void> removeOptionalModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the POST request for adding a mandatory software module type to a distribution set type.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @param smtId of the SoftwareModuleType to add
     * @return OK if the request was successful
     */
    @Operation(summary = "Add mandatory Software Module Type to a Distribution Set Type",
            description = "Handles the POST request for adding a mandatory software module type to a " +
                    "distribution set type.Note that a DS type cannot be changed after it has been used by a DS. " +
                    "Required Permission: UPDATE_REPOSITORY and READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully added"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> addMandatoryModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @RequestBody MgmtId smtId);

    /**
     * Handles the POST request for adding an optional software module type to a distribution set type.
     *
     * @param distributionSetTypeId of the DistributionSetType.
     * @param smtId of the SoftwareModuleType to add
     * @return OK if the request was successful
     */
    @Operation(summary = "Add optional Software Module Type to a Distribution Set Type",
            description = "Handles the POST request for adding an optional software module type to a " +
                    "distribution set type.Note that a DS type cannot be changed after it has been used by a DS. " +
                    "Required Permission: UPDATE_REPOSITORY and READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully added"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "The request requires user auth.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or " +
                            "data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Distribution Set Type not found.",
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
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{distributionSetTypeId}/" +
            MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> addOptionalModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @RequestBody MgmtId smtId);
}