/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.scenarios;

import java.util.List;

import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetTypeClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleTypeClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleAssigmentBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Default getting started scenario.
 *
 */
public class GettingStartedDefaultScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(GettingStartedDefaultScenario.class);

    /* known software module type name and key */
    private static final String SM_MODULE_TYPE = "gettingstarted";

    /* known distribution set type name and key */
    private static final String DS_MODULE_TYPE = SM_MODULE_TYPE;

    /* known distribution name of this getting started example */
    private static final String SM_EXAMPLE_NAME = "gettingstarted-example";

    /* known distribution name of this getting started example */
    private static final String DS_EXAMPLE_NAME = SM_EXAMPLE_NAME;

    @Autowired
    private MgmtDistributionSetClientResource distributionSetResource;

    @Autowired
    private MgmtDistributionSetTypeClientResource distributionSetTypeResource;

    @Autowired
    private MgmtSoftwareModuleClientResource softwareModuleResource;

    @Autowired
    private MgmtSoftwareModuleTypeClientResource softwareModuleTypeResource;

    /**
     * Run the default getting started scenario.
     */
    public void run() {

        LOGGER.info("Running Getting-Started-Scenario...");

        // create one SoftwareModuleTypes
        LOGGER.info("Creating software module type {}", SM_MODULE_TYPE);
        final List<MgmtSoftwareModuleType> createdSoftwareModuleTypes = softwareModuleTypeResource
                .createSoftwareModuleTypes(new SoftwareModuleTypeBuilder().key(SM_MODULE_TYPE).name(SM_MODULE_TYPE)
                        .maxAssignments(1).build())
                .getBody();

        // create one DistributionSetType
        LOGGER.info("Creating distribution set type {}", DS_MODULE_TYPE);
        distributionSetTypeResource.createDistributionSetTypes(new DistributionSetTypeBuilder().key(DS_MODULE_TYPE)
                .name(DS_MODULE_TYPE).mandatorymodules(createdSoftwareModuleTypes.get(0).getModuleId()).build());

        // create three DistributionSet
        final String dsVersion1 = "1.0.0";
        final String dsVersion2 = "2.0.0";
        final String dsVersion3 = "2.1.0";

        LOGGER.info("Creating distribution set {}:{}", DS_EXAMPLE_NAME, dsVersion1);
        final List<MgmtDistributionSet> distributionSetsRest1 = distributionSetResource.createDistributionSets(
                new DistributionSetBuilder().name(DS_EXAMPLE_NAME).version(dsVersion1).type(DS_MODULE_TYPE).build())
                .getBody();

        LOGGER.info("Creating distribution set {}:{}", DS_EXAMPLE_NAME, dsVersion2);
        final List<MgmtDistributionSet> distributionSetsRest2 = distributionSetResource.createDistributionSets(
                new DistributionSetBuilder().name(DS_EXAMPLE_NAME).version(dsVersion2).type(DS_MODULE_TYPE).build())
                .getBody();

        LOGGER.info("Creating distribution set {}:{}", DS_EXAMPLE_NAME, dsVersion3);
        final List<MgmtDistributionSet> distributionSetsRest3 = distributionSetResource.createDistributionSets(
                new DistributionSetBuilder().name(DS_EXAMPLE_NAME).version(dsVersion3).type(DS_MODULE_TYPE).build())
                .getBody();

        // create three SoftwareModules
        final String swVersion1 = "1";
        final String swVersion2 = "2";
        final String swVersion3 = "3";

        LOGGER.info("Creating distribution set {}:{}", SM_EXAMPLE_NAME, swVersion1);
        final List<MgmtSoftwareModule> softwareModulesRest1 = softwareModuleResource.createSoftwareModules(
                new SoftwareModuleBuilder().name(SM_EXAMPLE_NAME).version(swVersion1).type(SM_MODULE_TYPE).build())
                .getBody();
        LOGGER.info("Creating distribution set {}:{}", SM_EXAMPLE_NAME, swVersion2);
        final List<MgmtSoftwareModule> softwareModulesRest2 = softwareModuleResource.createSoftwareModules(
                new SoftwareModuleBuilder().name(SM_EXAMPLE_NAME).version(swVersion2).type(SM_MODULE_TYPE).build())
                .getBody();
        LOGGER.info("Creating distribution set {}:{}", SM_EXAMPLE_NAME, swVersion3);
        final List<MgmtSoftwareModule> softwareModulesRest3 = softwareModuleResource.createSoftwareModules(
                new SoftwareModuleBuilder().name(SM_EXAMPLE_NAME).version(swVersion3).type(SM_MODULE_TYPE).build())
                .getBody();

        // Assign SoftwareModules to DistributionSet
        LOGGER.info("Assign software module {}:{} to distribution set {}:{}", SM_EXAMPLE_NAME, swVersion1,
                DS_EXAMPLE_NAME, dsVersion1);
        distributionSetResource.assignSoftwareModules(distributionSetsRest1.get(0).getDsId(),
                new SoftwareModuleAssigmentBuilder().id(softwareModulesRest1.get(0).getModuleId()).build());

        LOGGER.info("Assign software module {}:{} to distribution set {}:{}", SM_EXAMPLE_NAME, swVersion2,
                DS_EXAMPLE_NAME, dsVersion2);
        distributionSetResource.assignSoftwareModules(distributionSetsRest2.get(0).getDsId(),
                new SoftwareModuleAssigmentBuilder().id(softwareModulesRest2.get(0).getModuleId()).build());

        LOGGER.info("Assign software module {}:{} to distribution set {}:{}", SM_EXAMPLE_NAME, swVersion3,
                DS_EXAMPLE_NAME, dsVersion3);
        distributionSetResource.assignSoftwareModules(distributionSetsRest3.get(0).getDsId(),
                new SoftwareModuleAssigmentBuilder().id(softwareModulesRest3.get(0).getModuleId()).build());
    }
}
