/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.specifications.TargetSpecifications;
import org.junit.Test;
import org.springframework.data.domain.Slice;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Target Management Searches")
public class TargetManagementSearchTest extends AbstractIntegrationTest {

    @Test
    @Description("Tests different parameter combinations for target search operations. "
            + "That includes both the test itself, as a count operation with the same filters "
            + "and query definitions by RSQL (named and un-named).")
    public void targetSearchWithVariousFilterCombinations() {
        final TargetTag targTagX = tagManagement.createTargetTag(new TargetTag("TargTag-X"));
        final TargetTag targTagY = tagManagement.createTargetTag(new TargetTag("TargTag-Y"));
        final TargetTag targTagZ = tagManagement.createTargetTag(new TargetTag("TargTag-Z"));
        final TargetTag targTagW = tagManagement.createTargetTag(new TargetTag("TargTag-W"));

        final DistributionSet setA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        final DistributionSet installedSet = TestDataUtil.generateDistributionSet("another", softwareManagement,
                distributionSetManagement);

        final String targetDsAIdPref = "targ-A";
        List<Target> targAs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsAIdPref, targetDsAIdPref.concat(" description")));
        targAs = targetManagement.toggleTagAssignment(targAs, targTagX).getAssignedTargets();

        final String targetDsBIdPref = "targ-B";
        List<Target> targBs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsBIdPref, targetDsBIdPref.concat(" description")));
        targBs = targetManagement.toggleTagAssignment(targBs, targTagY).getAssignedTargets();
        targBs = targetManagement.toggleTagAssignment(targBs, targTagW).getAssignedTargets();

        final String targetDsCIdPref = "targ-C";
        List<Target> targCs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsCIdPref, targetDsCIdPref.concat(" description")));
        targCs = targetManagement.toggleTagAssignment(targCs, targTagZ).getAssignedTargets();
        targCs = targetManagement.toggleTagAssignment(targCs, targTagW).getAssignedTargets();

        final String targetDsDIdPref = "targ-D";
        final List<Target> targDs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsDIdPref, targetDsDIdPref.concat(" description")));

        final String assignedC = targCs.iterator().next().getControllerId();
        deploymentManagement.assignDistributionSet(setA.getId(), assignedC);
        final String assignedA = targAs.iterator().next().getControllerId();
        deploymentManagement.assignDistributionSet(setA.getId(), assignedA);
        final String assignedB = targBs.iterator().next().getControllerId();
        deploymentManagement.assignDistributionSet(setA.getId(), assignedB);
        final String installedC = targCs.iterator().next().getControllerId();
        final Long actionId = deploymentManagement.assignDistributionSet(installedSet.getId(), assignedC).getActions()
                .get(0);

        // set one installed DS also
        final Action action = deploymentManagement.findActionWithDetails(actionId);
        action.setStatus(Status.FINISHED);
        controllerManagament.addUpdateActionStatus(
                new ActionStatus(action, Status.FINISHED, System.currentTimeMillis(), "message"), action);
        deploymentManagement.assignDistributionSet(setA.getId(), installedC);

        final List<TargetUpdateStatus> unknown = new ArrayList<>();
        unknown.add(TargetUpdateStatus.UNKNOWN);

        final List<TargetUpdateStatus> pending = new ArrayList<>();
        pending.add(TargetUpdateStatus.PENDING);

        final List<TargetUpdateStatus> both = new ArrayList<>();
        both.add(TargetUpdateStatus.UNKNOWN);
        both.add(TargetUpdateStatus.PENDING);

        // get final updated version of targets
        targAs = targetManagement.findTargetByControllerID(
                targAs.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));
        targBs = targetManagement.findTargetByControllerID(
                targBs.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));
        targCs = targetManagement.findTargetByControllerID(
                targCs.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        // try to find several targets with different filter settings
        verifyThatRepositoryContains400Targets();
        verifyThat200TargetsHaveTagD(targTagW, concat(targBs, targCs));
        verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(targTagY, targTagW, targBs);
        verifyThat1TargetHasTagHasDescOrNameAndDs(targTagW, setA, targetManagement.findTargetByControllerID(assignedC));
        verifyThat0TargetsWithTagAndDescOrNameHasDS(targTagW, setA);
        verifyThat0TargetsWithNameOrdescAndDSHaveTag(targTagX, setA);
        verifyThat3TargetsHaveDSAssigned(setA,
                targetManagement.findTargetByControllerID(Lists.newArrayList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithDescOrNameHasDS(setA, targetManagement.findTargetByControllerID(assignedA));
        List<Target> expected = concat(targAs, targBs, targCs, targDs);
        expected.removeAll(
                targetManagement.findTargetByControllerID(Lists.newArrayList(assignedA, assignedB, assignedC)));
        verifyThat397TargetsAreInStatusUnknown(unknown, expected);
        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.findTargetByControllerID(Lists.newArrayList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(targTagY, targTagW, unknown, expected);
        verfyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(setA, unknown);
        expected = concat(targAs);
        expected.remove(targetManagement.findTargetByControllerID(assignedA));
        verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(unknown, expected);
        expected = concat(targBs);
        expected.remove(targetManagement.findTargetByControllerID(assignedB));
        verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(targTagW, unknown, expected);
        verifyThat3TargetsAreInStatusPending(pending,
                targetManagement.findTargetByControllerID(Lists.newArrayList(assignedA, assignedB, assignedC)));
        verifyThat3TargetsWithGivenDSAreInPending(setA, pending,
                targetManagement.findTargetByControllerID(Lists.newArrayList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(setA, pending,
                targetManagement.findTargetByControllerID(assignedA));
        verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.findTargetByControllerID(assignedB));
        verifyThat2TargetsWithGivenTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.findTargetByControllerID(Lists.newArrayList(assignedB, assignedC)));
        verifyThat2TargetsWithGivenTagAreInPending(targTagW, pending,
                targetManagement.findTargetByControllerID(Lists.newArrayList(assignedB, assignedC)));
        verifyThat200targetsWithGivenTagAreInStatusPendingorUnknown(targTagW, both, concat(targBs, targCs));
        verfiyThat1TargetAIsInStatusPendingAndHasDSInstalled(installedSet, pending,
                targetManagement.findTargetByControllerID(installedC));
    }

    @Step
    private void verfiyThat1TargetAIsInStatusPendingAndHasDSInstalled(final DistributionSet installedSet,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final TargetIdName expectedIdName = convertToIdName(expected);
        final String query = "updatestatus==pending and installedds.name==" + installedSet.getName();

        assertThat(targetManagement
                .findTargetByFilters(pageReq, pending, null, installedSet.getId(), Boolean.FALSE, new String[0])
                .getContent()).as("has number of elements").hasSize(1)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(pending, null,
                                installedSet.getId(), Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, pending, null, installedSet.getId(),
                Boolean.FALSE, new String[0])).as("has number of elements").hasSize(1)
                        .as("and contains the following elements").containsExactly(expectedIdName)
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findAllTargetIdsByTargetFilterQuery(pageReq, new TargetFilterQuery("test", query)));

    }

    @Step
    private void verifyThat200targetsWithGivenTagAreInStatusPendingorUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> both, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);

        final String query = "(updatestatus==pending or updatestatus==unknown) and tag==" + targTagW.getName();

        assertThat(targetManagement.findTargetByFilters(pageReq, both, null, null, Boolean.FALSE, targTagW.getName())
                .getContent()).as("has number of elements").hasSize(200)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(both, null, null,
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, both, null, null, Boolean.FALSE,
                targTagW.getName())).as("has number of elements").hasSize(200).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    private static List<TargetIdName> convertToIdNames(final List<Target> expected) {
        return expected.stream()
                .map(target -> new TargetIdName(target.getId(), target.getControllerId(), target.getName()))
                .collect(Collectors.toList());
    }

    private static TargetIdName convertToIdName(final Target target) {
        return new TargetIdName(target.getId(), target.getControllerId(), target.getName());
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAreInPending(final TargetTag targTagW,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==pending and tag==" + targTagW.getName();

        assertThat(targetManagement.findTargetByFilters(pageReq, pending, null, null, Boolean.FALSE, targTagW.getName())
                .getContent()).as("has number of elements").hasSize(2)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(pending, null, null,
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, pending, null, null, Boolean.FALSE,
                targTagW.getName())).as("has number of elements").hasSize(2).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAndDSIsInPending(final TargetTag targTagW, final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and tag==" + targTagW.getName();

        assertThat(targetManagement
                .findTargetByFilters(pageReq, pending, null, setA.getId(), Boolean.FALSE, targTagW.getName())
                .getContent()).as("has number of elements").hasSize(2)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(pending, null, setA.getId(),
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, pending, null, null, Boolean.FALSE,
                targTagW.getName())).as("has number of elements").hasSize(2).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(final TargetTag targTagW,
            final DistributionSet setA, final List<TargetUpdateStatus> pending, final Target expected) {
        final TargetIdName expectedIdName = convertToIdName(expected);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-B* or description==*targ-B*) and tag==" + targTagW.getName();

        assertThat(targetManagement
                .findTargetByFilters(pageReq, pending, "%targ-B%", setA.getId(), Boolean.FALSE, targTagW.getName())
                .getContent()).as("has number of elements").hasSize(1)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(pending, "%targ-B%",
                                setA.getId(), Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, pending, "%targ-B%", setA.getId(), Boolean.FALSE,
                targTagW.getName())).as("has number of elements").hasSize(1).as("and contains the following elements")
                        .containsExactly(expectedIdName).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final TargetIdName expectedIdName = convertToIdName(expected);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement
                .findTargetByFilters(pageReq, pending, "%targ-A%", setA.getId(), Boolean.FALSE, new String[0])
                .getContent()).as("has number of elements").hasSize(1)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(pending, "%targ-A%",
                                setA.getId(), Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, pending, "%targ-A%", setA.getId(), Boolean.FALSE,
                new String[0])).as("has number of elements").hasSize(1).as("and contains the following elements")
                        .containsExactly(expectedIdName).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat3TargetsWithGivenDSAreInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement
                .findTargetByFilters(pageReq, pending, null, setA.getId(), Boolean.FALSE, new String[0]).getContent())
                        .as("has number of elements").hasSize(3).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(pending, null, setA.getId(),
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, pending, null, setA.getId(), Boolean.FALSE,
                new String[0])).as("has number of elements").hasSize(3).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat3TargetsAreInStatusPending(final List<TargetUpdateStatus> pending,
            final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==pending";

        assertThat(targetManagement.findTargetByFilters(pageReq, pending, null, null, Boolean.FALSE, new String[0])
                .getContent()).as("has number of elements").hasSize(3)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(pending, null, null,
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(
                targetManagement.findAllTargetIdsByFilters(pageReq, pending, null, null, Boolean.FALSE, new String[0]))
                        .as("has number of elements").hasSize(3).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==unknown and (name==*targ-B* or description==*targ-B*) and tag=="
                + targTagW.getName();

        assertThat(targetManagement
                .findTargetByFilters(pageReq, unknown, "%targ-B%", null, Boolean.FALSE, targTagW.getName())
                .getContent()).as("has number of elements").hasSize(99)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(unknown, "%targ-B%", null,
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, unknown, "%targ-B%", null, Boolean.FALSE,
                targTagW.getName())).as("has number of elements").hasSize(99).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==unknown and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement
                .findTargetByFilters(pageReq, unknown, "%targ-A%", null, Boolean.FALSE, new String[0]).getContent())
                        .as("has number of elements").hasSize(99).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(unknown, "%targ-A%", null,
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, unknown, "%targ-A%", null, Boolean.FALSE,
                new String[0])).as("has number of elements").hasSize(99).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verfyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(final DistributionSet setA,
            final List<TargetUpdateStatus> unknown) {
        final String query = "updatestatus==unknown and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement
                .findTargetByFilters(pageReq, unknown, null, setA.getId(), Boolean.FALSE, new String[0]).getContent())
                        .as("has number of elements").hasSize(0).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(unknown, null, setA.getId(),
                                Boolean.FALSE, new String[0])))
                        .as("and filter query returns the same result")
                        .hasSize(targetManagement.findTargetsAll(query, pageReq).getContent().size())
                        .as("and NAMED filter query returns the same result").hasSize(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent().size());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, unknown, null, setA.getId(), Boolean.FALSE,
                new String[0])).as("has number of elements").hasSize(0)
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findAllTargetIdsByTargetFilterQuery(pageReq, new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(final TargetTag targTagY,
            final TargetTag targTagW, final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==unknown and (tag==" + targTagY.getName() + " or tag==" + targTagW.getName()
                + ")";

        assertThat(targetManagement.findTargetByFilters(pageReq, unknown, null, null, Boolean.FALSE, targTagY.getName(),
                targTagW.getName()).getContent()).as("has number of elements").hasSize(198)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(unknown, null, null,
                                Boolean.FALSE, targTagY.getName(), targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, unknown, null, null, Boolean.FALSE,
                targTagY.getName(), targTagW.getName())).as("has number of elements").hasSize(198)
                        .as("and contains the following elements").containsAll(expectedIdNames)
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findAllTargetIdsByTargetFilterQuery(pageReq, new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat397TargetsAreInStatusUnknown(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "updatestatus==unknown";

        assertThat(targetManagement.findTargetByFilters(pageReq, unknown, null, null, Boolean.FALSE, new String[0])
                .getContent()).as("has number of elements").hasSize(397)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(unknown, null, null,
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(
                targetManagement.findAllTargetIdsByFilters(pageReq, unknown, null, null, Boolean.FALSE, new String[0]))
                        .as("has number of elements").hasSize(397).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat1TargetWithDescOrNameHasDS(final DistributionSet setA, final Target expected) {
        final TargetIdName expectedIdName = convertToIdName(expected);
        final String query = "(name==*targ-A* or description==*targ-A*) and (assignedds.name==" + setA.getName()
                + " or installedds.name==" + setA.getName() + ")";

        assertThat(targetManagement
                .findTargetByFilters(pageReq, null, "%targ-A%", setA.getId(), Boolean.FALSE, new String[0])
                .getContent()).as("has number of elements").hasSize(1)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(null, "%targ-A%",
                                setA.getId(), Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, null, "%targ-A%", setA.getId(), Boolean.FALSE,
                new String[0])).as("has number of elements").hasSize(1).as("and contains the following elements")
                        .containsExactly(expectedIdName).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat3TargetsHaveDSAssigned(final DistributionSet setA, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName();

        assertThat(targetManagement.findTargetByFilters(pageReq, null, null, setA.getId(), Boolean.FALSE, new String[0])
                .getContent()).as("has number of elements").hasSize(3)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(null, null, setA.getId(),
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, null, null, setA.getId(), Boolean.FALSE,
                new String[0])).as("has number of elements").hasSize(3).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat0TargetsWithNameOrdescAndDSHaveTag(final TargetTag targTagX, final DistributionSet setA) {
        final String query = "(name==*targ-C* or description==*targ-C*) and tag==" + targTagX.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement
                .findTargetByFilters(pageReq, null, "%targ-C%", setA.getId(), Boolean.FALSE, targTagX.getName())
                .getContent()).as("has number of elements").hasSize(0)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(null, "%targ-C%",
                                setA.getId(), Boolean.FALSE, targTagX.getName())))
                        .as("and filter query returns the same result")
                        .hasSize(targetManagement.findTargetsAll(query, pageReq).getContent().size())
                        .as("and NAMED filter query returns the same result").hasSize(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent().size());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagX.getName())).as("has number of elements").hasSize(0)
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findAllTargetIdsByTargetFilterQuery(pageReq, new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat0TargetsWithTagAndDescOrNameHasDS(final TargetTag targTagW, final DistributionSet setA) {
        final String query = "(name==*targ-A* or description==*targ-A*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement
                .findTargetByFilters(pageReq, null, "%targ-A%", setA.getId(), Boolean.FALSE, targTagW.getName())
                .getContent()).as("has number of elements").hasSize(0)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(null, "%targ-A%",
                                setA.getId(), Boolean.FALSE, targTagW.getName())))
                        .as("and filter query returns the same result")
                        .hasSize(targetManagement.findTargetsAll(query, pageReq).getContent().size())
                        .as("and NAMED filter query returns the same result").hasSize(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent().size());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, null, "%targ-A%", setA.getId(), Boolean.FALSE,
                targTagW.getName())).as("has number of elements").hasSize(0)
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findAllTargetIdsByTargetFilterQuery(pageReq, new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat1TargetHasTagHasDescOrNameAndDs(final TargetTag targTagW, final DistributionSet setA,
            final Target expected) {
        final TargetIdName expectedIdName = convertToIdName(expected);
        final String query = "(name==*targ-c* or description==*targ-C*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement
                .findTargetByFilters(pageReq, null, "%targ-C%", setA.getId(), Boolean.FALSE, targTagW.getName())
                .getContent()).as("has number of elements").hasSize(1)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(null, "%targ-C%",
                                setA.getId(), Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagW.getName())).as("has number of elements").hasSize(1).as("and contains the following elements")
                        .containsExactly(expectedIdName).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));
    }

    @Step
    private void verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(final TargetTag targTagY,
            final TargetTag targTagW, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "(name==*targ-B* or description==*targ-B*) and (tag==" + targTagY.getName() + " or tag=="
                + targTagW.getName() + ")";
        assertThat(targetManagement.findTargetByFilters(pageReq, null, "%targ-B%", null, Boolean.FALSE,
                targTagY.getName(), targTagW.getName()).getContent()).as("has number of elements").hasSize(100)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(null, "%targ-B%", null,
                                Boolean.FALSE, targTagY.getName(), targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, null, "%targ-B%", null, Boolean.FALSE,
                targTagY.getName(), targTagW.getName())).as("has number of elements").hasSize(100)
                        .as("and contains the following elements").containsAll(expectedIdNames)
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findAllTargetIdsByTargetFilterQuery(pageReq, new TargetFilterQuery("test", query)));

    }

    @SafeVarargs
    private final List<Target> concat(final List<Target>... targets) {
        final List<Target> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

    @Step
    private void verifyThat200TargetsHaveTagD(final TargetTag targTagD, final List<Target> expected) {
        final List<TargetIdName> expectedIdNames = convertToIdNames(expected);
        final String query = "tag==" + targTagD.getName();
        assertThat(targetManagement.findTargetByFilters(pageReq, null, null, null, Boolean.FALSE, targTagD.getName())
                .getContent()).as("Expected number of results is").hasSize(200)
                        .as("and is expected number of results is equal to ")
                        .hasSize(Ints.saturatedCast(targetManagement.countTargetByFilters(null, null, null,
                                Boolean.FALSE, targTagD.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findTargetsAll(query, pageReq).getContent())
                        .as("and NAMED filter query returns the same result").containsAll(targetManagement
                                .findTargetsAll(new TargetFilterQuery("test", query), pageReq).getContent());

        assertThat(targetManagement.findAllTargetIdsByFilters(pageReq, null, null, null, Boolean.FALSE,
                targTagD.getName())).as("has number of elements").hasSize(200).as("and contains the following elements")
                        .containsAll(expectedIdNames).as("and NAMED filter query returns the same result")
                        .containsAll(targetManagement.findAllTargetIdsByTargetFilterQuery(pageReq,
                                new TargetFilterQuery("test", query)));

    }

    @Step
    private void verifyThatRepositoryContains400Targets() {
        assertThat(targetManagement.findTargetByFilters(pageReq, null, null, null, null, new String[0]).getContent())
                .as("Overall we expect that many targets in the repository").hasSize(400)
                .as("which is also reflected by repository count")
                .hasSize(Ints.saturatedCast(targetManagement.countTargetsAll()))
                .as("which is also reflected by call without specification")
                .containsAll(targetManagement.findTargetsAll(pageReq).getContent());

    }

    @Test
    @Description("Tests the correct order of targets based on selected distribution set. The system expects to have an order based on installed, assigned DS.")
    public void targetSearchWithVariousFilterCombinationsAndOrderByDistributionSet() {

        final List<Target> notAssigned = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(3, "not", "first description"));
        List<Target> targAssigned = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(3, "assigned", "first description"));
        List<Target> targInstalled = targetManagement
                .createTargets(TestDataUtil.buildTargetFixtures(3, "installed", "first description"));

        final DistributionSet ds = TestDataUtil.generateDistributionSet("a", softwareManagement,
                distributionSetManagement);

        targAssigned = deploymentManagement.assignDistributionSet(ds, targAssigned).getAssignedTargets();
        targInstalled = deploymentManagement.assignDistributionSet(ds, targInstalled).getAssignedTargets();
        targInstalled = sendUpdateActionStatusToTargets(ds, targInstalled, Status.FINISHED, "installed");

        final Slice<Target> result = targetManagement.findTargetsAllOrderByLinkedDistributionSet(pageReq, ds.getId(),
                null, null, null, Boolean.FALSE, new String[0]);

        final Comparator<TenantAwareBaseEntity> byId = (e1, e2) -> Long.compare(e2.getId(), e1.getId());

        assertThat(result.getNumberOfElements()).isEqualTo(9);
        final List<Target> expected = new ArrayList<Target>();
        Collections.sort(targInstalled, byId);
        Collections.sort(targAssigned, byId);
        Collections.sort(notAssigned, byId);
        expected.addAll(targInstalled);
        expected.addAll(targAssigned);
        expected.addAll(notAssigned);

        assertThat(result.getContent()).containsExactly(expected.toArray(new Target[0]));

    }

    @Test
    @Description("Verfies that targets with given assigned DS are returned from repository.")
    public void findTargetByAssignedDistributionSet() {
        final DistributionSet assignedSet = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        targetManagement.createTargets(TestDataUtil.generateTargets(10, "unassigned"));
        List<Target> assignedtargets = targetManagement.createTargets(TestDataUtil.generateTargets(10, "assigned"));

        deploymentManagement.assignDistributionSet(assignedSet, assignedtargets);

        // get final updated version of targets
        assignedtargets = targetManagement.findTargetByControllerID(
                assignedtargets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        assertThat(targetManagement.findTargetByAssignedDistributionSet(assignedSet.getId(), pageReq))
                .as("Contains the assigned targets").containsAll(assignedtargets)
                .as("and that means the following expected amount").hasSize(10);

    }

    @Test
    @Description("Verfies that targets with given assigned DS and additonal specification are returned from repository.")
    public void findTargetByAssignedDistributionSetWithAdditonalSpecification() {
        final DistributionSet assignedSet = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final DistributionSet installedSet = TestDataUtil.generateDistributionSet("another", softwareManagement,
                distributionSetManagement);
        targetManagement.createTargets(TestDataUtil.generateTargets(10, "unassigned"));
        List<Target> assignedtargets = targetManagement.createTargets(TestDataUtil.generateTargets(10, "assigned"));

        // set on installed and assign another one
        deploymentManagement.assignDistributionSet(installedSet, assignedtargets).getActions().forEach(actionId -> {
            final Action action = deploymentManagement.findActionWithDetails(actionId);
            action.setStatus(Status.FINISHED);
            controllerManagament.addUpdateActionStatus(
                    new ActionStatus(action, Status.FINISHED, System.currentTimeMillis(), "message"), action);
        });
        deploymentManagement.assignDistributionSet(assignedSet, assignedtargets);

        assignedtargets = targetManagement.findTargetByControllerID(
                assignedtargets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        assertThat(targetManagement.findTargetByAssignedDistributionSet(assignedSet.getId(),
                TargetSpecifications.hasInstalledDistributionSet(installedSet.getId()), pageReq))
                        .as("Contains the assigned targets").containsAll(assignedtargets)
                        .as("and that means the following expected amount").hasSize(10);
    }

    @Test
    @Description("Verfies that targets with given installed DS are returned from repository.")
    public void findTargetByInstalledDistributionSet() {
        final DistributionSet assignedSet = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final DistributionSet installedSet = TestDataUtil.generateDistributionSet("another", softwareManagement,
                distributionSetManagement);
        targetManagement.createTargets(TestDataUtil.generateTargets(10, "unassigned"));
        List<Target> installedtargets = targetManagement.createTargets(TestDataUtil.generateTargets(10, "assigned"));

        // set on installed and assign another one
        deploymentManagement.assignDistributionSet(installedSet, installedtargets).getActions().forEach(actionId -> {
            final Action action = deploymentManagement.findActionWithDetails(actionId);
            action.setStatus(Status.FINISHED);
            controllerManagament.addUpdateActionStatus(
                    new ActionStatus(action, Status.FINISHED, System.currentTimeMillis(), "message"), action);
        });
        deploymentManagement.assignDistributionSet(assignedSet, installedtargets);

        // get final updated version of targets
        installedtargets = targetManagement.findTargetByControllerID(
                installedtargets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        assertThat(targetManagement.findTargetByInstalledDistributionSet(installedSet.getId(), pageReq))
                .as("Contains the assigned targets").containsAll(installedtargets)
                .as("and that means the following expected amount").hasSize(10);

    }

    @Test
    @Description("Verfies that targets with given installed DS and additonal specification  are returned from repository.")
    public void findTargetByInstalledDistributionSetWithAdditonalSpecification() {
        final DistributionSet assignedSet = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);
        final DistributionSet installedSet = TestDataUtil.generateDistributionSet("another", softwareManagement,
                distributionSetManagement);
        targetManagement.createTargets(TestDataUtil.generateTargets(10, "unassigned"));
        List<Target> installedtargets = targetManagement.createTargets(TestDataUtil.generateTargets(10, "assigned"));

        // set on installed and assign another one
        deploymentManagement.assignDistributionSet(installedSet, installedtargets).getActions().forEach(actionId -> {
            final Action action = deploymentManagement.findActionWithDetails(actionId);
            action.setStatus(Status.FINISHED);
            controllerManagament.addUpdateActionStatus(
                    new ActionStatus(action, Status.FINISHED, System.currentTimeMillis(), "message"), action);
        });
        deploymentManagement.assignDistributionSet(assignedSet, installedtargets);

        // get final updated version of targets
        installedtargets = targetManagement.findTargetByControllerID(
                installedtargets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        assertThat(targetManagement.findTargetByInstalledDistributionSet(installedSet.getId(),
                TargetSpecifications.hasAssignedDistributionSet(assignedSet.getId()), pageReq))
                        .as("Contains the assigned targets").containsAll(installedtargets)
                        .as("and that means the following expected amount").hasSize(10);

    }

    private List<Target> sendUpdateActionStatusToTargets(final DistributionSet dsA, final Iterable<Target> targs,
            final Status status, final String... msgs) {
        final List<Target> result = new ArrayList<Target>();
        for (final Target t : targs) {
            final List<Action> findByTarget = actionRepository.findByTarget(t);
            for (final Action action : findByTarget) {
                result.add(sendUpdateActionStatusToTarget(status, action, t, msgs));
            }
        }
        return result;
    }

    private Target sendUpdateActionStatusToTarget(final Status status, final Action updActA, final Target t,
            final String... msgs) {
        updActA.setStatus(status);

        final ActionStatus statusMessages = new ActionStatus();
        statusMessages.setAction(updActA);
        statusMessages.setOccurredAt(System.currentTimeMillis());
        statusMessages.setStatus(status);
        for (final String msg : msgs) {
            statusMessages.addMessage(msg);
        }
        controllerManagament.addUpdateActionStatus(statusMessages, updActA);
        return targetManagement.findTargetByControllerID(t.getControllerId());
    }

}
