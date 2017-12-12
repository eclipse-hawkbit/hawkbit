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
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private DistributionSetTagManagement distributionSetTagManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ResponseEntity<PagedList<MgmtTag>> getDistributionSetTags(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTagSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<DistributionSetTag> distributionSetTags;
        final long count;
        if (rsqlParam == null) {
            distributionSetTags = distributionSetTagManagement.findAll(pageable);
            count = distributionSetTagManagement.count();

        } else {
            final Page<DistributionSetTag> page = distributionSetTagManagement.findByRsql(pageable, rsqlParam);
            distributionSetTags = page;
            count = page.getTotalElements();

        }

        final List<MgmtTag> rest = MgmtTagMapper.toResponseDistributionSetTag(distributionSetTags.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, count));
    }

    @Override
    public ResponseEntity<MgmtTag> getDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId) {
        final DistributionSetTag distributionSetTag = findDistributionTagById(distributionsetTagId);

        final MgmtTag response = MgmtTagMapper.toResponse(distributionSetTag);
        MgmtTagMapper.addLinks(distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtTag>> createDistributionSetTags(
            @RequestBody final List<MgmtTagRequestBodyPut> tags) {
        LOG.debug("creating {} ds tags", tags.size());

        final List<DistributionSetTag> createdTags = distributionSetTagManagement
                .create(MgmtTagMapper.mapTagFromRequest(entityFactory, tags));

        return new ResponseEntity<>(MgmtTagMapper.toResponseDistributionSetTag(createdTags), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTag> updateDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final MgmtTagRequestBodyPut restDSTagRest) {

        final DistributionSetTag distributionSetTag = distributionSetTagManagement
                .update(entityFactory.tag().update(distributionsetTagId).name(restDSTagRest.getName())
                        .description(restDSTagRest.getDescription()).colour(restDSTagRest.getColour()));

        final MgmtTag response = MgmtTagMapper.toResponse(distributionSetTag);
        MgmtTagMapper.addLinks(distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSetTag(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId) {
        LOG.debug("Delete {} distribution set tag", distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        distributionSetTagManagement.delete(tag.getName());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> getAssignedDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId) {
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDistributionSets(distributionSetManagement
                .findByTag(new PageRequest(0, MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT),
                        distributionsetTagId)
                .getContent()));
    }

    @Override
    public ResponseEntity<PagedList<MgmtDistributionSet>> getAssignedDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTagSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        Page<DistributionSet> findDistrAll;
        if (rsqlParam == null) {
            findDistrAll = distributionSetManagement.findByTag(pageable, distributionsetTagId);

        } else {
            findDistrAll = distributionSetManagement.findByRsqlAndTag(pageable, rsqlParam, distributionsetTagId);
        }

        final List<MgmtDistributionSet> rest = MgmtDistributionSetMapper
                .toResponseFromDsList(findDistrAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, findDistrAll.getTotalElements()));
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

        return ResponseEntity.ok(tagAssigmentResultRest);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> assignDistributionSets(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        LOG.debug("Assign DistributionSet {} for ds tag {}", assignedDSRequestBodies.size(), distributionsetTagId);
        final List<DistributionSet> assignedDs = this.distributionSetManagement
                .assignTag(findDistributionSetIds(assignedDSRequestBodies), distributionsetTagId);
        LOG.debug("Assignd DistributionSet {}", assignedDs.size());
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDistributionSets(assignedDs));
    }

    @Override
    public ResponseEntity<Void> unassignDistributionSet(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @PathVariable("distributionsetId") final Long distributionsetId) {
        LOG.debug("Unassign ds {} for ds tag {}", distributionsetId, distributionsetTagId);
        this.distributionSetManagement.unAssignTag(distributionsetId, distributionsetTagId);
        return ResponseEntity.ok().build();
    }

    private DistributionSetTag findDistributionTagById(final Long distributionsetTagId) {
        return distributionSetTagManagement.get(distributionsetTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, distributionsetTagId));
    }

    private static List<Long> findDistributionSetIds(
            final List<MgmtAssignedDistributionSetRequestBody> assignedDistributionSetRequestBodies) {
        return assignedDistributionSetRequestBodies.stream()
                .map(MgmtAssignedDistributionSetRequestBody::getDistributionSetId).collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<MgmtDistributionSetTagAssigmentResult> toggleTagAssignmentUnpaged(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        return toggleTagAssignment(distributionsetTagId, assignedDSRequestBodies);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> assignDistributionSetsUnpaged(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @RequestBody final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        return assignDistributionSets(distributionsetTagId, assignedDSRequestBodies);
    }

    @Override
    public ResponseEntity<Void> unassignDistributionSetUnpaged(
            @PathVariable("distributionsetTagId") final Long distributionsetTagId,
            @PathVariable("distributionsetId") final Long distributionsetId) {
        return unassignDistributionSet(distributionsetTagId, distributionsetId);
    }

}
