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
import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedTargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TargetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for TargetTag CRUD operations.
 *
 */
@RequestMapping(RestConstants.TARGET_TAG_V1_REQUEST_MAPPING)
public interface TargetTagRestApi {

    /**
     * Handles the GET request of retrieving all target tags.
     *
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
     * @return a list of all target tags for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<PagedList<TagRest>> getTargetTags(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request of retrieving a single target tag.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     *
     * @return a single target tag with status OK.
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{targetTagId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TagRest> getTargetTag(@PathVariable("targetTagId") final Long targetTagId);

    /**
     * Handles the POST request of creating new target tag. The request body
     * must always be a list of tags.
     *
     * @param tags
     *            the target tags to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created. The Response Body are the created
     *         target tags but without ResponseBody.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TagsRest> createTargetTags(@RequestBody final List<TagRequestBodyPut> tags);

    /**
     *
     * Handles the PUT request of updating a single targetr tag.
     *
     * @param targetTagId
     *            the ID of the target tag
     * @param restTargetTagRest
     *            the the request body to be updated
     * @return status OK if update is successful and the updated target tag.
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{targetTagId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TagRest> updateTagretTag(@PathVariable("targetTagId") final Long targetTagId,
            @RequestBody final TagRequestBodyPut restTargetTagRest);

    /**
     * Handles the DELETE request for a single target tag.
     *
     * @param targetTagId
     *            the ID of the target tag
     * @return status OK if delete as successfully.
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{targetTagId}")
    public ResponseEntity<Void> deleteTargetTag(@PathVariable("targetTagId") final Long targetTagId);

    /**
     * Handles the GET request of retrieving all assigned targets by the given
     * tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     *
     * @return the list of assigned targets.
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    public ResponseEntity<List<TargetRest>> getAssignedTargets(@PathVariable("targetTagId") final Long targetTagId);

    /**
     * Handles the POST request to toggle the assignment of targets by the given
     * tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @param assignedTargetRequestBodies
     *            list of target ids to be toggled
     *
     * @return the list of assigned targets and unassigned targets.
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.POST, value = RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING
            + "/toggleTagAssignment")
    public ResponseEntity<TargetTagAssigmentResultRest> toggleTagAssignment(
            @PathVariable("targetTagId") final Long targetTagId,
            @RequestBody final List<AssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Handles the POST request to assign targets to the given tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @param assignedTargetRequestBodies
     *            list of target ids to be assigned
     *
     * @return the list of assigned targets.
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.POST, value = RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    public ResponseEntity<List<TargetRest>> assignTargets(@PathVariable("targetTagId") final Long targetTagId,
            @RequestBody final List<AssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Handles the DELETE request to unassign all targets from the given tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @return http status code
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    public ResponseEntity<Void> unassignTargets(@PathVariable("targetTagId") final Long targetTagId);

    /**
     * Handles the DELETE request to unassign one target from the given tag id.
     *
     * @param targetTagId
     *            the ID of the target tag
     * @param controllerId
     *            the ID of the target to unassign
     * @return http status code
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING
            + "/{controllerId}")
    public ResponseEntity<Void> unassignTarget(@PathVariable("targetTagId") final Long targetTagId,
            @PathVariable("controllerId") final String controllerId);
}
