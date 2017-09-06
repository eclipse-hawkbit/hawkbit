/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.util.CollectionUtils;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtTargetFilterQueryMapper {

    private MgmtTargetFilterQueryMapper() {
        // Utility class
    }

    static List<MgmtTargetFilterQuery> toResponse(final List<TargetFilterQuery> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }
        return filters.stream().map(MgmtTargetFilterQueryMapper::toResponse).collect(Collectors.toList());
    }

    static MgmtTargetFilterQuery toResponse(final TargetFilterQuery filter) {
        final MgmtTargetFilterQuery targetRest = new MgmtTargetFilterQuery();
        targetRest.setFilterId(filter.getId());
        targetRest.setName(filter.getName());
        targetRest.setQuery(filter.getQuery());

        targetRest.setCreatedBy(filter.getCreatedBy());
        targetRest.setLastModifiedBy(filter.getLastModifiedBy());

        targetRest.setCreatedAt(filter.getCreatedAt());
        targetRest.setLastModifiedAt(filter.getLastModifiedAt());

        final DistributionSet distributionSet = filter.getAutoAssignDistributionSet();
        if (distributionSet != null) {
            targetRest.setAutoAssignDistributionSet(distributionSet.getId());
        }

        targetRest.add(linkTo(methodOn(MgmtTargetFilterQueryRestApi.class).getFilter(filter.getId())).withSelfRel());

        return targetRest;
    }

    static void addLinks(final MgmtTargetFilterQuery targetRest) {
        targetRest.add(linkTo(methodOn(MgmtTargetFilterQueryRestApi.class)
                .postAssignedDistributionSet(targetRest.getFilterId(), null)).withRel("autoAssignDS"));
    }

    static TargetFilterQueryCreate fromRequest(final EntityFactory entityFactory,
            final MgmtTargetFilterQueryRequestBody filterRest) {

        return entityFactory.targetFilterQuery().create().name(filterRest.getName()).query(filterRest.getQuery());
    }

}
