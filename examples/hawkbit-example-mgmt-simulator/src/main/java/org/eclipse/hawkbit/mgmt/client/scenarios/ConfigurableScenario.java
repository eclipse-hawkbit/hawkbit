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
import java.util.Random;

import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties;
import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties.Scenario;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSystemManagementClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleAssigmentBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.TargetBuilder;
import org.eclipse.hawkbit.mgmt.client.scenarios.upload.ArtifactFile;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
    @Qualifier("mgmtSoftwareModuleClientResource")
    private MgmtSoftwareModuleClientResource softwareModuleResource;

    @Autowired
    @Qualifier("uploadSoftwareModule")
    private MgmtSoftwareModuleClientResource uploadSoftwareModule;

    @Autowired
    private MgmtTargetClientResource targetResource;

    @Autowired
    private MgmtSystemManagementClientResource systemManagementResource;

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
        systemManagementResource.deleteTenant(scenario.getTenant());
        createTargets(scenario);
        createDistributionSets(scenario);
    }

    private void createDistributionSets(final Scenario scenario) {
        final byte[] artifact = generateArtifact(scenario);

        distributionSetResource
                .createDistributionSets(new DistributionSetBuilder().name(scenario.getDsName()).type("os_app")
                        .version("1.0.").buildAsList(scenario.getDistributionSets()))
                .getBody().parallelStream().forEach(dsSet -> {
                    final List<MgmtSoftwareModule> modules = addModules(scenario, dsSet, artifact);

                    final SoftwareModuleAssigmentBuilder assign = new SoftwareModuleAssigmentBuilder();
                    modules.forEach(module -> assign.id(module.getModuleId()));
                    distributionSetResource.assignSoftwareModules(dsSet.getDsId(), assign.build());
                });
    }

    private List<MgmtSoftwareModule> addModules(final Scenario scenario, final MgmtDistributionSet dsSet,
            final byte[] artifact) {
        final List<MgmtSoftwareModule> modules = softwareModuleResource.createSoftwareModules(
                new SoftwareModuleBuilder().name(scenario.getSmFwName()).version(dsSet.getVersion()).type("os").build())
                .getBody();
        modules.addAll(softwareModuleResource
                .createSoftwareModules(
                        new SoftwareModuleBuilder().name(scenario.getSmSwName()).version(dsSet.getVersion() + ".")
                                .type("application").buildAsList(scenario.getAppModulesPerDistributionSet()))
                .getBody());

        for (int x = 0; x < scenario.getArtifactsPerSM(); x++) {
            modules.forEach(module -> {
                final ArtifactFile file = new ArtifactFile("dummyfile.dummy", null, "multipart/form-data", artifact);
                uploadSoftwareModule.uploadArtifact(module.getModuleId(), file, null, null, null);
            });
        }

        return modules;
    }

    private byte[] generateArtifact(final Scenario scenario) {
        // create random object
        final Random random = new Random();

        // create byte array
        final byte[] nbyte = new byte[parseSize(scenario.getArtifactSize())];

        // put the next byte in the array
        random.nextBytes(nbyte);

        return nbyte;
    }

    private void createTargets(final Scenario scenario) {
        for (int i = 0; i < scenario.getTargets() / 100; i++) {
            targetResource.createTargets(new TargetBuilder().controllerId(scenario.getTargetName())
                    .address(scenario.getTargetAddress()).buildAsList(i * 100,
                            (i + 1) * 100 > scenario.getTargets() ? scenario.getTargets() - i * 100 : 100));
        }
    }

    private int parseSize(final String s) {
        final String size = s.toUpperCase();
        if (size.endsWith("KB")) {
            return Integer.valueOf(size.substring(0, size.length() - 2)) * 1024;
        }
        if (size.endsWith("MB")) {
            return Integer.valueOf(size.substring(0, size.length() - 2)) * 1024 * 1024;
        }
        return Integer.valueOf(size);
    }
}
