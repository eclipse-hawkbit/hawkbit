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

import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.api.TargetTagRestApi;
import org.eclipse.hawkbit.rest.resource.model.tag.AssignedTargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.tag.TagPagedList;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.tag.TagRest;
import org.eclipse.hawkbit.rest.resource.model.tag.TargetTagAssigmentResultRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link Tag} CRUD operations.
 *
 */
@RestController
public class TargetTagResource implements TargetTagRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(TargetTagResource.class);

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private TargetManagement targetManagement;

    @Override
    public ResponseEntity<TagPagedList> getTargetTags(final int pagingOffsetParam, final int pagingLimitParam,
            final String sortParam, final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<TargetTag> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = this.tagManagement.findAllTargetTags(pageable);
            countTargetsAll = this.tagManagement.countTargetTags();

        } else {
            final Page<TargetTag> findTargetPage = this.tagManagement
                    .findAllTargetTags(RSQLUtility.parse(rsqlParam, TagFields.class), pageable);
            countTargetsAll = findTargetPage.getTotalElements();
            findTargetsAll = findTargetPage;

        }

        final List<TagRest> rest = TagMapper.toResponse(findTargetsAll.getContent());
        return new ResponseEntity<>(new TagPagedList(rest, countTargetsAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<TagRest> getTargetTag(final Long targetTagId) {
        final TargetTag tag = findTargetTagById(targetTagId);
        return new ResponseEntity<>(TagMapper.toResponse(tag), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TagRest>> createTargetTags(@RequestBody final List<TagRequestBodyPut> tags) {
        LOG.debug("creating {} target tags", tags.size());
        final List<TargetTag> createdTargetTags = this.tagManagement
                .createTargetTags(TagMapper.mapTargeTagFromRequest(tags));
        return new ResponseEntity<>(TagMapper.toResponse(createdTargetTags), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<TagRest> updateTagretTag(final Long targetTagId, final TagRequestBodyPut restTargetTagRest) {
        LOG.debug("update {} target tag", restTargetTagRest);

        final TargetTag targetTag = findTargetTagById(targetTagId);
        TagMapper.updateTag(restTargetTagRest, targetTag);
        final TargetTag updateTargetTag = this.tagManagement.updateTargetTag(targetTag);

        LOG.debug("target tag updated");

        return new ResponseEntity<>(TagMapper.toResponse(updateTargetTag), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteTargetTag(final Long targetTagId) {
        LOG.debug("Delete {} target tag", targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);

        this.tagManagement.deleteTargetTag(targetTag.getName());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TargetRest>> getAssignedTargets(final Long targetTagId) {
        final TargetTag targetTag = findTargetTagById(targetTagId);
        return new ResponseEntity<>(TargetMapper.toResponseWithLinksAndPollStatus(targetTag.getAssignedToTargets()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<TargetTagAssigmentResultRest> toggleTagAssignment(final Long targetTagId,
            final List<AssignedTargetRequestBody> assignedTargetRequestBodies) {
        LOG.debug("Toggle Target assignment {} for target tag {}", assignedTargetRequestBodies.size(), targetTagId);

        final TargetTag targetTag = findTargetTagById(targetTagId);
        final TargetTagAssignmentResult assigmentResult = this.targetManagement
                .toggleTagAssignment(findTargetControllerIds(assignedTargetRequestBodies), targetTag.getName());

        final TargetTagAssigmentResultRest tagAssigmentResultRest = new TargetTagAssigmentResultRest();
        tagAssigmentResultRest.setAssignedTargets(TargetMapper.toResponse(assigmentResult.getAssignedTargets()));
        tagAssigmentResultRest.setUnassignedTargets(TargetMapper.toResponse(assigmentResult.getUnassignedTargets()));
        return new ResponseEntity<>(tagAssigmentResultRest, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<TargetRest>> assignTargets(final Long targetTagId,
            final List<AssignedTargetRequestBody> assignedTargetRequestBodies) {
        LOG.debug("Assign Targets {} for target tag {}", assignedTargetRequestBodies.size(), targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);
        final List<Target> assignedTarget = this.targetManagement
                .assignTag(findTargetControllerIds(assignedTargetRequestBodies), targetTag);
        return new ResponseEntity<>(TargetMapper.toResponseWithLinksAndPollStatus(assignedTarget), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> unassignTargets(final Long targetTagId) {
        LOG.debug("Unassign all Targets for target tag {}", targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);
        if (targetTag.getAssignedToTargets() == null) {
            LOG.debug("No assigned targets found");
            return new ResponseEntity<>(HttpStatus.OK);
        }
        this.targetManagement.unAssignAllTargetsByTag(targetTag);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> unassignTarget(final Long targetTagId, final String controllerId) {
        LOG.debug("Unassign target {} for target tag {}", controllerId, targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);
        this.targetManagement.unAssignTag(controllerId, targetTag);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private TargetTag findTargetTagById(final Long targetTagId) {
        final TargetTag tag = this.tagManagement.findTargetTagById(targetTagId);
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
