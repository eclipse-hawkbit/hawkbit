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

import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.DISTRIBUTION_SET_TAG_ORDER;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT;
import static org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.DeleteResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetIfExistResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.GetResponses;
import static org.eclipse.hawkbit.rest.ApiResponsesConstants.PostCreateNoContentResponses;
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
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
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
 * REST API for DistributionSetTag CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Distribution Set Tags", description = "REST Resource handling for DistributionSetTag CRUD operations.",
        extensions = @Extension(name = OpenApi.X_HAWKBIT, properties = @ExtensionProperty(name = "order", value = DISTRIBUTION_SET_TAG_ORDER)))
public interface MgmtDistributionSetTagRestApi {

    String DISTRIBUTIONSETTAGS_V1 = MgmtRestConstants.REST_V1 + "/distributionsettags";
    String DISTRIBUTIONSET_TAG_ID_ASSIGNED = "/{distributionsetTagId}/assigned";

    /**
     * Handles the GET request of retrieving all DistributionSet tags.
     *
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of DistributionSet tags for pagination, might not be present in the rest request then default
     *         value will be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return a list of all target tags for a defined or default page request with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Distribution Set Tags",
            description = "Handles the GET request of retrieving all distribution set tags.")
    @GetIfExistResponses
    @GetMapping(value = DISTRIBUTIONSETTAGS_V1, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTag>> getDistributionSetTags(
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
     * Handles the GET request of retrieving a single distribution set tag.
     *
     * @param distributionsetTagId the ID of the distribution set tag to retrieve
     * @return a single distribution set tag with status OK.
     */
    @Operation(summary = "Return single Distribution Set Tag",
            description = "Handles the GET request of retrieving a single distribution set tag.")
    @GetResponses
    @GetMapping(value = DISTRIBUTIONSETTAGS_V1 + "/{distributionsetTagId}", produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> getDistributionSetTag(@PathVariable("distributionsetTagId") Long distributionsetTagId);

    /**
     * Handles the POST request of creating new distribution set tag. The request body must always be a list of tags.
     *
     * @param tags the distribution set tags to be created.
     * @return In case all modules could successful created the ResponseEntity with status code 201 - Created. The Response Body contains the
     *         created distribution set tags but without details.
     */
    @Operation(summary = "Creates new Distribution Set Tags", description = "Handles the POST request of creating " +
            "new distribution set tag. The request body must always be a list of distribution set tags.")
    @PostCreateResponses
    @PostMapping(value = DISTRIBUTIONSETTAGS_V1,
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTag>> createDistributionSetTags(@RequestBody List<MgmtTagRequestBodyPut> tags);

    /**
     * Handles the PUT request of updating a single distribution set tag.
     *
     * @param distributionsetTagId the ID of the distribution set tag
     * @param restDSTagRest the request body to be updated
     * @return status OK if update is successful and the updated distribution set tag.
     */
    @Operation(summary = "Update Distribution Set Tag",
            description = "Handles the PUT request of updating a distribution set tag.")
    @PutResponses
    @PutMapping(value = DISTRIBUTIONSETTAGS_V1 + "/{distributionsetTagId}",
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> updateDistributionSetTag(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @RequestBody MgmtTagRequestBodyPut restDSTagRest);

    /**
     * Handles the DELETE request for a single distribution set tag.
     *
     * @param distributionsetTagId the ID of the distribution set tag
     * @return status OK if delete as successfully.
     */
    @Operation(summary = "Delete a single distribution set tag",
            description = "Handles the DELETE request of deleting a single distribution set tag.")
    @DeleteResponses
    @DeleteMapping(value = DISTRIBUTIONSETTAGS_V1 + "/{distributionsetTagId}")
    ResponseEntity<Void> deleteDistributionSetTag(@PathVariable("distributionsetTagId") Long distributionsetTagId);

    /**
     * Handles the GET request of retrieving all assigned distribution sets by the given tag id.
     *
     * @param distributionsetTagId the ID of the distribution set tag
     * @param rsqlParam the search parameter in the request URL, syntax {@code q=name==abc}
     * @param pagingOffsetParam the offset of list of target tags for pagination, might not be present in the rest request then default value
     *         will be applied
     * @param pagingLimitParam the limit of the paged request, might not be present in the rest request then default value will be applied
     * @param sortParam the sorting parameter in the request URL, syntax {@code field:direction, field:direction}
     * @return the list of assigned distribution sets.
     */
    @Operation(summary = "Return all assigned distribution sets by given tag Id",
            description = "Handles the GET request of retrieving a list of assigned distributions.")
    @GetResponses
    @GetMapping(value = DISTRIBUTIONSETTAGS_V1 + DISTRIBUTIONSET_TAG_ID_ASSIGNED, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtDistributionSet>> getAssignedDistributionSets(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
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
     * Handles the POST request to assign distribution sets to the given tag id.
     *
     * @param distributionsetTagId the ID of the distribution set tag to retrieve
     * @param distributionsetId the distribution sets ids to be assigned
     * @return the list of assigned distribution set.
     */
    @Operation(summary = "Assign distribution set to the given tag id",
            description = "Handles the POST request of distribution assignment. Already assigned distribution will be ignored.")
    @PostCreateNoContentResponses
    @PostMapping(value = DISTRIBUTIONSETTAGS_V1 + DISTRIBUTIONSET_TAG_ID_ASSIGNED + "/{distributionsetId}")
    ResponseEntity<Void> assignDistributionSet(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @PathVariable("distributionsetId") Long distributionsetId);

    /**
     * Handles the POST request to assign distribution sets to the given tag id.
     *
     * @param distributionsetTagId the ID of the distribution set tag to retrieve
     * @param distributionsetIds list of distribution sets ids to be assigned
     * @return the list of assigned distribution set.
     */
    @Operation(summary = "Assign distribution sets to the given tag id",
            description = "Handles the POST request of distribution assignment. Already assigned distribution will be ignored.")
    @PostUpdateNoContentResponses
    @PostMapping(value = DISTRIBUTIONSETTAGS_V1 + DISTRIBUTIONSET_TAG_ID_ASSIGNED,
            consumes = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE }, produces = { HAL_JSON_VALUE, APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignDistributionSets(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @RequestBody List<Long> distributionsetIds);

    /**
     * Handles the DELETE request to unassign one distribution set from the given tag id.
     *
     * @param distributionsetTagId the ID of the distribution set tag
     * @param distributionsetId the ID of the distribution set to unassign
     * @return http status code
     */
    @Operation(summary = "Unassign one distribution set from the given tag id",
            description = "Handles the DELETE request of unassign the given distribution.")
    @DeleteResponses
    @DeleteMapping(value = DISTRIBUTIONSETTAGS_V1 + DISTRIBUTIONSET_TAG_ID_ASSIGNED + "/{distributionsetId}")
    ResponseEntity<Void> unassignDistributionSet(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @PathVariable("distributionsetId") Long distributionsetId);

    /**
     * Handles the DELETE request to unassign one distribution set from the given tag id.
     *
     * @param distributionsetTagId the ID of the distribution set tag
     * @param distributionsetIds the IDs of the distribution set to unassign
     * @return http status code
     */
    @Operation(summary = "Unassign multiple distribution sets from the given tag id",
            description = "Handles the DELETE request of unassign the given distribution.")
    @DeleteResponses
    @DeleteMapping(value = DISTRIBUTIONSETTAGS_V1 + DISTRIBUTIONSET_TAG_ID_ASSIGNED)
    ResponseEntity<Void> unassignDistributionSets(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @RequestBody List<Long> distributionsetIds);
}