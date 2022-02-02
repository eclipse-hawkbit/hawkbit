/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtDistributionSetTagAssigmentResult;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for DistributionSetTag CRUD operations.
 *
 */
@RequestMapping(MgmtRestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING)
public interface MgmtDistributionSetTagRestApi {
    /**
     * Handles the GET request of retrieving all DistributionSet tags.
     *
     * @param pagingOffsetParam
     *            the offset of list of DistributionSet tags for pagination,
     *            might not be present in the rest request then default value
     *            will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return a list of all target tags for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTag>> getDistributionSetTags(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request of retrieving a single distribution set tag.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag to retrieve
     *
     * @return a single distribution set tag with status OK.
     */
    @GetMapping(value = "/{distributionsetTagId}", produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> getDistributionSetTag(@PathVariable("distributionsetTagId") Long distributionsetTagId);

    /**
     * Handles the POST request of creating new distribution set tag. The request
     * body must always be a list of tags.
     *
     * @param tags
     *            the distribution set tags to be created.
     * @return In case all modules could successful created the ResponseEntity with
     *         status code 201 - Created. The Response Body contains the created
     *         distribution set tags but without details.
     */
    @PostMapping(consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTag>> createDistributionSetTags(List<MgmtTagRequestBodyPut> tags);

    /**
     *
     * Handles the PUT request of updating a single distribution set tag.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     * @param restDSTagRest
     *            the request body to be updated
     * @return status OK if update is successful and the updated distribution set
     *         tag.
     */
    @PutMapping(value = "/{distributionsetTagId}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> updateDistributionSetTag(@PathVariable("distributionsetTagId") Long distributionsetTagId,
            MgmtTagRequestBodyPut restDSTagRest);

    /**
     * Handles the DELETE request for a single distribution set tag.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     * @return status OK if delete as successfully.
     *
     */
    @DeleteMapping(value = "/{distributionsetTagId}")
    ResponseEntity<Void> deleteDistributionSetTag(@PathVariable("distributionsetTagId") Long distributionsetTagId);

    /**
     * Handles the GET request of retrieving all assigned distribution sets by
     * the given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     * @param pagingOffsetParam
     *            the offset of list of target tags for pagination, might not be
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
     *
     * @return the list of assigned distribution sets.
     */
    @GetMapping(value = MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtDistributionSet>> getAssignedDistributionSets(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the POST request to toggle the assignment of distribution sets by
     * the given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag to retrieve
     * @param assignedDSRequestBodies
     *            list of distribution set ids to be toggled
     *
     * @return the list of assigned distribution sets and unassigned
     *         distribution sets.
     */
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING
            + "/toggleTagAssignment")
    ResponseEntity<MgmtDistributionSetTagAssigmentResult> toggleTagAssignment(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies);

    /**
     * Handles the POST request to assign distribution sets to the given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag to retrieve
     * @param assignedDSRequestBodies
     *            list of distribution sets ids to be assigned
     *
     * @return the list of assigned distribution set.
     */
    @PostMapping(value = MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtDistributionSet>> assignDistributionSets(
            @PathVariable("distributionsetTagId") Long distributionsetTagId,
            List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies);

    /**
     * Handles the DELETE request to unassign one distribution set from the
     * given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     * @param distributionsetId
     *            the ID of the distribution set to unassign
     * @return http status code
     */
    @DeleteMapping(value = MgmtRestConstants.DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING
            + "/{distributionsetId}")
    ResponseEntity<Void> unassignDistributionSet(@PathVariable("distributionsetTagId") Long distributionsetTagId,
            @PathVariable("distributionsetId") Long distributionsetId);

}
