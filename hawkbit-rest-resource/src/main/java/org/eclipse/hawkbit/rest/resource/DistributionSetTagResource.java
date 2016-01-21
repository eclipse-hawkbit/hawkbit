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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssigmentResult;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetsRest;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.DistributionSetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagPagedList;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TagsRest;
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
 * REST Resource handling for {@link DistributionSetTag} CRUD operations.
 *
 */
@RestController
@RequestMapping(RestConstants.DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING)
public class DistributionSetTagResource {
    private static final Logger LOG = LoggerFactory.getLogger(DistributionSetTagResource.class);

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private EntityManager entityManager;

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
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<DistributionSetTag> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = tagManagement.findAllDistributionSetTags(pageable);
            countTargetsAll = tagManagement.countTargetTags();

        } else {
            final Page<DistributionSetTag> findTargetPage = tagManagement.findAllDistributionSetTags(
                    RSQLUtility.parse(rsqlParam, TagFields.class, entityManager), pageable);
            countTargetsAll = findTargetPage.getTotalElements();
            findTargetsAll = findTargetPage;

        }

        final List<TagRest> rest = TagMapper.toResponseDistributionSetTag(findTargetsAll.getContent());
        return new ResponseEntity<>(new TagPagedList(rest, countTargetsAll), HttpStatus.OK);
    }

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
    public ResponseEntity<TagRest> getDistributionSetTag(@PathVariable final Long distributionsetTagId) {
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        return new ResponseEntity<>(TagMapper.toResponse(tag), HttpStatus.OK);
    }

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
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE }, produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<TagsRest> createDistributionSetTags(@RequestBody final List<TagRequestBodyPut> tags) {
        LOG.debug("creating {} ds tags", tags.size());

        final List<DistributionSetTag> createdTags = tagManagement.createDistributionSetTags(TagMapper
                .mapDistributionSetTagFromRequest(tags));

        return new ResponseEntity<>(TagMapper.toResponseDistributionSetTag(createdTags), HttpStatus.CREATED);
    }

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
    public ResponseEntity<TagRest> updateDistributionSetTag(@PathVariable final Long distributionsetTagId,
            @RequestBody final TagRequestBodyPut restDSTagRest) {
        LOG.debug("update {} ds tag", restDSTagRest);

        final DistributionSetTag distributionSetTag = findDistributionTagById(distributionsetTagId);
        TagMapper.updateTag(restDSTagRest, distributionSetTag);
        final DistributionSetTag updateDistributionSetTag = tagManagement.updateDistributionSetTag(distributionSetTag);

        LOG.debug("ds tag updated");

        return new ResponseEntity<>(TagMapper.toResponse(updateDistributionSetTag), HttpStatus.OK);
    }

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
    public ResponseEntity<Void> deleteDistributionSetTag(@PathVariable final Long distributionsetTagId) {
        LOG.debug("Delete {} distribution set tag", distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        tagManagement.deleteDistributionSetTag(tag.getName());

        return new ResponseEntity<>(HttpStatus.OK);
    }

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
    public ResponseEntity<DistributionSetsRest> getAssignedDistributionSets(
            @PathVariable final Long distributionsetTagId) {
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        return new ResponseEntity<>(
                DistributionSetMapper.toResponseDistributionSets(tag.getAssignedToDistributionSet()), HttpStatus.OK);
    }

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
            @PathVariable final Long distributionsetTagId,
            @RequestBody final List<AssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        LOG.debug("Toggle distribution set assignment {} for ds tag {}", assignedDSRequestBodies.size(),
                distributionsetTagId);

        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        final DistributionSetTagAssigmentResult assigmentResult = distributionSetManagement.toggleTagAssignment(
                findDistributionSetIds(assignedDSRequestBodies), tag.getName());

        final DistributionSetTagAssigmentResultRest tagAssigmentResultRest = new DistributionSetTagAssigmentResultRest();
        tagAssigmentResultRest.setAssignedDistributionSets(DistributionSetMapper
                .toResponseDistributionSets(assigmentResult.getAssignedDs()));
        tagAssigmentResultRest.setUnassignedDistributionSets(DistributionSetMapper
                .toResponseDistributionSets(assigmentResult.getUnassignedDs()));

        LOG.debug("Toggled assignedDS {} and unassignedDS{}", assigmentResult.getAssigned(),
                assigmentResult.getUnassigned());

        return new ResponseEntity<>(tagAssigmentResultRest, HttpStatus.OK);
    }

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
    public ResponseEntity<DistributionSetsRest> assignDistributionSets(@PathVariable final Long distributionsetTagId,
            @RequestBody final List<AssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        LOG.debug("Assign DistributionSet {} for ds tag {}", assignedDSRequestBodies.size(), distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        final List<DistributionSet> assignedDs = distributionSetManagement.assignTag(
                findDistributionSetIds(assignedDSRequestBodies), tag);
        LOG.debug("Assignd DistributionSet {}", assignedDs.size());
        return new ResponseEntity<>(DistributionSetMapper.toResponseDistributionSets(assignedDs), HttpStatus.OK);
    }

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
    public ResponseEntity<Void> unassignDistributionSets(@PathVariable final Long distributionsetTagId) {
        LOG.debug("Unassign all DS for ds tag {}", distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        if (tag.getAssignedToDistributionSet() == null) {
            LOG.debug("No assigned ds founded");
            return new ResponseEntity<>(HttpStatus.OK);
        }

        final List<DistributionSet> distributionSets = distributionSetManagement.unAssignAllDistributionSetsByTag(tag);
        LOG.debug("Unassigned ds {}", distributionSets.size());
        return new ResponseEntity<>(HttpStatus.OK);
    }

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
    public ResponseEntity<Void> unassignDistributionSet(@PathVariable final Long distributionsetTagId,
            @PathVariable final Long distributionsetId) {
        LOG.debug("Unassign ds {} for ds tag {}", distributionsetId, distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        distributionSetManagement.unAssignTag(distributionsetId, tag);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private DistributionSetTag findDistributionTagById(final Long distributionsetTagId) {
        final DistributionSetTag tag = tagManagement.findDistributionSetTagById(distributionsetTagId);
        if (tag == null) {
            throw new EntityNotFoundException("Distribution Tag with Id {" + distributionsetTagId + "} does not exist");
        }
        return tag;
    }

    private List<Long> findDistributionSetIds(
            final List<AssignedDistributionSetRequestBody> assignedDistributionSetRequestBodies) {
        return assignedDistributionSetRequestBodies.stream().map(request -> request.getDistributionSetId())
                .collect(Collectors.toList());
    }
}
