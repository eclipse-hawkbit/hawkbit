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
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
final class MgmtDistributionSetTypeMapper {

    // private constructor, utility class
    private MgmtDistributionSetTypeMapper() {

    }

    static List<DistributionSetType> smFromRequest(final SoftwareManagement softwareManagement,
            final Iterable<MgmtDistributionSetTypeRequestBodyPost> smTypesRest) {
        final List<DistributionSetType> mappedList = new ArrayList<>();

        for (final MgmtDistributionSetTypeRequestBodyPost smRest : smTypesRest) {
            mappedList.add(fromRequest(softwareManagement, smRest));
        }
        return mappedList;
    }

    static DistributionSetType fromRequest(final SoftwareManagement softwareManagement,
            final MgmtDistributionSetTypeRequestBodyPost smsRest) {

        final DistributionSetType result = new DistributionSetType(smsRest.getKey(), smsRest.getName(),
                smsRest.getDescription());

        // Add mandatory
        smsRest.getMandatorymodules().stream().map(mand -> {
            final SoftwareModuleType smType = softwareManagement.findSoftwareModuleTypeById(mand.getId());

            if (smType == null) {
                throw new EntityNotFoundException("SoftwareModuleType with ID " + mand.getId() + " not found");
            }

            return smType;
        }).forEach(result::addMandatoryModuleType);

        // Add optional
        smsRest.getOptionalmodules().stream().map(opt -> {
            final SoftwareModuleType smType = softwareManagement.findSoftwareModuleTypeById(opt.getId());

            if (smType == null) {
                throw new EntityNotFoundException("SoftwareModuleType with ID " + opt.getId() + " not found");
            }

            return smType;
        }).forEach(result::addOptionalModuleType);

        return result;
    }

    static List<MgmtDistributionSetType> toTypesResponse(final List<DistributionSetType> types) {
        final List<MgmtDistributionSetType> response = new ArrayList<>();
        for (final DistributionSetType dsType : types) {
            response.add(toResponse(dsType));
        }
        return response;
    }

    static List<MgmtDistributionSetType> toListResponse(final List<DistributionSetType> types) {
        final List<MgmtDistributionSetType> response = new ArrayList<>();
        for (final DistributionSetType dsType : types) {
            response.add(toResponse(dsType));
        }
        return response;
    }

    static MgmtDistributionSetType toResponse(final DistributionSetType type) {
        final MgmtDistributionSetType result = new MgmtDistributionSetType();

        MgmtRestModelMapper.mapNamedToNamed(result, type);
        result.setKey(type.getKey());
        result.setModuleId(type.getId());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getDistributionSetType(result.getModuleId()))
                .withRel("self"));

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getMandatoryModules(result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULES));

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getOptionalModules(result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULES));

        return result;
    }

}
