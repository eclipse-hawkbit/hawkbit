/**
x * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.scenarios;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties;
import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties.Scenario;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtRolloutClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.RolloutBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleAssigmentBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.TargetBuilder;
import org.eclipse.hawkbit.mgmt.client.scenarios.upload.ArtifactFile;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
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
    private MgmtRolloutClientResource rolloutResource;

    @Autowired
    private ClientConfigurationProperties clientConfigurationProperties;

    /**
     * Run the default getting started scenario.
     */
    public void run() {

        LOGGER.info("Running Configurable Scenario...");

        clientConfigurationProperties.getScenarios().forEach(this::createScenario);
    }

    private void createScenario(final Scenario scenario) {
        if (scenario.isCleanRepository()) {
            cleanRepository();
        }

        createTargets(scenario);
        createDistributionSets(scenario);

        if (scenario.isRunRollouts()) {
            runRollouts(scenario);
        }
    }

    private void cleanRepository() {
        LOGGER.info("Cleaning repository");
        deleteTargets();
        deleteRollouts();
        deleteDistributionSets();
        deleteSoftwareModules();
        LOGGER.info("Cleaning repository -> Done");
    }

    private void deleteRollouts() {
        // TODO: complete this as soon as rollouts can be deleted

    }

    private void deleteSoftwareModules() {
        PagedList<MgmtSoftwareModule> modules;
        do {
            modules = softwareModuleResource.getSoftwareModules(0, 100, null, null).getBody();
            modules.getContent().forEach(module -> softwareModuleResource.deleteSoftwareModule(module.getModuleId()));
        } while (modules.getTotal() > 100);
    }

    private void deleteDistributionSets() {
        PagedList<MgmtDistributionSet> distributionSets;
        do {
            distributionSets = distributionSetResource.getDistributionSets(0, 100, null, null).getBody();
            distributionSets.getContent().forEach(set -> distributionSetResource.deleteDistributionSet(set.getDsId()));
        } while (distributionSets.getTotal() > 100);
    }

    private void deleteTargets() {
        PagedList<MgmtTarget> targets;
        do {
            targets = targetResource.getTargets(0, 100, null, null).getBody();
            targets.getContent().forEach(target -> targetResource.deleteTarget(target.getControllerId()));
        } while (targets.getTotal() > 100);
    }

    private void runRollouts(final Scenario scenario) {
        distributionSetResource.getDistributionSets(0, scenario.getDistributionSets(), null, null).getBody()
                .getContent().forEach(set -> runRollout(set, scenario));

    }

    private void runRollout(final MgmtDistributionSet set, final Scenario scenario) {
        LOGGER.info("Run rollout for set {}", set.getDsId());
        // create a Rollout
        final MgmtRolloutResponseBody rolloutResponseBody = rolloutResource
                .create(new RolloutBuilder().name("Rollout" + set.getName() + set.getVersion())
                        .groupSize(scenario.getRolloutDeploymentGroups()).targetFilterQuery("name==*")
                        .distributionSetId(set.getDsId()).successThreshold("80").errorThreshold("5").build())
                .getBody();

        // start the created Rollout
        rolloutResource.start(rolloutResponseBody.getRolloutId(), true);

        // wait until rollout is complete
        do {
            try {
                TimeUnit.SECONDS.sleep(35);
            } catch (final InterruptedException e) {
                LOGGER.warn("Interrupted!");
                Thread.currentThread().interrupt();
            }
        } while (targetResource.getTargets(0, 1, null, "updateStatus==IN_SYNC").getBody().getTotal() < scenario
                .getTargets());
        LOGGER.info("Run rollout for set {} -> Done", set.getDsId());
    }

    private void createDistributionSets(final Scenario scenario) {
        LOGGER.info("Creating {} distribution sets", scenario.getDistributionSets());
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

        LOGGER.info("Creating {} distribution sets -> Done", scenario.getDistributionSets());
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

    private static byte[] generateArtifact(final Scenario scenario) {

        // Exception squid:S2245 - not used for cryptographic function
        @SuppressWarnings("squid:S2245")
        final Random random = new Random();

        // create byte array
        final byte[] nbyte = new byte[parseSize(scenario.getArtifactSize())];

        // put the next byte in the array
        random.nextBytes(nbyte);

        return nbyte;
    }

    private void createTargets(final Scenario scenario) {
        LOGGER.info("Creating {} targets", scenario.getTargets());

        for (int i = 0; i < scenario.getTargets() / 100; i++) {
            targetResource.createTargets(new TargetBuilder().controllerId(scenario.getTargetName())
                    .address(scenario.getTargetAddress()).buildAsList(i * 100,
                            (i + 1) * 100 > scenario.getTargets() ? (scenario.getTargets() - (i * 100)) : 100));
        }

        LOGGER.info("Creating {} targets -> Done", scenario.getTargets());
    }

    private static int parseSize(final String s) {
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
