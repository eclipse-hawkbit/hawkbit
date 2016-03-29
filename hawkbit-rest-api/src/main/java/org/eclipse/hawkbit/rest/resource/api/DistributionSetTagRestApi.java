/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.api;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.RestConstants;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRest;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.DistributionSetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagPagedList;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for DistributionSetTag CRUD operations.
 *
 */
@RequestMapping(RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING)
public interface DistributionSetTagRestApi {
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
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TagPagedList> getDistributionSetTags(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request of retrieving a single distribution set tag.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag to retrieve
     *
     * @return a single distribution set tag with status OK.
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionsetTagId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TagRest> getDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId);

    /**
     * Handles the POST request of creating new distribution set tag. The
     * request body must always be a list of tags.
     *
     * @param tags
     *            the distribution set tags to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created. The Response Body are the created
     *         distribution set tags but without ResponseBody.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<List<TagRest>> createDistributionSetTags(@RequestBody final List<TagRequestBodyPut> tags);

    /**
     *
     * Handles the PUT request of updating a single distribution set tag.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     * @param restDSTagRest
     *            the the request body to be updated
     * @return status OK if update is successful and the updated distribution
     *         set tag.
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionsetTagId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TagRest> updateDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final TagRequestBodyPut restDSTagRest);

    /**
     * Handles the DELETE request for a single distribution set tag.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     * @return status OK if delete as successfully.
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionsetTagId}")
    public ResponseEntity<Void> deleteDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId);

    /**
     * Handles the GET request of retrieving all assigned distribution sets by
     * the given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     *
     * @return the list of assigned distribution sets.
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING)
    public ResponseEntity<List<DistributionSetRest>> getAssignedDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId);

    /**
     * Handles the POST request to toggle the assignment of distribution sets by
     * the given tag id.
     *
     * @param distributionsetTagIds
     *            the ID of the distribution set tag to retrieve
     * @param assignedDSRequestBodies
     *            list of distribution set ids to be toggled
     *
     * @return the list of assigned distribution sets and unassigned
     *         distribution sets.
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     */
    @RequestMapping(method = RequestMethod.POST, value = RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING
            + "/toggleTagAssignment")
    public ResponseEntity<DistributionSetTagAssigmentResultRest> toggleTagAssignment(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final List<AssignedDistributionSetRequestBody> assignedDSRequestBodies);

    /**
     * Handles the POST request to assign distribution sets to the given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag to retrieve
     * @param assignedDSRequestBodies
     *            list of distribution sets ids to be assigned
     *
     * @return the list of assigned distribution set.
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     */
    @RequestMapping(method = RequestMethod.POST, value = RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING)
    public ResponseEntity<List<DistributionSetRest>> assignDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final List<AssignedDistributionSetRequestBody> assignedDSRequestBodies);

    /**
     * Handles the DELETE request to unassign all distribution set from the
     * given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag to retrieve
     * @return http status code
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING)
    public ResponseEntity<Void> unassignDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId);

    /**
     * Handles the DELETE request to unassign one distribution set from the
     * given tag id.
     *
     * @param distributionsetTagId
     *            the ID of the distribution set tag
     * @param distributionsetId
     *            the ID of the distribution set to unassign
     * @return http status code
     * @throws EntityNotFoundException
     *             in case the given {@code distributionsetTagId} doesn't
     *             exists.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = RestConstants.DISTRIBUTIONSET_REQUEST_MAPPING
            + "/{distributionsetId}")
    public ResponseEntity<Void> unassignDistributionSet(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @PathVariable("distributionsetId") final Long distributionsetId);
}
