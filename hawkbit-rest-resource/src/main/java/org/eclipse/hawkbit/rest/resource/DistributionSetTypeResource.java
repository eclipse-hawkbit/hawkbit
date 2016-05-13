/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.api.DistributionSetTypeRestApi;
import org.eclipse.hawkbit.rest.resource.model.IdRest;
import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRest;
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
public class DistributionSetTypeResource implements DistributionSetTypeRestApi {

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Override
    public ResponseEntity<PagedList<DistributionSetTypeRest>> getDistributionSetTypes(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetTypeSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<DistributionSetType> findModuleTypessAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = distributionSetManagement.findDistributionSetTypesAll(
                    RSQLUtility.parse(rsqlParam, DistributionSetTypeFields.class), pageable);
            countModulesAll = ((Page<DistributionSetType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = distributionSetManagement.findDistributionSetTypesAll(pageable);
            countModulesAll = distributionSetManagement.countDistributionSetTypesAll();
        }

        final List<DistributionSetTypeRest> rest = DistributionSetTypeMapper
                .toListResponse(findModuleTypessAll.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, countModulesAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DistributionSetTypeRest> getDistributionSetType(
            @PathVariable final Long distributionSetTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        return new ResponseEntity<>(DistributionSetTypeMapper.toResponse(foundType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteDistributionSetType(@PathVariable final Long distributionSetTypeId) {
        final DistributionSetType module = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        distributionSetManagement.deleteDistributionSetType(module);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DistributionSetTypeRest> updateDistributionSetType(
            @PathVariable final Long distributionSetTypeId,
            @RequestBody final DistributionSetTypeRequestBodyPut restDistributionSetType) {
        final DistributionSetType type = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        // only description can be modified
        if (restDistributionSetType.getDescription() != null) {
            type.setDescription(restDistributionSetType.getDescription());
        }

        final DistributionSetType updatedDistributionSetType = distributionSetManagement
                .updateDistributionSetType(type);

        return new ResponseEntity<>(DistributionSetTypeMapper.toResponse(updatedDistributionSetType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<DistributionSetTypeRest>> createDistributionSetTypes(
            @RequestBody final List<DistributionSetTypeRequestBodyPost> distributionSetTypes) {

        final List<DistributionSetType> createdSoftwareModules = distributionSetManagement.createDistributionSetTypes(
                DistributionSetTypeMapper.smFromRequest(softwareManagement, distributionSetTypes));

        return new ResponseEntity<>(DistributionSetTypeMapper.toTypesResponse(createdSoftwareModules),
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
    public ResponseEntity<List<SoftwareModuleTypeRest>> getMandatoryModules(
            @PathVariable final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);
        return new ResponseEntity<>(SoftwareModuleTypeMapper.toListResponse(foundType.getMandatoryModuleTypes()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SoftwareModuleTypeRest> getMandatoryModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsMandatoryModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not part of this distribution set type!");
        }

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toResponse(foundSmType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SoftwareModuleTypeRest> getOptionalModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsOptionalModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not part of this distribution set type!");
        }

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toResponse(foundSmType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<SoftwareModuleTypeRest>> getOptionalModules(
            @PathVariable final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toListResponse(foundType.getOptionalModuleTypes()),
                HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeMandatoryModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {

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
    public ResponseEntity<Void> removeOptionalModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {
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
    public ResponseEntity<Void> addMandatoryModule(@PathVariable final Long distributionSetTypeId,
            @RequestBody final IdRest smtId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType smType = findSoftwareModuleTypeWithExceptionIfNotFound(smtId.getId());

        foundType.addMandatoryModuleType(smType);

        distributionSetManagement.updateDistributionSetType(foundType);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> addOptionalModule(@PathVariable final Long distributionSetTypeId,
            @RequestBody final IdRest smtId) {

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
