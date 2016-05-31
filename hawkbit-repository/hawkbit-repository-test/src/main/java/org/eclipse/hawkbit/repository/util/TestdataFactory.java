/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.util;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import net._01001111.text.LoremIpsum;

/**
 * Data generator utility for tests.
 */
public class TestdataFactory {
    private static final LoremIpsum LOREM = new LoremIpsum();

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
    public static final String DEFAULT_DESCRIPTION = "Desc: " + LOREM.words(10);

    /**
     * Key of test default {@link DistributionSetType}.
     */
    public static final String DS_TYPE_DEFAULT = "test_default_ds_type";

    /**
     * Key of test "os" {@link SoftwareModuleType} -> mandatory firmware in
     * {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_OS = "os";

    /**
     * Key of test "runtime" {@link SoftwareModuleType} -> optional firmware in
     * {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_RT = "runtime";

    /**
     * Key of test "application" {@link SoftwareModuleType} -> optional software
     * in {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_APP = "application";

    @Autowired
    private ControllerManagement controllerManagament;

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private DeploymentManagement deploymentManagement;

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private ArtifactManagement artifactManagement;

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     * 
     * @param prefix
     *            for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *            vendor and description.
     * 
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix) {
        return createDistributionSet(prefix, DEFAULT_VERSION, false);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION}.
     * 
     * @param prefix
     *            for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *            vendor and description.
     * @param isRequiredMigrationStep
     *            for {@link DistributionSet#isRequiredMigrationStep()}
     * 
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final boolean isRequiredMigrationStep) {
        return createDistributionSet(prefix, DEFAULT_VERSION, isRequiredMigrationStep);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     * 
     * @param prefix
     *            for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *            vendor and description.
     * @param tags
     *            {@link DistributionSet#getTags()}
     * 
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final Collection<DistributionSetTag> tags) {
        return createDistributionSet(prefix, DEFAULT_VERSION, tags);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP}.
     * 
     * @param prefix
     *            for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *            vendor and description.
     * @param version
     *            {@link DistributionSet#getVersion()} and
     *            {@link SoftwareModule#getVersion()} extended by a random
     *            number.
     * @param isRequiredMigrationStep
     *            for {@link DistributionSet#isRequiredMigrationStep()}
     * 
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final String version,
            final boolean isRequiredMigrationStep) {

        final SoftwareModule appMod = softwareManagement.createSoftwareModule(entityFactory.generateSoftwareModule(
                findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE), prefix + SM_TYPE_APP,
                version + "." + new Random().nextInt(100), LOREM.words(20), prefix + " vendor Limited, California"));
        final SoftwareModule runtimeMod = softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(findOrCreateSoftwareModuleType(SM_TYPE_RT),
                        prefix + "app runtime", version + "." + new Random().nextInt(100), LOREM.words(20),
                        prefix + " vendor GmbH, Stuttgart, Germany"));
        final SoftwareModule osMod = softwareManagement
                .createSoftwareModule(entityFactory.generateSoftwareModule(findOrCreateSoftwareModuleType(SM_TYPE_OS),
                        prefix + " Firmware", version + "." + new Random().nextInt(100), LOREM.words(20),
                        prefix + " vendor Limited Inc, California"));

        return distributionSetManagement
                .createDistributionSet(generateDistributionSet(prefix != null && prefix.length() > 0 ? prefix : "DS",
                        version, findOrCreateDefaultTestDsType(), Lists.newArrayList(osMod, runtimeMod, appMod))
                                .setRequiredMigrationStep(isRequiredMigrationStep));
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP}.
     * 
     * @param prefix
     *            for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *            vendor and description.
     * @param version
     *            {@link DistributionSet#getVersion()} and
     *            {@link SoftwareModule#getVersion()} extended by a random
     *            number.
     * @param tags
     *            {@link DistributionSet#getTags()}
     * 
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final String version,
            final Collection<DistributionSetTag> tags) {

        final DistributionSet set = createDistributionSet(prefix, version, false);

        final List<DistributionSet> sets = new ArrayList<>();
        sets.add(set);

        tags.forEach(tag -> distributionSetManagement.toggleTagAssignment(sets, tag));

        return distributionSetManagement.findDistributionSetById(set.getId());

    }

    /**
     * Creates {@link DistributionSet}s in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an
     * iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     * 
     * @param number
     *            of {@link DistributionSet}s to create
     * 
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSets(final int number) {

        return createDistributionSets("", number);
    }

    /**
     * Creates {@link DistributionSet}s in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an
     * iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     * 
     * @param prefix
     *            for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *            vendor and description.
     * @param number
     *            of {@link DistributionSet}s to create
     * 
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSets(final String prefix, final int number) {

        final List<DistributionSet> sets = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            sets.add(createDistributionSet(prefix, DEFAULT_VERSION + "." + i, false));
        }

        return sets;
    }

    /**
     * Creates {@link DistributionSet}s in repository with
     * {@link #DEFAULT_DESCRIPTION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     * 
     * @param name
     *            {@link DistributionSet#getName()}
     * @param version
     *            {@link DistributionSet#getVersion()}
     * 
     * @return {@link DistributionSet} entity
     */
    public DistributionSet createDistributionSetWithNoSoftwareModules(final String name, final String version) {

        final DistributionSet dis = entityFactory.generateDistributionSet();
        dis.setName(name);
        dis.setVersion(version);
        dis.setDescription(DEFAULT_DESCRIPTION);
        dis.setType(findOrCreateDefaultTestDsType());
        return distributionSetManagement.createDistributionSet(dis);
    }

    /**
     * Creates {@link LocalArtifact}s for given {@link SoftwareModule} with a
     * small text payload.
     * 
     * @param moduleId
     *            the {@link Artifact}s belong to.
     * 
     * @return {@link LocalArtifact} entity.
     */
    public List<LocalArtifact> createLocalArtifacts(final Long moduleId) {
        final List<LocalArtifact> artifacts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final InputStream stubInputStream = IOUtils.toInputStream("some test data" + i, Charset.forName("UTF-8"));
            artifacts.add(artifactManagement.createLocalArtifact(stubInputStream, moduleId, "filename" + i, false));

        }

        return artifacts;
    }

    /**
     * Creates {@link SoftwareModule} with {@link #DEFAULT_VENDOR} and
     * {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     * 
     * @param typeKey
     *            of the {@link SoftwareModuleType}
     * 
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModule(final String typeKey) {
        return softwareManagement.createSoftwareModule(entityFactory.generateSoftwareModule(
                findOrCreateSoftwareModuleType(typeKey), typeKey, DEFAULT_VERSION, LOREM.words(10), DEFAULT_VENDOR));
    }

    /**
     * @return persisted {@link Target} with {@link #DEFAULT_CONTROLLER_ID}.
     */
    public Target createTarget() {
        return targetManagement.createTarget(entityFactory.generateTarget(DEFAULT_CONTROLLER_ID));
    }

    /**
     * Creates {@link DistributionSet}s in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an
     * iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     * 
     * @return persisted {@link DistributionSet}.
     */
    public DistributionSet createTestDistributionSet() {
        DistributionSet set = createDistributionSet("");
        set.setVersion(DEFAULT_VERSION);
        set = distributionSetManagement.updateDistributionSet(set);

        set.getModules().forEach(module -> {
            module.setDescription("Updated " + DEFAULT_DESCRIPTION);
            softwareManagement.updateSoftwareModule(module);
        });

        // load also lazy stuff
        set = distributionSetManagement.findDistributionSetByIdWithDetails(set.getId());

        return set;
    }

    /**
     * @return {@link DistributionSetType} with key {@link #DS_TYPE_DEFAULT} and
     *         {@link SoftwareModuleType}s {@link #SM_TYPE_OS},
     *         {@link #SM_TYPE_RT} , {@link #SM_TYPE_APP}.
     */
    public DistributionSetType findOrCreateDefaultTestDsType() {
        final List<SoftwareModuleType> mand = new ArrayList<>();
        mand.add(findOrCreateSoftwareModuleType(SM_TYPE_OS));

        final List<SoftwareModuleType> opt = new ArrayList<>();
        opt.add(findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE));
        opt.add(findOrCreateSoftwareModuleType(SM_TYPE_RT));

        return findOrCreateDistributionSetType(DS_TYPE_DEFAULT, "OS (FW) mandatory, runtime (FW) and app (SW) optional",
                mand, opt);
    }

    /**
     * Creates {@link DistributionSetType} in repository.
     * 
     * @param dsTypeKey
     *            {@link DistributionSetType#getKey()}
     * @param dsTypeName
     *            {@link DistributionSetType#getName()}
     * 
     * @return persisted {@link DistributionSetType}
     */
    public DistributionSetType findOrCreateDistributionSetType(final String dsTypeKey, final String dsTypeName) {
        final DistributionSetType findDistributionSetTypeByname = distributionSetManagement
                .findDistributionSetTypeByKey(dsTypeKey);

        if (findDistributionSetTypeByname != null) {
            return findDistributionSetTypeByname;
        }

        final DistributionSetType type = entityFactory.generateDistributionSetType(dsTypeKey, dsTypeName,
                LOREM.words(10));

        return distributionSetManagement.createDistributionSetType(type);
    }

    /**
     * Finds {@link DistributionSetType} in repository with given
     * {@link DistributionSetType#getKey()} or creates if it does not exist yet.
     * 
     * @param dsTypeKey
     *            {@link DistributionSetType#getKey()}
     * @param dsTypeName
     *            {@link DistributionSetType#getName()}
     * @param mandatory
     *            {@link DistributionSetType#getMandatoryModuleTypes()}
     * @param optional
     *            {@link DistributionSetType#getOptionalModuleTypes()}
     * 
     * @return persisted {@link DistributionSetType}
     */
    public DistributionSetType findOrCreateDistributionSetType(final String dsTypeKey, final String dsTypeName,
            final Collection<SoftwareModuleType> mandatory, final Collection<SoftwareModuleType> optional) {
        final DistributionSetType findDistributionSetTypeByname = distributionSetManagement
                .findDistributionSetTypeByKey(dsTypeKey);

        if (findDistributionSetTypeByname != null) {
            return findDistributionSetTypeByname;
        }

        final DistributionSetType type = entityFactory.generateDistributionSetType(dsTypeKey, dsTypeName,
                LOREM.words(10));
        mandatory.forEach(type::addMandatoryModuleType);
        optional.forEach(type::addOptionalModuleType);

        return distributionSetManagement.createDistributionSetType(type);
    }

    /**
     * Finds {@link SoftwareModuleType} in repository with given
     * {@link SoftwareModuleType#getKey()} or creates if it does not exist yet
     * with {@link SoftwareModuleType#getMaxAssignments()} = 1.
     * 
     * @param key
     *            {@link SoftwareModuleType#getKey()}
     * 
     * @return persisted {@link SoftwareModuleType}
     */
    public SoftwareModuleType findOrCreateSoftwareModuleType(final String key) {
        return findOrCreateSoftwareModuleType(key, 1);
    }

    /**
     * Finds {@link SoftwareModuleType} in repository with given
     * {@link SoftwareModuleType#getKey()} or creates if it does not exist yet.
     * 
     * @param key
     *            {@link SoftwareModuleType#getKey()}
     * @param maxAssignments
     *            {@link SoftwareModuleType#getMaxAssignments()}
     * 
     * @return persisted {@link SoftwareModuleType}
     */
    public SoftwareModuleType findOrCreateSoftwareModuleType(final String key, final int maxAssignments) {
        final SoftwareModuleType findSoftwareModuleTypeByKey = softwareManagement.findSoftwareModuleTypeByKey(key);
        if (findSoftwareModuleTypeByKey != null) {
            return findSoftwareModuleTypeByKey;
        }
        return softwareManagement.createSoftwareModuleType(
                entityFactory.generateSoftwareModuleType(key, key, LOREM.words(10), maxAssignments));
    }

    /**
     * builder method for creating a {@link DistributionSet}.
     *
     * @param name
     *            {@link DistributionSet#getName()}
     * @param version
     *            {@link DistributionSet#getVersion()}
     * @param type
     *            {@link DistributionSet#getType()}
     * @param modules
     *            {@link DistributionSet#getModules()}
     * 
     * @return the created {@link DistributionSet}
     */
    public DistributionSet generateDistributionSet(final String name, final String version,
            final DistributionSetType type, final Collection<SoftwareModule> modules) {
        final DistributionSet distributionSet = entityFactory.generateDistributionSet(name, version, null, type,
                modules);
        distributionSet.setDescription(LOREM.words(10));
        return distributionSet;
    }

    /**
     * Creates {@link DistributionSetTag}s in repository.
     * 
     * @param number
     *            of {@link DistributionSetTag}s
     * 
     * @return the persisted {@link DistributionSetTag}s
     */
    public List<DistributionSetTag> createDistributionSetTags(final int number) {
        final List<DistributionSetTag> result = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            result.add(entityFactory.generateDistributionSetTag("tag" + i, "tagdesc" + i, String.valueOf(i)));
        }

        return tagManagement.createDistributionSetTags(result);
    }

    /**
     * builder method for creating a single target object.
     *
     * @param ctrlID
     *            the ID of the target
     * @param description
     *            of the target
     * @return the created target object
     */
    public Target generateTarget(final String ctrlID, final String description) {
        return generateTarget(ctrlID, description, null);
    }

    /**
     * Builds a single {@link Target} from the given parameters.
     *
     * @param ctrlID
     *            controllerID
     * @param description
     *            the description of the target
     * @param tags
     *            assigned {@link TargetTag}s
     * @return the generated {@link Target}
     */
    private Target generateTarget(final String ctrlID, final String description, final TargetTag[] tags) {
        final Target target = entityFactory.generateTarget(ctrlID);
        target.setName("Prov.Target ".concat(ctrlID));
        target.setDescription(description);
        if (tags != null && tags.length > 0) {
            for (final TargetTag t : tags) {
                target.getTags().add(t);
            }
        }
        return target;
    }

    /**
     * Creates {@link Target}s in repository and with
     * {@link #DEFAULT_CONTROLLER_ID} as prefix for
     * {@link Target#getControllerId()}.
     * 
     * @param number
     *            of {@link Target}s to create
     * 
     * @return {@link List} of {@link Target} entities
     */
    public List<Target> createTargets(final int number) {
        return targetManagement.createTargets(generateTargets(0, number, DEFAULT_CONTROLLER_ID));
    }

    /**
     * Builds {@link Target} objects with given prefix for
     * {@link Target#getControllerId()} followed by a number suffix.
     * 
     * @param start
     *            value for the controllerId suffix
     * @param numberOfTargets
     *            of {@link Target}s to generate
     * @param controllerIdPrefix
     *            for {@link Target#getControllerId()} generation.
     * @return list of {@link Target} objects
     */
    private List<Target> generateTargets(final int start, final int numberOfTargets, final String controllerIdPrefix) {
        final List<Target> targets = new ArrayList<>();
        for (int i = start; i < start + numberOfTargets; i++) {
            targets.add(entityFactory.generateTarget(controllerIdPrefix + i));
        }

        return targets;
    }

    /**
     * Builds {@link Target} objects with given prefix for
     * {@link Target#getControllerId()} followed by a number suffix starting
     * with 0.
     * 
     * @param numberOfTargets
     *            of {@link Target}s to generate
     * @param controllerIdPrefix
     *            for {@link Target#getControllerId()} generation.
     * @return list of {@link Target} objects
     */
    public List<Target> generateTargets(final int numberOfTargets, final String controllerIdPrefix) {
        return generateTargets(0, numberOfTargets, controllerIdPrefix);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets
     *            number of targets to create
     * @param controllerIdPrefix
     *            prefix used for the controller ID
     * @param descriptionPrefix
     *            prefix used for the description
     * @return set of {@link Target}
     */
    public List<Target> generateTargets(final int numberOfTargets, final String controllerIdPrefix,
            final String descriptionPrefix) {
        return generateTargets(numberOfTargets, controllerIdPrefix, descriptionPrefix, null);
    }

    /**
     * method creates set of targets by by generating the controller ID and the
     * description like: prefix + no of target.
     *
     * @param noOfTgts
     *            number of targets which should be created
     * @param controllerIdPrefix
     *            prefix of the controllerID which is concatenated with the
     *            number of the target. Randomly generated if <code>null</code>.
     * @param descriptionPrefix
     *            prefix of the target description which is concatenated with
     *            the number of the target
     * @param tags
     *            tags which should be added to the created {@link Target}s
     * @return set of created targets
     */
    private List<Target> generateTargets(final int noOfTgts, final String controllerIdPrefix,
            final String descriptionPrefix, final TargetTag[] tags) {
        final List<Target> list = new ArrayList<>();
        for (int i = 0; i < noOfTgts; i++) {
            String ctrlID = controllerIdPrefix;
            if (Strings.isNullOrEmpty(ctrlID)) {
                ctrlID = UUID.randomUUID().toString();
            }
            ctrlID = String.format("%s-%05d", ctrlID, i);

            final String description = descriptionPrefix + DEFAULT_DESCRIPTION;

            final Target target = generateTarget(ctrlID, description, tags);
            list.add(target);

        }
        return list;
    }

    /**
     * Generates {@link TargetTag}s.
     * 
     * @param number
     *            of {@link TargetTag}s to generate.
     * 
     * @return generated {@link TargetTag}s.
     */
    public List<TargetTag> generateTargetTags(final int number) {
        final List<TargetTag> result = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            result.add(entityFactory.generateTargetTag("tag" + i, "tagdesc" + i, String.valueOf(i)));
        }

        return result;
    }

    /**
     * Create a set of {@link TargetTag}s.
     *
     * @param noOfTags
     *            number of {@link TargetTag}. to be created
     * @param tagPrefix
     *            prefix for the {@link TargetTag#getName()}
     * @return the created set of {@link TargetTag}s
     */
    public List<TargetTag> generateTargetTags(final int noOfTags, final String tagPrefix) {
        final List<TargetTag> list = new ArrayList<>();
        for (int i = 0; i < noOfTags; i++) {
            String tagName = "myTag";
            if (!Strings.isNullOrEmpty(tagPrefix)) {
                tagName = tagPrefix;
            }
            tagName = String.format("%s-%05d", tagName, i);

            final TargetTag targetTag = entityFactory.generateTargetTag(tagName);
            list.add(targetTag);
        }
        return list;
    }

    private Action sendUpdateActionStatusToTarget(final Status status, final Action updActA, final String... msgs) {
        updActA.setStatus(status);

        final ActionStatus statusMessages = entityFactory.generateActionStatus();
        statusMessages.setAction(updActA);
        statusMessages.setOccurredAt(System.currentTimeMillis());
        statusMessages.setStatus(status);
        for (final String msg : msgs) {
            statusMessages.addMessage(msg);
        }

        return controllerManagament.addUpdateActionStatus(statusMessages);
    }

    /**
     * Append {@link ActionStatus} to all {@link Action}s of given
     * {@link Target}s.
     * 
     * @param targets
     *            to add {@link ActionStatus}
     * @param status
     *            to add
     * @param msgs
     *            to add
     * 
     * @return updated {@link Action}.
     */
    public List<Action> sendUpdateActionStatusToTargets(final Collection<Target> targets, final Status status,
            final String... msgs) {
        final List<Action> result = new ArrayList<>();
        for (final Target target : targets) {
            final List<Action> findByTarget = deploymentManagement.findActionsByTarget(target);
            for (final Action action : findByTarget) {
                result.add(sendUpdateActionStatusToTarget(status, action, msgs));
            }
        }
        return result;
    }
}
