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
import org.eclipse.hawkbit.mgmt.client.resource.MgmtRolloutClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleTypeClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.RolloutBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleAssigmentBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.TargetBuilder;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Example for creating and starting a Rollout.
 *
 */
public class CreateStartedRolloutExample {

    /* known software module type name and key */
    private static final String SM_MODULE_TYPE = "gettingstarted-rollout-example";

    /* known distribution set type name and key */
    private static final String DS_MODULE_TYPE = SM_MODULE_TYPE;

    @Autowired
    private MgmtDistributionSetClientResource distributionSetResource;

    @Autowired
    @Qualifier("mgmtSoftwareModuleClientResource")
    private MgmtSoftwareModuleClientResource softwareModuleResource;

    @Autowired
    private MgmtTargetClientResource targetResource;

    @Autowired
    private MgmtRolloutClientResource rolloutResource;

    @Autowired
    private MgmtDistributionSetTypeClientResource distributionSetTypeResource;

    @Autowired
    private MgmtSoftwareModuleTypeClientResource softwareModuleTypeResource;

    /**
     * Run the Rollout scenario.
     */
    public void run() {

        // create three SoftwareModuleTypes
        final List<MgmtSoftwareModuleType> createdSoftwareModuleTypes = softwareModuleTypeResource
                .createSoftwareModuleTypes(new SoftwareModuleTypeBuilder().key(SM_MODULE_TYPE).name(SM_MODULE_TYPE)
                        .maxAssignments(1).build())
                .getBody();

        // create one DistributionSetType
        distributionSetTypeResource.createDistributionSetTypes(new DistributionSetTypeBuilder().key(DS_MODULE_TYPE)
                .name(DS_MODULE_TYPE).mandatorymodules(createdSoftwareModuleTypes.get(0).getModuleId()).build())
                .getBody();

        // create one DistributionSet
        final List<MgmtDistributionSet> distributionSetsRest = distributionSetResource.createDistributionSets(
                new DistributionSetBuilder().name("rollout-example").version("1.0.0").type(DS_MODULE_TYPE).build())
                .getBody();

        // create three SoftwareModules
        final List<MgmtSoftwareModule> softwareModulesRest = softwareModuleResource
                .createSoftwareModules(
                        new SoftwareModuleBuilder().name("firmware").version("1.0.0").type(SM_MODULE_TYPE).build())
                .getBody();

        // Assign SoftwareModule to DistributionSet
        distributionSetResource.assignSoftwareModules(distributionSetsRest.get(0).getDsId(),
                new SoftwareModuleAssigmentBuilder().id(softwareModulesRest.get(0).getModuleId()).build());

        // create ten targets
        targetResource.createTargets(new TargetBuilder().controllerId("00-FF-AA-0").name("00-FF-AA-0")
                .description("Targets used for rollout example").buildAsList(10));

        // create a Rollout
        final MgmtRolloutResponseBody rolloutResponseBody = rolloutResource
                .create(new RolloutBuilder().name("MyRollout").groupSize(2).targetFilterQuery("name==00-FF-AA-0*")
                        .distributionSetId(distributionSetsRest.get(0).getDsId()).successThreshold("80")
                        .errorThreshold("50").build())
                .getBody();

        // start the created Rollout
        rolloutResource.start(rolloutResponseBody.getRolloutId(), false);
    }

}
