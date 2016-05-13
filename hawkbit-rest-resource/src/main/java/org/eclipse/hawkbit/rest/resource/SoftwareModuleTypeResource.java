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

import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.api.SoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifact} CRUD operations.
 *
 */
@RestController
public class SoftwareModuleTypeResource implements SoftwareModuleTypeRestApi {
    @Autowired
    private SoftwareManagement softwareManagement;

    @Override
    public ResponseEntity<PagedList<SoftwareModuleTypeRest>> getTypes(final int pagingOffsetParam,
            final int pagingLimitParam, final String sortParam, final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleTypeSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModuleType> findModuleTypessAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = this.softwareManagement.findSoftwareModuleTypesByPredicate(
                    RSQLUtility.parse(rsqlParam, SoftwareModuleTypeFields.class), pageable);
            countModulesAll = ((Page<SoftwareModuleType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = this.softwareManagement.findSoftwareModuleTypesAll(pageable);
            countModulesAll = this.softwareManagement.countSoftwareModuleTypesAll();
        }

        final List<SoftwareModuleTypeRest> rest = SoftwareModuleTypeMapper
                .toListResponse(findModuleTypessAll.getContent());
        return new ResponseEntity<>(new PagedList<>(rest, countModulesAll), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SoftwareModuleTypeRest> getSoftwareModuleType(final Long softwareModuleTypeId) {
        final SoftwareModuleType foundType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toResponse(foundType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteSoftwareModuleType(final Long softwareModuleTypeId) {
        final SoftwareModuleType module = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        this.softwareManagement.deleteSoftwareModuleType(module);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SoftwareModuleTypeRest> updateSoftwareModuleType(final Long softwareModuleTypeId,
            final SoftwareModuleTypeRequestBodyPut restSoftwareModuleType) {
        final SoftwareModuleType type = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        // only description can be modified
        if (restSoftwareModuleType.getDescription() != null) {
            type.setDescription(restSoftwareModuleType.getDescription());
        }

        final SoftwareModuleType updatedSoftwareModuleType = this.softwareManagement.updateSoftwareModuleType(type);
        return new ResponseEntity<>(SoftwareModuleTypeMapper.toResponse(updatedSoftwareModuleType), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<SoftwareModuleTypeRest>> createSoftwareModuleTypes(
            final List<SoftwareModuleTypeRequestBodyPost> softwareModuleTypes) {

        final List<SoftwareModuleType> createdSoftwareModules = this.softwareManagement
                .createSoftwareModuleType(SoftwareModuleTypeMapper.smFromRequest(softwareModuleTypes));

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toTypesResponse(createdSoftwareModules),
                HttpStatus.CREATED);
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {
        final SoftwareModuleType module = this.softwareManagement.findSoftwareModuleTypeById(softwareModuleTypeId);
        if (module == null) {
            throw new EntityNotFoundException(
                    "SoftwareModuleType with Id {" + softwareModuleTypeId + "} does not exist");
        }
        return module;
    }

}
