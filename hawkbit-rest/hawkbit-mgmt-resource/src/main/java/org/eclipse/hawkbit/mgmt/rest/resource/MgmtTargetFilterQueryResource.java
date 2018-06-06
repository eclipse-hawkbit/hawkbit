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
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * REST Resource handling target CRUD operations.
 */
@RestController
public class MgmtTargetFilterQueryResource implements MgmtTargetFilterQueryRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtTargetFilterQueryResource.class);

    @Autowired
    private TargetFilterQueryManagement filterManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> getFilter(@PathVariable("filterId") final Long filterId) {
        final TargetFilterQuery findTarget = findFilterWithExceptionIfNotFound(filterId);
        // to single response include poll status
        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(findTarget);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTargetFilterQuery>> getFilters(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetFilterQuerySortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<TargetFilterQuery> findTargetFiltersAll;
        final Long countTargetsAll;
        if (rsqlParam != null) {
            final Page<TargetFilterQuery> findFilterPage = filterManagement.findByRsql(pageable, rsqlParam);
            countTargetsAll = findFilterPage.getTotalElements();
            findTargetFiltersAll = findFilterPage;
        } else {
            findTargetFiltersAll = filterManagement.findAll(pageable);
            countTargetsAll = filterManagement.count();
        }

        final List<MgmtTargetFilterQuery> rest = MgmtTargetFilterQueryMapper
                .toResponse(findTargetFiltersAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countTargetsAll));
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> createFilter(
            @RequestBody final MgmtTargetFilterQueryRequestBody filter) {
        final TargetFilterQuery createdTarget = filterManagement
                .create(MgmtTargetFilterQueryMapper.fromRequest(entityFactory, filter));

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(createdTarget);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> updateFilter(@PathVariable("filterId") final Long filterId,
            @RequestBody final MgmtTargetFilterQueryRequestBody targetFilterRest) {
        LOG.debug("updating target filter query {}", filterId);

        final TargetFilterQuery updateFilter = filterManagement.update(entityFactory.targetFilterQuery()
                .update(filterId).name(targetFilterRest.getName()).query(targetFilterRest.getQuery()));

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(updateFilter);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteFilter(@PathVariable("filterId") final Long filterId) {
        filterManagement.delete(filterId);
        LOG.debug("{} target filter query deleted, return status {}", filterId, HttpStatus.OK);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtTargetFilterQuery> postAssignedDistributionSet(
            @PathVariable("filterId") final Long filterId, @RequestBody final MgmtId dsId) {

        final TargetFilterQuery updateFilter = filterManagement.updateAutoAssignDS(filterId, dsId.getId());

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(updateFilter);
        MgmtTargetFilterQueryMapper.addLinks(response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(
            @PathVariable("filterId") final Long filterId) {
        final TargetFilterQuery filter = findFilterWithExceptionIfNotFound(filterId);
        final DistributionSet autoAssignDistributionSet = filter.getAutoAssignDistributionSet();

        if (autoAssignDistributionSet == null) {
            return ResponseEntity.noContent().build();
        }

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(autoAssignDistributionSet);
        MgmtDistributionSetMapper.addLinks(autoAssignDistributionSet, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteAssignedDistributionSet(@PathVariable("filterId") final Long filterId) {
        filterManagement.updateAutoAssignDS(filterId, null);

        return ResponseEntity.noContent().build();
    }

    private TargetFilterQuery findFilterWithExceptionIfNotFound(final Long filterId) {
        return filterManagement.get(filterId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, filterId));
    }

}
