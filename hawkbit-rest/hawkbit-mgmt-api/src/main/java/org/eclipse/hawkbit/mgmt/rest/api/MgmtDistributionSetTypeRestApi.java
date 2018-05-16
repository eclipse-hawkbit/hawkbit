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

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
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
@RequestMapping(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING)
public interface MgmtDistributionSetTypeRestApi {

    /**
     * Handles the GET request of retrieving all DistributionSetTypes.
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
     * @return a list of all DistributionSetType for a defined or default page
     *         request with status OK. The response is always paged. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.GET, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtDistributionSetType>> getDistributionSetTypes(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request of retrieving a single DistributionSetType
     * within.
     *
     * @param distributionSetTypeId
     *            the ID of the module type to retrieve
     *
     * @return a single softwareModule with status OK.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetType> getDistributionSetType(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the DELETE request for a single Distribution Set Type.
     *
     * @param distributionSetTypeId
     *            the ID of the module to retrieve
     * @return status OK if delete as sucessfull.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetTypeId}")
    ResponseEntity<Void> deleteDistributionSetType(@PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the PUT request of updating a Distribution Set Type.
     *
     * @param distributionSetTypeId
     *            the ID of the software module in the URL
     * @param restDistributionSetType
     *            the module type to be updated.
     * @return status OK if update is successful
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionSetTypeId}", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSetType> updateDistributionSetType(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            MgmtDistributionSetTypeRequestBodyPut restDistributionSetType);

    /**
     * Handles the POST request of creating new DistributionSetTypes. The
     * request body must always be a list of types.
     *
     * @param distributionSetTypes
     *            the modules to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtDistributionSetType>> createDistributionSetTypes(
            List<MgmtDistributionSetTypeRequestBodyPost> distributionSetTypes);

    /**
     * Handles the GET request of retrieving the list of mandatory software
     * module types in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleType>> getMandatoryModules(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the GET request of retrieving the single mandatory software
     * module type in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @param softwareModuleTypeId
     *            of SoftwareModuleType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES
            + "/{softwareModuleTypeId}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> getMandatoryModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the GET request of retrieving the single optional software module
     * type in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @param softwareModuleTypeId
     *            of SoftwareModuleType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES
            + "/{softwareModuleTypeId}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleType> getOptionalModule(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the GET request of retrieving the list of optional software
     * module types in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleType>> getOptionalModules(
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles DELETE request for removing a mandatory module from the
     * DistributionSetType.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @param softwareModuleTypeId
     *            of the SoftwareModuleType to remove
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES + "/{softwareModuleTypeId}")
    ResponseEntity<Void> removeMandatoryModule(@PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles DELETE request for removing an optional module from the
     * DistributionSetType.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @param softwareModuleTypeId
     *            of the SoftwareModuleType to remove
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES + "/{softwareModuleTypeId}")
    ResponseEntity<Void> removeOptionalModule(@PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") Long softwareModuleTypeId);

    /**
     * Handles the POST request for adding a mandatory software module type to a
     * distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @param smtId
     *            of the SoftwareModuleType to add
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES, consumes = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> addMandatoryModule(@PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            MgmtId smtId);

    /**
     * Handles the POST request for adding an optional software module type to a
     * distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @param smtId
     *            of the SoftwareModuleType to add
     *
     * @return OK if the request was successful
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{distributionSetTypeId}/"
            + MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES, consumes = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> addOptionalModule(@PathVariable("distributionSetTypeId") Long distributionSetTypeId,
            MgmtId smtId);

}
