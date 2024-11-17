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

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.SoftwareModuleTypeNotInDistributionSetTypeException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
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
 * REST Resource handling for {@link DistributionSetType} CRUD operations.
 */
@RestController
public class MgmtDistributionSetTypeResource implements MgmtDistributionSetTypeRestApi {

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;
    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final EntityFactory entityFactory;

    MgmtDistributionSetTypeResource(
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement, final EntityFactory entityFactory) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    public ResponseEntity<PagedList<MgmtDistributionSetType>> getDistributionSetTypes(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetTypeSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<DistributionSetType> findModuleTypessAll;
        long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = distributionSetTypeManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = ((Page<DistributionSetType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = distributionSetTypeManagement.findAll(pageable);
            countModulesAll = distributionSetTypeManagement.count();
        }

        final List<MgmtDistributionSetType> rest = MgmtDistributionSetTypeMapper.toListResponse(findModuleTypessAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    public ResponseEntity<MgmtDistributionSetType> getDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final MgmtDistributionSetType response = MgmtDistributionSetTypeMapper.toResponse(foundType);
        MgmtDistributionSetTypeMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSetType(@PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {
        distributionSetTypeManagement.delete(distributionSetTypeId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtDistributionSetType> updateDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @RequestBody final MgmtDistributionSetTypeRequestBodyPut restDistributionSetType) {
        final DistributionSetType updated = distributionSetTypeManagement.update(entityFactory.distributionSetType()
                .update(distributionSetTypeId).description(restDistributionSetType.getDescription())
                .colour(restDistributionSetType.getColour()));

        final MgmtDistributionSetType response = MgmtDistributionSetTypeMapper.toResponse(updated);
        MgmtDistributionSetTypeMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSetType>> createDistributionSetTypes(
            @RequestBody final List<MgmtDistributionSetTypeRequestBodyPost> distributionSetTypes) {
        final List<DistributionSetType> createdSoftwareModules = distributionSetTypeManagement
                .create(MgmtDistributionSetTypeMapper.smFromRequest(entityFactory, distributionSetTypes));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MgmtDistributionSetTypeMapper.toListResponse(createdSoftwareModules));
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> getMandatoryModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toTypesResponse(foundType.getMandatoryModuleTypes()));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> getMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsMandatoryModuleType(foundSmType)) {
            throw new SoftwareModuleTypeNotInDistributionSetTypeException(softwareModuleTypeId, distributionSetTypeId);
        }

        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(foundSmType));
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> getOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsOptionalModuleType(foundSmType)) {
            throw new SoftwareModuleTypeNotInDistributionSetTypeException(softwareModuleTypeId, distributionSetTypeId);
        }

        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(foundSmType));
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> getOptionalModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toTypesResponse(foundType.getOptionalModuleTypes()));
    }

    @Override
    public ResponseEntity<Void> removeMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        distributionSetTypeManagement.unassignSoftwareModuleType(distributionSetTypeId, softwareModuleTypeId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> removeOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        return removeMandatoryModule(distributionSetTypeId, softwareModuleTypeId);
    }

    @Override
    public ResponseEntity<Void> addMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId, @RequestBody final MgmtId smtId) {
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(distributionSetTypeId, Collections.singletonList(smtId.getId()));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> addOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId, @RequestBody final MgmtId smtId) {
        distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(distributionSetTypeId, Collections.singletonList(smtId.getId()));

        return ResponseEntity.ok().build();
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final Long distributionSetTypeId) {
        return distributionSetTypeManagement.get(distributionSetTypeId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, distributionSetTypeId));
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {
        return softwareModuleTypeManagement.get(softwareModuleTypeId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, softwareModuleTypeId));
    }
}