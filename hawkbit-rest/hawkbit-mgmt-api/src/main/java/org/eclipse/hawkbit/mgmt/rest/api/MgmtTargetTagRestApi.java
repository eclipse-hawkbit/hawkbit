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
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedTargetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTargetTagAssigmentResult;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
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
 * REST Resource handling for TargetTag CRUD operations.
 *
 */
@RequestMapping(MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING)
public interface MgmtTargetTagRestApi {

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
    @GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTag>> getTargetTags(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request of retrieving a single target tag.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     *
     * @return a single target tag with status OK.
     */
    @GetMapping(value = "/{targetTagId}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> getTargetTag(@PathVariable("targetTagId") Long targetTagId);

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
    @PostMapping(consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTag>> createTargetTags(List<MgmtTagRequestBodyPut> tags);

    /**
     *
     * Handles the PUT request of updating a single targetr tag.
     *
     * @param targetTagId
     *            the ID of the target tag
     * @param restTargetTagRest
     *            the the request body to be updated
     * @return status OK if update is successful and the updated target tag.
     */
    @PutMapping(value = "/{targetTagId}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> updateTargetTag(@PathVariable("targetTagId") Long targetTagId,
            MgmtTagRequestBodyPut restTargetTagRest);

    /**
     * Handles the DELETE request for a single target tag.
     *
     * @param targetTagId
     *            the ID of the target tag
     * @return status OK if delete as successfully.
     *
     */
    @DeleteMapping(value = "/{targetTagId}")
    ResponseEntity<Void> deleteTargetTag(@PathVariable("targetTagId") Long targetTagId);

    /**
     * Handles the GET request of retrieving all assigned targets by the given
     * tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
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
     * @return the list of assigned targets.
     */
    @GetMapping(value = MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(@PathVariable("targetTagId") Long targetTagId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the POST request to toggle the assignment of targets by the given
     * tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @param assignedTargetRequestBodies
     *            list of controller ids to be toggled
     *
     * @return the list of assigned targets and unassigned targets.
     */
    @PostMapping(value = MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING + "/toggleTagAssignment", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetTagAssigmentResult> toggleTagAssignment(@PathVariable("targetTagId") Long targetTagId,
            List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Handles the POST request to assign targets to the given tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @param assignedTargetRequestBodies
     *            list of controller ids to be assigned
     *
     * @return the list of assigned targets.
     */
    @PostMapping(value = MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTarget>> assignTargets(@PathVariable("targetTagId") Long targetTagId,
            List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Handles the DELETE request to unassign one target from the given tag id.
     *
     * @param targetTagId
     *            the ID of the target tag
     * @param controllerId
     *            the ID of the target to unassign
     * @return http status code
     */
    @DeleteMapping(value = MgmtRestConstants.TARGET_TAG_TARGETS_REQUEST_MAPPING + "/{controllerId}")
    ResponseEntity<Void> unassignTarget(@PathVariable("targetTagId") Long targetTagId,
            @PathVariable("controllerId") String controllerId);
}
