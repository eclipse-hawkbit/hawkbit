/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtDistributionSetTypeMapper.toResponse;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtDistributionSetTypeMapper.toTypesResponse;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtSoftwareModuleTypeMapper.toResponse;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtSoftwareModuleTypeMapper.toTypesResponse;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.SoftwareModuleTypeNotInDistributionSetTypeException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

/**
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifact} CRUD operations.
 */
@RestController
public class MgmtDistributionSetTypeResource implements MgmtDistributionSetTypeRestApi {

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private EntityFactory entityFactory;

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
        Long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = distributionSetManagement.findDistributionSetTypesAll(rsqlParam, pageable);
            countModulesAll = ((Page<DistributionSetType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = distributionSetManagement.findDistributionSetTypesAll(pageable);
            countModulesAll = distributionSetManagement.countDistributionSetTypesAll();
        }

        final List<MgmtDistributionSetType> rest = MgmtDistributionSetTypeMapper
                .toListResponse(findModuleTypessAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    public ResponseEntity<MgmtDistributionSetType> getDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        return ResponseEntity.ok(toResponse(foundType));
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {
        distributionSetManagement.deleteDistributionSetType(distributionSetTypeId);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtDistributionSetType> updateDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @RequestBody final MgmtDistributionSetTypeRequestBodyPut restDistributionSetType) {

        return ResponseEntity.ok(toResponse(distributionSetManagement.updateDistributionSetType(entityFactory
                .distributionSetType().update(distributionSetTypeId)
                .description(restDistributionSetType.getDescription()).colour(restDistributionSetType.getColour()))));
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSetType>> createDistributionSetTypes(
            @RequestBody final List<MgmtDistributionSetTypeRequestBodyPost> distributionSetTypes) {

        final List<DistributionSetType> createdSoftwareModules = distributionSetManagement.createDistributionSetTypes(
                MgmtDistributionSetTypeMapper.smFromRequest(entityFactory, distributionSetTypes));

        return ResponseEntity.status(CREATED).body(toTypesResponse(createdSoftwareModules));
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final Long distributionSetTypeId) {
        return distributionSetManagement.findDistributionSetTypeById(distributionSetTypeId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, distributionSetTypeId));
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> getMandatoryModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        return ResponseEntity.ok(toTypesResponse(foundType.getMandatoryModuleTypes()));
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

        return ResponseEntity.ok(toResponse(foundSmType));
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

        return ResponseEntity.ok(toResponse(foundSmType));
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> getOptionalModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        return ResponseEntity.ok(toTypesResponse(foundType.getOptionalModuleTypes()));
    }

    @Override
    public ResponseEntity<Void> removeMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        distributionSetManagement.unassignSoftwareModuleType(distributionSetTypeId, softwareModuleTypeId);

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

        distributionSetManagement.assignMandatorySoftwareModuleTypes(distributionSetTypeId,
                Lists.newArrayList(smtId.getId()));

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> addOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId, @RequestBody final MgmtId smtId) {

        distributionSetManagement.assignOptionalSoftwareModuleTypes(distributionSetTypeId,
                Lists.newArrayList(smtId.getId()));

        return ResponseEntity.ok().build();
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {

        return softwareManagement.findSoftwareModuleTypeById(softwareModuleTypeId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, softwareModuleTypeId));
    }
}
