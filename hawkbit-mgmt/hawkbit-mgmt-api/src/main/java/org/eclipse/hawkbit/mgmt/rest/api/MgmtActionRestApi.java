/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.ACTION_ORDER;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.rest.OpenApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST API providing (read-only) access to actions.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Actions", description = "REST API providing (read-only) access to actions.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = ACTION_ORDER)))
public interface MgmtActionRestApi {

    String ACTIONS_V1 = MgmtRestConstants.REST_V1 + "/actions";

    /**
     * Handles the GET request of retrieving all actions.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=distributionSet.id==1}
     * @param pagingOffsetParam the offset of list of actions for pagination, might not be present in the rest request then default value will
     *         be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @param representationModeParam the representation mode parameter specifying whether a compact or a full representation shall be returned
     * @return a list of all actions for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all actions", description = "Handles the GET request of retrieving all actions.")
    @GetIfExistResponses
    @GetMapping(value = ACTIONS_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtAction>> getActions(
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
            @Schema(description = """
                    The representation mode. Can be "full" or "compact". Defaults to "compact"
                    """)
            String representationModeParam);

    /**
     * Handles the GET request of retrieving a specific {@link MgmtAction} by its <code>actionId</code>.
     *
     * @param actionId The ID of the requested action
     * @return the {@link MgmtAction}
     */
    @Operation(summary = "Return action by id", description = "Handles the GET request of retrieving a single action by actionId.")
    @GetResponses
    @GetMapping(value = ACTIONS_V1 + "/{actionId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAction> getAction(@PathVariable("actionId") Long actionId);

    /**
     * Handles the DELETE request for single action
     *
     * @param actionId - the id of action about to be deleted
     * @return status OK if delete as successful.
     */
    @Operation(summary = "Delete a single action by id", description = "Handles the DELETE request for single action within Bosch IoT Rollouts. Required Permission: DELETE_REPOSITORY")
    @DeleteResponses
    @DeleteMapping(value = ACTIONS_V1 + "/{actionId}")
    ResponseEntity<Void> deleteAction(@PathVariable("actionId") Long actionId);

    /**
     * Handles the DELETE request for multiple actions
     *
     * @param actionIds - list of action ids to be deleted
     * @param rsqlParam - rsql filter matching actions to be deleted
     * @return status OK if delete as successful.
     */
    @Operation(summary = "Delete multiple actions by list OR rsql filter", description = "Handles the DELETE request for multiple actions within Bosch IoT Rollouts. Either action id list OR rsql filter SHOULD be provided. Required Permission: DELETE_REPOSITORY")
    @DeleteResponses
    @DeleteMapping(value = ACTIONS_V1)
    ResponseEntity<Void> deleteActions(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false, defaultValue = "")
            @Schema(description = "Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for available fields.")
            String rsqlParam,
            @Schema(description = "List of action ids to be deleted", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "[253, 255]")
            @RequestBody(required = false)
            List<Long> actionIds);
}