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
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.RSQLUtility;
import org.eclipse.hawkbit.rest.resource.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.resource.model.IdRest;
import org.eclipse.hawkbit.rest.resource.model.action.ActionPagedList;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypePagedList;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyCreate;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyUpdate;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRest;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypesRest;
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
@RequestMapping(RestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING)
@Api(value = "distributionsettypes", description = "Distribution Set Types Management API")
public class DistributionSetTypeResource {

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private EntityManager entityManager;

    /**
     * Handles the GET request of retrieving all {@link DistributionSetType}s
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
     * @return a list of all {@link DistributionSetType} for a defined or
     *         default page request with status OK. The response is always
     *         paged. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = DistributionSetTypePagedList.class, value = "Get distribution det types", notes = "Handles the GET request of retrieving all distribution set types within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    public ResponseEntity<DistributionSetTypePagedList> getTypes(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @ApiParam(required = false, value = "FIQL syntax search query"
                    + "<table border=0>"
                    + "<tr><td>id=1,key=vhicletypex2015</td><td>Distribution Set Types with id 1 or key vhicletypex2015</td></tr>"
                    + "<tr><td>name=VehicleSeriesA</td><td>Distribution Set Types with name VehicleSeriesA</td></tr>"
                    + "</table>") @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<DistributionSetType> findModuleTypessAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = distributionSetManagement.findDistributionSetTypesByPredicate(
                    RSQLUtility.parse(rsqlParam, DistributionSetTypeFields.class, entityManager), pageable);
            countModulesAll = ((Page<DistributionSetType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = distributionSetManagement.findDistributionSetTypesAll(pageable);
            countModulesAll = distributionSetManagement.countDistributionSetTypesAll();
        }

        final List<DistributionSetTypeRest> rest = DistributionSetTypeMapper.toListResponse(findModuleTypessAll
                .getContent());
        return new ResponseEntity<>(new DistributionSetTypePagedList(rest, countModulesAll), HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving a single
     * {@link DistributionSetType} within SP.
     *
     * @param distributionSetTypeId
     *            the ID of the module type to retrieve
     *
     * @return a single softwareModule with status OK.
     * @throws EntityNotFoundException
     *             in case no with the given {@code softwareModuleId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = DistributionSetTypeRest.class, value = "Get distribution set type", notes = "Handles the GET request of retrieving a single distribution set type within SP. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not found distribution set type", response = ExceptionInfo.class))
    public ResponseEntity<DistributionSetTypeRest> getDistributionSetType(@PathVariable final Long distributionSetTypeId) {
        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        return new ResponseEntity<>(DistributionSetTypeMapper.toResponse(foundType), HttpStatus.OK);
    }

    /**
     * Handles the DELETE request for a single Distribution Set Type within SP.
     *
     * @param distributionSetTypeId
     *            the ID of the module to retrieve
     * @return status OK if delete as sucessfull.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetTypeId}")
    @ApiOperation(value = "Delete distribution set type", notes = "Handles the DELETE request for a single distribution set type within SP. Required Permission: "
            + SpPermission.DELETE_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not found distribution set type", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<Void> deleteDistributionSetType(@PathVariable final Long distributionSetTypeId) {
        final DistributionSetType module = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        distributionSetManagement.deleteDistributionSetType(module);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles the PUT request of updating a Distribution Set Type within SP.
     *
     * @param distributionSetTypeId
     *            the ID of the software module in the URL
     * @param restDistributionSetType
     *            the module type to be updated.
     * @return status OK if update is successful
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionSetTypeId}", consumes = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(value = "Update distribution set type", notes = "Handles the PUT request for a single distribution set type within SP. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found distribution set type", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<DistributionSetTypeRest> updateDistributionSetType(
            @PathVariable final Long distributionSetTypeId,
            @RequestBody final DistributionSetTypeRequestBodyUpdate restDistributionSetType) {
        final DistributionSetType type = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        // only description can be modified
        if (restDistributionSetType.getDescription() != null) {
            type.setDescription(restDistributionSetType.getDescription());
        }

        final DistributionSetType updatedDistributionSetType = distributionSetManagement
                .updateDistributionSetType(type);

        // we flush to ensure that entity is generated and we can return ID etc.
        entityManager.flush();

        return new ResponseEntity<>(DistributionSetTypeMapper.toResponse(updatedDistributionSetType), HttpStatus.OK);
    }

    /**
     * Handles the POST request of creating new {@link DistributionSetType}s
     * within SP. The request body must always be a list of types.
     *
     * @param distributionSetTypes
     *            the modules to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE }, produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = DistributionSetTypesRest.class, value = "Create distribution set types", notes = "Handles the POST request for creating new distribution set types within SP. The request body must always be a list of types. Required Permission: "
            + SpPermission.CREATE_REPOSITORY)
    @ApiResponses({
            @ApiResponse(code = 404, message = "Not Found Distribution Set Type", response = ExceptionInfo.class),
            @ApiResponse(code = 409, message = "Conflict Distribution Set Type already exists", response = ExceptionInfo.class) })
    @Transactional
    public ResponseEntity<DistributionSetTypesRest> createDistributionSetTypes(
            @RequestBody final List<DistributionSetTypeRequestBodyCreate> distributionSetTypes) {

        final List<DistributionSetType> createdSoftwareModules = distributionSetManagement
                .createDistributionSetTypes(DistributionSetTypeMapper.smFromRequest(softwareManagement,
                        distributionSetTypes));

        // we flush to ensure that entity is generated and we can return ID etc.
        entityManager.flush();

        return new ResponseEntity<>(DistributionSetTypeMapper.toTypesResponse(createdSoftwareModules),
                HttpStatus.CREATED);
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final Long distributionSetTypeId) {
        final DistributionSetType module = distributionSetManagement.findDistributionSetTypeById(distributionSetTypeId);
        if (module == null) {
            throw new EntityNotFoundException("DistributionSetType with Id {" + distributionSetTypeId
                    + "} does not exist");
        }
        return module;
    }

    /**
     * Handles the GET request of retrieving the list of mandatory software
     * module types in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES, produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = ActionPagedList.class, value = "Lists all mandatory software module types", notes = "Handles the GET request of retrieving the list of mandatory software module types in that distribution set type. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found distribution set type", response = ExceptionInfo.class))
    public ResponseEntity<SoftwareModuleTypesRest> getMandatoryModules(@PathVariable final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleTypesRest rest = new SoftwareModuleTypesRest();

        rest.addAll(SoftwareModuleTypeMapper.toListResponse(foundType.getMandatoryModuleTypes()));
        return new ResponseEntity<>(rest, HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving the single mandatory software
     * module type in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @param softwareModuleTypeId
     *            of {@link SoftwareModuleType}.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES + "/{softwareModuleTypeId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = ActionPagedList.class, value = "Retrieve mandatory software module type", notes = " Handles the GET request of retrieving the single mandatory software module type in that distribution set type. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found distribution set type", response = ExceptionInfo.class))
    public ResponseEntity<SoftwareModuleTypeRest> getMandatoryModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsMandatoryModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not part of this distribution set type!");
        }

        return new ResponseEntity<SoftwareModuleTypeRest>(SoftwareModuleTypeMapper.toResponse(foundSmType),
                HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving the single optional software module
     * type in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @param softwareModuleTypeId
     *            of {@link SoftwareModuleType}.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES + "/{softwareModuleTypeId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = ActionPagedList.class, value = "Retrieve optional software module type", notes = " Handles the GET request of retrieving the single optional software module type in that distribution set type. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found distribution set type", response = ExceptionInfo.class))
    public ResponseEntity<SoftwareModuleTypeRest> getOptionalModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(softwareModuleTypeId);

        if (!foundType.containsOptionalModuleType(foundSmType)) {
            throw new EntityNotFoundException(
                    "Software module with given ID is not part of this distribution set type!");
        }

        return new ResponseEntity<SoftwareModuleTypeRest>(SoftwareModuleTypeMapper.toResponse(foundSmType),
                HttpStatus.OK);
    }

    /**
     * Handles the GET request of retrieving the list of optional software
     * module types in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES, produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = ActionPagedList.class, value = "Lists all optional software module types", notes = "Handles the GET request of retrieving the list of optional software module types in that distribution set type. Required Permission: "
            + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found distribution set type", response = ExceptionInfo.class))
    public ResponseEntity<SoftwareModuleTypesRest> getOptionalModules(@PathVariable final Long distributionSetTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleTypesRest rest = new SoftwareModuleTypesRest();

        rest.addAll(SoftwareModuleTypeMapper.toListResponse(foundType.getOptionalModuleTypes()));
        return new ResponseEntity<>(rest, HttpStatus.OK);
    }

    /**
     * Handles DELETE request for removing a mandatory module from the
     * {@link DistributionSetType}.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @param softwareModuleTypeId
     *            of the {@link SoftwareModuleType} to remove
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES + "/{softwareModuleTypeId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = ActionPagedList.class, value = "Remove mandatory module from distribution set type", notes = "Handles the GET request of retrieving the list of software module types in that distribution set. "
            + "Note that a DS type cannot be changed after it has been used by a DS. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found distribution set type", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<Void> removeMandatoryModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        foundType.removeModuleType(softwareModuleTypeId);

        distributionSetManagement.updateDistributionSetType(foundType);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles DELETE request for removing an optional module from the
     * {@link DistributionSetType}.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @param softwareModuleTypeId
     *            of the {@link SoftwareModuleType} to remove
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES + "/{softwareModuleTypeId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = ActionPagedList.class, value = "Remove optional module from distribution set type", notes = "Handles DELETE request for removing an optional module from the distribution set type."
            + "Note that a DS type cannot be changed after it has been used by a DS. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found distribution set type", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<Void> removeOptionalModule(@PathVariable final Long distributionSetTypeId,
            @PathVariable final Long softwareModuleTypeId) {
        return removeMandatoryModule(distributionSetTypeId, softwareModuleTypeId);
    }

    /**
     * Handles the POST request for adding a mandatory software module type to a
     * distribution set type.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @param smtId
     *            of the {@link SoftwareModuleType} to add
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = Void.class, value = "Add mandatory software module type", notes = "Handles the POST request for adding a mandatory software module type to a distribution set type."
            + "Note that a DS type cannot be changed after it has been used by a DS. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found DS type or SP type", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<Void> addMandatoryModule(@PathVariable final Long distributionSetTypeId,
            @RequestBody @ApiParam(value = "Software module type ID", required = true) final IdRest smtId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType smType = findSoftwareModuleTypeWithExceptionIfNotFound(smtId.getId());

        foundType.addMandatoryModuleType(smType);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles the POST request for adding an optional software module type to a
     * distribution set type.
     *
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType}.
     * @param smtId
     *            of the {@link SoftwareModuleType} to add
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    @ApiOperation(response = Void.class, value = "Add optional software module type", notes = "Handles the POST request for adding  an optional software module type to a distribution set type."
            + "Note that a DS type cannot be changed after it has been used by a DS. Required Permission: "
            + SpPermission.UPDATE_REPOSITORY + " and " + SpPermission.READ_REPOSITORY)
    @ApiResponses(@ApiResponse(code = 404, message = "Not Found DS type or SP type", response = ExceptionInfo.class))
    @Transactional
    public ResponseEntity<Void> addOptionalModule(@PathVariable final Long distributionSetTypeId,
            @RequestBody @ApiParam(value = "Software module type ID", required = true) final IdRest smtId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(distributionSetTypeId);

        final SoftwareModuleType smType = findSoftwareModuleTypeWithExceptionIfNotFound(smtId.getId());

        foundType.addOptionalModuleType(smType);

        return new ResponseEntity<>(HttpStatus.OK);

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
