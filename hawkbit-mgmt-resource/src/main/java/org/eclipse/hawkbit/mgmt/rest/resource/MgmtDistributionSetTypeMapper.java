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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeAssigment;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.model.DistributionSetType;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
final class MgmtDistributionSetTypeMapper {

    // private constructor, utility class
    private MgmtDistributionSetTypeMapper() {

    }

    static List<DistributionSetTypeCreate> smFromRequest(final EntityFactory entityFactory,
            final Collection<MgmtDistributionSetTypeRequestBodyPost> smTypesRest) {
        if (smTypesRest == null) {
            return Collections.emptyList();
        }

        return smTypesRest.stream().map(smRest -> fromRequest(entityFactory, smRest)).collect(Collectors.toList());
    }

    private static DistributionSetTypeCreate fromRequest(final EntityFactory entityFactory,
            final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return entityFactory.distributionSetType().create().key(smsRest.getKey()).name(smsRest.getName())
                .description(smsRest.getDescription()).colour(smsRest.getColour())
                .mandatory(getMandatoryModules(smsRest)).optional(getOptionalmodules(smsRest));
    }

    private static Collection<Long> getMandatoryModules(final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return Optional.ofNullable(smsRest.getMandatorymodules()).map(
                modules -> modules.stream().map(MgmtSoftwareModuleTypeAssigment::getId).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private static Collection<Long> getOptionalmodules(final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return Optional.ofNullable(smsRest.getOptionalmodules()).map(
                modules -> modules.stream().map(MgmtSoftwareModuleTypeAssigment::getId).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    static List<MgmtDistributionSetType> toTypesResponse(final List<DistributionSetType> types) {
        if (types == null) {
            return Collections.emptyList();
        }

        return types.stream().map(MgmtDistributionSetTypeMapper::toResponse).collect(Collectors.toList());
    }

    static List<MgmtDistributionSetType> toListResponse(final List<DistributionSetType> types) {
        if (types == null) {
            return Collections.emptyList();
        }

        return types.stream().map(MgmtDistributionSetTypeMapper::toResponse).collect(Collectors.toList());
    }

    static MgmtDistributionSetType toResponse(final DistributionSetType type) {
        final MgmtDistributionSetType result = new MgmtDistributionSetType();

        MgmtRestModelMapper.mapNamedToNamed(result, type);
        result.setKey(type.getKey());
        result.setModuleId(type.getId());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getDistributionSetType(result.getModuleId()))
                .withSelfRel());

        return result;
    }

    static void addLinks(final MgmtDistributionSetType result) {

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getMandatoryModules(result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULES));

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getOptionalModules(result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULES));
    }

}
