/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Target Management Searches
 */
class TargetManagementSearchTest extends AbstractJpaIntegrationTest {

    private static final String SPACE_AND_DESCRIPTION = " description";

    /**
     * Verifies that targets with given target type are returned from repository.
     */
    @Test
    void findTargetByTargetType() {
        final TargetType testType = testdataFactory.createTargetType("testType", Set.of(standardDsType));
        final List<Target> unassigned = testdataFactory.createTargets(9, "unassigned");
        final List<Target> assigned = testdataFactory.createTargetsWithType(11, "assigned", testType);

        assertThat(targetManagement.findByFilters(new FilterParams(null, null, false, testType.getId()), PAGE))
                .as("Contains the targets with set type").containsAll(assigned)
                .as("and that means the following expected amount").hasSize(11);
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, false, testType.getId())))
                .as("Count the targets with set type").isEqualTo(11);

        assertThat(targetManagement.findByFilters(new FilterParams(null, null, true, null), PAGE))
                .as("Contains the targets without a type").containsAll(unassigned)
                .as("and that means the following expected amount").hasSize(9);
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, true, null)))
                .as("Counts the targets without a type").isEqualTo(9);

    }

    /**
     * Tests different parameter combinations for target search operations.
     * and query definitions by RSQL (named and un-named).
     */
    @Test
    void targetSearchWithVariousFilterCombinations() {
        final TargetTag targTagX = targetTagManagement.create(TargetTagManagement.Create.builder().name("TargTag-X").build());
        final TargetTag targTagY = targetTagManagement.create(TargetTagManagement.Create.builder().name("TargTag-Y").build());
        final TargetTag targTagZ = targetTagManagement.create(TargetTagManagement.Create.builder().name("TargTag-Z").build());
        final TargetTag targTagW = targetTagManagement.create(TargetTagManagement.Create.builder().name("TargTag-W").build());

        final DistributionSet setA = testdataFactory.createDistributionSet("A");
        final DistributionSet setB = testdataFactory.createDistributionSet("B");

        final TargetType targetTypeX = testdataFactory.createTargetType("TargetTypeX", Set.of(setB.getType()));

        final DistributionSet installedSet = testdataFactory.createDistributionSet("another");

        final Long lastTargetQueryNotOverdue = System.currentTimeMillis();
        final Long lastTargetQueryAlwaysOverdue = 0L;

        final String targetDsAIdPref = "targ-A";
        List<Target> targAs = testdataFactory.createTargets(100, targetDsAIdPref,
                targetDsAIdPref.concat(SPACE_AND_DESCRIPTION), lastTargetQueryNotOverdue);
        targAs = assignTag(targAs, targTagX);

        final Target targSpecialName = targetManagement
                .update(TargetManagement.Update.builder().id(targAs.get(0).getId()).name("targ-A-special").build());

        final String targetDsBIdPref = "targ-B";
        List<Target> targBs = testdataFactory.createTargets(100, targetDsBIdPref,
                targetDsBIdPref.concat(SPACE_AND_DESCRIPTION), lastTargetQueryAlwaysOverdue);

        targBs = assignTag(targBs, targTagY);
        targBs = assignTag(targBs, targTagW);

        final String targetDsCIdPref = "targ-C";
        List<Target> targCs = testdataFactory.createTargets(100, targetDsCIdPref,
                targetDsCIdPref.concat(SPACE_AND_DESCRIPTION), lastTargetQueryAlwaysOverdue);

        targCs = assignTag(targCs, targTagZ);
        targCs = assignTag(targCs, targTagW);

        final String targetDsDIdPref = "targ-D";
        final List<Target> targDs = testdataFactory.createTargets(100, targetDsDIdPref,
                targetDsDIdPref.concat(SPACE_AND_DESCRIPTION), null);

        final String targetDsEIdPref = "targ-E";
        final List<Target> targEs = testdataFactory.createTargetsWithType(100, targetDsEIdPref, targetTypeX);

        final String assignedC = targCs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedC);
        final String assignedA = targAs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedA);
        final String assignedB = targBs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedB);
        final String assignedE = targEs.iterator().next().getControllerId();
        assignDistributionSet(setB.getId(), assignedE);
        final String installedC = targCs.iterator().next().getControllerId();
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(installedSet.getId(), assignedC));

        // set one installed DS also
        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(actionId).status(Status.FINISHED).message("message"));
        assignDistributionSet(setA.getId(), installedC);

        final List<TargetUpdateStatus> unknown = List.of(TargetUpdateStatus.UNKNOWN);
        final List<TargetUpdateStatus> pending = List.of(TargetUpdateStatus.PENDING);
        final List<TargetUpdateStatus> both = List.of(TargetUpdateStatus.UNKNOWN, TargetUpdateStatus.PENDING);

        // get final updated version of targets
        targAs = targetManagement.getByControllerId(targAs.stream().map(Target::getControllerId).toList());
        targBs = targetManagement.getByControllerId(targBs.stream().map(Target::getControllerId).toList());
        targCs = targetManagement.getByControllerId(targCs.stream().map(Target::getControllerId).toList());

        // try to find several targets with different filter settings
        verifyThat1TargetHasNameAndId("targ-A-special", targSpecialName.getControllerId());
        verifyThatRepositoryContains500Targets();
        verifyThat200TargetsHaveTagD(targTagW, concat(targBs, targCs));
        verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(targTagY, targTagW, targBs);
        verifyThat1TargetHasTagHasDescOrNameAndDs(targTagW, setA, targetManagement.getByControllerId(assignedC).get());
        verifyThat0TargetsWithTagAndDescOrNameHasDS(targTagW, setA);
        verifyThat0TargetsWithNameOrdescAndDSHaveTag(targTagX, setA);
        verifyThat3TargetsHaveDSAssigned(setA, targetManagement.getByControllerId(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithDescOrNameHasDS(setA, targetManagement.getByControllerId(assignedA).get());
        List<Target> expected = concat(targAs, targBs, targCs, targDs);
        expected.removeAll(targetManagement.getByControllerId(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat496TargetsAreInStatusUnknown(unknown, expected);
        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerId(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(targTagY, targTagW, unknown, expected);
        verifyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(setA, unknown);
        expected = concat(targAs);
        expected.remove(targetManagement.getByControllerId(assignedA).get());
        verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(unknown, expected);
        expected = concat(targBs);
        expected.remove(targetManagement.getByControllerId(assignedB).get());
        verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(targTagW, unknown, expected);
        verifyThat4TargetsAreInStatusPending(pending,
                targetManagement.getByControllerId(Arrays.asList(assignedA, assignedB, assignedC, assignedE)));
        verifyThat3TargetsWithGivenDSAreInPending(setA, pending,
                targetManagement.getByControllerId(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(setA, pending,
                targetManagement.getByControllerId(assignedA).get());
        verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.getByControllerId(assignedB).get());
        verifyThat2TargetsWithGivenTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.getByControllerId(Arrays.asList(assignedB, assignedC)));
        verifyThat2TargetsWithGivenTagAreInPending(targTagW, pending,
                targetManagement.getByControllerId(Arrays.asList(assignedB, assignedC)));
        verifyThat200targetsWithGivenTagAreInStatusPendingOrUnknown(targTagW, both, concat(targBs, targCs));
        verifyThat1TargetAIsInStatusPendingAndHasDSInstalled(installedSet, pending,
                targetManagement.getByControllerId(installedC).get());
        verifyThat1TargetHasTypeAndDSAssigned(targetTypeX, setB, targetManagement.getByControllerId(assignedE).get());
        verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(setA,
                targetManagement.getByControllerId(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(installedSet,
                targetManagement.getByControllerId(Collections.singletonList(installedC)));
        verifyThat100TargetsContainsGivenTextAndHaveTypeAssigned(targetTypeX, targEs);
        verifyThat400TargetsContainsGivenTextAndHaveNoTypeAssigned(concat(targAs, targBs, targCs, targDs));

        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerId(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndOverdue(unknown, expected);
    }

    /**
     * Verifies that targets with given assigned DS are returned from repository.
     */
    @Test
    void findTargetByAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        testdataFactory.createTargets(10, "unassigned", "unassigned");
        List<Target> assignedtargets = testdataFactory.createTargets(10, "assigned", "assigned");

        assignDistributionSet(assignedSet, assignedtargets);

        // get final updated version of targets
        assignedtargets = targetManagement.getByControllerId(assignedtargets.stream().map(Target::getControllerId).toList());

        assertThat(targetManagement.findByAssignedDistributionSet(assignedSet.getId(), PAGE))
                .as("Contains the assigned targets").containsAll(assignedtargets)
                .as("and that means the following expected amount").hasSize(10);

    }

    /**
     * Verifies that targets without given assigned DS are returned from repository.
     */
    @Test
    void findTargetWithoutAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("tfq").query("name==*").build());
        final List<Target> unassignedTargets = testdataFactory.createTargets(12, "unassigned", "unassigned");
        final List<Target> assignedTargets = testdataFactory.createTargets(10, "assigned", "assigned");

        assignDistributionSet(assignedSet, assignedTargets);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(assignedSet.getId(), tfq.getQuery(), PAGE).getContent();
        assertThat(result).as("count of targets").hasSize(unassignedTargets.size()).as("contains all targets")
                .containsAll(unassignedTargets);

    }

    /**
     * Verifies that targets with given installed DS are returned from repository.
     */
    @Test
    void findTargetByInstalledDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        final DistributionSet installedSet = testdataFactory.createDistributionSet("another");
        testdataFactory.createTargets(10, "unassigned", "unassigned");
        List<Target> installedtargets = testdataFactory.createTargets(10, "assigned", "assigned");

        // set on installed and assign another one
        assignDistributionSet(installedSet, installedtargets).getAssignedEntity().forEach(action ->
                controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(Status.FINISHED)));
        assignDistributionSet(assignedSet, installedtargets);

        // get final updated version of targets
        installedtargets = targetManagement.getByControllerId(installedtargets.stream().map(Target::getControllerId).toList());

        assertThat(targetManagement.findByInstalledDistributionSet(installedSet.getId(), PAGE))
                .as("Contains the assigned targets").containsAll(installedtargets)
                .as("and that means the following expected amount").hasSize(10);

    }

    /**
     * Verifies that all compatible targets are returned from repository.
     */
    @Test
    void shouldFindAllTargetsCompatibleWithDS() {
        final DistributionSet testDs = testdataFactory.createDistributionSet();
        final TargetType targetType = testdataFactory.createTargetType("testType", Set.of(testDs.getType()));
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("test-filter").query("name==*").build());
        final List<Target> targets = testdataFactory.createTargets(20, "withOutType");
        final List<Target> targetWithCompatibleTypes = testdataFactory.createTargetsWithType(20, "compatible",
                targetType);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(testDs.getId(), tfq.getQuery(), PAGE).getContent();

        assertThat(result).as("count of targets").hasSize(targets.size() + targetWithCompatibleTypes.size())
                .as("contains all targets").containsAll(targetWithCompatibleTypes).containsAll(targets);
    }

    /**
     * Verifies that incompatible targets are not returned from repository.
     */
    @Test
    void shouldNotFindTargetsIncompatibleWithDS() {
        final DistributionSetType dsType = testdataFactory.findOrCreateDistributionSetType("test-ds-type", "test-ds-type");
        final DistributionSet testDs = distributionSetManagement.create(DistributionSetManagement.Create.builder()
                .type(dsType).name("test-ds").version("1.0").build());
        final TargetType compatibleTargetType = testdataFactory.createTargetType("compTestType", Set.of(dsType));
        final TargetType incompatibleTargetType = testdataFactory.createTargetType(
                "incompTestType", Set.of(testdataFactory.createDistributionSet().getType()));
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("test-filter").query("name==*").build());

        final List<Target> targetsWithOutType = testdataFactory.createTargets(20, "withOutType");
        final List<Target> targetsWithCompatibleType = testdataFactory.createTargetsWithType(20, "compatible",
                compatibleTargetType);
        final List<Target> targetsWithIncompatibleType = testdataFactory.createTargetsWithType(20, "incompatible",
                incompatibleTargetType);

        final List<Target> testTargets = new ArrayList<>();
        testTargets.addAll(targetsWithOutType);
        testTargets.addAll(targetsWithCompatibleType);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(testDs.getId(), tfq.getQuery(), PAGE).getContent();

        assertThat(result).as("count of targets").hasSize(testTargets.size()).as("contains all compatible targets")
                .containsExactlyInAnyOrderElementsOf(testTargets).as("does not contain incompatible targets")
                .doesNotContainAnyElementsOf(targetsWithIncompatibleType);
    }

    private void verifyThat1TargetAIsInStatusPendingAndHasDSInstalled(final DistributionSet installedSet,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, installedSet.getId(), Boolean.FALSE);
        final String query = "updatestatus==pending and installedds.name==" + installedSet.getName();
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat200targetsWithGivenTagAreInStatusPendingOrUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> both, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(both, null, null, null, Boolean.FALSE, targTagW.getName());
        final String query = "(updatestatus==pending or updatestatus==unknown) and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(200).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat2TargetsWithGivenTagAreInPending(final TargetTag targTagW,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, null, Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==pending and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(2).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat2TargetsWithGivenTagAndDSIsInPending(final TargetTag targTagW, final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(2).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(final TargetTag targTagW,
            final DistributionSet setA, final List<TargetUpdateStatus> pending, final Target expected) {
        final FilterParams filterParams = new FilterParams(pending, null, "%targ-B%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-B* or description==*targ-B*) and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final FilterParams filterParams = new FilterParams(pending, null, "%targ-A%", setA.getId(), Boolean.FALSE);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat3TargetsWithGivenDSAreInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(3).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat4TargetsAreInStatusPending(final List<TargetUpdateStatus> pending,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, null, Boolean.FALSE);
        final String query = "updatestatus==pending";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(4).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, "%targ-B%", null, Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==unknown and (name==*targ-B* or description==*targ-B*) and tag=="
                + targTagW.getName();

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(99).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, "%targ-A%", null, Boolean.FALSE);
        final String query = "updatestatus==unknown and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(99).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(final DistributionSet setA,
            final List<TargetUpdateStatus> unknown) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, setA.getId(), Boolean.FALSE);
        final String query = "updatestatus==unknown and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent())
                .as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and filter query returns the same result")
                .hasSize(targetManagement.findByRsql(query, PAGE).getContent().size())
                .as("has number of elements")
                .isEmpty();
    }

    private void verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(final TargetTag targTagY,
            final TargetTag targTagW, final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, null, Boolean.FALSE, targTagY.getName(),
                targTagW.getName());
        final String query = "updatestatus==unknown and (tag==" + targTagY.getName() + " or tag==" + targTagW.getName()
                + ")";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(198).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat496TargetsAreInStatusUnknown(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, null, Boolean.FALSE);
        final String query = "updatestatus==unknown";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(496).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat198TargetsAreInStatusUnknownAndOverdue(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, Boolean.TRUE, null, null, Boolean.FALSE);
        // be careful: simple filters are concatenated using AND-gating
        final String query = "lastcontrollerrequestat=le=${overdue_ts};updatestatus==UNKNOWN";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(198).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat1TargetWithDescOrNameHasDS(final DistributionSet setA, final Target expected) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-A%", setA.getId(), Boolean.FALSE);
        final String query = "(name==*targ-A* or description==*targ-A*) and (assignedds.name==" + setA.getName()
                + " or installedds.name==" + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat3TargetsHaveDSAssigned(final DistributionSet setA, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, null, setA.getId(), Boolean.FALSE);
        final String query = "assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName();

        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(3).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat0TargetsWithNameOrdescAndDSHaveTag(final TargetTag targTagX, final DistributionSet setA) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagX.getName());
        final String query = "(name==*targ-C* or description==*targ-C*) and tag==" + targTagX.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent())
                .as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and filter query returns the same result")
                .hasSize(targetManagement.findByRsql(query, PAGE).getContent().size())
                .as("has number of elements")
                .isEmpty();
    }

    private void verifyThat0TargetsWithTagAndDescOrNameHasDS(final TargetTag targTagW, final DistributionSet setA) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-A%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "(name==*targ-A* or description==*targ-A*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent())
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and filter query returns the same result")
                .hasSize(targetManagement.findByRsql(query, PAGE).getContent().size())
                .as("has number of elements")
                .isEmpty();
    }

    private void verifyThat1TargetHasTagHasDescOrNameAndDs(final TargetTag targTagW, final DistributionSet setA,
            final Target expected) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "(name==*targ-c* or description==*targ-C*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThat1TargetHasNameAndId(final String name, final String controllerId) {
        final FilterParams filterParamsByName = new FilterParams(null, null, name, null, Boolean.FALSE);
        assertThat(targetManagement.findByFilters(filterParamsByName, PAGE).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParamsByName));

        final FilterParams filterParamsByControllerId = new FilterParams(null, null, controllerId, null, Boolean.FALSE);
        assertThat(targetManagement.findByFilters(filterParamsByControllerId, PAGE).getContent())
                .as("has number of elements").hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParamsByControllerId));
    }

    private void verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(final TargetTag targTagY,
            final TargetTag targTagW, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-B%", null, Boolean.FALSE,
                targTagY.getName(), targTagW.getName());
        final String query = "(name==*targ-B* or description==*targ-B*) and (tag==" + targTagY.getName() + " or tag=="
                + targTagW.getName() + ")";
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(100).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    @SafeVarargs
    private List<Target> concat(final List<Target>... targets) {
        final List<Target> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

    private void verifyThat200TargetsHaveTagD(final TargetTag targTagD, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, null, null, Boolean.FALSE, targTagD.getName());
        final String query = "tag==" + targTagD.getName();
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("Expected number of results is")
                .hasSize(200).as("and is expected number of results is equal to ")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(query, PAGE).getContent());
    }

    private void verifyThatRepositoryContains500Targets() {
        final FilterParams filterParams = new FilterParams(null, null, null, null, null);
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent())
                .as("Overall we expect that many targets in the repository").hasSize(500)
                .as("which is also reflected by repository count").hasSize((int) targetManagement.count())
                .as("which is also reflected by call without specification")
                .containsAll(targetManagement.findAll(PAGE).getContent());
    }

    private void verifyThat1TargetHasTypeAndDSAssigned(final TargetType type, final DistributionSet set,
            final Target expected) {
        final FilterParams filterParams = new FilterParams(null, set.getId(), Boolean.FALSE, type.getId());
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected);
    }

    private void verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(final DistributionSet set,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, set.getId(), Boolean.TRUE, null);
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(expected.size()).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected);
    }

    private void verifyThat100TargetsContainsGivenTextAndHaveTypeAssigned(final TargetType targetType,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams("%targ-E%", null, Boolean.FALSE, targetType.getId());
        final List<Target> filteredTargets = targetManagement.findByFilters(filterParams, PAGE).getContent();
        assertThat(filteredTargets).as("has number of elements").hasSize(100)
                .as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams));
        // Comparing the controller ids, as one of the targets was modified, so
        // a 1:1
        // comparison of the objects is not possible
        assertThat(filteredTargets.stream().map(Target::getControllerId).toList())
                .containsAll(expected.stream().map(Target::getControllerId).toList());
    }

    private void verifyThat400TargetsContainsGivenTextAndHaveNoTypeAssigned(final List<Target> expected) {
        final FilterParams filterParams = new FilterParams("%targ-%", null, Boolean.TRUE, null);
        assertThat(targetManagement.findByFilters(filterParams, PAGE).getContent()).as("has number of elements")
                .hasSize(400).as("that number is also returned by count query")
                .hasSize((int) targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected);
    }
}