/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSetTag} CRUD operations.
 */
@Slf4j
@RestController
public class MgmtDistributionSetTagResource implements MgmtDistributionSetTagRestApi {

    private final DistributionSetTagManagement distributionSetTagManagement;
    private final DistributionSetManagement distributionSetManagement;
    private final EntityFactory entityFactory;

    MgmtDistributionSetTagResource(
            final DistributionSetTagManagement distributionSetTagManagement,
            final DistributionSetManagement distributionSetManagement, final EntityFactory entityFactory) {
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    public ResponseEntity<PagedList<MgmtTag>> getDistributionSetTags(
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
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
            final Page<DistributionSetTag> page = distributionSetTagManagement.findByRsql(rsqlParam, pageable);
            distributionSetTags = page;
            count = page.getTotalElements();
        }

        final List<MgmtTag> rest = MgmtTagMapper.toResponseDistributionSetTag(distributionSetTags.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, count));
    }

    @Override
    public ResponseEntity<MgmtTag> getDistributionSetTag(
            final Long distributionsetTagId) {
        final DistributionSetTag distributionSetTag = findDistributionTagById(distributionsetTagId);

        final MgmtTag response = MgmtTagMapper.toResponse(distributionSetTag);
        MgmtTagMapper.addLinks(distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtTag>> createDistributionSetTags(
            final List<MgmtTagRequestBodyPut> tags) {
        log.debug("creating {} ds tags", tags.size());

        final List<DistributionSetTag> createdTags = distributionSetTagManagement.create(MgmtTagMapper.mapTagFromRequest(entityFactory, tags));

        return new ResponseEntity<>(MgmtTagMapper.toResponseDistributionSetTag(createdTags), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTag> updateDistributionSetTag(
            final Long distributionsetTagId,
            final MgmtTagRequestBodyPut restDSTagRest) {

        final DistributionSetTag distributionSetTag = distributionSetTagManagement
                .update(entityFactory.tag().update(distributionsetTagId).name(restDSTagRest.getName())
                        .description(restDSTagRest.getDescription()).colour(restDSTagRest.getColour()));

        final MgmtTag response = MgmtTagMapper.toResponse(distributionSetTag);
        MgmtTagMapper.addLinks(distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSetTag(
            final Long distributionsetTagId) {
        log.debug("Delete {} distribution set tag", distributionsetTagId);
        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        distributionSetTagManagement.delete(tag.getName());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtDistributionSet>> getAssignedDistributionSets(
            final Long distributionsetTagId,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam, final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTagSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        Page<DistributionSet> findDistrAll;
        if (rsqlParam == null) {
            findDistrAll = distributionSetManagement.findByTag(distributionsetTagId, pageable);
        } else {
            findDistrAll = distributionSetManagement.findByRsqlAndTag(rsqlParam, distributionsetTagId, pageable);
        }

        final List<MgmtDistributionSet> rest = MgmtDistributionSetMapper.toResponseFromDsList(findDistrAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, findDistrAll.getTotalElements()));
    }

    @Override
    public ResponseEntity<Void> assignDistributionSet(
            final Long distributionsetTagId,
            final Long distributionsetId) {
        log.debug("Assign ds {} for ds tag {}", distributionsetId, distributionsetTagId);
        this.distributionSetManagement.assignTag(List.of(distributionsetId), distributionsetTagId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> assignDistributionSets(final Long distributionsetTagId, final List<Long> distributionsetIds) {
        log.debug("Assign DistributionSet {} for ds tag {}", distributionsetIds.size(), distributionsetTagId);
        final List<DistributionSet> assignedDs = this.distributionSetManagement.assignTag(distributionsetIds, distributionsetTagId);
        log.debug("Assigned DistributionSet {}", assignedDs.size());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unassignDistributionSet(
            final Long distributionsetTagId,
            final Long distributionsetId) {
        log.debug("Unassign ds {} for ds tag {}", distributionsetId, distributionsetTagId);
        this.distributionSetManagement.unassignTag(List.of(distributionsetId), distributionsetTagId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unassignDistributionSets(
            final Long distributionsetTagId,
            final List<Long> distributionsetIds) {
        log.debug("Unassign DistributionSet {} for ds tag {}", distributionsetIds.size(), distributionsetTagId);
        final List<DistributionSet> assignedDs = this.distributionSetManagement.unassignTag(distributionsetIds, distributionsetTagId);
        log.debug("Unassigned DistributionSet {}", assignedDs.size());
        return ResponseEntity.ok().build();
    }

    private DistributionSetTag findDistributionTagById(final Long distributionsetTagId) {
        return distributionSetTagManagement.get(distributionsetTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, distributionsetTagId));
    }
}