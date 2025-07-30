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

import static org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility.sanitizeSoftwareModuleTypeSortParam;

import java.util.List;

import org.eclipse.hawkbit.audit.AuditLog;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleTypeMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link SoftwareModuleType} CRUD operations.
 */
@RestController
public class MgmtSoftwareModuleTypeResource implements MgmtSoftwareModuleTypeRestApi {

    private final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement;

    MgmtSoftwareModuleTypeResource(final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModuleType>> getTypes(
            final String rsqlParam, final int pagingOffsetParam, final int pagingLimitParam, final String sortParam) {
        final Pageable pageable = PagingUtility.toPageable(pagingOffsetParam, pagingLimitParam, sanitizeSoftwareModuleTypeSortParam(sortParam));
        final Slice<? extends SoftwareModuleType> findModuleTypessAll;
        final long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = softwareModuleTypeManagement.findByRsql(rsqlParam, pageable);
            countModulesAll = ((Page<?>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = softwareModuleTypeManagement.findAll(pageable);
            countModulesAll = softwareModuleTypeManagement.count();
        }

        final List<MgmtSoftwareModuleType> rest = MgmtSoftwareModuleTypeMapper.toTypesResponse(findModuleTypessAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> getSoftwareModuleType(final Long softwareModuleTypeId) {
        final SoftwareModuleType foundType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);
        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(foundType));
    }

    @Override
    @AuditLog(entity = "SoftwareModuleType", type = AuditLog.Type.DELETE, description = "Delete Software Module Type")
    public ResponseEntity<Void> deleteSoftwareModuleType(final Long softwareModuleTypeId) {
        softwareModuleTypeManagement.delete(softwareModuleTypeId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> updateSoftwareModuleType(
            final Long softwareModuleTypeId, final MgmtSoftwareModuleTypeRequestBodyPut restSoftwareModuleType) {
        final SoftwareModuleType updatedSoftwareModuleType = softwareModuleTypeManagement.update(
                SoftwareModuleTypeManagement.Update.builder().id(softwareModuleTypeId)
                        .description(restSoftwareModuleType.getDescription())
                        .colour(restSoftwareModuleType.getColour())
                        .build());

        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(updatedSoftwareModuleType));
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> createSoftwareModuleTypes(
            final List<MgmtSoftwareModuleTypeRequestBodyPost> softwareModuleTypes) {
        final List<? extends SoftwareModuleType> createdSoftwareModules = softwareModuleTypeManagement
                .create(MgmtSoftwareModuleTypeMapper.smFromRequest(softwareModuleTypes));

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toTypesResponse(createdSoftwareModules), HttpStatus.CREATED);
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {
        return softwareModuleTypeManagement.get(softwareModuleTypeId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, softwareModuleTypeId));
    }
}