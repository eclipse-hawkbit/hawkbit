/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import net._01001111.text.LoremIpsum;

/**
 * Data generator utility for tests.
 *
 *
 *
 */
public class TestDataUtil {
    private static final LoremIpsum LOREM = new LoremIpsum();

    public static List<DistributionSet> generateDistributionSets(final String suffix, final int number,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement) {

        final List<DistributionSet> sets = new ArrayList<DistributionSet>();
        for (int i = 0; i < number; i++) {
            sets.add(generateDistributionSet(suffix, "v1." + i, softwareManagement, distributionSetManagement, false));
        }

        return sets;
    }

    public static DistributionSet generateDistributionSetWithNoSoftwareModules(final String name, final String version,
            final DistributionSetManagement distributionSetManagement) {

        final DistributionSet dis = new DistributionSet();
        dis.setName(name);
        dis.setVersion(version);
        dis.setDescription("Test describtion for " + name);
        return distributionSetManagement.createDistributionSet(dis);
    }

    public static List<DistributionSet> generateDistributionSets(final int number,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement) {

        return generateDistributionSets("", number, softwareManagement, distributionSetManagement);
    }

    public static DistributionSet generateDistributionSet(final String suffix, final String version,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement,
            final boolean isRequiredMigrationStep) {

        final SoftwareModule ah = softwareManagement.createSoftwareModule(new SoftwareModule(
                findOrCreateSoftwareModuleType(softwareManagement, "application"), suffix + "application",
                version + "." + new Random().nextInt(100), LOREM.words(20), suffix + " vendor Limited, California"));
        final SoftwareModule jvm = softwareManagement
                .createSoftwareModule(new SoftwareModule(findOrCreateSoftwareModuleType(softwareManagement, "runtime"),
                        suffix + "app runtime", version + "." + new Random().nextInt(100), LOREM.words(20),
                        suffix + " vendor GmbH, Stuttgart, Germany"));
        final SoftwareModule os = softwareManagement
                .createSoftwareModule(new SoftwareModule(findOrCreateSoftwareModuleType(softwareManagement, "os"),
                        suffix + " Firmware", version + "." + new Random().nextInt(100), LOREM.words(20),
                        suffix + " vendor Limited Inc, California"));

        final List<SoftwareModuleType> mand = new ArrayList<>();
        mand.add(findOrCreateSoftwareModuleType(softwareManagement, "os"));

        final List<SoftwareModuleType> opt = new ArrayList<>();
        opt.add(findOrCreateSoftwareModuleType(softwareManagement, "application"));
        opt.add(findOrCreateSoftwareModuleType(softwareManagement, "runtime"));

        return distributionSetManagement.createDistributionSet(
                buildDistributionSet(suffix != null && suffix.length() > 0 ? suffix : "DS", version,
                        findOrCreateDistributionSetType(distributionSetManagement, "ecl_os_app_jvm",
                                "OC mandatory App/JVM optional", mand, opt),
                        os, jvm, ah).setRequiredMigrationStep(isRequiredMigrationStep));
    }

    public static DistributionSet generateDistributionSet(final String suffix, final String version,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement,
            final Collection<DistributionSetTag> tags) {

        final DistributionSet set = generateDistributionSet(suffix, version, softwareManagement,
                distributionSetManagement, false);

        final List<DistributionSet> sets = new ArrayList<DistributionSet>();
        sets.add(set);

        tags.forEach(tag -> distributionSetManagement.toggleTagAssignment(sets, tag));

        return distributionSetManagement.findDistributionSetById(set.getId());

    }

    public static List<Target> generateTargets(final int number) {
        return generateTargets(0, number, "Test target ");
    }

    public static List<Target> generateTargets(final int number, final String prefix) {
        return generateTargets(0, number, prefix);
    }

    public static List<Target> generateTargets(final int start, final int number, final String prefix) {
        final List<Target> targets = new ArrayList<>();
        for (int i = start; i < start + number; i++) {
            targets.add(new Target(prefix + i));
        }

        return targets;
    }

    public static List<TargetTag> generateTargetTags(final int number) {
        final List<TargetTag> result = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            result.add(new TargetTag("tag" + i, "tagdesc" + i, "" + i));
        }

        return result;
    }

    public static List<DistributionSetTag> generateDistributionSetTags(final int number) {
        final List<DistributionSetTag> result = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            result.add(new DistributionSetTag("tag" + i, "tagdesc" + i, "" + i));
        }

        return result;
    }

    public static DistributionSet generateDistributionSet(final String suffix,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement,
            final boolean isRequiredMigrationStep) {
        return generateDistributionSet(suffix, "v1.0", softwareManagement, distributionSetManagement,
                isRequiredMigrationStep);
    }

    public static DistributionSet generateDistributionSet(final String suffix,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement) {
        return generateDistributionSet(suffix, "v1.0", softwareManagement, distributionSetManagement, false);
    }

    public static List<org.eclipse.hawkbit.repository.model.Artifact> generateArtifacts(
            final ArtifactManagement artifactManagement, final Long moduleId) {
        final List<org.eclipse.hawkbit.repository.model.Artifact> artifacts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final InputStream stubInputStream = IOUtils.toInputStream("some test data" + i);
            artifacts.add(artifactManagement.createLocalArtifact(stubInputStream, moduleId, "filename" + i, false));

        }

        return artifacts;
    }

    public static Target createTarget(final TargetManagement targetManagement) {
        final String targetExist = "targetExist";
        final Target target = new Target(targetExist);
        targetManagement.createTarget(target);
        return target;
    }

    public static DistributionSet generateDistributionSet(final String suffix,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement,
            final Collection<DistributionSetTag> tags) {
        return generateDistributionSet(suffix, "v1.0", softwareManagement, distributionSetManagement, tags);
    }

    public static SoftwareModuleType findOrCreateSoftwareModuleType(final SoftwareManagement softwareManagement,
            final String softwareModuleType) {
        final SoftwareModuleType findSoftwareModuleTypeByKey = softwareManagement
                .findSoftwareModuleTypeByKey(softwareModuleType);
        if (findSoftwareModuleTypeByKey != null) {
            return findSoftwareModuleTypeByKey;
        }
        return softwareManagement.createSoftwareModuleType(new SoftwareModuleType(softwareModuleType,
                softwareModuleType, "Standard type " + softwareManagement, 1));
    }

    public static DistributionSetType findOrCreateDistributionSetType(
            final DistributionSetManagement distributionSetManagement, final String dsTypeKey, final String dsTypeName,
            final Collection<SoftwareModuleType> mandatory, final Collection<SoftwareModuleType> optional) {
        final DistributionSetType findDistributionSetTypeByname = distributionSetManagement
                .findDistributionSetTypeByKey(dsTypeKey);

        if (findDistributionSetTypeByname != null) {
            return findDistributionSetTypeByname;
        }

        final DistributionSetType type = new DistributionSetType(dsTypeKey, dsTypeName, "Standard type" + dsTypeName);
        mandatory.forEach(entry -> type.addMandatoryModuleType(entry));
        optional.forEach(entry -> type.addOptionalModuleType(entry));

        return distributionSetManagement.createDistributionSetType(type);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param noOfTgts
     *            number of targets to create
     * @param ctlrIDPrefix
     *            prefix used for the controller ID
     * @param descriptionPrefix
     *            prefix used for the description
     * @return set of {@link Target}
     */
    public static List<Target> buildTargetFixtures(final int noOfTgts, final String ctlrIDPrefix,
            final String descriptionPrefix) {
        return buildTargetFixtures(noOfTgts, ctlrIDPrefix, descriptionPrefix, null);
    }

    /**
     * method creates set of targets by by generating the controller ID and the
     * description like: prefix + no of target.
     *
     * @param noOfTgts
     *            number of targets which should be created
     * @param ctlrIDPrefix
     *            prefix of the controllerID which is concatenated with the
     *            number of the target
     * @param descriptionPrefix
     *            prefix of the target description which is concatenated with
     *            the number of the target
     * @param tags
     *            tags which should be added to the created {@link Target}s
     * @return set of created targets
     */
    public static List<Target> buildTargetFixtures(final int noOfTgts, final String ctlrIDPrefix,
            final String descriptionPrefix, final TargetTag[] tags) {
        final List<Target> list = new ArrayList<Target>();
        for (int i = 0; i < noOfTgts; i++) {
            String ctrlID = ctlrIDPrefix;
            if (Strings.isNullOrEmpty(ctrlID)) {
                ctrlID = UUID.randomUUID().toString();
            }
            ctrlID = String.format("%s-%05d", ctrlID, i);

            final String description = String.format("the description of ProvisioningTarget: [%s]", ctrlID);

            final Target target = buildTargetFixture(ctrlID, description, tags);
            list.add(target);

        }
        return list;
    }

    /**
     * builds a single {@link Target} fixture from the given parameters.
     *
     * @param ctrlID
     *            controllerID
     * @param description
     *            the description of the target
     * @param tags
     *            assigned {@link TargetTag}s
     * @return the created {@link Target}
     */
    public static Target buildTargetFixture(final String ctrlID, final String description, final TargetTag[] tags) {
        final Target target = new Target(ctrlID);
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
     * builder method for creating a single target object.
     *
     * @param ctrlID
     *            the ID of the target
     * @param description
     *            of the target
     * @return the created target object
     */
    public static Target buildTargetFixture(final String ctrlID, final String description) {
        return buildTargetFixture(ctrlID, description, null);
    }

    /**
     * builder method for creating a {@link DistributionSet}.
     *
     * @param name
     *            of the DS
     * @param version
     *            of the DS
     * @param os
     *            operating system of the DS
     * @param jvm
     *            java virtual machine of the DS
     * @param agentHub
     *            of the DS
     * @return the created {@link DistributionSet}
     */
    public static DistributionSet buildDistributionSet(final String name, final String version,
            final DistributionSetType type, final SoftwareModule os, final SoftwareModule jvm,
            final SoftwareModule agentHub) {
        final DistributionSet distributionSet = new DistributionSet(name, version, null, type,
                Lists.newArrayList(os, jvm, agentHub));
        distributionSet.setDescription(
                String.format("description of DistributionSet; name = '%s', version = '%s'", name, version));
        return distributionSet;
    }

    /**
     * builder method for creating a set of {@link TargetTag}.
     *
     * @param noOfTags
     *            number of {@link TargetTag}. to be created
     * @param tagPrefix
     *            prefix for the {@link TargetTag.getName()}
     * @return the created set of {@link TargetTag}s
     */
    public static List<TargetTag> buildTargetTagFixtures(final int noOfTags, final String tagPrefix) {
        final List<TargetTag> list = new ArrayList<>();
        for (int i = 0; i < noOfTags; i++) {
            String tagName = "myTag";
            if (!Strings.isNullOrEmpty(tagPrefix)) {
                tagName = tagPrefix;
            }
            tagName = String.format("%s-%05d", tagName, i);

            final TargetTag targetTag = buildTargetTagFixture(tagName);
            list.add(targetTag);
        }
        return list;
    }

    /**
     * builder method for creating a simple {@link TargetTag}.
     *
     * @param tagName
     *            name of the Tag
     * @return the {@link TargetTag}
     */
    public static TargetTag buildTargetTagFixture(final String tagName) {
        return new TargetTag(tagName);
    }
}
