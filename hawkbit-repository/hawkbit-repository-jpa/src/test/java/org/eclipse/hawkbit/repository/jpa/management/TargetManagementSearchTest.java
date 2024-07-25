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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Target Management Searches")
class TargetManagementSearchTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests different parameter combinations for target search operations. "
            + "That includes both the test itself, as a count operation with the same filters "
            + "and query definitions by RSQL (named and un-named).")
    void targetSearchWithVariousFilterCombinations() {
        final TargetTag targTagX = targetTagManagement.create(entityFactory.tag().create().name("TargTag-X"));
        final TargetTag targTagY = targetTagManagement.create(entityFactory.tag().create().name("TargTag-Y"));
        final TargetTag targTagZ = targetTagManagement.create(entityFactory.tag().create().name("TargTag-Z"));
        final TargetTag targTagW = targetTagManagement.create(entityFactory.tag().create().name("TargTag-W"));

        final DistributionSet setA = testdataFactory.createDistributionSet("A");
        final DistributionSet setB = testdataFactory.createDistributionSet("B");

        final TargetType targetTypeX = testdataFactory.createTargetType("TargetTypeX",
                Collections.singletonList(setB.getType()));

        final DistributionSet installedSet = testdataFactory.createDistributionSet("another");

        final Long lastTargetQueryNotOverdue = Instant.now().toEpochMilli();
        final Long lastTargetQueryAlwaysOverdue = 0L;

        final String targetDsAIdPref = "targ-A";
        List<Target> targAs = testdataFactory.createTargets(100, targetDsAIdPref,
                targetDsAIdPref.concat(" description"), lastTargetQueryNotOverdue);
        targAs = toggleTagAssignment(targAs, targTagX).getAssignedEntity();

        final Target targSpecialName = targetManagement
                .update(entityFactory.target().update(targAs.get(0).getControllerId()).name("targ-A-special"));

        final String targetDsBIdPref = "targ-B";
        List<Target> targBs = testdataFactory.createTargets(100, targetDsBIdPref,
                targetDsBIdPref.concat(" description"), lastTargetQueryAlwaysOverdue);

        targBs = toggleTagAssignment(targBs, targTagY).getAssignedEntity();
        targBs = toggleTagAssignment(targBs, targTagW).getAssignedEntity();

        final String targetDsCIdPref = "targ-C";
        List<Target> targCs = testdataFactory.createTargets(100, targetDsCIdPref,
                targetDsCIdPref.concat(" description"), lastTargetQueryAlwaysOverdue);

        targCs = toggleTagAssignment(targCs, targTagZ).getAssignedEntity();
        targCs = toggleTagAssignment(targCs, targTagW).getAssignedEntity();

        final String targetDsDIdPref = "targ-D";
        final List<Target> targDs = testdataFactory.createTargets(100, targetDsDIdPref,
                targetDsDIdPref.concat(" description"), null);

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
        targAs = targetManagement
                .getByControllerID(targAs.stream().map(Target::getControllerId).collect(Collectors.toList()));
        targBs = targetManagement
                .getByControllerID(targBs.stream().map(Target::getControllerId).collect(Collectors.toList()));
        targCs = targetManagement
                .getByControllerID(targCs.stream().map(Target::getControllerId).collect(Collectors.toList()));

        // try to find several targets with different filter settings
        verifyThat1TargetHasNameAndId("targ-A-special", targSpecialName.getControllerId());
        verifyThatRepositoryContains500Targets();
        verifyThat200TargetsHaveTagD(targTagW, concat(targBs, targCs));
        verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(targTagY, targTagW, targBs);
        verifyThat1TargetHasTagHasDescOrNameAndDs(targTagW, setA, targetManagement.getByControllerID(assignedC).get());
        verifyThat0TargetsWithTagAndDescOrNameHasDS(targTagW, setA);
        verifyThat0TargetsWithNameOrdescAndDSHaveTag(targTagX, setA);
        verifyThat3TargetsHaveDSAssigned(setA,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithDescOrNameHasDS(setA, targetManagement.getByControllerID(assignedA).get());
        List<Target> expected = concat(targAs, targBs, targCs, targDs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat496TargetsAreInStatusUnknown(unknown, expected);
        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(targTagY, targTagW, unknown, expected);
        verifyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(setA, unknown);
        expected = concat(targAs);
        expected.remove(targetManagement.getByControllerID(assignedA).get());
        verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(unknown, expected);
        expected = concat(targBs);
        expected.remove(targetManagement.getByControllerID(assignedB).get());
        verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(targTagW, unknown, expected);
        verifyThat4TargetsAreInStatusPending(pending,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC, assignedE)));
        verifyThat3TargetsWithGivenDSAreInPending(setA, pending,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(setA, pending,
                targetManagement.getByControllerID(assignedA).get());
        verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.getByControllerID(assignedB).get());
        verifyThat2TargetsWithGivenTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat2TargetsWithGivenTagAreInPending(targTagW, pending,
                targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat200targetsWithGivenTagAreInStatusPendingorUnknown(targTagW, both, concat(targBs, targCs));
        verifyThat1TargetAIsInStatusPendingAndHasDSInstalled(installedSet, pending,
                targetManagement.getByControllerID(installedC).get());
        verifyThat1TargetHasTypeAndDSAssigned(targetTypeX, setB, targetManagement.getByControllerID(assignedE).get());
        verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(setA,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(installedSet,
                targetManagement.getByControllerID(Collections.singletonList(installedC)));
        verifyThat100TargetsContainsGivenTextAndHaveTypeAssigned(targetTypeX, targEs);
        verifyThat400TargetsContainsGivenTextAndHaveNoTypeAssigned(concat(targAs, targBs, targCs, targDs));

        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndOverdue(unknown, expected);
    }

    @Step
    private void verifyThat1TargetAIsInStatusPendingAndHasDSInstalled(final DistributionSet installedSet,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, installedSet.getId(), Boolean.FALSE);
        final String query = "updatestatus==pending and installedds.name==" + installedSet.getName();
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat200targetsWithGivenTagAreInStatusPendingorUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> both, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(both, null, null, null, Boolean.FALSE, targTagW.getName());
        final String query = "(updatestatus==pending or updatestatus==unknown) and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(200).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAreInPending(final TargetTag targTagW,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, null, Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==pending and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(2).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAndDSIsInPending(final TargetTag targTagW, final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(2).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(final TargetTag targTagW,
            final DistributionSet setA, final List<TargetUpdateStatus> pending, final Target expected) {
        final FilterParams filterParams = new FilterParams(pending, null, "%targ-B%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-B* or description==*targ-B*) and tag==" + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final FilterParams filterParams = new FilterParams(pending, null, "%targ-A%", setA.getId(), Boolean.FALSE);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat3TargetsWithGivenDSAreInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE);
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(3).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat4TargetsAreInStatusPending(final List<TargetUpdateStatus> pending,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, null, Boolean.FALSE);
        final String query = "updatestatus==pending";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(4).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, "%targ-B%", null, Boolean.FALSE,
                targTagW.getName());
        final String query = "updatestatus==unknown and (name==*targ-B* or description==*targ-B*) and tag=="
                + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(99).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, "%targ-A%", null, Boolean.FALSE);
        final String query = "updatestatus==unknown and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(99).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(final DistributionSet setA,
            final List<TargetUpdateStatus> unknown) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, setA.getId(), Boolean.FALSE);
        final String query = "updatestatus==unknown and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(0).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and filter query returns the same result")
                .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());
    }

    @Step
    private void verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(final TargetTag targTagY,
            final TargetTag targTagW, final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, null, Boolean.FALSE, targTagY.getName(),
                targTagW.getName());
        final String query = "updatestatus==unknown and (tag==" + targTagY.getName() + " or tag==" + targTagW.getName()
                + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(198).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat496TargetsAreInStatusUnknown(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, null, Boolean.FALSE);
        final String query = "updatestatus==unknown";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(496).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat198TargetsAreInStatusUnknownAndOverdue(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, Boolean.TRUE, null, null, Boolean.FALSE);
        // be careful: simple filters are concatenated using AND-gating
        final String query = "lastcontrollerrequestat=le=${overdue_ts};updatestatus==UNKNOWN";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(198).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithDescOrNameHasDS(final DistributionSet setA, final Target expected) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-A%", setA.getId(), Boolean.FALSE);
        final String query = "(name==*targ-A* or description==*targ-A*) and (assignedds.name==" + setA.getName()
                + " or installedds.name==" + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat3TargetsHaveDSAssigned(final DistributionSet setA, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, null, setA.getId(), Boolean.FALSE);
        final String query = "assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(3).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat0TargetsWithNameOrdescAndDSHaveTag(final TargetTag targTagX, final DistributionSet setA) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagX.getName());
        final String query = "(name==*targ-C* or description==*targ-C*) and tag==" + targTagX.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(0).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and filter query returns the same result")
                .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());
    }

    @Step
    private void verifyThat0TargetsWithTagAndDescOrNameHasDS(final TargetTag targTagW, final DistributionSet setA) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-A%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "(name==*targ-A* or description==*targ-A*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(0).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and filter query returns the same result")
                .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());
    }

    @Step
    private void verifyThat1TargetHasTagHasDescOrNameAndDs(final TargetTag targTagW, final DistributionSet setA,
            final Target expected) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "(name==*targ-c* or description==*targ-C*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetHasNameAndId(final String name, final String controllerId) {
        final FilterParams filterParamsByName = new FilterParams(null, null, name, null, Boolean.FALSE);
        assertThat(targetManagement.findByFilters(PAGE, filterParamsByName).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParamsByName));

        final FilterParams filterParamsByControllerId = new FilterParams(null, null, controllerId, null, Boolean.FALSE);
        assertThat(targetManagement.findByFilters(PAGE, filterParamsByControllerId).getContent())
                .as("has number of elements").hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParamsByControllerId));
    }

    @Step
    private void verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(final TargetTag targTagY,
            final TargetTag targTagW, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, "%targ-B%", null, Boolean.FALSE,
                targTagY.getName(), targTagW.getName());
        final String query = "(name==*targ-B* or description==*targ-B*) and (tag==" + targTagY.getName() + " or tag=="
                + targTagW.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(100).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @SafeVarargs
    private List<Target> concat(final List<Target>... targets) {
        final List<Target> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

    @Step
    private void verifyThat200TargetsHaveTagD(final TargetTag targTagD, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, null, null, Boolean.FALSE, targTagD.getName());
        final String query = "tag==" + targTagD.getName();
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("Expected number of results is")
                .hasSize(200).as("and is expected number of results is equal to ")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected)
                .as("and filter query returns the same result")
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThatRepositoryContains500Targets() {
        final FilterParams filterParams = new FilterParams(null, null, null, null, null);
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .as("Overall we expect that many targets in the repository").hasSize(500)
                .as("which is also reflected by repository count").hasSize((int)targetManagement.count())
                .as("which is also reflected by call without specification")
                .containsAll(targetManagement.findAll(PAGE).getContent());
    }

    @Step
    private void verifyThat1TargetHasTypeAndDSAssigned(final TargetType type, final DistributionSet set,
            final Target expected) {
        final FilterParams filterParams = new FilterParams(null, set.getId(), Boolean.FALSE, type.getId());
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(1).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsExactly(expected);
    }

    @Step
    private void verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(final DistributionSet set,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, set.getId(), Boolean.TRUE, null);
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(expected.size()).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected);
    }

    @Step
    private void verifyThat100TargetsContainsGivenTextAndHaveTypeAssigned(final TargetType targetType,
            final List<Target> expected) {
        final FilterParams filterParams = new FilterParams("%targ-E%", null, Boolean.FALSE, targetType.getId());
        final List<Target> filteredTargets = targetManagement.findByFilters(PAGE, filterParams).getContent();
        assertThat(filteredTargets).as("has number of elements").hasSize(100)
                .as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams));
        // Comparing the controller ids, as one of the targets was modified, so
        // a 1:1
        // comparison of the objects is not possible
        assertThat(filteredTargets.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .containsAll(expected.stream().map(Target::getControllerId).collect(Collectors.toList()));
    }

    @Step
    private void verifyThat400TargetsContainsGivenTextAndHaveNoTypeAssigned(final List<Target> expected) {
        final FilterParams filterParams = new FilterParams("%targ-%", null, Boolean.TRUE, null);
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).as("has number of elements")
                .hasSize(400).as("that number is also returned by count query")
                .hasSize((int)targetManagement.countByFilters(filterParams))
                .as("and contains the following elements").containsAll(expected);
    }

    @Test
    @Description("Tests the correct order of targets based on selected distribution set. The system expects to have an order based on installed, assigned DS.")
    void targetSearchWithVariousFilterCombinationsAndOrderByDistributionSet() {

        final List<Target> notAssigned = testdataFactory.createTargets(3, "not", "first description");
        List<Target> targAssigned = testdataFactory.createTargets(3, "assigned", "first description");
        List<Target> targInstalled = testdataFactory.createTargets(3, "installed", "first description");

        final DistributionSet ds = testdataFactory.createDistributionSet("a");

        targAssigned = assignDistributionSet(ds, targAssigned).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = assignDistributionSet(ds, targInstalled).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = testdataFactory
                .sendUpdateActionStatusToTargets(targInstalled, Status.FINISHED, Collections.singletonList("installed"))
                .stream().map(Action::getTarget).collect(Collectors.toList());

        final Slice<Target> result = targetManagement.findByFilterOrderByLinkedDistributionSet(PAGE, ds.getId(),
                new FilterParams(null, null, null, null, Boolean.FALSE));

        final Comparator<TenantAwareBaseEntity> byId = Comparator.comparingLong(Identifiable::getId);

        assertThat(result.getNumberOfElements()).isEqualTo(9);
        final List<Target> expected = new ArrayList<>();
        targInstalled.sort(byId);
        targAssigned.sort(byId);
        notAssigned.sort(byId);
        expected.addAll(targInstalled);
        expected.addAll(targAssigned);
        expected.addAll(notAssigned);

        assertThat(result.getContent()).usingElementComparator(controllerIdComparator())
                .containsExactly(expected.toArray(new Target[0]));
    }

    @Test
    @Description("Tests the correct order of targets based on selected distribution set and sort parameter. The system expects to have an order based on installed, assigned DS.")
    void targetSearchWithOrderByDistributionSetAndSortParam() {

        final List<Target> notAssigned = testdataFactory.createTargets(3, "not", "first description");
        List<Target> targAssigned = testdataFactory.createTargets(3, "assigned", "first description");
        List<Target> targInstalled = testdataFactory.createTargets(3, "installed", "first description");

        final DistributionSet ds = testdataFactory.createDistributionSet("a");

        targAssigned = assignDistributionSet(ds, targAssigned).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = assignDistributionSet(ds, targInstalled).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = testdataFactory
                .sendUpdateActionStatusToTargets(targInstalled, Status.FINISHED, Collections.singletonList("installed"))
                .stream().map(Action::getTarget).collect(Collectors.toList());

        final List<Target> targetsOrderedByDistAndName = targetManagement
                .findByFilterOrderByLinkedDistributionSet(PageRequest.of(0, 500, Sort.by(Direction.DESC, "name")),
                        ds.getId(), new FilterParams(null, null, null, null, Boolean.FALSE))
                .getContent();
        assertThat(targetsOrderedByDistAndName).hasSize(9);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 0, targInstalled, 2);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 1, targInstalled, 1);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 2, targInstalled, 0);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 3, targAssigned, 2);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 4, targAssigned, 1);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 5, targAssigned, 0);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 6, notAssigned, 2);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 7, notAssigned, 1);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 8, notAssigned, 0);
    }

    private void assertThatTargetNameEquals(final List<Target> targets1, final int index1, final List<Target> targets2,
            final int index2) {
        assertThat(targets1.get(index1).getName()).isEqualTo(targets2.get(index2).getName());
    }

    @Test
    @Description("Tests the correct order of targets with applied overdue filter based on selected distribution set. The system expects to have an order based on installed, assigned DS.")
    void targetSearchWithOverdueFilterAndOrderByDistributionSet() {

        final Long lastTargetQueryAlwaysOverdue = 0L;
        final long lastTargetQueryNotOverdue = Instant.now().toEpochMilli();

        final Long[] overdueMix = { lastTargetQueryAlwaysOverdue, lastTargetQueryNotOverdue,
                lastTargetQueryAlwaysOverdue, null, lastTargetQueryAlwaysOverdue };

        final List<Target> notAssigned = new ArrayList<>(overdueMix.length);
        List<Target> targAssigned = new ArrayList<>(overdueMix.length);
        List<Target> targInstalled = new ArrayList<>(overdueMix.length);

        for (int i = 0; i < overdueMix.length; i++) {
            notAssigned.add(targetManagement
                    .create(entityFactory.target().create().controllerId("not" + i).lastTargetQuery(overdueMix[i])));
            targAssigned.add(targetManagement.create(
                    entityFactory.target().create().controllerId("assigned" + i).lastTargetQuery(overdueMix[i])));
            targInstalled.add(targetManagement.create(
                    entityFactory.target().create().controllerId("installed" + i).lastTargetQuery(overdueMix[i])));
        }

        final DistributionSet ds = testdataFactory.createDistributionSet("a");

        targAssigned = assignDistributionSet(ds, targAssigned).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = assignDistributionSet(ds, targInstalled).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = testdataFactory
                .sendUpdateActionStatusToTargets(targInstalled, Status.FINISHED, Collections.singletonList("installed"))
                .stream().map(Action::getTarget).collect(Collectors.toList());

        final Slice<Target> result = targetManagement.findByFilterOrderByLinkedDistributionSet(PAGE, ds.getId(),
                new FilterParams(null, Boolean.TRUE, null, null, Boolean.FALSE));

        final Comparator<TenantAwareBaseEntity> byId = Comparator.comparingLong(Identifiable::getId);

        assertThat(result.getNumberOfElements()).isEqualTo(9);
        final List<Target> expected = new ArrayList<>();
        expected.addAll(targInstalled.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .toList());
        expected.addAll(targAssigned.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .toList());
        expected.addAll(notAssigned.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .toList());

        assertThat(result.getContent()).usingElementComparator(controllerIdComparator())
                .containsExactly(expected.toArray(new Target[0]));

    }

    @Test
    @Description("Verfies that targets with given assigned DS are returned from repository.")
    void findTargetByAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        testdataFactory.createTargets(10, "unassigned", "unassigned");
        List<Target> assignedtargets = testdataFactory.createTargets(10, "assigned", "assigned");

        assignDistributionSet(assignedSet, assignedtargets);

        // get final updated version of targets
        assignedtargets = targetManagement
                .getByControllerID(assignedtargets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, assignedSet.getId()))
                .as("Contains the assigned targets").containsAll(assignedtargets)
                .as("and that means the following expected amount").hasSize(10);

    }

    @Test
    @Description("Verifies that targets without given assigned DS are returned from repository.")
    void findTargetWithoutAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("tfq").query("name==*"));
        final List<Target> unassignedTargets = testdataFactory.createTargets(12, "unassigned", "unassigned");
        final List<Target> assignedTargets = testdataFactory.createTargets(10, "assigned", "assigned");

        assignDistributionSet(assignedSet, assignedTargets);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(PAGE, assignedSet.getId(), tfq.getQuery()).getContent();
        assertThat(result).as("count of targets").hasSize(unassignedTargets.size()).as("contains all targets")
                .containsAll(unassignedTargets);

    }

    @Test
    @Description("Verifies that targets with given installed DS are returned from repository.")
    void findTargetByInstalledDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        final DistributionSet installedSet = testdataFactory.createDistributionSet("another");
        testdataFactory.createTargets(10, "unassigned", "unassigned");
        List<Target> installedtargets = testdataFactory.createTargets(10, "assigned", "assigned");

        // set on installed and assign another one
        assignDistributionSet(installedSet, installedtargets).getAssignedEntity().forEach(action -> controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(Status.FINISHED)));
        assignDistributionSet(assignedSet, installedtargets);

        // get final updated version of targets
        installedtargets = targetManagement
                .getByControllerID(installedtargets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, installedSet.getId()))
                .as("Contains the assigned targets").containsAll(installedtargets)
                .as("and that means the following expected amount").hasSize(10);

    }

    @Test
    @Description("Verifies that all compatible targets are returned from repository.")
    void shouldFindAllTargetsCompatibleWithDS() {
        final DistributionSet testDs = testdataFactory.createDistributionSet();
        final TargetType targetType = testdataFactory.createTargetType("testType",
                Collections.singletonList(testDs.getType()));
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("test-filter").query("name==*"));
        final List<Target> targets = testdataFactory.createTargets(20, "withOutType");
        final List<Target> targetWithCompatibleTypes = testdataFactory.createTargetsWithType(20, "compatible",
                targetType);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(PAGE, testDs.getId(), tfq.getQuery()).getContent();

        assertThat(result).as("count of targets").hasSize(targets.size() + targetWithCompatibleTypes.size())
                .as("contains all targets").containsAll(targetWithCompatibleTypes).containsAll(targets);
    }

    @Test
    @Description("Verifies that incompatible targets are not returned from repository.")
    void shouldNotFindTargetsIncompatibleWithDS() {
        final DistributionSetType dsType = testdataFactory.findOrCreateDistributionSetType("test-ds-type",
                "test-ds-type");
        final DistributionSet testDs = createDistSetWithType(dsType);
        final TargetType compatibleTargetType = testdataFactory.createTargetType("compTestType",
                Collections.singletonList(dsType));
        final TargetType incompatibleTargetType = testdataFactory.createTargetType("incompTestType",
                Collections.singletonList(testdataFactory.createDistributionSet().getType()));
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("test-filter").query("name==*"));

        final List<Target> targetsWithOutType = testdataFactory.createTargets(20, "withOutType");
        final List<Target> targetsWithCompatibleType = testdataFactory.createTargetsWithType(20, "compatible",
                compatibleTargetType);
        final List<Target> targetsWithIncompatibleType = testdataFactory.createTargetsWithType(20, "incompatible",
                incompatibleTargetType);

        final List<Target> testTargets = new ArrayList<>();
        testTargets.addAll(targetsWithOutType);
        testTargets.addAll(targetsWithCompatibleType);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(PAGE, testDs.getId(), tfq.getQuery()).getContent();

        assertThat(result).as("count of targets").hasSize(testTargets.size()).as("contains all compatible targets")
                .containsExactlyInAnyOrderElementsOf(testTargets).as("does not contain incompatible targets")
                .doesNotContainAnyElementsOf(targetsWithIncompatibleType);
    }

    private DistributionSet createDistSetWithType(final DistributionSetType type) {
        final DistributionSetCreate dsCreate = entityFactory.distributionSet().create().name("test-ds").version("1.0")
                .type(type);
        return distributionSetManagement.create(dsCreate);
    }

    @Test
    @Description("Verifies that targets with given target type are returned from repository.")
    public void findTargetByTargetType() {
        final TargetType testType = testdataFactory.createTargetType("testType",
                Collections.singletonList(standardDsType));
        final List<Target> unassigned = testdataFactory.createTargets(9, "unassigned");
        final List<Target> assigned = testdataFactory.createTargetsWithType(11, "assigned", testType);

        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, false, testType.getId())))
                .as("Contains the targets with set type").containsAll(assigned)
                .as("and that means the following expected amount").hasSize(11);
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, false, testType.getId())))
                .as("Count the targets with set type").isEqualTo(11);

        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, true, null)))
                .as("Contains the targets without a type").containsAll(unassigned)
                .as("and that means the following expected amount").hasSize(9);
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, true, null)))
                .as("Counts the targets without a type").isEqualTo(9);

    }

}
