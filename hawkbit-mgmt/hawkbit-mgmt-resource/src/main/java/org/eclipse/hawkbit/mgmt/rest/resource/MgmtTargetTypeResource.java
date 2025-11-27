/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeTargetTypeSortParam;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetTypeMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetTypeMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement.Update;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link TargetType} CRUD operations.
 */
@Slf4j
@RestController
public class MgmtTargetTypeResource implements MgmtTargetTypeRestApi {

    private final TargetTypeManagement<? extends TargetType> targetTypeManagement;
    private final MgmtTargetTypeMapper mgmtTargetTypeMapper;

    public MgmtTargetTypeResource(
            final TargetTypeManagement<? extends TargetType> targetTypeManagement, final MgmtTargetTypeMapper mgmtTargetTypeMapper) {
        this.targetTypeManagement = targetTypeManagement;
        this.mgmtTargetTypeMapper = mgmtTargetTypeMapper;
    }

    @Override
    public ResponseEntity<PagedList<MgmtTargetType>> getTargetTypes(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeTargetTypeSortParam(sortParam));
        final Page<? extends TargetType> findTargetTypes;
        if (rsqlParam != null) {
            findTargetTypes = targetTypeManagement.findByRsql(rsqlParam, pageable);
        } else {
            findTargetTypes = targetTypeManagement.findAll(pageable);
        }
        return ResponseEntity.ok(
                new PagedList<>(MgmtTargetTypeMapper.toListResponse(findTargetTypes.getContent()),  findTargetTypes.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtTargetType> getTargetType(final Long targetTypeId) {
        final TargetType foundType = findTargetTypeWithExceptionIfNotFound(targetTypeId);
        final MgmtTargetType response = MgmtTargetTypeMapper.toResponse(foundType);
        MgmtTargetTypeMapper.addLinks(response);
        return ResponseEntity.ok(response);
    }

    @Override
    @AuditLog(entity = "TargetType", type = AuditLog.Type.DELETE, description = "Delete Target Type")
    public ResponseEntity<Void> deleteTargetType(final Long targetTypeId) {
        log.debug("Delete {} target type", targetTypeId);
        targetTypeManagement.delete(targetTypeId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MgmtTargetType> updateTargetType(final Long targetTypeId, final MgmtTargetTypeRequestBodyPut restTargetType) {
        final TargetType updated = targetTypeManagement
                .update(Update.builder().id(targetTypeId)
                        .name(restTargetType.getName()).description(restTargetType.getDescription()).colour(restTargetType.getColour())
                        .build());
        final MgmtTargetType response = MgmtTargetTypeMapper.toResponse(updated);
        MgmtTargetTypeMapper.addLinks(response);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtTargetType>> createTargetTypes(final List<MgmtTargetTypeRequestBodyPost> targetTypes) {
        final List<? extends TargetType> createdTargetTypes = targetTypeManagement.create(mgmtTargetTypeMapper.targetFromRequest(targetTypes));
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtTargetTypeMapper.toListResponse(createdTargetTypes));
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSetType>> getCompatibleDistributionSets(final Long targetTypeId) {
        final TargetType foundType = findTargetTypeWithExceptionIfNotFound(targetTypeId);
        return ResponseEntity.ok(MgmtDistributionSetTypeMapper.toListResponse(foundType.getDistributionSetTypes()));
    }

    @Override
    @AuditLog(entity = "TargetType", type = AuditLog.Type.DELETE, description = "Remove Compatible Distribution Set From Target Type")
    public ResponseEntity<Void> removeCompatibleDistributionSet(final Long targetTypeId, final Long distributionSetTypeId) {
        targetTypeManagement.unassignDistributionSetType(targetTypeId, distributionSetTypeId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @AuditLog(entity = "TargetType", type = AuditLog.Type.UPDATE, description = "Add Compatible Distribution Set To Target Type")
    public ResponseEntity<Void> addCompatibleDistributionSets(
            final Long targetTypeId, final List<MgmtDistributionSetTypeAssignment> distributionSetTypeIds) {
        targetTypeManagement.assignCompatibleDistributionSetTypes(
                targetTypeId, distributionSetTypeIds.stream().map(MgmtDistributionSetTypeAssignment::getId).toList());
        return ResponseEntity.noContent().build();
    }

    private TargetType findTargetTypeWithExceptionIfNotFound(final Long targetTypeId) {
        return targetTypeManagement.find(targetTypeId)
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, targetTypeId));
    }
}