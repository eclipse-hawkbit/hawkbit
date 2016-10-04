/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Test class for {@link AutoAssignChecker}.
 *
 */
@Features("Component Tests - Repository")
@Stories("Auto assign checker")
public class AutoAssignCheckerTest extends AbstractJpaIntegrationTest {

    @Autowired
    private AutoAssignChecker autoAssignChecker;

    @Test
    @Description("Test auto assignment of a DS to filtered targets")
    public void checkAutoAssign() {

        final DistributionSet setA = testdataFactory.createDistributionSet("dsA"); // will be auto assigned
        final DistributionSet setB = testdataFactory.createDistributionSet("dsB");

        // target filter query that matches all targets
        TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery("filterA", "name==*"));
        targetFilterQuery.setAutoAssignDistributionSet(setA);
        targetFilterQueryManagement.updateTargetFilterQuery(targetFilterQuery);


        final String targetDsAIdPref = "targ";
        List<Target> targets = targetManagement.createTargets(
                testdataFactory.generateTargets(100, targetDsAIdPref, targetDsAIdPref.concat(" description")));
        int targetsCount = targets.size();

        // assign set A to first 10 targets
        deploymentManagement.assignDistributionSet(setA, targets.subList(0, 10));
        verifyThatTargetsHaveDistributionSetAssignment(setA, targets.subList(0, 10), targetsCount);

        // assign set B to first 5 targets
        // they have now 2 DS in their action history and should not get updated with dsA
        deploymentManagement.assignDistributionSet(setB, targets.subList(0, 5));
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(0, 5), targetsCount);

        // assign set B to next 10 targets
        deploymentManagement.assignDistributionSet(setB, targets.subList(10, 20));
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(10, 20), targetsCount);

        // Count the number of targets that will be assigned with setA
        assertThat(targetManagement.countTargetsByTargetFilterQueryAndNonDS(setA.getId(), targetFilterQuery))
                .isEqualTo(90);

        // Run the check
        autoAssignChecker.check();

        verifyThatTargetsHaveDistributionSetAssignment(setA, targets.subList(5, 100), targetsCount);

        // first 5 should keep their dsB, because they already had the dsA once
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(0, 5), targetsCount);

    }

    @Test
    @Description("Test auto assignment of an incomplete DS to filtered targets, that causes failures")
    public void checkAutoAssignWithFailures() {

        // incomplete distribution set that will be assigned
        final DistributionSet setF = distributionSetManagement
                .createDistributionSet(entityFactory.generateDistributionSet("dsA", "1", "incomplete ds",
                        testdataFactory.findOrCreateDefaultTestDsType(), null));
        final DistributionSet setA = testdataFactory.createDistributionSet("dsA");
        final DistributionSet setB = testdataFactory.createDistributionSet("dsB");

        final String targetDsAIdPref = "targA";
        final String targetDsFIdPref = "targB";

        // target filter query that matches first bunch of targets, that should
        // fail
        targetFilterQueryManagement.createTargetFilterQuery(
                new JpaTargetFilterQuery("filterA", "id==" + targetDsFIdPref + "*", (JpaDistributionSet) setF));

        // target filter query that matches failed bunch of targets
        targetFilterQueryManagement.createTargetFilterQuery(
                new JpaTargetFilterQuery("filterB", "id==" + targetDsAIdPref + "*", (JpaDistributionSet) setA));

        List<Target> targetsF = targetManagement.createTargets(
                testdataFactory.generateTargets(10, targetDsFIdPref, targetDsFIdPref.concat(" description")));

        List<Target> targetsA = targetManagement.createTargets(
                testdataFactory.generateTargets(10, targetDsAIdPref, targetDsAIdPref.concat(" description")));

        int targetsCount = targetsA.size() + targetsF.size();

        // assign set B to first 5 targets of fail group
        deploymentManagement.assignDistributionSet(setB, targetsF.subList(0, 5));
        verifyThatTargetsHaveDistributionSetAssignment(setB, targetsF.subList(0, 5), targetsCount);

        // Run the check
        autoAssignChecker.check();

        // first 5 targets of the fail group should still have setB
        verifyThatTargetsHaveDistributionSetAssignment(setB, targetsF.subList(0, 5), targetsCount);

        // all targets of A group should have received setA
        verifyThatTargetsHaveDistributionSetAssignment(setA, targetsA, targetsCount);

    }

    /**
     * @param set the expected distribution set
     * @param targets the targets that should have it
     */
    @Step
    private void verifyThatTargetsHaveDistributionSetAssignment(final DistributionSet set, List<Target> targets, int count) {
        List<Long> targetIds = targets.stream().map(Target::getId).collect(Collectors.toList());

        Slice<Target> targetsAll = targetManagement.findTargetsAll(new PageRequest(0, 1000));
        assertThat(targetsAll).as("Count of targets").hasSize(count);

        for (Target target : targetsAll) {
            if(targetIds.contains(target.getId())) {
                assertThat(target.getAssignedDistributionSet()).as("assigned DS").isEqualTo(set);
            }
        }

    }

}
