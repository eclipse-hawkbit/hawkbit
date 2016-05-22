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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 *
 *
 *
 */
final class MgmtSoftwareModuleTypeMapper {

    // private constructor, utility class
    private MgmtSoftwareModuleTypeMapper() {

    }

    static List<SoftwareModuleType> smFromRequest(final SoftwareManagement softwareManagement,
            final Iterable<MgmtSoftwareModuleTypeRequestBodyPost> smTypesRest) {
        final List<SoftwareModuleType> mappedList = new ArrayList<>();

        for (final MgmtSoftwareModuleTypeRequestBodyPost smRest : smTypesRest) {
            mappedList.add(fromRequest(softwareManagement, smRest));
        }
        return mappedList;
    }

    static SoftwareModuleType fromRequest(final SoftwareManagement softwareManagement,
            final MgmtSoftwareModuleTypeRequestBodyPost smsRest) {
        final SoftwareModuleType result = softwareManagement.generateSoftwareModuleType();
        result.setName(smsRest.getName());
        result.setKey(smsRest.getKey());
        result.setDescription(smsRest.getDescription());
        result.setMaxAssignments(smsRest.getMaxAssignments());

        return result;
    }

    static List<MgmtSoftwareModuleType> toTypesResponse(final List<SoftwareModuleType> types) {
        final List<MgmtSoftwareModuleType> response = new ArrayList<>();
        for (final SoftwareModuleType softwareModule : types) {
            response.add(toResponse(softwareModule));
        }
        return response;
    }

    static List<MgmtSoftwareModuleType> toListResponse(final Collection<SoftwareModuleType> types) {
        final List<MgmtSoftwareModuleType> response = new ArrayList<>();
        for (final SoftwareModuleType softwareModule : types) {
            response.add(toResponse(softwareModule));
        }
        return response;
    }

    static MgmtSoftwareModuleType toResponse(final SoftwareModuleType type) {
        final MgmtSoftwareModuleType result = new MgmtSoftwareModuleType();

        MgmtRestModelMapper.mapNamedToNamed(result, type);
        result.setKey(type.getKey());
        result.setMaxAssignments(type.getMaxAssignments());
        result.setModuleId(type.getId());

        result.add(linkTo(methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(result.getModuleId()))
                .withRel("self"));

        return result;
    }

}
