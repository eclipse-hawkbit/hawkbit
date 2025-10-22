/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement.Create;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.json.model.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtSoftwareModuleTypeMapper {

    public static List<Create> smFromRequest(final Collection<MgmtSoftwareModuleTypeRequestBodyPost> request) {
        if (request == null) {
            return List.of();
        }
        return request.stream().map(MgmtSoftwareModuleTypeMapper::fromRequest).toList();
    }

    public static List<MgmtSoftwareModuleType> toTypesResponse(final Collection<? extends SoftwareModuleType> types) {
        if (types == null) {
            return List.of();
        }
        return new ResponseList<>(types.stream().map(MgmtSoftwareModuleTypeMapper::toResponse).toList());
    }

    public static MgmtSoftwareModuleType toResponse(final SoftwareModuleType type) {
        final MgmtSoftwareModuleType result = new MgmtSoftwareModuleType();
        MgmtRestModelMapper.mapTypeToType(result, type);
        result
                .setId(type.getId())
                .setMinArtifacts(type.getMinArtifacts())
                .setMaxAssignments(type.getMaxAssignments());
        result.add(linkTo(methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(result.getId())).withSelfRel().expand());
        return result;
    }

    private static Create fromRequest(final MgmtSoftwareModuleTypeRequestBodyPost request) {
        return Create.builder()
                .key(request.getKey()).name(request.getName())
                .description(request.getDescription()).colour(request.getColour())
                .minArtifacts(request.getMinArtifacts())
                .maxAssignments(request.getMaxAssignments())
                .build();
    }
}