/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.scenarios;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedTargetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.google.common.collect.Lists;

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

        clientConfigurationProperties.getScenarios().forEach(this::createScenario);
    }

    private void createScenario(final Scenario scenario) {
        if (scenario.isCleanRepository()) {
            cleanRepository();
        }

        final List<Long> deviceGroupTags = createDeviceGroupTags(scenario.getDeviceGroups());
        createTargets(scenario, deviceGroupTags);
        createDistributionSets(scenario);

        if (scenario.isRunRollouts()) {
            runRollouts(scenario);
        }

        if (scenario.isRunSemiAutomaticRollouts() && !scenario.getDeviceGroups().isEmpty()) {
            runSemiAutomaticRollouts(scenario);
        }
    }

    private List<Long> createDeviceGroupTags(final List<String> deviceGroups) {
        return deviceGroups.stream()
                .map(group -> targetTagResource
                        .createTargetTags(new TagBuilder().name(group).description("Group " + group).build()).getBody()
                        .get(0).getTagId())
                .collect(Collectors.toList());
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
        LOGGER.info("Delete Rollouts");

        PagedList<MgmtRolloutResponseBody> rollouts;

        while ((rollouts = rolloutResource.getRollouts(0, PAGE_SIZE, null, null).getBody()).getTotal() > 0) {
            rollouts.getContent().parallelStream().forEach(rollout -> {
                rolloutResource.delete(rollout.getRolloutId());
                waitUntilRolloutNoLongerExists(rollout.getRolloutId());
            });
        }
    }

    private void deleteSoftwareModules() {
        LOGGER.info("Delete SoftwareModules");
        PagedList<MgmtSoftwareModule> modules;
        do {
            modules = softwareModuleResource.getSoftwareModules(0, PAGE_SIZE, null, null).getBody();
            modules.getContent().parallelStream()
                    .forEach(module -> softwareModuleResource.deleteSoftwareModule(module.getModuleId()));
        } while (modules.getTotal() > PAGE_SIZE);
    }

    private void deleteDistributionSets() {
        LOGGER.info("Delete DistributionSets");

        PagedList<MgmtDistributionSet> distributionSets;
        do {
            distributionSets = distributionSetResource.getDistributionSets(0, PAGE_SIZE, null, null).getBody();
            distributionSets.getContent().parallelStream()
                    .forEach(set -> distributionSetResource.deleteDistributionSet(set.getDsId()));
        } while (distributionSets.getTotal() > PAGE_SIZE);

        deleteDistributionSetTags();
    }

    private void deleteDistributionSetTags() {
        PagedList<MgmtTag> dsTags;
        do {
            dsTags = distributionSetTagResource.getDistributionSetTags(0, PAGE_SIZE, null, null).getBody();
            dsTags.getContent().parallelStream()
                    .forEach(ds -> distributionSetTagResource.deleteDistributionSetTag(ds.getTagId()));
        } while (dsTags.getTotal() > PAGE_SIZE);
    }

    private void deleteTargets() {
        LOGGER.info("Delete Targets");

        PagedList<MgmtTarget> targets;
        do {
            targets = targetResource.getTargets(0, PAGE_SIZE, null, null).getBody();
            targets.getContent().parallelStream()
                    .forEach(target -> targetResource.deleteTarget(target.getControllerId()));
        } while (targets.getTotal() > PAGE_SIZE);

        deleteTargetTags();
    }

    private void deleteTargetTags() {
        PagedList<MgmtTag> targetTags;
        do {
            targetTags = targetTagResource.getTargetTags(0, PAGE_SIZE, null, null).getBody();
            targetTags.getContent().parallelStream()
                    .forEach(target -> targetTagResource.deleteTargetTag(target.getTagId()));
        } while (targetTags.getTotal() > PAGE_SIZE);
    }

    private void runRollouts(final Scenario scenario) {
        distributionSetResource.getDistributionSets(0, scenario.getDistributionSets(), null, null).getBody()
                .getContent().forEach(set -> runRollout(set, scenario));
    }

    private void runSemiAutomaticRollouts(final Scenario scenario) {
        distributionSetResource.getDistributionSets(0, scenario.getDistributionSets(), null, null).getBody()
                .getContent().forEach(set -> runSemiAutomaticRollout(set, scenario));
    }

    private void runSemiAutomaticRollout(final MgmtDistributionSet set, final Scenario scenario) {
        LOGGER.info("Run semi automatic rollout for set {}", set.getDsId());
        // create a Rollout
        final MgmtRolloutResponseBody rolloutResponseBody = rolloutResource.create(new RolloutBuilder()
                .name("SemiAutomaticRollout" + set.getName() + set.getVersion())
                .semiAutomaticGroups(createRolloutGroups(scenario)).targetFilterQuery("name==*")
                .distributionSetId(set.getDsId())
                .successThreshold(String.valueOf(scenario.getRolloutSuccessThreshold())).errorThreshold("5").build())
                .getBody();

        waitUntilRolloutIsReady(rolloutResponseBody.getRolloutId());

        // start the created Rollout
        rolloutResource.start(rolloutResponseBody.getRolloutId());

        waitUntilRolloutIsComplete(rolloutResponseBody.getRolloutId());
        LOGGER.info("Run rollout for set {} -> Done", set.getDsId());
    }

    private static List<MgmtRolloutGroup> createRolloutGroups(final Scenario scenario) {
        final List<MgmtRolloutGroup> result = Lists
                .newArrayListWithExpectedSize((scenario.getDeviceGroups().size() * 3) + 1);

        scenario.getDeviceGroups().forEach(groupname -> {
            result.add(createGroup(1, groupname, 10F));
            result.add(createGroup(2, groupname, 50F));
            result.add(createGroup(3, groupname, 100F));
        });
        result.add(createFinalGroup());

        return result;
    }

    private static MgmtRolloutGroup createGroup(final int number, final String groupname, final Float percent) {
        final MgmtRolloutGroup one = new MgmtRolloutGroup();
        one.setName(groupname + "-" + number);
        one.setDescription("Group of " + groupname);
        one.setTargetFilterQuery("tag==" + groupname);
        one.setTargetPercentage(percent);
        return one;
    }

    private static MgmtRolloutGroup createFinalGroup() {
        final MgmtRolloutGroup one = new MgmtRolloutGroup();
        one.setName("final");
        one.setDescription("Group of non tagged devices");
        one.setTargetFilterQuery("name==*");
        one.setTargetPercentage(100F);
        return one;
    }

    private void runRollout(final MgmtDistributionSet set, final Scenario scenario) {
        LOGGER.info("Run rollout for set {}", set.getDsId());
        // create a Rollout
        final MgmtRolloutResponseBody rolloutResponseBody = rolloutResource.create(new RolloutBuilder()
                .name("Rollout" + set.getName() + set.getVersion()).groupSize(scenario.getRolloutDeploymentGroups())
                .targetFilterQuery("name==*").distributionSetId(set.getDsId())
                .successThreshold(String.valueOf(scenario.getRolloutSuccessThreshold())).errorThreshold("5").build())
                .getBody();

        waitUntilRolloutIsReady(rolloutResponseBody.getRolloutId());

        // start the created Rollout
        rolloutResource.start(rolloutResponseBody.getRolloutId());

        waitUntilRolloutIsComplete(rolloutResponseBody.getRolloutId());
        LOGGER.info("Run rollout for set {} -> Done", set.getDsId());
    }

    private void waitUntilRolloutNoLongerExists(final Long id) {
        do {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (final InterruptedException e) {
                LOGGER.warn("Interrupted!");
                Thread.currentThread().interrupt();
            }
        } while (rolloutResource.getRollout(id).getStatusCode() != HttpStatus.NOT_FOUND);
    }

    private void waitUntilRolloutIsComplete(final Long id) {
        do {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (final InterruptedException e) {
                LOGGER.warn("Interrupted!");
                Thread.currentThread().interrupt();
            }
        } while (!"FINISHED".equalsIgnoreCase(rolloutResource.getRollout(id).getBody().getStatus()));
    }

    private void waitUntilRolloutIsReady(final Long id) {
        do {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (final InterruptedException e) {
                LOGGER.warn("Interrupted!");
                Thread.currentThread().interrupt();
            }
        } while (!"READY".equalsIgnoreCase(rolloutResource.getRollout(id).getBody().getStatus()));
    }

    private void createDistributionSets(final Scenario scenario) {
        LOGGER.info("Creating {} distribution sets", scenario.getDistributionSets());
        final BigDecimal pages = new BigDecimal(scenario.getDistributionSets())
                .divide(new BigDecimal(PAGE_SIZE), BigDecimal.ROUND_UP).max(new BigDecimal(1));

        IntStream.range(0, pages.intValue()).parallel().forEach(i -> createDistributionSetPage(scenario, i));
        LOGGER.info("Creating {} distribution sets -> Done", scenario.getDistributionSets());
    }

    private void createDistributionSetPage(final Scenario scenario, final int page) {

        final List<MgmtDistributionSet> sets = distributionSetResource
                .createDistributionSets(new DistributionSetBuilder().name(scenario.getDsName()).type("os_app")
                        .version("1.0.").buildAsList(calculateOffset(page),
                                (page + 1) * PAGE_SIZE > scenario.getDistributionSets()
                                        ? (scenario.getDistributionSets() - calculateOffset(page)) : PAGE_SIZE))
                .getBody();

        assignSoftwareModulesTo(scenario, sets);

        tagDistributionSets(page, sets);

    }

    private void tagDistributionSets(final int page, final List<MgmtDistributionSet> sets) {
        final MgmtTag tag = distributionSetTagResource
                .createDistributionSetTags(
                        new TagBuilder().name("Page " + page).description("DS tag for DS page" + page).build())
                .getBody().get(0);

        distributionSetTagResource.assignDistributionSets(tag.getTagId(),
                sets.stream()
                        .map(set -> new MgmtAssignedDistributionSetRequestBody().setDistributionSetId(set.getDsId()))
                        .collect(Collectors.toList()));
    }

    private void assignSoftwareModulesTo(final Scenario scenario, final List<MgmtDistributionSet> sets) {
        final byte[] artifact = generateArtifact(scenario);
        sets.forEach(dsSet -> {
            final List<MgmtSoftwareModule> modules = addModules(scenario, dsSet, artifact);

            final SoftwareModuleAssigmentBuilder assign = new SoftwareModuleAssigmentBuilder();
            modules.forEach(module -> assign.id(module.getModuleId()));
            distributionSetResource.assignSoftwareModules(dsSet.getDsId(), assign.build());
        });
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
            final int count = iArtifact;
            modules.forEach(module -> {
                final ArtifactFile file = new ArtifactFile("dummyfile.dummy" + count, null, "multipart/form-data",
                        artifact);
                uploadSoftwareModule.uploadArtifact(module.getModuleId(), file, null, null, null);
            });
        }

        return modules;
    }

    private void createTargets(final Scenario scenario, final List<Long> deviceGroupTags) {
        LOGGER.info("Creating {} targets", scenario.getTargets());
        final BigDecimal pages = new BigDecimal(scenario.getTargets())
                .divide(new BigDecimal(PAGE_SIZE), BigDecimal.ROUND_UP).max(new BigDecimal(1));

        IntStream.range(0, pages.intValue()).parallel().forEach(i -> createTargetPage(scenario, i, deviceGroupTags));
        LOGGER.info("Creating {} targets -> Done", scenario.getTargets());
    }

    private void createTargetPage(final Scenario scenario, final int page, final List<Long> deviceGroupTags) {
        final List<MgmtTarget> targets = createTargets(scenario, page);

        tagTargets(scenario, page, targets, deviceGroupTags);

    }

    private List<MgmtTarget> createTargets(final Scenario scenario, final int page) {
        return targetResource
                .createTargets(
                        new TargetBuilder().controllerId(scenario.getTargetName()).address(scenario.getTargetAddress())
                                .buildAsList(calculateOffset(page),
                                        (page + 1) * PAGE_SIZE > scenario.getTargets()
                                                ? (scenario.getTargets() - calculateOffset(page)) : PAGE_SIZE))
                .getBody();
    }

    private void tagTargets(final Scenario scenario, final int page, final List<MgmtTarget> targets,
            final List<Long> deviceGroupTags) {
        if (scenario.getTargetTags() > 0) {
            targetTagResource
                    .createTargetTags(new TagBuilder().name("Page " + page)
                            .description("Target tag for target page " + page).buildAsList(scenario.getTargetTags()))
                    .getBody()
                    .forEach(tag -> targetTagResource.assignTargets(tag.getTagId(), targets.stream().map(
                            target -> new MgmtAssignedTargetRequestBody().setControllerId(target.getControllerId()))
                            .collect(Collectors.toList())));
        }

        if (!deviceGroupTags.isEmpty()) {
            final Long tagid = deviceGroupTags.get(new SecureRandom().nextInt(deviceGroupTags.size()));

            targetTagResource.assignTargets(tagid,
                    targets.stream().map(
                            target -> new MgmtAssignedTargetRequestBody().setControllerId(target.getControllerId()))
                            .collect(Collectors.toList()));
        }
    }

    private static int calculateOffset(final int page) {
        return page * PAGE_SIZE;
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
