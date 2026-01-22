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

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeTagSortParam;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTagMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSetTag} CRUD operations.
 */
@Slf4j
@RestController
class MgmtDistributionSetTagResource implements MgmtDistributionSetTagRestApi {

    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final DistributionSetTagManagement<? extends DistributionSetTag> distributionSetTagManagement;

    MgmtDistributionSetTagResource(
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final DistributionSetTagManagement<? extends DistributionSetTag> distributionSetTagManagement) {
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public ResponseEntity<PagedList<MgmtTag>> getDistributionSetTags(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTagSortParam(sortParam));
        final Slice<? extends DistributionSetTag> distributionSetTags;
        final long count;
        if (rsqlParam == null) {
            distributionSetTags = distributionSetTagManagement.findAll(pageable);
            count = distributionSetTagManagement.count();
        } else {
            final Page<? extends DistributionSetTag> page = distributionSetTagManagement.findByRsql(rsqlParam, pageable);
            distributionSetTags = page;
            count = page.getTotalElements();
        }

        final List<MgmtTag> rest = MgmtTagMapper.toResponseDistributionSetTag(distributionSetTags.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, count));
    }

    @Override
    public ResponseEntity<MgmtTag> getDistributionSetTag(final Long distributionsetTagId) {
        final DistributionSetTag distributionSetTag = findDistributionTagById(distributionsetTagId);

        final MgmtTag response = MgmtTagMapper.toResponse(distributionSetTag);
        MgmtTagMapper.addLinks(distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtTag>> createDistributionSetTags(final List<MgmtTagRequestBodyPut> tags) {
        log.debug("creating {} ds tags", tags.size());

        final List<? extends DistributionSetTag> createdTags = distributionSetTagManagement
                .create(MgmtDistributionSetMapper.mapTagFromRequest(tags));
        return new ResponseEntity<>(MgmtTagMapper.toResponseDistributionSetTag(createdTags), HttpStatus.CREATED);
    }

    @Override
    @AuditLog(entity = "DistributionSetTag", type = AuditLog.Type.UPDATE, description = "Update Distribution Set Tag")
    public ResponseEntity<MgmtTag> updateDistributionSetTag(final Long distributionSetTagId, final MgmtTagRequestBodyPut restDSTagRest) {

        final DistributionSetTag distributionSetTag = distributionSetTagManagement
                .update(DistributionSetTagManagement.Update.builder().id(distributionSetTagId).name(restDSTagRest.getName())
                        .description(restDSTagRest.getDescription()).colour(restDSTagRest.getColour()).build());

        final MgmtTag response = MgmtTagMapper.toResponse(distributionSetTag);
        MgmtTagMapper.addLinks(distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "DistributionSetTag", type = AuditLog.Type.DELETE, description = "Delete Distribution Set Tag")
    public ResponseEntity<Void> deleteDistributionSetTag(final Long distributionsetTagId) {
        log.debug("Delete {} distribution set tag", distributionsetTagId);
        distributionSetTagManagement.delete(distributionsetTagId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtDistributionSet>> getAssignedDistributionSets(
            final Long distributionSetTagId, final String rsqlParam,
            final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTagSortParam(sortParam));
        final Page<? extends DistributionSet> distributionSets;
        if (rsqlParam == null) {
            distributionSets = distributionSetManagement.findByTag(distributionSetTagId, pageable);
        } else {
            distributionSets = distributionSetManagement.findByRsqlAndTag(rsqlParam, distributionSetTagId, pageable);
        }

        final List<MgmtDistributionSet> rest = MgmtDistributionSetMapper.toResponseFromDsList(distributionSets.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, distributionSets.getTotalElements()));
    }

    @Override
    public ResponseEntity<Void> assignDistributionSet(final Long distributionSetTagId, final Long distributionSetId) {
        log.debug("Assign ds {} for ds tag {}", distributionSetId, distributionSetTagId);
        this.distributionSetManagement.assignTag(List.of(distributionSetId), distributionSetTagId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> assignDistributionSets(final Long distributionSetTagId, final List<Long> distributionSetIds) {
        log.debug("Assign DistributionSet {} for ds tag {}", distributionSetIds.size(), distributionSetTagId);
        final List<? extends DistributionSet> assignedDs = this.distributionSetManagement.assignTag(distributionSetIds, distributionSetTagId);
        log.debug("Assigned DistributionSet {}", assignedDs.size());
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "DistributionSetTag", type = AuditLog.Type.UPDATE, description = "Unassign Distribution Set From Tag")
    public ResponseEntity<Void> unassignDistributionSet(final Long distributionsetTagId, final Long distributionsetId) {
        log.debug("Unassign ds {} for ds tag {}", distributionsetId, distributionsetTagId);
        this.distributionSetManagement.unassignTag(List.of(distributionsetId), distributionsetTagId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "DistributionSetTag", type = AuditLog.Type.UPDATE, description = "Unassign Distribution Sets From Tag")
    public ResponseEntity<Void> unassignDistributionSets(final Long distributionsetTagId, final List<Long> distributionsetIds) {
        log.debug("Unassign DistributionSet {} for ds tag {}", distributionsetIds.size(), distributionsetTagId);
        final List<? extends DistributionSet> assignedDs = this.distributionSetManagement.unassignTag(distributionsetIds, distributionsetTagId);
        log.debug("Unassigned DistributionSet {}", assignedDs.size());
        return ResponseEntity.noContent().build();
    }

    private DistributionSetTag findDistributionTagById(final Long distributionsetTagId) {
        return distributionSetTagManagement.find(distributionsetTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, distributionsetTagId));
    }
}