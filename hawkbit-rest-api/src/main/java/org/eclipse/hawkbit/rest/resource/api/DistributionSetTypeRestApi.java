/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.api;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.RestConstants;
import org.eclipse.hawkbit.rest.resource.model.IdRest;
import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyPut;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for SoftwareModule and related Artifact CRUD
 * operations.
 *
 */
@RequestMapping(RestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING)
public interface DistributionSetTypeRestApi {

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
    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<PagedList<DistributionSetTypeRest>> getDistributionSetTypes(
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = RestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request of retrieving a single DistributionSetType
     * within.
     *
     * @param distributionSetTypeId
     *            the ID of the module type to retrieve
     *
     * @return a single softwareModule with status OK.
     * @throws EntityNotFoundException
     *             in case no with the given {@code softwareModuleId} exists.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DistributionSetTypeRest> getDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId);

    /**
     * Handles the DELETE request for a single Distribution Set Type.
     *
     * @param distributionSetTypeId
     *            the ID of the module to retrieve
     * @return status OK if delete as sucessfull.
     *
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{distributionSetTypeId}")
    public ResponseEntity<Void> deleteDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId);

    /**
     * Handles the PUT request of updating a Distribution Set Type.
     *
     * @param distributionSetTypeId
     *            the ID of the software module in the URL
     * @param restDistributionSetType
     *            the module type to be updated.
     * @return status OK if update is successful
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{distributionSetTypeId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<DistributionSetTypeRest> updateDistributionSetType(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @RequestBody final DistributionSetTypeRequestBodyPut restDistributionSetType);

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
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<List<DistributionSetTypeRest>> createDistributionSetTypes(
            @RequestBody final List<DistributionSetTypeRequestBodyPost> distributionSetTypes);

    /**
     * Handles the GET request of retrieving the list of mandatory software
     * module types in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES, produces = { "application/hal+json",
                    MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<List<SoftwareModuleTypeRest>> getMandatoryModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId);

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
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES
            + "/{softwareModuleTypeId}", produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SoftwareModuleTypeRest> getMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId);

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
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES
            + "/{softwareModuleTypeId}", produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<SoftwareModuleTypeRest> getOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId);

    /**
     * Handles the GET request of retrieving the list of optional software
     * module types in that distribution set type.
     *
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     * @return Unpaged list of module types and OK in case of success.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{distributionSetTypeId}/"
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES, produces = { "application/hal+json",
                    MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<List<SoftwareModuleTypeRest>> getOptionalModules(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId);

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
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES
            + "/{softwareModuleTypeId}", produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> removeMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId);

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
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES
            + "/{softwareModuleTypeId}", produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> removeOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId,
            @PathVariable("softwareModuleTypeId") final Long softwareModuleTypeId);

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
            + RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES, consumes = { "application/hal+json",
                    MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json",
                            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> addMandatoryModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId, @RequestBody final IdRest smtId);

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
            + RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES, consumes = { "application/hal+json",
                    MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json",
                            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Void> addOptionalModule(
            @PathVariable("distributionSetTypeId") final Long distributionSetTypeId, @RequestBody final IdRest smtId);

}
