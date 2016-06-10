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

import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties;
import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties.Scenario;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleAssigmentBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.TargetBuilder;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Default getting started scenario.
 *
 */
public class ConfigurableScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableScenario.class);

    @Autowired
    private MgmtDistributionSetClientResource distributionSetResource;

    @Autowired
    private MgmtSoftwareModuleClientResource softwareModuleResource;

    @Autowired
    private MgmtTargetClientResource targetResource;

    @Autowired
    private ClientConfigurationProperties clientConfigurationProperties;

    /**
     * Run the default getting started scenario.
     */
    public void run() {

        LOGGER.info("Running Configurable Scenario...");

        clientConfigurationProperties.getScenarios().parallelStream().forEach(this::createScenario);
    }

    private void createScenario(final Scenario scenario) {
        createTargets(scenario);
        createDistributionSets(scenario);
    }

    private void createDistributionSets(final Scenario scenario) {
        distributionSetResource
                .createDistributionSets(new DistributionSetBuilder().name(scenario.getDsName()).type("os_app")
                        .version("1.0.").buildAsList(scenario.getDistributionSets()))
                .getBody().parallelStream().forEach(dsSet -> {
                    final List<MgmtSoftwareModule> modules = softwareModuleResource
                            .createSoftwareModules(new SoftwareModuleBuilder().name(scenario.getSmFwName())
                                    .version(dsSet.getVersion()).type("os").build())
                            .getBody();
                    modules.addAll(
                            softwareModuleResource
                                    .createSoftwareModules(new SoftwareModuleBuilder().name(scenario.getSmSwName())
                                            .version(dsSet.getVersion() + ".").type("application")
                                            .buildAsList(scenario.getAppModulesPerDistributionSet()))
                                    .getBody());

                    final SoftwareModuleAssigmentBuilder assign = new SoftwareModuleAssigmentBuilder();
                    modules.forEach(module -> assign.id(module.getModuleId()));

                    distributionSetResource.assignSoftwareModules(dsSet.getDsId(), assign.build());
                });
    }

    private void createTargets(final Scenario scenario) {
        for (int i = 0; i < scenario.getTargets() / 100; i++) {
            targetResource.createTargets(new TargetBuilder().controllerId(scenario.getTargetName()).buildAsList(i * 100,
                    (i + 1) * 100 > scenario.getTargets() ? scenario.getTargets() : (i + 1) * 100));
        }
    }
}
