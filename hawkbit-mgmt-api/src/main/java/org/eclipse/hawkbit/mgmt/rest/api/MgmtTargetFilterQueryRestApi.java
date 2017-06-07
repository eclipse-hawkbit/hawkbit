/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Api for handling target operations.
 */
@RequestMapping(MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING)
public interface MgmtTargetFilterQueryRestApi {

    /**
     * Handles the GET request of retrieving a single target filter.
     *
     * @param filterId
     *            the ID of the target filter to retrieve
     * @return a single target with status OK.
     */

    @RequestMapping(method = RequestMethod.GET, value = "/{filterId}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> getFilter(@PathVariable("filterId") Long filterId);

    /**
     * Handles the GET request of retrieving all filters.
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

    @RequestMapping(method = RequestMethod.GET, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTargetFilterQuery>> getFilters(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the POST request of creating new target filters. The request body
     * must always be a list of target filters.
     *
     * @param filter
     *            the filters to be created.
     * @return In case all filters were successfully created the ResponseEntity
     *         with status code 201 with a list of successfully created entities
     *         is returned. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> createFilter(@RequestBody MgmtTargetFilterQueryRequestBody filter);

    /**
     * Handles the PUT request of updating a target filter. The ID is within the
     * URL path of the request. A given ID in the request body is ignored. It's
     * not possible to set fields to {@code null} values.
     *
     * @param filterId
     *            the path parameter which contains the ID of the target filter
     * @param targetFilterRest
     *            the request body which contains the fields which should be
     *            updated, fields which are not given are ignored for the
     *            update.
     * @return the updated target filter response which contains all fields
     *         including fields which have not been updated
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{filterId}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> updateFilter(@PathVariable("filterId") Long filterId,
            @RequestBody MgmtTargetFilterQueryRequestBody targetFilterRest);

    /**
     * Handles the DELETE request of deleting a target filter.
     *
     * @param filterId
     *            the ID of the target filter to be deleted
     * @return If the given controllerId could exists and could be deleted Http
     *         OK. In any failure the JsonResponseExceptionHandler is handling
     *         the response.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{filterId}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deleteFilter(@PathVariable("filterId") Long filterId);

    /**
     * Handles the GET request of retrieving the distribution set for auto
     * assignment of an specific target filter.
     *
     * @param filterId
     *            the ID of the target to retrieve the assigned distribution
     * @return the assigned distribution set with status OK, if none is assigned
     *         than {@code null} content (e.g. "{}")
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{filterId}/autoAssignDS", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(@PathVariable("filterId") Long filterId);

    /**
     * Handles the POST request for changing distribution set for auto
     * assignment of a target filter.
     *
     * @param filterId
     *            of the target to change
     * @param dsId
     *            of the Id of the auto assign distribution set
     * @return http status
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{filterId}/autoAssignDS", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> postAssignedDistributionSet(@PathVariable("filterId") Long filterId,
            @RequestBody MgmtId dsId);

    /**
     * Handles the DELETE request for removing the distribution set for auto
     * assignment of a target filter.
     *
     * @param filterId
     *            of the target to change
     * @return http status
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{filterId}/autoAssignDS")
    ResponseEntity<Void> deleteAssignedDistributionSet(@PathVariable("filterId") Long filterId);

}
