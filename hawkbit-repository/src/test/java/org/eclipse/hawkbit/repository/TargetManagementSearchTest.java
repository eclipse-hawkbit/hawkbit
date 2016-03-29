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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.TestDataUtil;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import com.google.common.primitives.Ints;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Target Management Searches")
public class TargetManagementSearchTest extends AbstractIntegrationTest {

    @Test
    @Description("Tests different parameter combinations for target search operations. That includes both the test itself as a count operation with the same filters.")
    public void targetSearchWithVariousFilterCombinations() {
        final TargetTag targTagA = tagManagement.createTargetTag(new TargetTag("TargTag-A"));
        final TargetTag targTagB = tagManagement.createTargetTag(new TargetTag("TargTag-B"));
        final TargetTag targTagC = tagManagement.createTargetTag(new TargetTag("TargTag-C"));
        final TargetTag targTagD = tagManagement.createTargetTag(new TargetTag("TargTag-D"));

        // TODO kaizimmerm: test also installedDS (not only assignedDS)

        final DistributionSet setA = TestDataUtil.generateDistributionSet("", softwareManagement,
                distributionSetManagement);

        final String targetDsAIdPref = "targ-A";
        List<Target> targAs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsAIdPref, targetDsAIdPref.concat(" description")));
        targAs = targetManagement.toggleTagAssignment(targAs, targTagA).getAssignedTargets();

        final String targetDsBIdPref = "targ-B";
        List<Target> targBs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsBIdPref, targetDsBIdPref.concat(" description")));
        targBs = targetManagement.toggleTagAssignment(targBs, targTagB).getAssignedTargets();
        targBs = targetManagement.toggleTagAssignment(targBs, targTagD).getAssignedTargets();

        final String targetDsCIdPref = "targ-C";
        List<Target> targCs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsCIdPref, targetDsCIdPref.concat(" description")));
        targCs = targetManagement.toggleTagAssignment(targCs, targTagC).getAssignedTargets();
        targCs = targetManagement.toggleTagAssignment(targCs, targTagD).getAssignedTargets();

        final String targetDsDIdPref = "targ-D";
        final Iterable<Target> targDs = targetManagement.createTargets(
                TestDataUtil.buildTargetFixtures(100, targetDsDIdPref, targetDsDIdPref.concat(" description")));

        deploymentManagement.assignDistributionSet(setA.getId(), targCs.iterator().next().getControllerId());
        deploymentManagement.assignDistributionSet(setA.getId(), targAs.iterator().next().getControllerId());
        deploymentManagement.assignDistributionSet(setA.getId(), targBs.iterator().next().getControllerId());

        final List<TargetUpdateStatus> unknown = new ArrayList<TargetUpdateStatus>();
        unknown.add(TargetUpdateStatus.UNKNOWN);

        final List<TargetUpdateStatus> pending = new ArrayList<TargetUpdateStatus>();
        pending.add(TargetUpdateStatus.PENDING);

        final List<TargetUpdateStatus> both = new ArrayList<TargetUpdateStatus>();
        both.add(TargetUpdateStatus.UNKNOWN);
        both.add(TargetUpdateStatus.PENDING);

        final PageRequest pageReq = new PageRequest(0, 500);
        // try to find several targets with different filter settings

        // TODO kaizimmerm: comment and check also the content itself, not only
        // the numbers
        // (containsOnly)
        assertThat(targetManagement.countTargetsAll()).isEqualTo(400);

        assertThat(targetManagement.findTargetByFilters(pageReq, null, null, null, Boolean.FALSE, targTagD.getName())
                .getNumberOfElements()).isEqualTo(200).isEqualTo(Ints.saturatedCast(
                        targetManagement.countTargetByFilters(null, null, null, Boolean.FALSE, targTagD.getName())));

        Slice<Target> x = targetManagement.findTargetByFilters(pageReq, null, "%targ-B%", null, Boolean.FALSE,
                targTagB.getName(), targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(100).isEqualTo(Ints.saturatedCast(targetManagement
                .countTargetByFilters(null, "%targ-B%", null, Boolean.FALSE, targTagB.getName(), targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(1).isEqualTo(Ints.saturatedCast(targetManagement
                .countTargetByFilters(null, "%targ-C%", setA.getId(), Boolean.FALSE, targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, null, "%targ-A%", setA.getId(), Boolean.FALSE,
                targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(0).isEqualTo(Ints.saturatedCast(targetManagement
                .countTargetByFilters(null, "%targ-A%", setA.getId(), Boolean.FALSE, targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, null, "%targ-C%", setA.getId(), Boolean.FALSE,
                targTagA.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(0).isEqualTo(Ints.saturatedCast(targetManagement
                .countTargetByFilters(null, "%targ-C%", setA.getId(), Boolean.FALSE, targTagA.getName())));

        x = targetManagement.findTargetByFilters(pageReq, null, null, setA.getId(), Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(3).isEqualTo(Ints
                .saturatedCast(targetManagement.countTargetByFilters(null, null, setA.getId(), Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, null, "%targ-A%", setA.getId(), Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(1).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(null, "%targ-A%", setA.getId(), Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, unknown, null, null, Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(397).isEqualTo(
                Ints.saturatedCast(targetManagement.countTargetByFilters(unknown, null, null, Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, unknown, null, null, Boolean.FALSE, targTagB.getName(),
                targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(198).isEqualTo(Ints.saturatedCast(targetManagement
                .countTargetByFilters(unknown, null, null, Boolean.FALSE, targTagB.getName(), targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, unknown, null, setA.getId(), Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(0).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(unknown, null, setA.getId(), Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, unknown, "%targ-A%", null, Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(99).isEqualTo(Ints
                .saturatedCast(targetManagement.countTargetByFilters(unknown, "%targ-A%", null, Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, unknown, "%targ-B%", null, Boolean.FALSE, targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(99).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(unknown, "%targ-B%", null, Boolean.FALSE, targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, unknown, null, null, Boolean.FALSE, targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(198).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(unknown, null, null, Boolean.FALSE, targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, pending, null, null, Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(3).isEqualTo(
                Ints.saturatedCast(targetManagement.countTargetByFilters(pending, null, null, Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, pending, null, setA.getId(), Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(3).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(pending, null, setA.getId(), Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, pending, "%targ-A%", setA.getId(), Boolean.FALSE, null);
        assertThat(x.getNumberOfElements()).isEqualTo(1).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(pending, "%targ-A%", setA.getId(), Boolean.FALSE, null)));

        x = targetManagement.findTargetByFilters(pageReq, pending, "%targ-B%", setA.getId(), Boolean.FALSE,
                targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(1).isEqualTo(Ints.saturatedCast(targetManagement
                .countTargetByFilters(pending, "%targ-B%", setA.getId(), Boolean.FALSE, targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, pending, null, setA.getId(), Boolean.FALSE,
                targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(2).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(pending, null, setA.getId(), Boolean.FALSE, targTagD.getName())));

        x = targetManagement.findTargetByFilters(pageReq, pending, null, null, Boolean.FALSE, targTagD.getName());
        assertThat(x.getNumberOfElements()).isEqualTo(2).isEqualTo(Ints.saturatedCast(
                targetManagement.countTargetByFilters(pending, null, null, Boolean.FALSE, targTagD.getName())));

        // Both status: 2 pending and 198 unknown
        assertThat(targetManagement.findTargetByFilters(pageReq, both, null, null, Boolean.FALSE, targTagD.getName())
                .getNumberOfElements()).isEqualTo(200).isEqualTo(Ints.saturatedCast(
                        targetManagement.countTargetByFilters(both, null, null, Boolean.FALSE, targTagD.getName())));

    }

    // TODO kaizimmerm: add filter tests
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
                null, null, null, Boolean.FALSE, null);

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
