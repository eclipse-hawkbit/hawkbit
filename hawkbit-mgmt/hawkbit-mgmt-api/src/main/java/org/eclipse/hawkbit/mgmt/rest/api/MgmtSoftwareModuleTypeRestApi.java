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
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.SOFTWARE_MODULE_TYPE_ORDER;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.NOT_FOUND_404;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPut;
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
 * REST API for SoftwareModule and related Artifact CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Software Module Types", description = "REST API for SoftwareModuleTypes CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = SOFTWARE_MODULE_TYPE_ORDER)))
public interface MgmtSoftwareModuleTypeRestApi {

    String SOFTWAREMODULETYPES_V1 = MgmtRestConstants.REST_V1 + "/softwaremoduletypes";

    /**
     * Handles the GET request of retrieving all SoftwareModuleTypes .
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of modules for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all module type for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Software Module Types",
            description = "Handles the GET request of retrieving all software module types. Required Permission: READ_REPOSITORY")
    @GetIfExistResponses
    @GetMapping(value = SOFTWAREMODULETYPES_V1, produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModuleType>> getTypes(
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
     * Handles the GET request of retrieving a single software module type .
     *
     * @param softwareModuleTypeId the ID of the module type to retrieve
     * @return a single softwareModule with status OK.
     */
    @Operation(summary = "Return single Software Module Type",
            description = "Handles the GET request of retrieving a single software module type. Required Permission: READ_REPOSITORY")
    @GetResponses
    @GetMapping(value = SOFTWAREMODULETYPES_V1 + "/{softwareModuleTypeId}",
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> getSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the DELETE request for a single software module type .
     *
     * @param softwareModuleTypeId the ID of the module to retrieve
     * @return status OK if delete as successfully.
     */
    @Operation(summary = "Delete Software Module Type by Id",
            description = "Handles the DELETE request for a single software module type. Required Permission: DELETE_REPOSITORY")
    @DeleteResponses
    @DeleteMapping(value = SOFTWAREMODULETYPES_V1 + "/{softwareModuleTypeId}")
    ResponseEntity<Void> deleteSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the PUT request of updating a software module type .
     *
     * @param softwareModuleTypeId the ID of the software module in the URL
     * @param restSoftwareModuleType the module type to be updated.
     * @return status OK if update is successful
     */
    @Operation(summary = "Update Software Module Type",
            description = "Handles the PUT request for a single software module type. Required Permission: UPDATE_REPOSITORY")
    @PutResponses
    @PutMapping(value = SOFTWAREMODULETYPES_V1 + "/{softwareModuleTypeId}",
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> updateSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId,
            @RequestBody MgmtSoftwareModuleTypeRequestBodyPut restSoftwareModuleType);

    /**
     * Handles the POST request of creating new SoftwareModuleTypes. The request body must always be a list of types.
     *
     * @param softwareModuleTypes the modules to be created.
     * @return In case all modules could successful created the ResponseEntity with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Creates new Software Module Types",
            description = "Handles the POST request of creating new software module types. The request body must " +
                    "always be a list of module types. Required Permission: CREATE_REPOSITORY")
    @PostCreateResponses
    @ApiResponses(value = {
            @ApiResponse(responseCode = NOT_FOUND_404, description = "Software Module not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = SOFTWAREMODULETYPES_V1,
            consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleType>> createSoftwareModuleTypes(
            @RequestBody List<MgmtSoftwareModuleTypeRequestBodyPost> softwareModuleTypes);
}