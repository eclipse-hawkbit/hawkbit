/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
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

import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeAssignment;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.rest.json.model.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
final class MgmtDistributionSetTypeMapper {

    // private constructor, utility class
    private MgmtDistributionSetTypeMapper() {

    }

    static List<DistributionSetTypeCreate> smFromRequest(final EntityFactory entityFactory, final Collection<MgmtDistributionSetTypeRequestBodyPost> smTypesRest) {
        if (smTypesRest == null) {
            return Collections.emptyList();
        }

        return smTypesRest.stream().map(smRest -> fromRequest(entityFactory, smRest)).toList();
    }

    static List<MgmtDistributionSetType> toListResponse(final Collection<DistributionSetType> types) {
        if (types == null) {
            return Collections.emptyList();
        }
        return new ResponseList<>(types.stream().map(MgmtDistributionSetTypeMapper::toResponse).toList());
    }

    static MgmtDistributionSetType toResponse(final DistributionSetType type) {
        final MgmtDistributionSetType result = new MgmtDistributionSetType();

        MgmtRestModelMapper.mapTypeToType(result, type);
        result.setModuleId(type.getId());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getDistributionSetType(result.getModuleId()))
                .withSelfRel().expand());

        return result;
    }

    static void addLinks(final MgmtDistributionSetType result) {

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getMandatoryModules(result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULES).expand());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getOptionalModules(result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULES).expand());
    }

    private static DistributionSetTypeCreate fromRequest(final EntityFactory entityFactory,
            final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return entityFactory.distributionSetType().create().key(smsRest.getKey()).name(smsRest.getName())
                .description(smsRest.getDescription()).colour(smsRest.getColour())
                .mandatory(getMandatoryModules(smsRest)).optional(getOptionalModules(smsRest));
    }

    private static Collection<Long> getMandatoryModules(final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return Optional.ofNullable(smsRest.getMandatorymodules())
                .map(modules -> modules.stream().map(MgmtSoftwareModuleTypeAssignment::getId).toList())
                .orElse(Collections.emptyList());
    }

    private static Collection<Long> getOptionalModules(final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return Optional.ofNullable(smsRest.getOptionalmodules())
                .map(modules -> modules.stream().map(MgmtSoftwareModuleTypeAssignment::getId).toList())
                .orElse(Collections.emptyList());
    }
}