/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Slice;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

@Feature("Component Tests - Repository")
@Story("Target Management Searches")
public class TargetManagementSearchTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests different parameter combinations for target search operations. "
            + "That includes both the test itself, as a count operation with the same filters "
            + "and query definitions by RSQL (named and un-named).")
    public void targetSearchWithVariousFilterCombinations() {
        final TargetTag targTagX = targetTagManagement.create(entityFactory.tag().create().name("TargTag-X"));
        final TargetTag targTagY = targetTagManagement.create(entityFactory.tag().create().name("TargTag-Y"));
        final TargetTag targTagZ = targetTagManagement.create(entityFactory.tag().create().name("TargTag-Z"));
        final TargetTag targTagW = targetTagManagement.create(entityFactory.tag().create().name("TargTag-W"));

        final DistributionSet setA = testdataFactory.createDistributionSet("");

        final DistributionSet installedSet = testdataFactory.createDistributionSet("another");

        final Long lastTargetQueryNotOverdue = Instant.now().toEpochMilli();
        final Long lastTargetQueryAlwaysOverdue = 0L;
        final Long lastTargetNull = null;

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
                targetDsDIdPref.concat(" description"), lastTargetNull);

        final String assignedC = targCs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedC);
        final String assignedA = targAs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedA);
        final String assignedB = targBs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedB);
        final String installedC = targCs.iterator().next().getControllerId();
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(installedSet.getId(), assignedC));

        // add attributes to match against only attribute value or attribute
        // value and name
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key", "targ-C-attribute-value");
        final Target targAttribute = controllerManagement.updateControllerAttributes(targCs.get(0).getControllerId(),
                attributes, UpdateMode.REPLACE);
        // prepare one target with an attribute value equal to controller id
        Target targAttributeId = targCs.get(15);
        final Map<String, String> idAttributes = new HashMap<>();
        idAttributes.put("key", targAttributeId.getControllerId());
        targAttributeId = controllerManagement.updateControllerAttributes(targAttributeId.getControllerId(),
                idAttributes, UpdateMode.REPLACE);

        // set one installed DS also
        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(actionId).status(Status.FINISHED).message("message"));
        assignDistributionSet(setA.getId(), installedC);

        final List<TargetUpdateStatus> unknown = Arrays.asList(TargetUpdateStatus.UNKNOWN);
        final List<TargetUpdateStatus> pending = Arrays.asList(TargetUpdateStatus.PENDING);
        final List<TargetUpdateStatus> both = Arrays.asList(TargetUpdateStatus.UNKNOWN, TargetUpdateStatus.PENDING);

        // get final updated version of targets
        targAs = targetManagement
                .getByControllerID(targAs.stream().map(Target::getControllerId).collect(Collectors.toList()));
        targBs = targetManagement
                .getByControllerID(targBs.stream().map(Target::getControllerId).collect(Collectors.toList()));
        targCs = targetManagement
                .getByControllerID(targCs.stream().map(Target::getControllerId).collect(Collectors.toList()));

        // try to find several targets with different filter settings
        verifyThat1TargetHasNameAndId("targ-A-special", targSpecialName.getControllerId());
        verifyThat1TargetHasAttributeValue("%c-attribute%", targAttribute.getControllerId());
        verifyThat1TargetHasAttributeValue("%" + targAttributeId.getControllerId() + "%",
                targAttributeId.getControllerId());
        verifyThatRepositoryContains400Targets();
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
        verifyThat397TargetsAreInStatusUnknown(unknown, expected);
        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(targTagY, targTagW, unknown, expected);
        verfyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(setA, unknown);
        expected = concat(targAs);
        expected.remove(targetManagement.getByControllerID(assignedA).get());
        verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(unknown, expected);
        expected = concat(targBs);
        expected.remove(targetManagement.getByControllerID(assignedB).get());
        verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(targTagW, unknown, expected);
        verifyThat3TargetsAreInStatusPending(pending,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
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
        verfiyThat1TargetAIsInStatusPendingAndHasDSInstalled(installedSet, pending,
                targetManagement.getByControllerID(installedC).get());

        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndOverdue(unknown, expected);
    }

    @Step
    private void verfiyThat1TargetAIsInStatusPendingAndHasDSInstalled(final DistributionSet installedSet,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final String query = "updatestatus==pending and installedds.name==" + installedSet.getName();
        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(pending, null, null, installedSet.getId(), Boolean.FALSE, new String[0]))
                .getContent())
                        .as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(pending, null, null,
                                installedSet.getId(), Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @Step
    private void verifyThat200targetsWithGivenTagAreInStatusPendingorUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> both, final List<Target> expected) {
        final String query = "(updatestatus==pending or updatestatus==unknown) and tag==" + targTagW.getName();

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(both, null, null, null, Boolean.FALSE, targTagW.getName()))
                .getContent()).as("has number of elements").hasSize(200)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(both, null, null, null,
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAreInPending(final TargetTag targTagW,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final String query = "updatestatus==pending and tag==" + targTagW.getName();

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(pending, null, null, null, Boolean.FALSE, targTagW.getName()))
                .getContent())
                        .as("has number of elements").hasSize(2).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(pending, null, null, null,
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAndDSIsInPending(final TargetTag targTagW, final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and tag==" + targTagW.getName();

        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE, targTagW.getName()))
                .getContent())
                        .as("has number of elements").hasSize(2).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(pending, null, null, setA.getId(),
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(final TargetTag targTagW,
            final DistributionSet setA, final List<TargetUpdateStatus> pending, final Target expected) {
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-B* or description==*targ-B*) and tag==" + targTagW.getName();

        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(pending, null, "%targ-B%", setA.getId(), Boolean.FALSE, targTagW.getName()))
                .getContent())
                        .as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(pending, null, "%targ-B%",
                                setA.getId(), Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final Target expected) {
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ") and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(pending, null, "%targ-A%", setA.getId(), Boolean.FALSE, new String[0]))
                .getContent())
                        .as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(pending, null, "%targ-A%",
                                setA.getId(), Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat3TargetsWithGivenDSAreInPending(final DistributionSet setA,
            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final String query = "updatestatus==pending and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE, new String[0]))
                .getContent())
                        .as("has number of elements").hasSize(3).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(pending, null, null, setA.getId(),
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat3TargetsAreInStatusPending(final List<TargetUpdateStatus> pending,
            final List<Target> expected) {
        final String query = "updatestatus==pending";

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(pending, null, null, null, Boolean.FALSE, new String[0]))
                .getContent())
                        .as("has number of elements").hasSize(3).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(pending, null, null, null,
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(final TargetTag targTagW,
            final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final String query = "updatestatus==unknown and (name==*targ-B* or description==*targ-B*) and tag=="
                + targTagW.getName();

        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(unknown, null, "%targ-B%", null, Boolean.FALSE, targTagW.getName()))
                .getContent()).as("has number of elements").hasSize(99)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(unknown, null, "%targ-B%", null,
                                Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final String query = "updatestatus==unknown and (name==*targ-A* or description==*targ-A*)";

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(unknown, null, "%targ-A%", null, Boolean.FALSE, new String[0]))
                .getContent()).as("has number of elements").hasSize(99)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(unknown, null, "%targ-A%", null,
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @Step
    private void verfyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(final DistributionSet setA,
            final List<TargetUpdateStatus> unknown) {
        final String query = "updatestatus==unknown and (assignedds.name==" + setA.getName() + " or installedds.name=="
                + setA.getName() + ")";

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(unknown, null, null, setA.getId(), Boolean.FALSE, new String[0]))
                .getContent())
                        .as("has number of elements").hasSize(0).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(unknown, null, null, setA.getId(),
                                Boolean.FALSE, new String[0])))
                        .as("and filter query returns the same result")
                        .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());
    }

    @Step
    private void verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(final TargetTag targTagY,
            final TargetTag targTagW, final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final String query = "updatestatus==unknown and (tag==" + targTagY.getName() + " or tag==" + targTagW.getName()
                + ")";

        assertThat(targetManagement.findByFilters(PAGE,
                new FilterParams(unknown, null, null, null, Boolean.FALSE, targTagY.getName(), targTagW.getName()))
                .getContent()).as("has number of elements").hasSize(198)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(unknown, null, null, null,
                                Boolean.FALSE, targTagY.getName(), targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat397TargetsAreInStatusUnknown(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        final String query = "updatestatus==unknown";

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(unknown, null, null, null, Boolean.FALSE, new String[0]))
                .getContent()).as("has number of elements").hasSize(397)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(unknown, null, null, null,
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @Step
    private void verifyThat198TargetsAreInStatusUnknownAndOverdue(final List<TargetUpdateStatus> unknown,
            final List<Target> expected) {
        // be careful: simple filters are concatenated using AND-gating
        final String query = "lastcontrollerrequestat=le=${overdue_ts};updatestatus==UNKNOWN";

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(unknown, Boolean.TRUE, null, null, Boolean.FALSE, new String[0]))
                .getContent()).as("has number of elements").hasSize(198)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(unknown, Boolean.TRUE, null, null,
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithDescOrNameHasDS(final DistributionSet setA, final Target expected) {
        final String query = "(name==*targ-A* or description==*targ-A*) and (assignedds.name==" + setA.getName()
                + " or installedds.name==" + setA.getName() + ")";

        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(null, null, "%targ-A%", setA.getId(), Boolean.FALSE, new String[0]))
                .getContent())
                        .as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(null, null, "%targ-A%",
                                setA.getId(), Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @Step
    private void verifyThat3TargetsHaveDSAssigned(final DistributionSet setA, final List<Target> expected) {
        final String query = "assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName();

        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(null, null, null, setA.getId(), Boolean.FALSE, new String[0]))
                .getContent())
                        .as("has number of elements").hasSize(3).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(null, null, null, setA.getId(),
                                Boolean.FALSE, new String[0])))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @Step
    private void verifyThat0TargetsWithNameOrdescAndDSHaveTag(final TargetTag targTagX, final DistributionSet setA) {
        final String query = "(name==*targ-C* or description==*targ-C*) and tag==" + targTagX.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(null, null, "%targ-C%", setA.getId(), Boolean.FALSE, targTagX.getName()))
                .getContent())
                        .as("has number of elements").hasSize(0).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(null, null, "%targ-C%",
                                setA.getId(), Boolean.FALSE, targTagX.getName())))
                        .as("and filter query returns the same result")
                        .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());

    }

    @Step
    private void verifyThat0TargetsWithTagAndDescOrNameHasDS(final TargetTag targTagW, final DistributionSet setA) {
        final String query = "(name==*targ-A* or description==*targ-A*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(null, null, "%targ-A%", setA.getId(), Boolean.FALSE, targTagW.getName()))
                .getContent())
                        .as("has number of elements").hasSize(0).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(null, null, "%targ-A%",
                                setA.getId(), Boolean.FALSE, targTagW.getName())))
                        .as("and filter query returns the same result")
                        .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());

    }

    @Step
    private void verifyThat1TargetHasTagHasDescOrNameAndDs(final TargetTag targTagW, final DistributionSet setA,
            final Target expected) {
        final String query = "(name==*targ-c* or description==*targ-C*) and tag==" + targTagW.getName()
                + " and (assignedds.name==" + setA.getName() + " or installedds.name==" + setA.getName() + ")";
        assertThat(targetManagement
                .findByFilters(PAGE,
                        new FilterParams(null, null, "%targ-C%", setA.getId(), Boolean.FALSE, targTagW.getName()))
                .getContent())
                        .as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(null, null, "%targ-C%",
                                setA.getId(), Boolean.FALSE, targTagW.getName())))
                        .as("and contains the following elements").containsExactly(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @Step
    private void verifyThat1TargetHasNameAndId(final String name, final String controllerId) {
        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, name, null, Boolean.FALSE))
                .getContent()).as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints
                                .saturatedCast(targetManagement.countByFilters(null, null, name, null, Boolean.FALSE)));

        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, controllerId, null, Boolean.FALSE))
                .getContent()).as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(
                                targetManagement.countByFilters(null, null, controllerId, null, Boolean.FALSE)));
    }

    @Step
    private void verifyThat1TargetHasAttributeValue(final String value, final String controllerId) {
        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, value, null, Boolean.FALSE))
                .getContent()).as("has number of elements").hasSize(1).as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(
                                targetManagement.countByFilters(null, null, value, null, Boolean.FALSE)));
    }

    @Step
    private void verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(final TargetTag targTagY,
            final TargetTag targTagW, final List<Target> expected) {
        final String query = "(name==*targ-B* or description==*targ-B*) and (tag==" + targTagY.getName() + " or tag=="
                + targTagW.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE,
                new FilterParams(null, null, "%targ-B%", null, Boolean.FALSE, targTagY.getName(), targTagW.getName()))
                .getContent()).as("has number of elements").hasSize(100)
                        .as("that number is also returned by count query")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(null, null, "%targ-B%", null,
                                Boolean.FALSE, targTagY.getName(), targTagW.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @SafeVarargs
    private final List<Target> concat(final List<Target>... targets) {
        final List<Target> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

    @Step
    private void verifyThat200TargetsHaveTagD(final TargetTag targTagD, final List<Target> expected) {
        final String query = "tag==" + targTagD.getName();
        assertThat(targetManagement
                .findByFilters(PAGE, new FilterParams(null, null, null, null, Boolean.FALSE, targTagD.getName()))
                .getContent()).as("Expected number of results is").hasSize(200)
                        .as("and is expected number of results is equal to ")
                        .hasSize(Ints.saturatedCast(targetManagement.countByFilters(null, null, null, null,
                                Boolean.FALSE, targTagD.getName())))
                        .as("and contains the following elements").containsAll(expected)
                        .as("and filter query returns the same result")
                        .containsAll(targetManagement.findByRsql(PAGE, query).getContent());

    }

    @Step
    private void verifyThatRepositoryContains400Targets() {
        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, null, null, null, new String[0]))
                .getContent()).as("Overall we expect that many targets in the repository").hasSize(400)
                        .as("which is also reflected by repository count")
                        .hasSize(Ints.saturatedCast(targetManagement.count()))
                        .as("which is also reflected by call without specification")
                        .containsAll(targetManagement.findAll(PAGE).getContent());

    }

    @Test
    @Description("Tests the correct order of targets based on selected distribution set. The system expects to have an order based on installed, assigned DS.")
    public void targetSearchWithVariousFilterCombinationsAndOrderByDistributionSet() {

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
                new FilterParams(null, null, null, null, Boolean.FALSE, new String[0]));

        final Comparator<TenantAwareBaseEntity> byId = (e1, e2) -> Long.compare(e2.getId(), e1.getId());

        assertThat(result.getNumberOfElements()).isEqualTo(9);
        final List<Target> expected = new ArrayList<>();
        Collections.sort(targInstalled, byId);
        Collections.sort(targAssigned, byId);
        Collections.sort(notAssigned, byId);
        expected.addAll(targInstalled);
        expected.addAll(targAssigned);
        expected.addAll(notAssigned);

        assertThat(result.getContent()).usingElementComparator(controllerIdComparator())
                .containsExactly(expected.toArray(new Target[0]));

    }

    @Test
    @Description("Tests the correct order of targets with applied overdue filter based on selected distribution set. The system expects to have an order based on installed, assigned DS.")
    public void targetSearchWithOverdueFilterAndOrderByDistributionSet() {

        final Long lastTargetQueryAlwaysOverdue = 0L;
        final Long lastTargetQueryNotOverdue = Instant.now().toEpochMilli();
        final Long lastTargetNull = null;

        final Long[] overdueMix = { lastTargetQueryAlwaysOverdue, lastTargetQueryNotOverdue,
                lastTargetQueryAlwaysOverdue, lastTargetNull, lastTargetQueryAlwaysOverdue };

        final List<Target> notAssigned = Lists.newArrayListWithExpectedSize(overdueMix.length);
        List<Target> targAssigned = Lists.newArrayListWithExpectedSize(overdueMix.length);
        List<Target> targInstalled = Lists.newArrayListWithExpectedSize(overdueMix.length);

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
                new FilterParams(null, Boolean.TRUE, null, null, Boolean.FALSE, new String[0]));

        final Comparator<TenantAwareBaseEntity> byId = (e1, e2) -> Long.compare(e2.getId(), e1.getId());

        assertThat(result.getNumberOfElements()).isEqualTo(9);
        final List<Target> expected = new ArrayList<>();
        expected.addAll(targInstalled.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .collect(Collectors.toList()));
        expected.addAll(targAssigned.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .collect(Collectors.toList()));
        expected.addAll(notAssigned.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .collect(Collectors.toList()));

        assertThat(result.getContent()).usingElementComparator(controllerIdComparator())
                .containsExactly(expected.toArray(new Target[0]));

    }

    @Test
    @Description("Verfies that targets with given assigned DS are returned from repository.")
    public void findTargetByAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        testdataFactory.createTargets(10, "unassigned", "unassigned");
        List<Target> assignedtargets = testdataFactory.createTargets(10, "assigned", "assigned");

        assignDistributionSet(assignedSet, assignedtargets);

        // get final updated version of targets
        assignedtargets = targetManagement.getByControllerID(
                assignedtargets.stream().map(target -> target.getControllerId()).collect(Collectors.toList()));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, assignedSet.getId()))
                .as("Contains the assigned targets").containsAll(assignedtargets)
                .as("and that means the following expected amount").hasSize(10);

    }

    @Test
    @Description("Verifies that targets without given assigned DS are returned from repository.")
    public void findTargetWithoutAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("tfq").query("name==*"));
        final List<Target> unassignedTargets = testdataFactory.createTargets(12, "unassigned", "unassigned");
        final List<Target> assignedTargets = testdataFactory.createTargets(10, "assigned", "assigned");

        assignDistributionSet(assignedSet, assignedTargets);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDS(PAGE, assignedSet.getId(), tfq.getQuery()).getContent();
        assertThat(result).as("count of targets").hasSize(unassignedTargets.size()).as("contains all targets")
                .containsAll(unassignedTargets);

    }

    @Test
    @Description("Verfies that targets with given installed DS are returned from repository.")
    public void findTargetByInstalledDistributionSet() {
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
}
