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
import java.util.List;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRequestBodyCreate;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypeRest;
import org.eclipse.hawkbit.rest.resource.model.distributionsettype.DistributionSetTypesRest;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 *
 *
 *
 */
final class DistributionSetTypeMapper {

    // private constructor, utility class
    private DistributionSetTypeMapper() {

    }

    static List<DistributionSetType> smFromRequest(final SoftwareManagement softwareManagement,
            final Iterable<DistributionSetTypeRequestBodyCreate> smTypesRest) {
        final List<DistributionSetType> mappedList = new ArrayList<>();

        for (final DistributionSetTypeRequestBodyCreate smRest : smTypesRest) {
            mappedList.add(fromRequest(softwareManagement, smRest));
        }
        return mappedList;
    }

    static DistributionSetType fromRequest(final SoftwareManagement softwareManagement,
            final DistributionSetTypeRequestBodyCreate smsRest) {

        final DistributionSetType result = new DistributionSetType(smsRest.getKey(), smsRest.getName(),
                smsRest.getDescription());

        // Add mandatory
        smsRest.getMandatorymodules().stream().map(mand -> {
            final SoftwareModuleType smType = softwareManagement.findSoftwareModuleTypeById(mand.getId());

            if (smType == null) {
                throw new EntityNotFoundException("SoftwareModuleType with ID " + mand.getId() + " not found");
            }

            return smType;
        }).forEach(softmType -> result.addMandatoryModuleType(softmType));

        // Add optional
        smsRest.getOptionalmodules().stream().map(opt -> {
            final SoftwareModuleType smType = softwareManagement.findSoftwareModuleTypeById(opt.getId());

            if (smType == null) {
                throw new EntityNotFoundException("SoftwareModuleType with ID " + opt.getId() + " not found");
            }

            return smType;
        }).forEach(softmType -> result.addOptionalModuleType(softmType));

        return result;
    }

    static DistributionSetTypesRest toTypesResponse(final List<DistributionSetType> types) {
        final DistributionSetTypesRest response = new DistributionSetTypesRest();
        for (final DistributionSetType dsType : types) {
            response.add(toResponse(dsType));
        }
        return response;
    }

    static List<DistributionSetTypeRest> toListResponse(final List<DistributionSetType> types) {
        final List<DistributionSetTypeRest> response = new ArrayList<DistributionSetTypeRest>();
        for (final DistributionSetType dsType : types) {
            response.add(toResponse(dsType));
        }
        return response;
    }

    static DistributionSetTypeRest toResponse(final DistributionSetType type) {
        final DistributionSetTypeRest result = new DistributionSetTypeRest();

        RestModelMapper.mapNamedToNamed(result, type);
        result.setKey(type.getKey());
        result.setModuleId(type.getId());

        result.add(linkTo(methodOn(DistributionSetTypeResource.class).getDistributionSetType(result.getModuleId()))
                .withRel("self"));

        result.add(linkTo(methodOn(DistributionSetTypeResource.class).getMandatoryModules(result.getModuleId()))
                .withRel(RestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULES));

        result.add(linkTo(methodOn(DistributionSetTypeResource.class).getOptionalModules(result.getModuleId()))
                .withRel(RestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULES));

        return result;
    }

}
