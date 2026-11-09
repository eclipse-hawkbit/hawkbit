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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.ACTION_STATUS_CODE_ORDER;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REST_V1;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCode;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCodeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCodeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtUndocumentedActionStatusCode;
import org.eclipse.hawkbit.rest.ApiPermission;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateResponses;
import org.eclipse.hawkbit.rest.ApiResponsesConstants.PutResponses;
import org.eclipse.hawkbit.rest.OpenApi;
import org.eclipse.hawkbit.rest.RequiredPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST API for action status code catalogue operations.
 */
@Tag(name = "Action Status Codes", description = "REST API for action status code catalogue",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = ACTION_STATUS_CODE_ORDER)))
public interface MgmtActionStatusCodeRestApi {

    String ACTION_STATUS_CODES_V1 = REST_V1 + "/action-status-codes";

    /**
     * Handles the GET request for retrieving all action status codes.
     *
     * @param rsqlParam the RSQL query string for filtering
     * @param pagingOffsetParam the offset for pagination
     * @param pagingLimitParam the limit for pagination
     * @param sortParam the sort criteria
     * @return paged list of action status codes
     */
    @Operation(summary = "Return all action status codes present in the catalogue",
            description = "Handles the GET request of retrieving all action status codes in the catalogue.")
    @GetIfExistResponses
    @RequiredPermissions({ ApiPermission.READ_TARGET })
    @GetMapping(value = ACTION_STATUS_CODES_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtActionStatusCode>> getActionStatusCodes(
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
            String sortParam
    );

    /**
     * Handles the GET request for retrieving a single action status code.
     *
     * @param id the id of the action status code
     * @return the action status code
     */
    @Operation(summary = "Return a single action status code",
            description = "Handles the GET request of retrieving a single action status code.")
    @GetResponses
    @RequiredPermissions({ ApiPermission.READ_TARGET })
    @GetMapping(value = ACTION_STATUS_CODES_V1 + "/{id}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtActionStatusCode> getActionStatusCode(@PathVariable("id") long id);

    /**
     * Handles the POST request for creating an action status code.
     *
     * @param body the request body
     * @return the created action status code with status 201
     */
    @Operation(summary = "Create a new action status code",
            description = "Handles the POST request for creating an action status code in the catalogue.")
    @PostCreateResponses
    @RequiredPermissions({ ApiPermission.CREATE_TARGET })
    @PostMapping(value = ACTION_STATUS_CODES_V1,
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtActionStatusCode> create(@Valid @RequestBody MgmtActionStatusCodeRequestBodyPost body);

    /**
     * Handles the PUT request for updating an action status code.
     *
     * @param id the id of the action status code
     * @param body the update request body
     * @return the updated action status code with status 200
     */
    @Operation(summary = "Update an action status code",
            description = "Handles the PUT request for updating an action status code. The code field is immutable.")
    @PutResponses
    @RequiredPermissions({ ApiPermission.UPDATE_TARGET })
    @PutMapping(value = ACTION_STATUS_CODES_V1 + "/{id}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtActionStatusCode> update(
            @PathVariable("id") long id,
            @RequestBody MgmtActionStatusCodeRequestBodyPut body);

    /**
     * Handles the GET request for retrieving undocumented action status codes.
     *
     * @param pagingOffsetParam the offset for pagination
     * @param pagingLimitParam the limit for pagination
     * @return paged list of undocumented action status codes
     */
    @Operation(summary = "Return undocumented action status codes",
            description = "Handles the GET request of retrieving action status codes reported on action statuses (by devices or written by the server) that have no catalogue entry. This is a read-only endpoint. To document a code, POST it to the catalogue.")
    @GetIfExistResponses
    @RequiredPermissions({ ApiPermission.READ_TARGET })
    @GetMapping(value = ACTION_STATUS_CODES_V1 + "/undocumented", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtUndocumentedActionStatusCode>> getUndocumentedActionStatusCodes(
            @RequestParam(value = REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            @Schema(description = "The paging offset (default is 0)")
            int pagingOffsetParam,
            @RequestParam(value = REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            @Schema(description = "The maximum number of entries in a page (default is 50)")
            int pagingLimitParam
    );
}
