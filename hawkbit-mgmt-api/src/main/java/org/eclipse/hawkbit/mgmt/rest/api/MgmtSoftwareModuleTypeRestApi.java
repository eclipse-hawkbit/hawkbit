/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPut;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for SoftwareModule and related Artifact CRUD
 * operations.
 *
 */
@RequestMapping(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING)
public interface MgmtSoftwareModuleTypeRestApi {
    /**
     * Handles the GET request of retrieving all SoftwareModuleTypes .
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
    @RequestMapping(method = RequestMethod.GET, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModuleType>> getTypes(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request of retrieving a single software module type .
     *
     * @param softwareModuleTypeId
     *            the ID of the module type to retrieve
     *
     * @return a single softwareModule with status OK.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{softwareModuleTypeId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> getSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the DELETE request for a single software module type .
     *
     * @param softwareModuleTypeId
     *            the ID of the module to retrieve
     * @return status OK if delete as successfully.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{softwareModuleTypeId}")
    ResponseEntity<Void> deleteSoftwareModuleType(@PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the PUT request of updating a software module type .
     *
     * @param softwareModuleTypeId
     *            the ID of the software module in the URL
     * @param restSoftwareModuleType
     *            the module type to be updated.
     * @return status OK if update is successful
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{softwareModuleTypeId}", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> updateSoftwareModuleType(
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId,
            MgmtSoftwareModuleTypeRequestBodyPut restSoftwareModuleType);

    /**
     * Handles the POST request of creating new SoftwareModuleTypes. The request
     * body must always be a list of types.
     *
     * @param softwareModuleTypes
     *            the modules to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleType>> createSoftwareModuleTypes(
            List<MgmtSoftwareModuleTypeRequestBodyPost> softwareModuleTypes);

}
