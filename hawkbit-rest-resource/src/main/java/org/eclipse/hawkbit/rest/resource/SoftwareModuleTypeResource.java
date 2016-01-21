/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.List;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypePagedList;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypesRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifact} CRUD operations.
 *
 *
 *
 *
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping(RestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)
@Api(value = "softwaremoduletypes", description = "Software Module Types Management API")
public class SoftwareModuleTypeResource {
    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private EntityManager entityManager;

    /**
     * Handles the GET request of retrieving all {@link SoftwareModuleType}s
     * within SP.
     *
     * @param pagingOffsetParam
     *            the offset of list of modules for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     *
     * @return a list of all module type for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = SoftwareModuleTypePagedList.class, value = "Get Software Module Types", notes = "Handles the GET request of retrieving all software module types within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public ResponseEntity<SoftwareModuleTypePagedList> getTypes(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @ApiParam(required = false, value = "FIQL syntax search query" + "<table border=0>"
                    + "<tr><td>id=1,key=os</td><td>software module types with id 1 or key os</td></tr>"
                    + "<tr><td>name=Application</td><td>software module types with name Application</td></tr>"
                    + "</table>") @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModuleType> findModuleTypessAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = softwareManagement.findSoftwareModuleTypesByPredicate(
                    RSQLUtility.parse(rsqlParam, SoftwareModuleTypeFields.class, entityManager), pageable);
            countModulesAll = ((Page<SoftwareModuleType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = softwareManagement.findSoftwareModuleTypesAll(pageable);
            countModulesAll = softwareManagement.countSoftwareModuleTypesAll();
        }

        final List<SoftwareModuleTypeRest> rest = SoftwareModuleTypeMapper.toListResponse(findModuleTypessAll
                .getContent());
        return new ResponseEntity<>(new SoftwareModuleTypePagedList(rest, countModulesAll), HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving a single software module type
     * within SP.
     *
     * @param softwareModuleTypeId
     *            the ID of the module type to retrieve
     *
     * @return a single softwareModule with status OK.
     * @throws EntityNotFoundException
     *             in case no with the given {@code softwareModuleId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleTypeId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = SoftwareModuleTypeRest.class, value = "Get Software Module Type", notes = "Handles the GET request of retrieving a single software module type within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Software Module Type", response = ExceptionInfo.class))
    public ResponseEntity<SoftwareModuleTypeRest> getSoftwareModuleType(@PathVariable final Long softwareModuleTypeId) {
        final SoftwareModuleType foundType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toResponse(foundType), HttpStatus.OK);
    }

    /**
     * Handles the DELETE request for a single software module type within SP.
     *
     * @param softwareModuleTypeId
     *            the ID of the module to retrieve
     * @return status OK if delete as sucessfull.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{softwareModuleTypeId}")
    @ApiOperation(value = "Delete Software Module Type", notes = "Handles the DELETE request for a single software module Type within SP. Required Permission: "
            + SpPermission.DELETE_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Software Module Type", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<Void> deleteSoftwareModuleType(@PathVariable final Long softwareModuleTypeId) {
        final SoftwareModuleType module = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        softwareManagement.deleteSoftwareModuleType(module);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles the PUT request of updating a software module type within SP.
     *
     * @param softwareModuleTypeId
     *            the ID of the software module in the URL
     * @param restSoftwareModuleType
     *            the module type to be updated.
     * @return status OK if update is successful
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{softwareModuleTypeId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(value = "Updates Software Module Type", notes = "Handles the PUT request for a single software module type within SP. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found Software Module", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<SoftwareModuleTypeRest> updateSoftwareModuleType(
            @PathVariable final Long softwareModuleTypeId,
            @RequestBody final SoftwareModuleTypeRequestBodyPut restSoftwareModuleType) {
        final SoftwareModuleType type = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        // only description can be modified
        if (restSoftwareModuleType.getDescription() != null) {
            type.setDescription(restSoftwareModuleType.getDescription());
        }

        final SoftwareModuleType updatedSoftwareModuleType = softwareManagement.updateSoftwareModuleType(type);

        // we flush to ensure that entity is generated and we can return ID etc.
        entityManager.flush();

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toResponse(updatedSoftwareModuleType), HttpStatus.OK);
    }

    /**
     * Handles the POST request of creating new {@link SoftwareModuleType}s
     * within SP. The request body must always be a list of types.
     *
     * @param softwareModuleTypes
     *            the modules to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE }, produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = SoftwareModuleTypesRest.class, value = "Create Software Module Types", notes = "Handles the POST request of creating new software module types within SP. The request body must always be a list of module types. Required Permission: "
            + SpPermission.CREATE_REPOSITORY)
    @ApiResponses({
            @ApiResponse(code = 404, message = "Not Found Software Module Type", response = ExceptionInfo.class),
            @ApiResponse(code = 409, message = "Conflict Software Module Type already exists", response = ExceptionInfo.class) })
    @Transactional
    public ResponseEntity<SoftwareModuleTypesRest> createSoftwareModuleTypes(
            @RequestBody final List<SoftwareModuleTypeRequestBodyPost> softwareModuleTypes) {

        final List<SoftwareModuleType> createdSoftwareModules = softwareManagement
                .createSoftwareModuleTypes(SoftwareModuleTypeMapper.smFromRequest(softwareModuleTypes));

        // we flush to ensure that entity is generated and we can return ID etc.
        entityManager.flush();

        return new ResponseEntity<>(SoftwareModuleTypeMapper.toTypesResponse(createdSoftwareModules),
                HttpStatus.CREATED);
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {
        final SoftwareModuleType module = softwareManagement.findSoftwareModuleTypeById(softwareModuleTypeId);
        if (module == null) {
            throw new EntityNotFoundException("SoftwareModuleType with Id {" + softwareModuleTypeId
                    + "} does not exist");
        }
        return module;
    }

}
