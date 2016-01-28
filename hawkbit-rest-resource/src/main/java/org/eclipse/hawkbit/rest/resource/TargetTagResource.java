/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssigmentResult;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedTargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.TagPagedList;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TargetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link Tag} CRUD operations.
 *
 */
@RestController
@RequestMapping(RestConstants.TARGET_TAG_V1_REQUEST_MAPPING)
public class TargetTagResource {
    private static final Logger LOG = LoggerFactory.getLogger(TargetTagResource.class);
    private static final String TARGET_TAG_TAGERTS_REQUEST_MAPPING = RestConstants.TARGET_TAG_TAGERTS_REQUEST_MAPPING;

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private EntityManager entityManager;

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
    public ResponseEntity<TagPagedList> getTargetTags(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<TargetTag> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = tagManagement.findAllTargetTags(pageable);
            countTargetsAll = tagManagement.countTargetTags();

        } else {
            final Page<TargetTag> findTargetPage = tagManagement
                    .findAllTargetTags(RSQLUtility.parse(rsqlParam, TagFields.class), pageable);
            countTargetsAll = findTargetPage.getTotalElements();
            findTargetsAll = findTargetPage;

        }

        final List<TagRest> rest = TagMapper.toResponse(findTargetsAll.getContent());
        return new ResponseEntity<>(new TagPagedList(rest, countTargetsAll), HttpStatus.OK);
    }

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
    public ResponseEntity<TagRest> getTargetTag(@PathVariable final Long targetTagId) {
        final TargetTag tag = findTargetTagById(targetTagId);
        return new ResponseEntity<>(TagMapper.toResponse(tag), HttpStatus.OK);
    }

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
    public ResponseEntity<TagsRest> createTargetTags(@RequestBody final List<TagRequestBodyPut> tags) {
        LOG.debug("creating {} target tags", tags.size());
        final List<TargetTag> createdTargetTags = tagManagement
                .createTargetTags(TagMapper.mapTargeTagFromRequest(tags));
        return new ResponseEntity<>(TagMapper.toResponse(createdTargetTags), HttpStatus.CREATED);
    }

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
    public ResponseEntity<TagRest> updateTagretTag(@PathVariable final Long targetTagId,
            @RequestBody final TagRequestBodyPut restTargetTagRest) {
        LOG.debug("update {} target tag", restTargetTagRest);

        final TargetTag targetTag = findTargetTagById(targetTagId);
        TagMapper.updateTag(restTargetTagRest, targetTag);
        final TargetTag updateTargetTag = tagManagement.updateTargetTag(targetTag);

        LOG.debug("target tag updated");

        return new ResponseEntity<>(TagMapper.toResponse(updateTargetTag), HttpStatus.OK);
    }

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
    public ResponseEntity<Void> deleteTargetTag(@PathVariable final Long targetTagId) {
        LOG.debug("Delete {} target tag", targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);

        tagManagement.deleteTargetTag(targetTag.getName());

        return new ResponseEntity<>(HttpStatus.OK);
    }

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
    @RequestMapping(method = RequestMethod.GET, value = TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    public ResponseEntity<TargetsRest> getAssignedTargets(@PathVariable final Long targetTagId) {
        final TargetTag targetTag = findTargetTagById(targetTagId);
        return new ResponseEntity<>(TargetMapper.toResponseWithLinksAndPollStatus(targetTag.getAssignedToTargets()),
                HttpStatus.OK);
    }

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
    @RequestMapping(method = RequestMethod.POST, value = TARGET_TAG_TAGERTS_REQUEST_MAPPING + "/toggleTagAssignment")
    public ResponseEntity<TargetTagAssigmentResultRest> toggleTagAssignment(@PathVariable final Long targetTagId,
            @RequestBody final List<AssignedTargetRequestBody> assignedTargetRequestBodies) {
        LOG.debug("Toggle Target assignment {} for target tag {}", assignedTargetRequestBodies.size(), targetTagId);

        final TargetTag targetTag = findTargetTagById(targetTagId);
        final TargetTagAssigmentResult assigmentResult = targetManagement
                .toggleTagAssignment(findTargetControllerIds(assignedTargetRequestBodies), targetTag.getName());

        final TargetTagAssigmentResultRest tagAssigmentResultRest = new TargetTagAssigmentResultRest();
        tagAssigmentResultRest.setAssignedTargets(TargetMapper.toResponse(assigmentResult.getAssignedTargets()));
        tagAssigmentResultRest.setUnassignedTargets(TargetMapper.toResponse(assigmentResult.getUnassignedTargets()));
        return new ResponseEntity<>(tagAssigmentResultRest, HttpStatus.OK);
    }

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
    @RequestMapping(method = RequestMethod.POST, value = TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    public ResponseEntity<TargetsRest> assignTargets(@PathVariable final Long targetTagId,
            @RequestBody final List<AssignedTargetRequestBody> assignedTargetRequestBodies) {
        LOG.debug("Assign Targets {} for target tag {}", assignedTargetRequestBodies.size(), targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);
        final List<Target> assignedTarget = targetManagement
                .assignTag(findTargetControllerIds(assignedTargetRequestBodies), targetTag);
        return new ResponseEntity<>(TargetMapper.toResponseWithLinksAndPollStatus(assignedTarget), HttpStatus.OK);
    }

    /**
     * Handles the DELETE request to unassign all targets from the given tag id.
     *
     * @param targetTagId
     *            the ID of the target tag to retrieve
     * @return http status code
     * @throws EntityNotFoundException
     *             in case the given {@code targetTagId} doesn't exists.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = TARGET_TAG_TAGERTS_REQUEST_MAPPING)
    public ResponseEntity<Void> unassignTargets(@PathVariable final Long targetTagId) {
        LOG.debug("Unassign all Targets for target tag {}", targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);
        if (targetTag.getAssignedToTargets() == null) {
            LOG.debug("No assigned targets found");
            return new ResponseEntity<>(HttpStatus.OK);
        }
        targetManagement.unAssignAllTargetsByTag(targetTag);
        return new ResponseEntity<>(HttpStatus.OK);
    }

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
    @RequestMapping(method = RequestMethod.DELETE, value = TARGET_TAG_TAGERTS_REQUEST_MAPPING + "/{controllerId}")
    public ResponseEntity<Void> unassignTarget(@PathVariable final Long targetTagId,
            @PathVariable final String controllerId) {
        LOG.debug("Unassign target {} for target tag {}", controllerId, targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);
        targetManagement.unAssignTag(controllerId, targetTag);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private TargetTag findTargetTagById(final Long targetTagId) {
        final TargetTag tag = tagManagement.findTargetTagById(targetTagId);
        if (tag == null) {
            throw new EntityNotFoundException("Target Tag with Id {" + targetTagId + "} does not exist");
        }
        return tag;
    }

    private List<String> findTargetControllerIds(final List<AssignedTargetRequestBody> assignedTargetRequestBodies) {
        return assignedTargetRequestBodies.stream().map(request -> request.getControllerId())
                .collect(Collectors.toList());
    }
}
