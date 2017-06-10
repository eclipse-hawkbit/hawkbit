/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.util;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtNamedEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * This class provides utility methods to map a {@link DistributionSet} to
 * {@link MgmtDistributionSet} that can then be used to populate the HTTP
 * response.
 */
public final class OfflineUpdateMapperUtil {

    /**
     * Private constructor to disable the creation of an instance as this is a
     * utility class.
     */
    private OfflineUpdateMapperUtil() {
        throw new IllegalAccessError("This is a utility class. Use static methods.");
    }

    /**
     * Converts a {@link DistributionSet} type to {@link MgmtDistributionSet}
     * type.
     *
     * @param distributionSet
     *            {@link DistributionSet} to convert
     *
     * @return {@link MgmtDistributionSet}.
     */
    public static MgmtDistributionSet toResponse(final DistributionSet distributionSet) {
        if (distributionSet == null) {
            return null;
        }

        List<MgmtSoftwareModule> softwareModules = new ArrayList<>();
        distributionSet.getModules()
                .forEach(module -> softwareModules.add(convertSoftwareModuletoMgmtSoftwareModule(module)));

        final MgmtDistributionSet response = new MgmtDistributionSet(distributionSet.getId(),
                distributionSet.getVersion(), softwareModules, distributionSet.isRequiredMigrationStep(),
                distributionSet.getType().getKey(), distributionSet.isComplete());
        response.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class)
                .getDistributionSetType(distributionSet.getType().getId())).withRel("type"));

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(response.getDsId()))
                .withSelfRel());

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getMetadata(response.getDsId(),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("metadata"));
        setNamedEntityToResponse(response, distributionSet);

        return response;
    }

    /**
     * Converts {@link SoftwareModule} to {@link MgmtSoftwareModule} type.
     *
     * @param sofwareModule
     *            {@link SoftwareModule} to convert
     *
     * @return {@link MgmtSoftwareModule}.
     */
    private static MgmtSoftwareModule convertSoftwareModuletoMgmtSoftwareModule(final SoftwareModule sofwareModule) {
        if (sofwareModule == null) {
            return null;
        }

        final MgmtSoftwareModule mgmtSoftwareModule = new MgmtSoftwareModule(sofwareModule.getId(),
                sofwareModule.getVersion(), sofwareModule.getType().getKey(), sofwareModule.getVendor());
        setNamedEntityToResponse(mgmtSoftwareModule, sofwareModule);

        mgmtSoftwareModule.add(
                linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getSoftwareModule(mgmtSoftwareModule.getModuleId()))
                        .withSelfRel());

        mgmtSoftwareModule.add(linkTo(
                methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(sofwareModule.getType().getId()))
                        .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_TYPE));

        return mgmtSoftwareModule;
    }

    /**
     * Maps the {@link NamedEntity} values to the response of type
     * {@link MgmtNamedEntity}.
     *
     * @param response
     *            {@link MgmtNamedEntity} type to set the base
     * @param base
     *            {@link NamedEntity} to retrieve the values to set the
     *            response.
     */
    private static void setNamedEntityToResponse(final MgmtNamedEntity response, final NamedEntity base) {
        response.setCreatedBy(base.getCreatedBy());
        response.setLastModifiedBy(base.getLastModifiedBy());
        if (base.getCreatedAt() != null) {
            response.setCreatedAt(base.getCreatedAt());
        }
        if (base.getLastModifiedAt() != null) {
            response.setLastModifiedAt(base.getLastModifiedAt());
        }

        response.setName(base.getName());
        response.setDescription(base.getDescription());
    }
}
