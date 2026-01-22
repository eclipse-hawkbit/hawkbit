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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.rest.json.model.ResponseList;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@Service
public final class MgmtTargetTypeMapper {

    private final DistributionSetTypeManagement<? extends DistributionSetType> distributionSetTypeManagement;

    public MgmtTargetTypeMapper(final DistributionSetTypeManagement<? extends DistributionSetType> distributionSetTypeManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
    }

    public List<TargetTypeManagement.Create> targetFromRequest(final Collection<MgmtTargetTypeRequestBodyPost> targetTypesRest) {
        if (targetTypesRest == null) {
            return Collections.emptyList();
        }
        return targetTypesRest.stream()
                .map(this::fromRequest)
                .toList();
    }

    public static List<MgmtTargetType> toListResponse(final List<? extends TargetType> types) {
        if (types == null) {
            return Collections.emptyList();
        }
        return new ResponseList<>(types.stream().map(MgmtTargetTypeMapper::toResponse).toList());
    }

    public static MgmtTargetType toResponse(final TargetType type) {
        final MgmtTargetType result = new MgmtTargetType();

        MgmtRestModelMapper.mapTypeToType(result, type);
        result.setId(type.getId());
        result.add(
                linkTo(methodOn(MgmtTargetTypeRestApi.class).getTargetType(result.getId())).withSelfRel().expand());
        return result;
    }

    public static void addLinks(final MgmtTargetType result) {
        result.add(linkTo(methodOn(MgmtTargetTypeRestApi.class).getCompatibleDistributionSets(result.getId()))
                .withRel(MgmtTargetTypeRestApi.COMPATIBLEDISTRIBUTIONSETTYPES).expand());
    }

    private TargetTypeManagement.Create fromRequest(final MgmtTargetTypeRequestBodyPost targetTypesRest) {
        return TargetTypeManagement.Create.builder()
                .name(targetTypesRest.getName()).description(targetTypesRest.getDescription())
                .key(targetTypesRest.getKey()).colour(targetTypesRest.getColour())
                .distributionSetTypes(getDistributionSets(targetTypesRest))
                .build();
    }

    private Set<DistributionSetType> getDistributionSets(final MgmtTargetTypeRequestBodyPost targetTypesRest) {
        return Optional.ofNullable(targetTypesRest.getCompatibledistributionsettypes())
                .map(compatibleDs -> compatibleDs.stream().map(MgmtDistributionSetTypeAssignment::getId).toList())
                .map(this::getDistributionSetTypes)
                .orElse(Collections.emptySet());
    }

    private Set<DistributionSetType> getDistributionSetTypes(final Collection<Long> distributionSetTypeId) {
        if (CollectionUtils.isEmpty(distributionSetTypeId)) {
            return Collections.emptySet();
        }

        final Collection<? extends DistributionSetType> type = distributionSetTypeManagement.get(distributionSetTypeId);
        if (type.size() < distributionSetTypeId.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, distributionSetTypeId);
        }

        return type.stream().map(DistributionSetType.class::cast).collect(Collectors.toSet());
    }
}