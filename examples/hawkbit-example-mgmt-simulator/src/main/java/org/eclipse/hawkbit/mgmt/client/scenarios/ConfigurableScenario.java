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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties;
import org.eclipse.hawkbit.mgmt.client.ClientConfigurationProperties.Scenario;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtDistributionSetTagClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtRolloutClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtSoftwareModuleClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.MgmtTargetTagClientResource;
import org.eclipse.hawkbit.mgmt.client.resource.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.RolloutBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleAssigmentBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.TagBuilder;
import org.eclipse.hawkbit.mgmt.client.resource.builder.TargetBuilder;
import org.eclipse.hawkbit.mgmt.client.scenarios.upload.ArtifactFile;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedTargetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A configurable scenario which runs the configured scenarios.
 * 
 * @see {@link ClientConfigurationProperties#getScenarios()}
 *
 */
public class ConfigurableScenario {

    private static final int PAGE_SIZE = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableScenario.class);

    private final MgmtDistributionSetClientResource distributionSetResource;

    private final MgmtSoftwareModuleClientResource softwareModuleResource;

    private final MgmtTargetClientResource targetResource;

    private final MgmtTargetTagClientResource targetTagResource;

    private final MgmtDistributionSetTagClientResource distributionSetTagResource;

    private final MgmtRolloutClientResource rolloutResource;

    private final ClientConfigurationProperties clientConfigurationProperties;

    private final MgmtSoftwareModuleClientResource uploadSoftwareModule;

    public ConfigurableScenario(final MgmtDistributionSetClientResource distributionSetResource,
            final MgmtSoftwareModuleClientResource softwareModuleResource,
            final MgmtSoftwareModuleClientResource uploadSoftwareModule, final MgmtTargetClientResource targetResource,
            final MgmtRolloutClientResource rolloutResource, final MgmtTargetTagClientResource targetTagResource,
            final MgmtDistributionSetTagClientResource distributionSetTagResource,
            final ClientConfigurationProperties clientConfigurationProperties) {
        this.targetTagResource = targetTagResource;
        this.distributionSetResource = distributionSetResource;
        this.softwareModuleResource = softwareModuleResource;
        this.uploadSoftwareModule = uploadSoftwareModule;
        this.targetResource = targetResource;
        this.rolloutResource = rolloutResource;
        this.distributionSetTagResource = distributionSetTagResource;
        this.clientConfigurationProperties = clientConfigurationProperties;
    }

    /**
     * Run the default getting started scenario.
     */
    public void run() {

        LOGGER.info("Running Configurable Scenario...");

        // while (1==1) {
        clientConfigurationProperties.getScenarios().forEach(this::createScenario);
        // }
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
            modules = softwareModuleResource.getSoftwareModules(0, PAGE_SIZE, null, null).getBody();
            modules.getContent().forEach(module -> softwareModuleResource.deleteSoftwareModule(module.getModuleId()));
        } while (modules.getTotal() > PAGE_SIZE);
    }

    private void deleteDistributionSets() {
        PagedList<MgmtDistributionSet> distributionSets;
        do {
            distributionSets = distributionSetResource.getDistributionSets(0, PAGE_SIZE, null, null).getBody();
            distributionSets.getContent().forEach(set -> distributionSetResource.deleteDistributionSet(set.getDsId()));
        } while (distributionSets.getTotal() > PAGE_SIZE);

        deleteDistributionSetTags();
    }

    private void deleteDistributionSetTags() {
        PagedList<MgmtTag> dsTags;
        do {
            dsTags = distributionSetTagResource.getDistributionSetTags(0, PAGE_SIZE, null, null).getBody();
            dsTags.getContent().forEach(ds -> distributionSetTagResource.deleteDistributionSetTag(ds.getTagId()));
        } while (dsTags.getTotal() > PAGE_SIZE);
    }

    private void deleteTargets() {
        PagedList<MgmtTarget> targets;
        do {
            targets = targetResource.getTargets(0, PAGE_SIZE, null, null).getBody();
            targets.getContent().forEach(target -> targetResource.deleteTarget(target.getControllerId()));
        } while (targets.getTotal() > PAGE_SIZE);

        deleteTargetTags();
    }

    private void deleteTargetTags() {
        PagedList<MgmtTag> targetTags;
        do {
            targetTags = targetTagResource.getTargetTags(0, PAGE_SIZE, null, null).getBody();
            targetTags.getContent().forEach(target -> targetTagResource.deleteTargetTag(target.getTagId()));
        } while (targetTags.getTotal() > PAGE_SIZE);
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

        final List<MgmtDistributionSet> sets = distributionSetResource
                .createDistributionSets(new DistributionSetBuilder().name(scenario.getDsName()).type("os_app")
                        .version("1.0.").buildAsList(scenario.getDistributionSets()))
                .getBody();

        sets.forEach(dsSet -> {
            final List<MgmtSoftwareModule> modules = addModules(scenario, dsSet, artifact);

            final SoftwareModuleAssigmentBuilder assign = new SoftwareModuleAssigmentBuilder();
            modules.forEach(module -> assign.id(module.getModuleId()));
            distributionSetResource.assignSoftwareModules(dsSet.getDsId(), assign.build());
        });

        for (int i = 0; i < scenario.getDsTags(); i++) {
            final MgmtTag tag = distributionSetTagResource
                    .createDistributionSetTags(
                            new TagBuilder().name("DS Tag" + i).description("DS tag for DS " + i).build())
                    .getBody().get(0);

            distributionSetTagResource.assignDistributionSets(tag.getTagId(),
                    sets.stream().map(
                            set -> new MgmtAssignedDistributionSetRequestBody().setDistributionSetId(set.getDsId()))
                            .collect(Collectors.toList()));
        }

        LOGGER.info("Creating {} distribution sets -> Done", scenario.getDistributionSets());
    }

    private List<MgmtSoftwareModule> addModules(final Scenario scenario, final MgmtDistributionSet dsSet,
            final byte[] artifact) {
        final List<MgmtSoftwareModule> modules = softwareModuleResource
                .createSoftwareModules(new SoftwareModuleBuilder().name(scenario.getSmFwName() + "-os")
                        .version(dsSet.getVersion()).type("os").build())
                .getBody();
        modules.addAll(softwareModuleResource.createSoftwareModules(
                new SoftwareModuleBuilder().name(scenario.getSmSwName() + "-app").version(dsSet.getVersion() + ".")
                        .type("application").buildAsList(scenario.getAppModulesPerDistributionSet()))
                .getBody());

        for (int iArtifact = 0; iArtifact < scenario.getArtifactsPerSM(); iArtifact++) {
            modules.forEach(module -> {
                final ArtifactFile file = new ArtifactFile("dummyfile.dummy", null, "multipart/form-data", artifact);
                uploadSoftwareModule.uploadArtifact(module.getModuleId(), file, null, null, null);
            });
        }

        return modules;
    }

    private void createTargets(final Scenario scenario) {
        LOGGER.info("Creating {} targets", scenario.getTargets());

        for (int i = 0; i < (scenario.getTargets() / PAGE_SIZE); i++) {

            final List<MgmtTarget> targets = targetResource.createTargets(
                    new TargetBuilder().controllerId(scenario.getTargetName()).address(scenario.getTargetAddress())
                            .buildAsList(i * PAGE_SIZE, (i + 1) * PAGE_SIZE > scenario.getTargets()
                                    ? (scenario.getTargets() - (i * PAGE_SIZE)) : PAGE_SIZE))
                    .getBody();

            if (scenario.isCreateTargetTags()) {
                final Long tagid = targetTagResource.createTargetTags(
                        new TagBuilder().name("Page " + i).description("Target tag for target page " + i).build())
                        .getBody().get(0).getTagId();

                targetTagResource
                        .assignTargets(tagid,
                                targets.stream()
                                        .map(target -> new MgmtAssignedTargetRequestBody()
                                                .setControllerId(target.getControllerId()))
                                        .collect(Collectors.toList()));
            }

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
}
