/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPut;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for TargetType CRUD operations.
 *
 */
@RequestMapping(MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING)
public interface MgmtTargetTypeRestApi {

    /**
     * Handles the GET request of retrieving all TargetTypes.
     *
     * @param pagingOffsetParam
     *            the offset of list of target types for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the rest
     *            request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     *
     * @return a list of all TargetTypes for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTargetType>> getTargetTypes(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request of retrieving a single TargetType.
     *
     * @param targetTypeId
     *            the ID of the target type to retrieve
     *
     * @return a single target type with status OK.
     */
    @GetMapping(value = "/{targetTypeId}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetType> getTargetType(@PathVariable("targetTypeId") Long targetTypeId);

    /**
     * Handles the DELETE request for a single Target Type.
     *
     * @param targetTypeId
     *            the ID of the target type to retrieve
     * @return status OK if delete is successful.
     *
     */
    @DeleteMapping(value = "/{targetTypeId}")
    ResponseEntity<Void> deleteTargetType(@PathVariable("targetTypeId") Long targetTypeId);

    /**
     * Handles the PUT request of updating a Target Type.
     *
     * @param targetTypeId
     *            the ID of the target type in the URL
     * @param restTargetType
     *            the target type to be updated.
     * @return status OK if update is successful
     */
    @PutMapping(value = "/{targetTypeId}", consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetType> updateTargetType(@PathVariable("targetTypeId") Long targetTypeId,
            MgmtTargetTypeRequestBodyPut restTargetType);

    /**
     * Handles the POST request of creating new Target Types. The request body must
     * always be a list of types.
     *
     * @param targetTypes
     *            the target types to be created.
     * @return In case all target types could be successfully created the
     *         ResponseEntity with status code 201 - Created but without
     *         ResponseBody. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @PostMapping(consumes = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTargetType>> createTargetTypes(List<MgmtTargetTypeRequestBodyPost> targetTypes);

    /**
     * Handles the GET request of retrieving the list of compatible distribution set
     * types in that target type.
     *
     * @param targetTypeId
     *            of the TargetType.
     * @return Unpaged list of distribution set types and OK in case of success.
     */
    @GetMapping(value = "/{targetTypeId}/" + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtDistributionSetType>> getCompatibleDistributionSets(
            @PathVariable("targetTypeId") Long targetTypeId);

    /**
     * Handles DELETE request for removing the compatibility of a distribution set
     * type from the target type.
     *
     * @param targetTypeId
     *            of the TargetType.
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     *
     * @return OK if the request was successful
     */
    @DeleteMapping(value = "/{targetTypeId}/" + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES + "/{distributionSetTypeId}")
    ResponseEntity<Void> removeCompatibleDistributionSet(@PathVariable("targetTypeId") Long targetTypeId,
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the POST request for adding the compatibility of a distribution set
     * type to a target type.
     *
     * @param targetTypeId
     *            of the TargetType.
     * @param distributionSetTypeIds
     *            of the DistributionSetTypes as a List.
     *
     * @return OK if the request was successful
     */
    @PostMapping(value = "/{targetTypeId}/" + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> addCompatibleDistributionSets(@PathVariable("targetTypeId") final Long targetTypeId,
            final List<MgmtDistributionSetTypeAssignment> distributionSetTypeIds);
}
