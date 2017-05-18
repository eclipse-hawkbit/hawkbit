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

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
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
 */
@RestController
public class MgmtSoftwareModuleTypeResource implements MgmtSoftwareModuleTypeRestApi {
    @Autowired
    private SoftwareModuleTypeManagement softwareModuleTypeManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ResponseEntity<PagedList<MgmtSoftwareModuleType>> getTypes(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleTypeSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModuleType> findModuleTypessAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = softwareModuleTypeManagement.findSoftwareModuleTypesAll(rsqlParam, pageable);
            countModulesAll = ((Page<SoftwareModuleType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = softwareModuleTypeManagement.findSoftwareModuleTypesAll(pageable);
            countModulesAll = softwareModuleTypeManagement.countSoftwareModuleTypesAll();
        }

        final List<MgmtSoftwareModuleType> rest = MgmtSoftwareModuleTypeMapper
                .toTypesResponse(findModuleTypessAll.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, countModulesAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> getSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        final SoftwareModuleType foundType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toResponse(foundType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId) {
        softwareModuleTypeManagement.deleteSoftwareModuleType(softwareModuleTypeId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<MgmtSoftwareModuleType> updateSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId,
            @RequestBody final MgmtSoftwareModuleTypeRequestBodyPut restSoftwareModuleType) {

        final SoftwareModuleType updatedSoftwareModuleType = softwareModuleTypeManagement.updateSoftwareModuleType(entityFactory
                .softwareModuleType().update(softwareModuleTypeId).description(restSoftwareModuleType.getDescription())
                .colour(restSoftwareModuleType.getColour()));

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toResponse(updatedSoftwareModuleType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<MgmtSoftwareModuleType>> createSoftwareModuleTypes(
            @RequestBody final List<MgmtSoftwareModuleTypeRequestBodyPost> softwareModuleTypes) {

        final List<SoftwareModuleType> createdSoftwareModules = softwareModuleTypeManagement.createSoftwareModuleType(
                MgmtSoftwareModuleTypeMapper.smFromRequest(entityFactory, softwareModuleTypes));

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toTypesResponse(createdSoftwareModules),
                HttpStatus.CREATED);
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {
        return softwareModuleTypeManagement.findSoftwareModuleTypeById(softwareModuleTypeId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, softwareModuleTypeId));
    }

}
