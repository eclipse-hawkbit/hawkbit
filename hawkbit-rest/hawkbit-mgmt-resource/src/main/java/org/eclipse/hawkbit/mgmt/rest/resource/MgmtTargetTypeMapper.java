/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.rest.data.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
public final class MgmtTargetTypeMapper {

    // private constructor, utility class
    private MgmtTargetTypeMapper() {
    }

    static List<TargetTypeCreate> targetFromRequest(final EntityFactory entityFactory,
            final Collection<MgmtTargetTypeRequestBodyPost> targetTypesRest) {
        if (targetTypesRest == null) {
            return Collections.emptyList();
        }
        return targetTypesRest.stream().map(targetRest -> fromRequest(entityFactory, targetRest))
                .collect(Collectors.toList());
    }

    private static TargetTypeCreate fromRequest(final EntityFactory entityFactory,
            final MgmtTargetTypeRequestBodyPost targetTypesRest) {
        return entityFactory.targetType().create()
                .name(targetTypesRest.getName()).description(targetTypesRest.getDescription())
                .key(targetTypesRest.getKey()).colour(targetTypesRest.getColour())
                .compatible(getDistributionSets(targetTypesRest));
    }

    private static Collection<Long> getDistributionSets(final MgmtTargetTypeRequestBodyPost targetTypesRest) {
        return Optional.ofNullable(targetTypesRest.getCompatibleDsTypes())
                .map(ds -> ds.stream().map(MgmtDistributionSetTypeAssignment::getId).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    static List<MgmtTargetType> toListResponse(final List<TargetType> types) {
        if (types == null) {
            return Collections.emptyList();
        }
        return new ResponseList<>(types.stream().map(MgmtTargetTypeMapper::toResponse).collect(Collectors.toList()));
    }

    static MgmtTargetType toResponse(final TargetType type) {
        final MgmtTargetType result = new MgmtTargetType();

        MgmtRestModelMapper.mapTypeToType(result, type);
        result.setTypeId(type.getId());
        result.add(
                linkTo(methodOn(MgmtTargetTypeRestApi.class).getTargetType(result.getTypeId())).withSelfRel().expand());
        return result;
    }

    static void addLinks(final MgmtTargetType result) {
        result.add(linkTo(methodOn(MgmtTargetTypeRestApi.class).getCompatibleDistributionSets(result.getTypeId()))
                .withRel(MgmtRestConstants.TARGETTYPE_V1_DS_TYPES).expand());
    }
}
