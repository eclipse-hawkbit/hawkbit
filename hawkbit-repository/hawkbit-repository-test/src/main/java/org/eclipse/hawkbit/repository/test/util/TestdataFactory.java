/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.context.AccessContext.asSystem;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.Constants;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValueCreate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * Data generator utility for tests.
 */
@Service
@Profile("test")
@SuppressWarnings("java:S107")
public class TestdataFactory {

    @SuppressWarnings("java:S2245") // used for tests only, no need of secure random
    public static final Random RND = new Random();

    public static final String VISIBLE_SM_MD_KEY = "visibleMetdataKey";
    public static final String VISIBLE_SM_MD_VALUE = "visibleMetdataValue";
    public static final String INVISIBLE_SM_MD_KEY = "invisibleMetdataKey";
    public static final String INVISIBLE_SM_MD_VALUE = "invisibleMetdataValue";

    public static final AtomicLong COUNTER = new AtomicLong();

    /**
     * default {@link Target#getControllerId()}.
     */
    public static final String DEFAULT_CONTROLLER_ID = "targetExist";

    /**
     * Default {@link SoftwareModule#getVendor()}.
     */
    public static final String DEFAULT_VENDOR = "Vendor Limited, California";

    /**
     * Default {@link NamedVersionedEntity#getVersion()}.
     */
    public static final String DEFAULT_VERSION = "1.0";

    /**
     * Default {@link NamedEntity#getDescription()}.
     */
    public static final String DEFAULT_DESCRIPTION = "Desc: " + randomDescriptionShort();

    /**
     * Key of test default {@link DistributionSetType}.
     */
    public static final String DS_TYPE_DEFAULT = "test_default_ds_type";

    /**
     * Key of test "os" {@link SoftwareModuleType} : mandatory firmware in {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_OS = "os";

    /**
     * Key of test "runtime" {@link SoftwareModuleType} : optional firmware in {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_RT = "runtime";

    /**
     * Key of test "application" {@link SoftwareModuleType} : optional software in {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_APP = "application";

    public static final String DEFAULT_COLOUR = "#000000";

    private static final String SPACE_AND_DESCRIPTION = " description";

    private final ControllerManagement controllerManagement;
    private final ArtifactManagement artifactManagement;
    private final SoftwareModuleManagement<?> softwareModuleManagement;
    private final SoftwareModuleTypeManagement<?> softwareModuleTypeManagement;
    private final DistributionSetManagement<?> distributionSetManagement;
    private final DistributionSetTagManagement<?> distributionSetTagManagement;
    private final DistributionSetTypeManagement<?> distributionSetTypeManagement;
    private final DistributionSetInvalidationManagement distributionSetInvalidationManagement;
    private final TargetManagement<? extends Target> targetManagement;
    private final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    private final TargetTypeManagement<? extends TargetType> targetTypeManagement;
    private final TargetTagManagement<? extends TargetTag> targetTagManagement;
    private final DeploymentManagement deploymentManagement;
    private final RolloutManagement rolloutManagement;
    private final RolloutHandler rolloutHandler;
    private final QuotaManagement quotaManagement;

    public TestdataFactory(
            final ControllerManagement controllerManagement, final ArtifactManagement artifactManagement,
            final SoftwareModuleManagement<? extends SoftwareModule> softwareModuleManagement,
            final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final DistributionSetTypeManagement<? extends DistributionSetType> distributionSetTypeManagement,
            final DistributionSetTagManagement<? extends DistributionSetTag> distributionSetTagManagement,
            final DistributionSetInvalidationManagement distributionSetInvalidationManagement,
            final TargetManagement<? extends Target> targetManagement,
            final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement,
            final TargetTypeManagement<? extends TargetType> targetTypeManagement,
            final TargetTagManagement<? extends TargetTag> targetTagManagement,
            final DeploymentManagement deploymentManagement,
            final RolloutManagement rolloutManagement, final RolloutHandler rolloutHandler,
            final QuotaManagement quotaManagement) {
        this.controllerManagement = controllerManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.distributionSetInvalidationManagement = distributionSetInvalidationManagement;
        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.targetTypeManagement = targetTypeManagement;
        this.targetTagManagement = targetTagManagement;
        this.deploymentManagement = deploymentManagement;
        this.artifactManagement = artifactManagement;
        this.rolloutManagement = rolloutManagement;
        this.rolloutHandler = rolloutHandler;
        this.quotaManagement = quotaManagement;
    }

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static String randomString(final int len) {
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHABET.charAt(RND.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static byte[] randomBytes(final int len) {
        return randomString(len).getBytes();
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix) {
        return createDistributionSet(prefix, DEFAULT_VERSION, false);
    }

    public DistributionSet createDistributionSetLocked(final String prefix) {
        return distributionSetManagement.lock(createDistributionSet(prefix));
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet() {
        return createDistributionSet(UUID.randomUUID().toString(), DEFAULT_VERSION, false);
    }

    public DistributionSet createDistributionSetLocked() {
        return distributionSetManagement.lock(createDistributionSet());
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param modules of {@link DistributionSet#getModules()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final Collection<SoftwareModule> modules) {
        return createDistributionSet("", DEFAULT_VERSION, false, modules);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param modules of {@link DistributionSet#getModules()}
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final Collection<SoftwareModule> modules, final String prefix) {
        return createDistributionSet(prefix, DEFAULT_VERSION, false, modules);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION}.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @param isRequiredMigrationStep for {@link DistributionSet#isRequiredMigrationStep()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final boolean isRequiredMigrationStep) {
        return createDistributionSet(prefix, DEFAULT_VERSION, isRequiredMigrationStep);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @param tags DistributionSet tags
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final Collection<DistributionSetTag> tags) {
        return createDistributionSet(prefix, DEFAULT_VERSION, tags);
    }

    /**
     * Creates {@link DistributionSet} in repository including three {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP}.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @param version {@link DistributionSet#getVersion()} and {@link SoftwareModule#getVersion()} extended by a random number.
     * @param isRequiredMigrationStep for {@link DistributionSet#isRequiredMigrationStep()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final String version, final boolean isRequiredMigrationStep) {
        final SoftwareModule appMod = softwareModuleManagement.create(
                SoftwareModuleManagement.Create.builder()
                        .type(findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE))
                        .name(prefix + SM_TYPE_APP)
                        .version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong())
                        .vendor(prefix + " vendor Limited, California")
                        .build());
        final SoftwareModule runtimeMod = softwareModuleManagement
                .create(SoftwareModuleManagement.Create.builder()
                        .type(findOrCreateSoftwareModuleType(SM_TYPE_RT))
                        .name(prefix + "app runtime")
                        .version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(prefix + " vendor GmbH, Stuttgart, Germany")
                        .build());
        final SoftwareModule osMod = softwareModuleManagement
                .create(SoftwareModuleManagement.Create.builder()
                        .type(findOrCreateSoftwareModuleType(SM_TYPE_OS))
                        .name(prefix + " Firmware")
                        .version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(prefix + " vendor Limited Inc, California")
                        .build());

        return distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .type(findOrCreateDefaultTestDsType())
                        .name(ObjectUtils.isEmpty(prefix) ? "DS" : prefix)
                        .version(version)
                        .description(randomDescriptionShort())
                        .modules(Set.of(osMod, runtimeMod, appMod))
                        .requiredMigrationStep(isRequiredMigrationStep)
                        .build());
    }

    /**
     * Adds software module metadata to every module of given {@link DistributionSet}.
     * <p/>
     * {@link #VISIBLE_SM_MD_VALUE}, {@link #VISIBLE_SM_MD_KEY} with
     * {@link MetadataValueCreate#isTargetVisible()} and
     * {@link #INVISIBLE_SM_MD_KEY}, {@link #INVISIBLE_SM_MD_VALUE} without
     * {@link MetadataValueCreate#isTargetVisible()}
     *
     * @param set to add metadata to
     */
    public void addSoftwareModuleMetadata(final DistributionSet set) {
        set.getModules().forEach(this::addTestModuleMetadata);
    }

    /**
     * Creates {@link DistributionSet} in repository.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @param version {@link DistributionSet#getVersion()} and {@link SoftwareModule#getVersion()} extended by a random number.
     * @param isRequiredMigrationStep for {@link DistributionSet#isRequiredMigrationStep()}
     * @param modules for {@link DistributionSet#getModules()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(
            final String prefix, final String version, final boolean isRequiredMigrationStep, final Collection<SoftwareModule> modules) {
        return distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .type(findOrCreateDefaultTestDsType())
                        .name(prefix != null && !prefix.isEmpty() ? prefix : "DS")
                        .version(version).description(randomDescriptionShort())
                        .modules(new HashSet<>(modules))
                        .requiredMigrationStep(isRequiredMigrationStep)
                        .build());
    }

    /**
     * Creates {@link DistributionSet} in repository including three {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT} ,
     * {@link #SM_TYPE_APP}.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @param version {@link DistributionSet#getVersion()} and {@link SoftwareModule#getVersion()} extended by a random number.
     * @param tags DistributionSet tags
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final String version, final Collection<DistributionSetTag> tags) {
        final DistributionSet set = createDistributionSet(prefix, version, false);
        tags.forEach(tag -> distributionSetManagement.assignTag(List.of(set.getId()), tag.getId()));
        return distributionSetManagement.find(set.getId()).orElseThrow();
    }

    /**
     * Creates {@link DistributionSet}s in repository including three {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT},
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     *
     * @param number of {@link DistributionSet}s to create
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSets(final int number) {
        return createDistributionSets("", number);
    }

    /**
     * Create a list of {@link DistributionSet}s without modules, i.e. incomplete.
     *
     * @param number of {@link DistributionSet}s to create
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSetsWithoutModules(final int number) {
        final List<DistributionSet> sets = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            sets.add(distributionSetManagement.create(
                    DistributionSetManagement.Create.builder()
                            .type(findOrCreateDefaultTestDsType())
                            .name("DS" + i)
                            .version(DEFAULT_VERSION + "." + i)
                            .description(randomDescriptionShort())
                            .build()));
        }
        return sets;
    }

    /**
     * Creates {@link DistributionSet}s in repository including three {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT},
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an iterative count and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name, vendor and description.
     * @param count of {@link DistributionSet}s to create
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSets(final String prefix, final int count) {
        final List<DistributionSet> sets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sets.add(createDistributionSet(prefix, DEFAULT_VERSION + "." + i, false));
        }
        return sets;
    }

    /**
     * Creates {@link DistributionSet}s in repository with {@link #DEFAULT_DESCRIPTION} and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     *
     * @param name {@link DistributionSet#getName()}
     * @param version {@link DistributionSet#getVersion()}
     * @return {@link DistributionSet} entity
     */
    public DistributionSet createDistributionSetWithNoSoftwareModules(final String name, final String version) {
        return distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .type(findOrCreateDefaultTestDsType())
                        .name(name)
                        .version(version)
                        .description(DEFAULT_DESCRIPTION)
                        .build());
    }

    /**
     * Creates {@link Artifact}s for given {@link SoftwareModule} with a small text payload.
     *
     * @param moduleId the {@link Artifact}s belong to.
     * @return {@link Artifact} entity.
     */
    public List<Artifact> createArtifacts(final Long moduleId) {
        final List<Artifact> artifacts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final String artifactData = "some test data" + i;
            artifacts.add(createArtifact(artifactData, moduleId, "filename" + i));
        }
        return artifacts;
    }

    /**
     * Create an {@link Artifact} for given {@link SoftwareModule} with a small text payload.
     *
     * @param artifactData the {@link Artifact} InputStream
     * @param moduleId the {@link Artifact} belongs to
     * @param filename that was provided during upload.
     * @return {@link Artifact} entity.
     */
    public Artifact createArtifact(final String artifactData, final Long moduleId, final String filename) {
        final InputStream stubInputStream = IOUtils.toInputStream(artifactData, StandardCharsets.UTF_8);
        return artifactManagement.create(new ArtifactUpload(stubInputStream, null, artifactData.length(), null, moduleId, filename, false));
    }

    /**
     * Create an {@link Artifact} for given {@link SoftwareModule} with a small text payload.
     *
     * @param artifactData the {@link Artifact} InputStream
     * @param moduleId the {@link Artifact} belongs to
     * @param filename that was provided during upload.
     * @param fileSize the file size
     * @return {@link Artifact} entity.
     */
    public Artifact createArtifact(final byte[] artifactData, final Long moduleId, final String filename, final int fileSize) {
        return artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(artifactData), null, fileSize, null, moduleId, filename, false));
    }

    /**
     * Creates {@link SoftwareModule} with {@link #DEFAULT_VENDOR} and {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @param typeKey of the {@link SoftwareModuleType}
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModule(final String typeKey) {
        return createSoftwareModule(typeKey, "", false);
    }

    /**
     * Creates {@link SoftwareModule} of type {@value Constants#SMT_DEFAULT_APP_KEY} with {@link #DEFAULT_VENDOR} and {@link #DEFAULT_VERSION}
     * and random generated {@link Target#getDescription()} in the repository.
     *
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleApp() {
        return createSoftwareModule(Constants.SMT_DEFAULT_APP_KEY, "", false);
    }

    /**
     * Creates {@link SoftwareModule} of type {@value Constants#SMT_DEFAULT_APP_KEY} with {@link #DEFAULT_VENDOR} and {@link #DEFAULT_VERSION}
     * and random generated {@link Target#getDescription()} in the repository.
     *
     * @param prefix added to name and version
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleApp(final String prefix) {
        return createSoftwareModule(Constants.SMT_DEFAULT_APP_KEY, prefix, false);
    }

    /**
     * Creates {@link SoftwareModule} of type {@value Constants#SMT_DEFAULT_OS_KEY} with {@link #DEFAULT_VENDOR} and {@link #DEFAULT_VERSION}
     * and random generated {@link Target#getDescription()} in the repository.
     *
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleOs() {
        return createSoftwareModule(Constants.SMT_DEFAULT_OS_KEY, "", false);
    }

    /**
     * Creates {@link SoftwareModule} of type {@value Constants#SMT_DEFAULT_OS_KEY} with {@link #DEFAULT_VENDOR} and {@link #DEFAULT_VERSION}
     * and random generated {@link Target#getDescription()} in the repository.
     *
     * @param prefix added to name and version
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleOs(final String prefix) {
        return createSoftwareModule(Constants.SMT_DEFAULT_OS_KEY, prefix, false);
    }

    /**
     * Creates {@link SoftwareModule} with {@link #DEFAULT_VENDOR} and {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @param typeKey of the {@link SoftwareModuleType}
     * @param prefix added to name and version
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModule(final String typeKey, final String prefix, final boolean encrypted) {
        return softwareModuleManagement.create(
                SoftwareModuleManagement.Create.builder()
                        .type(findOrCreateSoftwareModuleType(typeKey))
                        .name(prefix + typeKey + "_" + COUNTER.incrementAndGet())
                        .version(prefix + DEFAULT_VERSION)
                        .description(randomDescriptionShort())
                        .vendor(DEFAULT_VENDOR)
                        .encrypted(encrypted)
                        .build());
    }

    /**
     * @return persisted {@link Target} with {@link #DEFAULT_CONTROLLER_ID}.
     */
    public Target createTarget() {
        return createTarget(DEFAULT_CONTROLLER_ID);
    }

    /**
     * @param controllerId of the target
     * @return persisted {@link Target}
     */
    public Target createTarget(final String controllerId) {
        return createTarget(controllerId, controllerId);
    }

    /**
     * @param controllerId of the target
     * @param targetName name of the target
     * @return persisted {@link Target}
     */
    public Target createTarget(final String controllerId, final String targetName) {
        final Target target = targetManagement.create(TargetManagement.Create.builder().controllerId(controllerId).name(targetName).build());
        assertTargetProperlyCreated(target);
        return target;
    }

    public Target createTarget(final String controllerId, final String targetName, final String address) {
        final Target target = targetManagement.create(
                TargetManagement.Create.builder().controllerId(controllerId).name(targetName).address(address).build());
        assertTargetProperlyCreated(target);
        return target;
    }

    public Target createTarget(final String controllerId, final String targetName, final TargetType targetType) {
        final Target target = targetManagement.create(
                TargetManagement.Create.builder().controllerId(controllerId).name(targetName).targetType(targetType).build());
        assertTargetProperlyCreated(target);
        return target;
    }

    /**
     * Creates {@link DistributionSet}s in repository including three {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT},
     * {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     * <p/>
     * In addition, it updates the created {@link DistributionSet}s and {@link SoftwareModule}s to ensure that
     * {@link BaseEntity#getLastModifiedAt()} and {@link BaseEntity#getLastModifiedBy()} is filled.
     *
     * @return persisted {@link DistributionSet}.
     */
    public DistributionSet createUpdatedDistributionSet() {
        DistributionSet set = createDistributionSet("");
        set = distributionSetManagement.update(
                DistributionSetManagement.Update.builder().id(set.getId()).description("Updated " + DEFAULT_DESCRIPTION).build());

        set.getModules().forEach(module -> softwareModuleManagement.update(
                SoftwareModuleManagement.Update.builder().id(module.getId()).description("Updated " + DEFAULT_DESCRIPTION).build()));

        // load also lazy stuff
        return distributionSetManagement.getWithDetails(set.getId());
    }

    /**
     * @return {@link DistributionSetType} with key {@link #DS_TYPE_DEFAULT} and {@link SoftwareModuleType}s {@link #SM_TYPE_OS},
     *             {@link #SM_TYPE_RT} , {@link #SM_TYPE_APP}.
     */
    public DistributionSetType findOrCreateDefaultTestDsType() {
        final List<SoftwareModuleType> swt = new ArrayList<>();
        swt.add(findOrCreateSoftwareModuleType(SM_TYPE_OS));

        final List<SoftwareModuleType> opt = new ArrayList<>();
        opt.add(findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE));
        opt.add(findOrCreateSoftwareModuleType(SM_TYPE_RT));

        return findOrCreateDistributionSetType(DS_TYPE_DEFAULT, "OS (FW) mandatory, runtime (FW) and app (SW) optional", swt, opt);
    }

    /**
     * Creates {@link DistributionSetType} in repository.
     *
     * @param dsTypeKey {@link DistributionSetType#getKey()}
     * @param dsTypeName {@link DistributionSetType#getName()}
     * @return persisted {@link DistributionSetType}
     */
    public DistributionSetType findOrCreateDistributionSetType(final String dsTypeKey, final String dsTypeName) {
        return distributionSetTypeManagement.findByKey(dsTypeKey)
                .map(DistributionSetType.class::cast)
                .orElseGet(() -> distributionSetTypeManagement.create(
                        DistributionSetTypeManagement.Create.builder()
                                .key(dsTypeKey).name(dsTypeName).description(randomDescriptionShort()).colour("black").build()));
    }

    /**
     * Finds {@link DistributionSetType} in repository with given
     * {@link DistributionSetType#getKey()} or creates if it does not exist yet.
     *
     * @param dsTypeKey {@link DistributionSetType#getKey()}
     * @param dsTypeName {@link DistributionSetType#getName()}
     * @param mandatory {@link DistributionSetType#getMandatoryModuleTypes()}
     * @param optional {@link DistributionSetType#getOptionalModuleTypes()}
     * @return persisted {@link DistributionSetType}
     */
    public DistributionSetType findOrCreateDistributionSetType(final String dsTypeKey, final String dsTypeName,
            final Collection<SoftwareModuleType> mandatory, final Collection<SoftwareModuleType> optional) {
        return distributionSetTypeManagement.findByKey(dsTypeKey)
                .map(DistributionSetType.class::cast)
                .orElseGet(() -> distributionSetTypeManagement.create(
                        DistributionSetTypeManagement.Create.builder()
                                .key(dsTypeKey)
                                .name(dsTypeName)
                                .description(randomDescriptionShort())
                                .colour("black")
                                .mandatoryModuleTypes(new HashSet<>(mandatory))
                                .optionalModuleTypes(new HashSet<>(optional))
                                .build()));
    }

    /**
     * Finds {@link SoftwareModuleType} in repository with given {@link SoftwareModuleType#getKey()} or creates if it does not exist yet with
     * {@link SoftwareModuleType#getMaxAssignments()} = 1.
     *
     * @param key {@link SoftwareModuleType#getKey()}
     * @return persisted {@link SoftwareModuleType}
     */
    public SoftwareModuleType findOrCreateSoftwareModuleType(final String key) {
        return findOrCreateSoftwareModuleType(key, 1);
    }

    /**
     * Finds {@link SoftwareModuleType} in repository with given {@link SoftwareModuleType#getKey()} or creates if it does not exist yet.
     *
     * @param key {@link SoftwareModuleType#getKey()}
     * @param maxAssignments {@link SoftwareModuleType#getMaxAssignments()}
     * @return persisted {@link SoftwareModuleType}
     */
    public SoftwareModuleType findOrCreateSoftwareModuleType(final String key, final int maxAssignments) {
        return softwareModuleTypeManagement.findByKey(key)
                .map(SoftwareModuleType.class::cast)
                .orElseGet(() -> softwareModuleTypeManagement.create(
                        SoftwareModuleTypeManagement.Create.builder()
                                .key(key)
                                .name(key)
                                .description(randomDescriptionShort()).colour("#ffffff")
                                .maxAssignments(maxAssignments)
                                .build()));
    }

    /**
     * Creates a {@link DistributionSet}.
     *
     * @param name {@link DistributionSet#getName()}
     * @param version {@link DistributionSet#getVersion()}
     * @param type {@link DistributionSet#getType()}
     * @param modules {@link DistributionSet#getModules()}
     * @return the created {@link DistributionSet}
     */
    public DistributionSet createDistributionSet(
            final String name, final String version, final DistributionSetType type, final Collection<SoftwareModule> modules) {
        return distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .type(type)
                        .name(name)
                        .version(version)
                        .description(randomDescriptionShort())
                        .modules(new HashSet<>(modules))
                        .build());
    }

    /**
     * Generates {@link DistributionSet} object without persisting it.
     *
     * @param name {@link DistributionSet#getName()}
     * @param version {@link DistributionSet#getVersion()}
     * @param type {@link DistributionSet#getType()}
     * @param modules {@link DistributionSet#getModules()}
     * @param requiredMigrationStep {@link DistributionSet#isRequiredMigrationStep()}
     * @return the created {@link DistributionSet}
     */
    public DistributionSetManagement.Create generateDistributionSet(
            final String name, final String version, final DistributionSetType type, final Collection<SoftwareModule> modules,
            final boolean requiredMigrationStep) {
        return DistributionSetManagement.Create.builder()
                .type(type)
                .name(name)
                .version(version)
                .description(randomDescriptionShort())
                .modules(new HashSet<>(modules))
                .requiredMigrationStep(requiredMigrationStep)
                .build();
    }

    /**
     * Generates {@link DistributionSet} object without persisting it.
     *
     * @param name {@link DistributionSet#getName()}
     * @param version {@link DistributionSet#getVersion()}
     * @param type {@link DistributionSet#getType()}
     * @param modules {@link DistributionSet#getModules()}
     * @return the created {@link DistributionSet}
     */
    public DistributionSetManagement.Create generateDistributionSet(
            final String name, final String version, final DistributionSetType type, final Collection<SoftwareModule> modules) {
        return generateDistributionSet(name, version, type, modules, false);
    }

    /**
     * builder method for generating a {@link DistributionSet}.
     *
     * @param name {@link DistributionSet#getName()}
     * @return the generated {@link DistributionSet}
     */
    public DistributionSetManagement.Create generateDistributionSet(final String name) {
        return generateDistributionSet(name, DEFAULT_VERSION, findOrCreateDefaultTestDsType(), Collections.emptyList(), false);
    }

    /**
     * Creates {@link Target}s in repository and with {@link #DEFAULT_CONTROLLER_ID} as prefix for {@link Target#getControllerId()}.
     *
     * @param number of {@link Target}s to create
     * @return {@link List} of {@link Target} entities
     */
    public List<Target> createTargets(final int number) {
        return createTargets(DEFAULT_CONTROLLER_ID, number);
    }

    public List<Target> createTargets(final String prefix, final int number) {
        return createTargets(prefix, 0, number);
    }

    public List<Target> createTargets(final String prefix, final int offset, final int number) {
        final List<TargetManagement.Create> targets = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            targets.add(TargetManagement.Create.builder().controllerId(prefix + (offset + i)).build());
        }
        return createTargets(targets);
    }

    /**
     * Creates {@link Target}s in repository and with {@link TargetType}.
     *
     * @param number of {@link Target}s to create
     * @param controllerIdPrefix prefix for the controller id
     * @param targetType targetType of targets to create
     * @return {@link List} of {@link Target} entities
     */
    public List<Target> createTargetsWithType(final int number, final String controllerIdPrefix, final TargetType targetType) {
        final List<TargetManagement.Create> targets = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            targets.add(TargetManagement.Create.builder().controllerId(controllerIdPrefix + i).targetType(targetType).build());
        }
        return createTargets(targets);
    }

    /**
     * Creates {@link Target}s in repository and with given targetIds.
     *
     * @param targetIds specifies the IDs of the targets
     * @return {@link List} of {@link Target} entities
     */
    public List<Target> createTargets(final String... targetIds) {
        final List<TargetManagement.Create> targets = new ArrayList<>();
        for (final String targetId : targetIds) {
            targets.add(TargetManagement.Create.builder().controllerId(targetId).build());
        }
        return createTargets(targets);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets number of targets to create
     * @param prefix prefix used for the controller ID and description
     * @return list of {@link Target}
     */
    public List<Target> createTargets(final int numberOfTargets, final String prefix) {
        return createTargets(numberOfTargets, prefix, prefix);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets number of targets to create
     * @param controllerIdPrefix prefix used for the controller ID
     * @param descriptionPrefix prefix used for the description
     * @return list of {@link Target}
     */
    public List<Target> createTargets(final int numberOfTargets, final String controllerIdPrefix, final String descriptionPrefix) {
        final List<TargetManagement.Create> targets = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> TargetManagement.Create.builder()
                        .controllerId(String.format("%s-%05d", controllerIdPrefix, i))
                        .description(descriptionPrefix + i)
                        .build())
                .map(TargetManagement.Create.class::cast)
                .toList();
        return createTargets(targets);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets number of targets to create
     * @param controllerIdPrefix prefix used for the controller ID
     * @param descriptionPrefix prefix used for the description
     * @param lastTargetQuery last time the target polled
     * @return list of {@link Target}
     */
    public List<Target> createTargets(
            final int numberOfTargets, final String controllerIdPrefix, final String descriptionPrefix, final Long lastTargetQuery) {
        final List<TargetManagement.Create> targets = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> TargetManagement.Create.builder()
                        .controllerId(String.format("%s-%05d", controllerIdPrefix, i))
                        .description(descriptionPrefix + i).lastTargetQuery(lastTargetQuery)
                        .build())
                .map(TargetManagement.Create.class::cast)
                .toList();
        return createTargets(targets);
    }

    /**
     * Create a set of {@link TargetTag}s.
     *
     * @param number number of {@link TargetTag}. to be created
     * @param tagPrefix prefix for the {@link TargetTag#getName()}
     * @return the created set of {@link TargetTag}s
     */
    public List<? extends TargetTag> createTargetTags(final int number, final String tagPrefix) {
        final List<TargetTagManagement.Create> result = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            result.add(TargetTagManagement.Create.builder().name(tagPrefix + i).description(tagPrefix + i).colour(String.valueOf(i)).build());
        }
        return targetTagManagement.create(result);
    }

    /**
     * Creates {@link DistributionSetTag}s in repository.
     *
     * @param number of {@link DistributionSetTag}s
     * @return the persisted {@link DistributionSetTag}s
     */
    public List<DistributionSetTag> createDistributionSetTags(final int number) {
        final List<DistributionSetTagManagement.Create> result = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            result.add(
                    DistributionSetTagManagement.Create.builder().name("tag" + i).description("tagdesc" + i).colour(String.valueOf(i)).build());
        }
        return distributionSetTagManagement.create(result).stream().map(DistributionSetTag.class::cast).toList();
    }

    /**
     * Append {@link ActionStatus} to all {@link Action}s of given {@link Target}s.
     *
     * @param targets to add {@link ActionStatus}
     * @param status to add
     * @param message to add
     */
    public void sendUpdateActionStatusToTargets(final Collection<Target> targets, final Status status, final String message) {
        sendUpdateActionStatusToTargets(targets, status, List.of(message));
    }

    /**
     * Append {@link ActionStatus} to all {@link Action}s of given {@link Target}s.
     *
     * @param targets to add {@link ActionStatus}
     * @param status to add
     * @param msgs to add
     * @return updated {@link Action}.
     */
    public List<Action> sendUpdateActionStatusToTargets(final Collection<Target> targets, final Status status, final Collection<String> msgs) {
        final List<Action> result = new ArrayList<>();
        for (final Target target : targets) {
            deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 400)).getContent()
                    .forEach(action -> result.add(sendUpdateActionStatusToTarget(status, action, msgs)));
        }
        return result;
    }

    public TargetFilterQuery createTargetFilterWithTargetsAndActiveAutoAssignment() {
        createTargets(quotaManagement.getMaxTargetsPerAutoAssignment());
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("testName").query("id==*").build());
        return targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(createDistributionSet().getId()));
    }

    /**
     * Creates rollout based on given parameters.
     *
     * @param rolloutName of the {@link Rollout}
     * @param rolloutDescription of the {@link Rollout}
     * @param groupSize of the {@link Rollout}
     * @param filterQuery to identify the {@link Target}s
     * @param distributionSet to assign
     * @param successCondition to switch to next group
     * @param errorCondition to switch to next group
     * @return created {@link Rollout}
     */
    public Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final String errorCondition) {
        return createRolloutByVariables(rolloutName, rolloutDescription, groupSize, filterQuery, distributionSet,
                successCondition, RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP, errorCondition, Action.ActionType.FORCED, null, false);
    }

    public Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final String errorCondition, final boolean confirmationRequired) {
        return createRolloutByVariables(rolloutName, rolloutDescription, groupSize, filterQuery, distributionSet,
                successCondition, RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP, errorCondition, Action.ActionType.FORCED, null, confirmationRequired);
    }

    public Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final RolloutGroup.RolloutGroupSuccessAction successAction, final String errorCondition, final boolean confirmationRequired,
            final boolean dynamic) {
        return createRolloutByVariables(rolloutName, rolloutDescription, groupSize, filterQuery, distributionSet,
                successCondition, successAction, errorCondition, Action.ActionType.FORCED, null, confirmationRequired, dynamic);
    }

    public Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final RolloutGroup.RolloutGroupSuccessAction successAction, final String errorCondition, final Action.ActionType actionType,
            final Integer weight, final boolean confirmationRequired) {
        return createRolloutByVariables(rolloutName, rolloutDescription, groupSize, filterQuery, distributionSet,
                successCondition, successAction, errorCondition, actionType, weight, confirmationRequired, false);
    }

    /**
     * Creates rollout based on given parameters.
     *
     * @param rolloutName of the {@link Rollout}
     * @param rolloutDescription of the {@link Rollout}
     * @param groupSize of the {@link Rollout}
     * @param filterQuery to identify the {@link Target}s
     * @param distributionSet to assign
     * @param successCondition to switch to next group
     * @param errorCondition to switch to next group
     * @param actionType the type of the Rollout
     * @param weight weight of the Rollout
     * @param confirmationRequired if the confirmation is required (considered with confirmation flow active)
     * @param dynamic is dynamic
     * @return created {@link Rollout}
     */
    public Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final RolloutGroup.RolloutGroupSuccessAction successAction, final String errorCondition, final Action.ActionType actionType,
            final Integer weight, final boolean confirmationRequired, final boolean dynamic) {
        return createRolloutByVariables(rolloutName, rolloutDescription, groupSize, filterQuery, distributionSet,
                successCondition, successAction, errorCondition, actionType, weight, confirmationRequired, dynamic, null);
    }

    public Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final RolloutGroup.RolloutGroupSuccessAction successAction, final String errorCondition,
            final Action.ActionType actionType,
            final Integer weight, final boolean confirmationRequired, final boolean dynamic,
            final RolloutManagement.DynamicRolloutGroupTemplate dynamicRolloutGroupTemplate) {
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder().withDefaults()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, successCondition)
                .successAction(successAction, "")
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, errorCondition)
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        final Rollout rollout = rolloutManagement.create(
                RolloutManagement.Create.builder()
                        .name(rolloutName)
                        .description(rolloutDescription)
                        .targetFilterQuery(filterQuery)
                        .distributionSet(distributionSet)
                        .actionType(actionType == null ? Action.ActionType.FORCED : actionType)
                        .weight(weight)
                        .dynamic(dynamic)
                        .build(),
                groupSize, confirmationRequired, conditions, dynamicRolloutGroupTemplate);

        // Run here, because Scheduler is disabled during tests
        rolloutHandleAll();

        return rolloutManagement.get(rollout.getId());
    }

    /**
     * Create {@link Rollout} with a new {@link DistributionSet} and {@link Target}s.
     *
     * @param prefix for rollouts name, description, {@link Target#getControllerId()} filter
     * @return created {@link Rollout}
     */
    public Rollout createRollout(final String prefix) {
        createTargets(quotaManagement.getMaxTargetsPerRolloutGroup() * quotaManagement.getMaxRolloutGroupsPerRollout(), prefix);
        return createRolloutByVariables(prefix, prefix + SPACE_AND_DESCRIPTION,
                quotaManagement.getMaxRolloutGroupsPerRollout(), "controllerId==" + prefix + "*",
                createDistributionSet(prefix), "50", "5");
    }

    /**
     * Create {@link Rollout} with a new {@link DistributionSet} and {@link Target}s.
     *
     * @return created {@link Rollout}
     */
    public Rollout createRollout() {
        final String prefix = randomString(5);
        createTargets(quotaManagement.getMaxTargetsPerRolloutGroup() * quotaManagement.getMaxRolloutGroupsPerRollout(), prefix);
        return createRolloutByVariables(prefix, prefix + SPACE_AND_DESCRIPTION,
                quotaManagement.getMaxRolloutGroupsPerRollout(), "controllerId==" + prefix + "*",
                createDistributionSet(prefix), "50", "5");
    }

    /**
     * Create {@link Rollout} with a new {@link DistributionSet} and {@link Target}s.
     *
     * @return created {@link Rollout}
     */
    public Rollout createAndStartRollout() {
        return startAndReloadRollout(createRollout());
    }

    public Rollout startRollout(final Rollout rollout) {
        return startAndReloadRollout(rollout);
    }

    /**
     * Create the data for a simple rollout scenario
     *
     * @param amountTargetsForRollout the amount of targets used for the rollout
     * @param amountOtherTargets amount of other targets not included in the rollout
     * @param amountGroups the size of the rollout group
     * @param successCondition success condition
     * @param errorCondition error condition
     * @return the created {@link Rollout}
     */
    public Rollout createAndStartRollout(
            final int amountTargetsForRollout, final int amountOtherTargets,
            final int amountGroups, final String successCondition, final String errorCondition) {
        final Rollout createdRollout = createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout, amountOtherTargets, amountGroups, successCondition, errorCondition);
        return startAndReloadRollout(createdRollout);
    }

    /**
     * Create the data for a simple rollout scenario
     *
     * @param amountTargetsForRollout the amount of targets used for the rollout
     * @param amountOtherTargets amount of other targets not included in the rollout
     * @param amountOfGroups the size of the rollout group
     * @param successCondition success condition
     * @param errorCondition error condition
     * @return the created {@link Rollout}
     */
    public Rollout createSimpleTestRolloutWithTargetsAndDistributionSet(
            final int amountTargetsForRollout,
            final int amountOtherTargets, final int amountOfGroups, final String successCondition,
            final String errorCondition) {
        return createSimpleTestRolloutWithTargetsAndDistributionSet(
                amountTargetsForRollout, amountOtherTargets, amountOfGroups, successCondition, errorCondition, ActionType.FORCED, null);
    }

    /**
     * Create the data for a simple rollout scenario
     *
     * @param amountTargetsForRollout the amount of targets used for the rollout
     * @param amountOtherTargets amount of other targets not included in the rollout
     * @param amountOfGroups the size of the rollout group
     * @param successCondition success condition
     * @param errorCondition error condition
     * @param actionType action Type
     * @param weight weight
     * @return the created {@link Rollout}
     */
    public Rollout createSimpleTestRolloutWithTargetsAndDistributionSet(final int amountTargetsForRollout,
            final int amountOtherTargets, final int amountOfGroups, final String successCondition,
            final String errorCondition, final ActionType actionType, final Integer weight) {
        final String suffix = randomString(5);
        final DistributionSet rolloutDS = createDistributionSet("rolloutDS-" + suffix);
        createTargets(amountTargetsForRollout, "rollout-" + suffix + "-", "rollout");
        createTargets(amountOtherTargets, "others-" + suffix + "-", "rollout");
        final String filterQuery = "controllerId==rollout-" + suffix + "-*";
        return createRolloutByVariables("rollout-" + suffix, "test-rollout-description", amountOfGroups, filterQuery,
                rolloutDS, successCondition, RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP, errorCondition, actionType, weight, false);
    }

    /**
     * Create the soft deleted {@link Rollout} with a new {@link DistributionSet} and {@link Target}s.
     *
     * @param prefix for rollouts name, description, {@link Target#getControllerId()} filter
     * @return created {@link Rollout}
     */
    public Rollout createSoftDeletedRollout(final String prefix) {
        final Rollout newRollout = createRollout(prefix);
        rolloutManagement.start(newRollout.getId());
        rolloutHandleAll();
        rolloutManagement.delete(newRollout.getId());
        rolloutHandleAll();
        return newRollout;
    }

    /**
     * Finds {@link TargetType} in repository with given {@link TargetType#getName()} or creates if it does not exist yet. No ds types
     * are assigned on creation.
     *
     * @param targetTypeName {@link TargetType#getName()}
     * @return persisted {@link TargetType}
     */
    public TargetType findOrCreateTargetType(final String targetTypeName) {
        return targetTypeManagement.findByRsql("name==" + targetTypeName, Pageable.unpaged())
                .stream().findAny()
                .map(TargetType.class::cast)
                .orElseGet(() -> targetTypeManagement.create(TargetTypeManagement.Create.builder()
                        .name(targetTypeName).description(targetTypeName + SPACE_AND_DESCRIPTION)
                        .key(targetTypeName + " key").colour(DEFAULT_COLOUR)
                        .build()));
    }

    /**
     * Creates {@link TargetType} in repository with given {@link TargetType#getName()}. Compatible distribution set types are assigned
     * on creation
     *
     * @param targetTypeName {@link TargetType#getName()}
     * @return persisted {@link TargetType}
     */
    public TargetType createTargetType(final String targetTypeName, final Set<DistributionSetType> compatibleDsTypes) {
        return targetTypeManagement.create(TargetTypeManagement.Create.builder()
                .name(targetTypeName).description(targetTypeName + SPACE_AND_DESCRIPTION).colour(DEFAULT_COLOUR)
                .distributionSetTypes(compatibleDsTypes)
                .build());
    }

    /**
     * Creates {@link TargetType} in repository with given {@link TargetType#getName()}. No ds types are assigned on creation.
     *
     * @param targetTypePrefix {@link TargetType#getName()}
     * @return persisted {@link TargetType}
     */
    public List<? extends TargetType> createTargetTypes(final String targetTypePrefix, final int count) {
        final List<TargetTypeManagement.Create> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(TargetTypeManagement.Create.builder()
                    .name(targetTypePrefix + i).description(targetTypePrefix + SPACE_AND_DESCRIPTION)
                    .key(targetTypePrefix + i + " key").colour(DEFAULT_COLOUR)
                    .build());
        }
        return targetTypeManagement.create(result);
    }

    /**
     * Creates a distribution set and directly invalidates it. No actions will be canceled and no rollouts will be stopped with this
     * invalidation.
     *
     * @return created invalidated {@link DistributionSet}
     */
    public DistributionSet createAndInvalidateDistributionSet() {
        final DistributionSet distributionSet = createDistributionSet();
        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(List.of(distributionSet.getId()), ActionCancellationType.NONE));
        return distributionSetManagement.find(distributionSet.getId()).orElseThrow();
    }

    /**
     * Creates a distribution set that has no software modules assigned, so it is
     * incomplete.
     *
     * @return created incomplete {@link DistributionSet}
     */
    public DistributionSet createIncompleteDistributionSet() {
        return distributionSetManagement.create(
                DistributionSetManagement.Create.builder()
                        .name(UUID.randomUUID().toString()).version(DEFAULT_VERSION).description(randomDescriptionShort())
                        .type(findOrCreateDefaultTestDsType())
                        .requiredMigrationStep(false)
                        .build());
    }

    private static String randomDescriptionShort() {
        return randomString(100);
    }

    private static String randomDescriptionLong() {
        return randomString(200);
    }

    private void addTestModuleMetadata(final SoftwareModule module) {
        softwareModuleManagement.createMetadata(
                module.getId(),
                VISIBLE_SM_MD_KEY,
                new MetadataValueCreate(VISIBLE_SM_MD_VALUE, true));
        softwareModuleManagement.createMetadata(
                module.getId(),
                INVISIBLE_SM_MD_KEY,
                new MetadataValueCreate(INVISIBLE_SM_MD_VALUE, false));
    }

    private void assertTargetProperlyCreated(final Target target) {
        assertThat(target.getCreatedBy()).isNotNull();
        assertThat(target.getCreatedAt()).isNotNull();
        assertThat(target.getLastModifiedBy()).isNotNull();
        assertThat(target.getLastModifiedAt()).isNotNull();

        assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);
    }

    private List<Target> createTargets(final Collection<TargetManagement.Create> targetCreates) {
        // init new instance of array list since the TargetManagement#create will provide an unmodifiable list
        return new ArrayList<>(targetManagement.create(targetCreates));
    }

    private Action sendUpdateActionStatusToTarget(final Status status, final Action updActA, final Collection<String> msgs) {
        return controllerManagement.addUpdateActionStatus(
                ActionStatusCreate.builder().actionId(updActA.getId()).status(status).messages(msgs).build());
    }

    private Rollout startAndReloadRollout(final Rollout rollout) {
        rolloutManagement.start(rollout.getId());
        // Run here, because scheduler is disabled during tests
        rolloutHandleAll();
        return reloadRollout(rollout);
    }

    private void rolloutHandleAll() {
        final String tenant = AccessContext.tenant();
        if (tenant == null) {
            throw new IllegalStateException("AccessContext is null");
        }
        asSystem(rolloutHandler::handleAll);
    }

    private Rollout reloadRollout(final Rollout rollout) {
        return rolloutManagement.get(rollout.getId());
    }
}