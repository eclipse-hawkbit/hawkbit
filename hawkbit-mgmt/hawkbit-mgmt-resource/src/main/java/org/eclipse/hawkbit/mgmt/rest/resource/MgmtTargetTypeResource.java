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

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link TargetType} CRUD operations.
 */
@Slf4j
@RestController
public class MgmtTargetTypeResource implements MgmtTargetTypeRestApi {

    private final TargetTypeManagement targetTypeManagement;
    private final EntityFactory entityFactory;

    public MgmtTargetTypeResource(final TargetTypeManagement targetTypeManagement, final EntityFactory entityFactory) {
        this.targetTypeManagement = targetTypeManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    public ResponseEntity<PagedList<MgmtTargetType>> getTargetTypes(
            final int pagingOffsetParam,
            final int pagingLimitParam,
            final String sortParam,
            final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetTypeSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<TargetType> findTargetTypesAll;
        long countTargetTypesAll;
        if (rsqlParam != null) {
            findTargetTypesAll = targetTypeManagement.findByRsql(pageable, rsqlParam);
            countTargetTypesAll = ((Page<TargetType>) findTargetTypesAll).getTotalElements();
        } else {
            findTargetTypesAll = targetTypeManagement.findAll(pageable);
            countTargetTypesAll = targetTypeManagement.count();
        }

        final List<MgmtTargetType> rest = MgmtTargetTypeMapper.toListResponse(findTargetTypesAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countTargetTypesAll));
    }

    @Override
    public ResponseEntity<MgmtTargetType> getTargetType(final Long targetTypeId) {
        final TargetType foundType = findTargetTypeWithExceptionIfNotFound(targetTypeId);
        final MgmtTargetType response = MgmtTargetTypeMapper.toResponse(foundType);
        MgmtTargetTypeMapper.addLinks(response);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteTargetType(final Long targetTypeId) {
        log.debug("Delete {} target type", targetTypeId);
        targetTypeManagement.delete(targetTypeId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtTargetType> updateTargetType(
            final Long targetTypeId,
            final MgmtTargetTypeRequestBodyPut restTargetType) {
        final TargetType updated = targetTypeManagement
                .update(entityFactory.targetType().update(targetTypeId).name(restTargetType.getName())
                        .description(restTargetType.getDescription()).colour(restTargetType.getColour()));
        final MgmtTargetType response = MgmtTargetTypeMapper.toResponse(updated);
        MgmtTargetTypeMapper.addLinks(response);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtTargetType>> createTargetTypes(final List<MgmtTargetTypeRequestBodyPost> targetTypes) {

        final List<TargetType> createdTargetTypes = targetTypeManagement
                .create(MgmtTargetTypeMapper.targetFromRequest(entityFactory, targetTypes));
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtTargetTypeMapper.toListResponse(createdTargetTypes));
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSetType>> getCompatibleDistributionSets(
            final Long targetTypeId) {
        final TargetType foundType = findTargetTypeWithExceptionIfNotFound(targetTypeId);
        return ResponseEntity.ok(MgmtDistributionSetTypeMapper.toListResponse(foundType.getCompatibleDistributionSetTypes()));
    }

    @Override
    public ResponseEntity<Void> removeCompatibleDistributionSet(
            final Long targetTypeId,
            final Long distributionSetTypeId) {
        targetTypeManagement.unassignDistributionSetType(targetTypeId, distributionSetTypeId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> addCompatibleDistributionSets(
            final Long targetTypeId,
            final List<MgmtDistributionSetTypeAssignment> distributionSetTypeIds) {
        targetTypeManagement.assignCompatibleDistributionSetTypes(
                targetTypeId, distributionSetTypeIds.stream().map(MgmtDistributionSetTypeAssignment::getId).collect(Collectors.toList()));
        return ResponseEntity.ok().build();
    }

    private TargetType findTargetTypeWithExceptionIfNotFound(final Long targetTypeId) {
        return targetTypeManagement.get(targetTypeId)
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, targetTypeId));
    }
}