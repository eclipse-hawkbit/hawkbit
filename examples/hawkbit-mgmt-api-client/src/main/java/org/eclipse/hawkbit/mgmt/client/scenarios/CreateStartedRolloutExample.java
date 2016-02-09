/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.client.scenarios;

import org.eclipse.hawkbit.mgmt.client.resource.DistributionSetResourceClient;
import org.eclipse.hawkbit.mgmt.client.resource.DistributionSetTypeResourceClient;
import org.eclipse.hawkbit.mgmt.client.resource.RolloutResourceClient;
import org.eclipse.hawkbit.mgmt.client.resource.SoftwareModuleResourceClient;
import org.eclipse.hawkbit.mgmt.client.resource.SoftwareModuleTypeResourceClient;
import org.eclipse.hawkbit.mgmt.client.resource.TargetResourceClient;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.RolloutBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleAssigmentBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.TargetBuilder;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetsRest;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutResponseBody;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModulesRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremoduletype.SoftwareModuleTypesRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Example for creating and starting a Rollout.
 *
 */
@Component
public class CreateStartedRolloutExample {

    /* known software module type name and key */
    private static final String SM_MODULE_TYPE = "firmware";

    /* known distribution set type name and key */
    private static final String DS_MODULE_TYPE = "firmware";

    @Autowired
    private DistributionSetResourceClient distributionSetResource;

    @Autowired
    private SoftwareModuleResourceClient softwareModuleResource;

    @Autowired
    private TargetResourceClient targetResource;

    @Autowired
    private RolloutResourceClient rolloutResource;

    @Autowired
    private DistributionSetTypeResourceClient distributionSetTypeResource;

    @Autowired
    private SoftwareModuleTypeResourceClient softwareModuleTypeResource;

    /**
     * Run the Rollout scenario.
     */
    public void run() {

        // create three SoftwareModuleTypes
        final SoftwareModuleTypesRest createdSoftwareModuleTypes = softwareModuleTypeResource.createSoftwareModuleTypes(
                new SoftwareModuleTypeBuilder().key(SM_MODULE_TYPE).name(SM_MODULE_TYPE).maxAssignments(1).build())
                .getBody();

        // create one DistributionSetType
        distributionSetTypeResource.createDistributionSetTypes(new DistributionSetTypeBuilder().key(DS_MODULE_TYPE)
                .name(DS_MODULE_TYPE).mandatorymodules(createdSoftwareModuleTypes.get(0).getModuleId()).build())
                .getBody();

        // create one DistributionSet
        final DistributionSetsRest distributionSetsRest = distributionSetResource.createDistributionSets(
                new DistributionSetBuilder().name("rollout-example").version("1.0.0").type(DS_MODULE_TYPE).build())
                .getBody();

        // create three SoftwareModules
        final SoftwareModulesRest softwareModulesRest = softwareModuleResource
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
        final RolloutResponseBody rolloutResponseBody = rolloutResource
                .create(new RolloutBuilder().name("MyRollout").groupSize(2).targetFilterQuery("name==00-FF-AA-0*")
                        .distributionSetId(distributionSetsRest.get(0).getDsId()).successThreshold("80")
                        .errorThreshold("50").build())
                .getBody();

        // start the created Rollout
        rolloutResource.start(rolloutResponseBody.getRolloutId(), false);
    }

}
