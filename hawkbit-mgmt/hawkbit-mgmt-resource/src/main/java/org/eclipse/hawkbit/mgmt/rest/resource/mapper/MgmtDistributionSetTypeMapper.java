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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeAssignment;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.json.model.ResponseList;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@Service
public final class MgmtDistributionSetTypeMapper {

    private final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement;

    MgmtDistributionSetTypeMapper(final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    public List<DistributionSetTypeManagement.Create> smFromRequest(final Collection<MgmtDistributionSetTypeRequestBodyPost> smTypesRest) {
        if (smTypesRest == null) {
            return Collections.emptyList();
        }

        return smTypesRest.stream().map(this::fromRequest).toList();
    }

    public static List<MgmtDistributionSetType> toListResponse(final Collection<? extends DistributionSetType> types) {
        if (types == null) {
            return Collections.emptyList();
        }
        return new ResponseList<>(types.stream().map(MgmtDistributionSetTypeMapper::toResponse).toList());
    }

    public static MgmtDistributionSetType toResponse(final DistributionSetType type) {
        final MgmtDistributionSetType result = new MgmtDistributionSetType();

        MgmtRestModelMapper.mapTypeToType(result, type);
        result.setId(type.getId());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getDistributionSetType(result.getId()))
                .withSelfRel().expand());

        return result;
    }

    public static void addLinks(final MgmtDistributionSetType result) {

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getMandatoryModules(result.getId()))
                .withRel(MgmtRestConstants.MANDATORYMODULES).expand());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getOptionalModules(result.getId()))
                .withRel(MgmtRestConstants.OPTIONALMODULES).expand());
    }

    private DistributionSetTypeManagement.Create fromRequest(final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return DistributionSetTypeManagement.Create.builder()
                .key(smsRest.getKey()).name(smsRest.getName())
                .description(smsRest.getDescription()).colour(smsRest.getColour())
                .mandatoryModuleTypes(getModules(smsRest.getMandatorymodules()))
                .optionalModuleTypes(getModules(smsRest.getOptionalmodules()))
                .build();
    }

    private Set<? extends SoftwareModuleType> getModules(final List<MgmtSoftwareModuleTypeAssignment> moduleAssignments) {
        return Optional.ofNullable(moduleAssignments)
                .map(modules -> modules.stream().map(MgmtSoftwareModuleTypeAssignment::getId).collect(Collectors.toSet()))
                .map(this::findSoftwareModuleTypeWithExceptionIfNotFound)
                .orElse(Collections.emptySet());
    }

    private Set<? extends SoftwareModuleType> findSoftwareModuleTypeWithExceptionIfNotFound(final Collection<Long> softwareModuleTypeId) {
        if (CollectionUtils.isEmpty(softwareModuleTypeId)) {
            return Collections.emptySet();
        }

        final List<? extends SoftwareModuleType> module = softwareModuleTypeManagement.get(softwareModuleTypeId);
        if (module.size() < softwareModuleTypeId.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, softwareModuleTypeId);
        }

        return new HashSet<>(module);
    }
}