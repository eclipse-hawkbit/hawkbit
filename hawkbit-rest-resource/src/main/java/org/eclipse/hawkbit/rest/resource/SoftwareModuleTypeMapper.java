/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.resource.api.SoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypeRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypesRest;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 *
 *
 *
 */
final class SoftwareModuleTypeMapper {

    // private constructor, utility class
    private SoftwareModuleTypeMapper() {

    }

    static List<SoftwareModuleType> smFromRequest(final Iterable<SoftwareModuleTypeRequestBodyPost> smTypesRest) {
        final List<SoftwareModuleType> mappedList = new ArrayList<>();

        for (final SoftwareModuleTypeRequestBodyPost smRest : smTypesRest) {
            mappedList.add(fromRequest(smRest));
        }
        return mappedList;
    }

    static SoftwareModuleType fromRequest(final SoftwareModuleTypeRequestBodyPost smsRest) {
        return new SoftwareModuleType(smsRest.getKey(), smsRest.getName(), smsRest.getDescription(),
                smsRest.getMaxAssignments());
    }

    static SoftwareModuleTypesRest toTypesResponse(final List<SoftwareModuleType> types) {
        final SoftwareModuleTypesRest response = new SoftwareModuleTypesRest();
        for (final SoftwareModuleType softwareModule : types) {
            response.add(toResponse(softwareModule));
        }
        return response;
    }

    static List<SoftwareModuleTypeRest> toListResponse(final Collection<SoftwareModuleType> types) {
        final List<SoftwareModuleTypeRest> response = new ArrayList<>();
        for (final SoftwareModuleType softwareModule : types) {
            response.add(toResponse(softwareModule));
        }
        return response;
    }

    static SoftwareModuleTypeRest toResponse(final SoftwareModuleType type) {
        final SoftwareModuleTypeRest result = new SoftwareModuleTypeRest();

        RestModelMapper.mapNamedToNamed(result, type);
        result.setKey(type.getKey());
        result.setMaxAssignments(type.getMaxAssignments());
        result.setModuleId(type.getId());

        result.add(linkTo(methodOn(SoftwareModuleTypeRestApi.class).getSoftwareModuleType(result.getModuleId()))
                .withRel("self"));

        return result;
    }

}
