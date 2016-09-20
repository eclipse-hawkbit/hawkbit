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
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
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
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifact} CRUD operations.
 *
 *
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
        return new ResponseEntity<>(new PagedList<>(rest, countModulesAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtDistributionSetType> getDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        return new ResponseEntity<>(MgmtDistributionSetTypeMapper.toResponse(foundType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {
        final DistributionSetType module = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        distributionSetManagement.deleteDistributionSetType(module);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtDistributionSetType> updateDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @RequestBody final MgmtDistributionSetTypeRequestBodyPut restDistributionSetType) {
        final DistributionSetType type = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        // only description can be modified
        if (restDistributionSetType.getDescription() != null) {
            type.setDescription(restDistributionSetType.getDescription());
        }

        final DistributionSetType updatedDistributionSetType = distributionSetManagement
                .updateDistributionSetType(type);

        return new ResponseEntity<>(MgmtDistributionSetTypeMapper.toResponse(updatedDistributionSetType),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSetType>> createDistributionSetTypes(
            @RequestBody final List<MgmtDistributionSetTypeRequestBodyPost> distributionSetTypes) {

        final List<DistributionSetType> createdSoftwareModules = distributionSetManagement.createDistributionSetTypes(
                MgmtDistributionSetTypeMapper.smFromRequest(entityFactory, softwareManagement, distributionSetTypes));

        return new ResponseEntity<>(MgmtDistributionSetTypeMapper.toTypesResponse(createdSoftwareModules),
                HttpStatus.CREATED);
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final Long distributionSetTypeId) {
        final DistributionSetType module = distributionSetManagement.findDistributionSetTypeById(distributionSetTypeId);
        if (module == null) {
            throw new EntityNotFoundException(
                    "DistributionSetType with Id {" + distributionSetTypeId + "} does not exist");
        }
        return module;
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> getMandatoryModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toTypesResponse(foundType.getMandatoryModuleTypes()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> getMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsMandatoryModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not part of this distribution set type!");
        }

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toResponse(foundSmType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> getOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsOptionalModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not part of this distribution set type!");
        }

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toResponse(foundSmType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> getOptionalModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toTypesResponse(foundType.getOptionalModuleTypes()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsMandatoryModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not mandatory part of this distribution set type!");
        }

        foundType.removeModuleType(softwareModuleTypeId);

        distributionSetManagement.updateDistributionSetType(foundType);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsOptionalModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not optional part of this distribution set type!");
        }

        foundType.removeModuleType(softwareModuleTypeId);

        distributionSetManagement.updateDistributionSetType(foundType);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> addMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId, @RequestBody final MgmtId smtId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType smType = findSoftwareModuleTypeWithExceptionIfNotFound(smtId.getId());

        foundType.addMandatoryModuleType(smType);

        distributionSetManagement.updateDistributionSetType(foundType);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> addOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId, @RequestBody final MgmtId smtId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType smType = findSoftwareModuleTypeWithExceptionIfNotFound(smtId.getId());

        foundType.addOptionalModuleType(smType);

        distributionSetManagement.updateDistributionSetType(foundType);

        return new ResponseEntity<>(HttpStatus.OK);

    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {
        final SoftwareModuleType module = softwareManagement.findSoftwareModuleTypeById(softwareModuleTypeId);
        if (module == null) {
            throw new EntityNotFoundException(
                    "SoftwareModuleType with Id {" + softwareModuleTypeId + "} does not exist");
        }
        return module;
    }
}
