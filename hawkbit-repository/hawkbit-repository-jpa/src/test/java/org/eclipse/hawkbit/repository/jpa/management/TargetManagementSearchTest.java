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
import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.Test;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Target Management Searches
 */
class TargetManagementSearchTest extends AbstractJpaIntegrationTest {

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
                controllerManagement.addUpdateActionStatus(
                        ActionStatusCreate.builder().actionId(action.getId()).status(Status.FINISHED).build()));
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
}