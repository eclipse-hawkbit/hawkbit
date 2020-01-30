/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.data.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
final class MgmtSoftwareModuleTypeMapper {

    // private constructor, utility class
    private MgmtSoftwareModuleTypeMapper() {

    }

    static List<SoftwareModuleTypeCreate> smFromRequest(final EntityFactory entityFactory,
            final Collection<MgmtSoftwareModuleTypeRequestBodyPost> smTypesRest) {
        if (smTypesRest == null) {
            return Collections.emptyList();
        }

        return smTypesRest.stream().map(smRest -> fromRequest(entityFactory, smRest)).collect(Collectors.toList());
    }

    private static SoftwareModuleTypeCreate fromRequest(final EntityFactory entityFactory,
            final MgmtSoftwareModuleTypeRequestBodyPost smsRest) {
        return entityFactory.softwareModuleType().create().key(smsRest.getKey()).name(smsRest.getName())
                .description(smsRest.getDescription()).colour(smsRest.getColour())
                .maxAssignments(smsRest.getMaxAssignments());
    }

    static List<MgmtSoftwareModuleType> toTypesResponse(final Collection<SoftwareModuleType> types) {
        if (types == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                types.stream().map(MgmtSoftwareModuleTypeMapper::toResponse).collect(Collectors.toList()));
    }

    static MgmtSoftwareModuleType toResponse(final SoftwareModuleType type) {
        final MgmtSoftwareModuleType result = new MgmtSoftwareModuleType();

        MgmtRestModelMapper.mapNamedToNamed(result, type);
        result.setKey(type.getKey());
        result.setMaxAssignments(type.getMaxAssignments());
        result.setModuleId(type.getId());
        result.setDeleted(type.isDeleted());

        result.add(linkTo(methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(result.getModuleId()))
                .withSelfRel());

        return result;
    }

}
