/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtDistributionSetTagAssigmentResult;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSetTag} CRUD operations.
 *
 */
@RestController
public class MgmtDistributionSetTagResource implements MgmtDistributionSetTagRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtDistributionSetTagResource.class);

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Override
    public ResponseEntity<PagedList<MgmtTag>> getDistributionSetTags(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<DistributionSetTag> findTargetsAll;
        final Long countTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = this.tagManagement.findAllDistributionSetTags(pageable);
            countTargetsAll = this.tagManagement.countTargetTags();

        } else {
            final Page<DistributionSetTag> findTargetPage = this.tagManagement.findAllDistributionSetTags(rsqlParam,
                    pageable);
            countTargetsAll = findTargetPage.getTotalElements();
            findTargetsAll = findTargetPage;

        }

        final List<MgmtTag> rest = MgmtTagMapper.toResponseDistributionSetTag(findTargetsAll.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, countTargetsAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtTag> getDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId) {
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        return new ResponseEntity<>(MgmtTagMapper.toResponse(tag), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtTag>> createDistributionSetTags(
            @RequestBody final List<MgmtTagRequestBodyPut> tags) {
        LOG.debug("creating {} ds tags", tags.size());

        final List<DistributionSetTag> createdTags = this.tagManagement
                .createDistributionSetTags(MgmtTagMapper.mapDistributionSetTagFromRequest(tagManagement, tags));

        return new ResponseEntity<>(MgmtTagMapper.toResponseDistributionSetTag(createdTags), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTag> updateDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final MgmtTagRequestBodyPut restDSTagRest) {
        LOG.debug("update {} ds tag", restDSTagRest);

        final DistributionSetTag distributionSetTag = findDistributionTagById(distributionsetTagId);
        MgmtTagMapper.updateTag(restDSTagRest, distributionSetTag);
        final DistributionSetTag updateDistributionSetTag = this.tagManagement
                .updateDistributionSetTag(distributionSetTag);

        LOG.debug("ds tag updated");

        return new ResponseEntity<>(MgmtTagMapper.toResponse(updateDistributionSetTag), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId) {
        LOG.debug("Delete {} distribution set tag", distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        this.tagManagement.deleteDistributionSetTag(tag.getName());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> getAssignedDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId) {
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        return new ResponseEntity<>(
                MgmtDistributionSetMapper.toResponseDistributionSets(tag.getAssignedToDistributionSet()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtDistributionSetTagAssigmentResult> toggleTagAssignment(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        LOG.debug("Toggle distribution set assignment {} for ds tag {}", assignedDSRequestBodies.size(),
                distributionsetTagId);

        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        final DistributionSetTagAssignmentResult assigmentResult = this.distributionSetManagement
                .toggleTagAssignment(findDistributionSetIds(assignedDSRequestBodies), tag.getName());

        final MgmtDistributionSetTagAssigmentResult tagAssigmentResultRest = new MgmtDistributionSetTagAssigmentResult();
        tagAssigmentResultRest.setAssignedDistributionSets(
                MgmtDistributionSetMapper.toResponseDistributionSets(assigmentResult.getAssignedEntity()));
        tagAssigmentResultRest.setUnassignedDistributionSets(
                MgmtDistributionSetMapper.toResponseDistributionSets(assigmentResult.getUnassignedEntity()));

        LOG.debug("Toggled assignedDS {} and unassignedDS{}", assigmentResult.getAssigned(),
                assigmentResult.getUnassigned());

        return new ResponseEntity<>(tagAssigmentResultRest, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> assignDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        LOG.debug("Assign DistributionSet {} for ds tag {}", assignedDSRequestBodies.size(), distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        final List<DistributionSet> assignedDs = this.distributionSetManagement
                .assignTag(findDistributionSetIds(assignedDSRequestBodies), tag);
        LOG.debug("Assignd DistributionSet {}", assignedDs.size());
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDistributionSets(assignedDs), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> unassignDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId) {
        LOG.debug("Unassign all DS for ds tag {}", distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        if (tag.getAssignedToDistributionSet() == null) {
            LOG.debug("No assigned ds founded");
            return new ResponseEntity<>(HttpStatus.OK);
        }

        final List<DistributionSet> distributionSets = this.distributionSetManagement
                .unAssignAllDistributionSetsByTag(tag);
        LOG.debug("Unassigned ds {}", distributionSets.size());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> unassignDistributionSet(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @PathVariable("distributionsetId") final Long distributionsetId) {
        LOG.debug("Unassign ds {} for ds tag {}", distributionsetId, distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);
        this.distributionSetManagement.unAssignTag(distributionsetId, tag);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private DistributionSetTag findDistributionTagById(final Long distributionsetTagId) {
        final DistributionSetTag tag = this.tagManagement.findDistributionSetTagById(distributionsetTagId);
        if (tag == null) {
            throw new EntityNotFoundException("Distribution Tag with Id {" + distributionsetTagId + "} does not exist");
        }
        return tag;
    }

    private List<Long> findDistributionSetIds(
            final List<MgmtAssignedDistributionSetRequestBody> assignedDistributionSetRequestBodies) {
        return assignedDistributionSetRequestBodies.stream().map(request -> request.getDistributionSetId())
                .collect(Collectors.toList());
    }
}
