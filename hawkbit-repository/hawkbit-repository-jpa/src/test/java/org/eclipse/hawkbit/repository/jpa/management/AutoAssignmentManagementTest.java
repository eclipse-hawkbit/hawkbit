/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.ALLOWED_ACTION_TYPES;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolationException;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement.Create;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement.Update;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAutoAssignment;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Test class for {@link AutoAssignmentManagement}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Auto Assignment Management
 */
class AutoAssignmentManagementTest extends AbstractRepositoryManagementTest<AutoAssignment, Create, Update> {

    @SuppressWarnings("unchecked")
    @Override
    protected <O> O forType(final Class<O> type) {
        if (DistributionSet.class.isAssignableFrom(type)) {
            // need to be completed in order to be assigned
            incrementEvents(DistributionSet.class, EventType.CREATED);
            incrementEvents(SoftwareModule.class, EventType.CREATED, 3);
            // creating an auto assignment implicitly locks its distribution set (and, in turn, its software modules)
            incrementEvents(DistributionSet.class, EventType.UPDATED);
            incrementEvents(SoftwareModule.class, EventType.UPDATED, 3);
            return (O) testdataFactory.createDistributionSet();
        } else if (type == Action.ActionType.class) {
            return (O) ALLOWED_ACTION_TYPES.toArray()[RND.nextInt(ALLOWED_ACTION_TYPES.size())];
        }

        return super.forType(type);
    }

    @Override
    protected Object builderParameterValue(final Method builderSetter) {
        // the query must be a valid RSQL expression
        if ("targetFilterQuery".equals(builderSetter.getName())) {
            return "name==PendingTargets001";
        }

        return super.builderParameterValue(builderSetter);
    }

    /**
     * Create an auto assignment with a query string that addresses too many targets.
     */
    @Test
    void createAutoAssignmentThatExceedsQuota() {
        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet set = testdataFactory.createDistributionSet();

        final Create create = Create.builder().name("testFilter").distributionSet(set).targetFilterQuery("name==target*").build();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> autoAssignmentManagement.create(create));
    }

    /**
     * Test creating auto assignments with the different (allowed) action types.
     */
    @Test
    void createWithActionType() {
        verifyCreateWithActionType("default", null, ActionType.FORCED);
        verifyCreateWithActionType("soft", ActionType.SOFT, ActionType.SOFT);
        verifyCreateWithActionType("downloadOnly", ActionType.DOWNLOAD_ONLY, ActionType.DOWNLOAD_ONLY);
    }

    /**
     * Creating an auto assignment with an action type that is not allowed (e.g. TIMEFORCED) is rejected.
     */
    @Test
    void createWithInvalidActionTypeFails() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final Create create = Create.builder()
                .name("invalidActionType").targetFilterQuery("name==*").distributionSet(distributionSet).actionType(ActionType.TIMEFORCED)
                .build();
        assertThatExceptionOfType(InvalidAutoAssignActionTypeException.class)
                .isThrownBy(() -> autoAssignmentManagement.create(create));
    }

    /**
     * Creating an auto assignment with an incomplete distribution set is rejected.
     */
    @Test
    void createWithIncompleteDistributionSetFails() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();
        final Create create = Create.builder().name("incompleteDs").targetFilterQuery("name==*").distributionSet(distributionSet).build();
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> autoAssignmentManagement.create(create));
    }

    /**
     * Creating an auto assignment with an invalid distribution set is rejected.
     */
    @Test
    void createWithInvalidDistributionSetFails() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();
        final Create create = Create.builder().name("invalidDs").targetFilterQuery("name==*").distributionSet(distributionSet).build();
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> autoAssignmentManagement.create(create));
    }

    /**
     * Creating an auto assignment with a soft deleted distribution set is rejected.
     */
    @Test
    void createWithSoftDeletedDistributionSetFails() {
        final DistributionSet softDeletedDs = testdataFactory.createDistributionSet("softDeleted");
        // assign the distribution set to a target, so that a delete becomes a soft delete
        assignDistributionSet(softDeletedDs, testdataFactory.createTarget("forSoftDeletedDs"));
        distributionSetManagement.delete(softDeletedDs.getId());

        // re-load the distribution set as the assignment implicitly locked (and thus updated) it
        final Create create = Create.builder().name("softDeletedDs").targetFilterQuery("name==*")
                .distributionSet(distributionSetManagement.get(softDeletedDs.getId())).build();
        assertThatExceptionOfType(DeletedException.class)
                .as("Soft deleted distributionSet should throw an exception")
                .isThrownBy(() -> autoAssignmentManagement.create(create));
    }

    /**
     * Deleting a distribution set removes the auto assignments referencing it.
     */
    @Test
    void deleteDistributionSetRemovesAutoAssignment() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final AutoAssignment autoAssignment = autoAssignmentManagement.create(
                Create.builder().name("toBeRemoved").targetFilterQuery("name==*").distributionSet(distributionSet).build());
        assertThat(autoAssignmentManagement.find(autoAssignment.getId())).isPresent();

        distributionSetManagement.delete(distributionSet.getId());

        assertThat(autoAssignmentManagement.find(autoAssignment.getId()))
                .as("Auto assignment should be removed together with its distribution set").isEmpty();
    }

    /**
     * Soft deleting a distribution set removes the auto assignments referencing it.
     */
    @Test
    void softDeleteDistributionSetRemovesAutoAssignment() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dist_set");
        final Target target = testdataFactory.createTarget();
        // assign the distribution set to a target, to force a soft delete in a later step
        assignDistributionSet(distributionSet.getId(), target.getControllerId());

        // re-load the distribution set as the assignment implicitly locked (and thus updated) it
        final AutoAssignment autoAssignment = autoAssignmentManagement.create(
                Create.builder().name("toBeRemoved").targetFilterQuery("name==*")
                        .distributionSet(distributionSetManagement.get(distributionSet.getId())).build());

        distributionSetManagement.delete(distributionSet.getId());

        // distribution set is still in the database with the deleted flag
        assertThat(distributionSetManagement.get(distributionSet.getId()).isDeleted()).as("Distribution set should be soft deleted").isTrue();
        assertThat(autoAssignmentManagement.find(autoAssignment.getId()))
                .as("Auto assignment should be removed together with its distribution set").isEmpty();
    }

    /**
     * Removing a distribution set from all auto assignments removes the affected auto assignments.
     */
    @Test
    void cancelAutoAssignmentForDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final AutoAssignment autoAssignment = autoAssignmentManagement.create(
                Create.builder().name("toBeCanceled").targetFilterQuery("name==*").distributionSet(distributionSet).build());

        autoAssignmentManagement.cancelAutoAssignmentForDistributionSet(distributionSet.getId());

        assertThat(autoAssignmentManagement.find(autoAssignment.getId())).isEmpty();
    }

    /**
     * Test finding auto assignments by their distribution set and an optional RSQL filter.
     */
    @Test
    void findByDistributionSetAndRsql() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet("2");

        final AutoAssignment autoAssignment = autoAssignmentManagement.create(
                Create.builder().name("c").targetFilterQuery("name==x").distributionSet(distributionSet).actionType(ActionType.SOFT).build());
        final AutoAssignment autoAssignment2 = autoAssignmentManagement.create(
                Create.builder().name("d").targetFilterQuery("name==z*").distributionSet(distributionSet2).build());

        // only one auto assignment references the distribution set
        verifyFindByDistributionSetAndRsql(distributionSet, null, autoAssignment);

        final AutoAssignment autoAssignment3 = autoAssignmentManagement.create(
                Create.builder().name("e").targetFilterQuery("name==*").distributionSet(distributionSet).build());

        // now two auto assignments reference the distribution set
        verifyFindByDistributionSetAndRsql(distributionSet, null, autoAssignment, autoAssignment3);
        // check if find works with name filter
        verifyFindByDistributionSetAndRsql(distributionSet, "name==e", autoAssignment3);
        // distribution set 2 is not affected
        verifyFindByDistributionSetAndRsql(distributionSet2, null, autoAssignment2);
    }

    /**
     * Test retrieving all active auto assignments.
     */
    @Test
    void findActiveAutoAssignments() {
        final AutoAssignment autoAssignment = autoAssignmentManagement.create(
                Create.builder().name("a").targetFilterQuery("name==*").distributionSet(testdataFactory.createDistributionSet("1")).build());
        final AutoAssignment autoAssignment2 = autoAssignmentManagement.create(
                Create.builder().name("b").targetFilterQuery("name==*").distributionSet(testdataFactory.createDistributionSet("2")).build());

        final Slice<AutoAssignment> active = autoAssignmentManagement.getActiveAutoAssignments(PageRequest.of(0, 500));
        assertThat(active.map(AutoAssignment::getId))
                .containsExactlyInAnyOrder(autoAssignment.getId(), autoAssignment2.getId());
    }

    /**
     * Creating an auto assignment with a weight outside the allowed range is rejected, otherwise the weight is stored.
     */
    @Test
    void weightValidatedAndSaved() {
        final Create tooHigh = Create.builder().name("tooHigh").targetFilterQuery("name==*")
                .distributionSet(testdataFactory.createDistributionSet("tooHigh")).weight(Action.WEIGHT_MAX + 1).build();
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> autoAssignmentManagement.create(tooHigh));

        final Create tooLow = Create.builder().name("tooLow").targetFilterQuery("name==*")
                .distributionSet(testdataFactory.createDistributionSet("tooLow")).weight(Action.WEIGHT_MIN - 1).build();
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> autoAssignmentManagement.create(tooLow));

        final AutoAssignment maxWeight = autoAssignmentManagement.create(
                Create.builder().name("maxWeight").targetFilterQuery("name==*")
                        .distributionSet(testdataFactory.createDistributionSet("maxWeight")).weight(Action.WEIGHT_MAX).build());
        assertThat(autoAssignmentManagement.get(maxWeight.getId()).getWeight()).contains(Action.WEIGHT_MAX);

        final AutoAssignment minWeight = autoAssignmentManagement.create(
                Create.builder().name("minWeight").targetFilterQuery("name==*")
                        .distributionSet(testdataFactory.createDistributionSet("minWeight")).weight(Action.WEIGHT_MIN).build());
        assertThat(autoAssignmentManagement.get(minWeight.getId()).getWeight()).contains(Action.WEIGHT_MIN);
    }

    /**
     * Tests the auto assign action type mapping.
     */
    @Test
    void autoAssignActionTypeConvert() {
        for (final ActionType actionType : ActionType.values()) {
            final Supplier<Long> create = () -> autoAssignmentManagement.create(
                    Create.builder()
                            .name("testAutoAssignActionTypeConvert_" + actionType)
                            .targetFilterQuery("name==*")
                            .distributionSet(testdataFactory.createDistributionSet("convert_" + actionType))
                            .actionType(actionType)
                            .build())
                    .getId();
            if (ALLOWED_ACTION_TYPES.contains(actionType)) {
                assertThat(autoAssignmentManagement.find(create.get()).orElseThrow().getActionType()).isEqualTo(actionType);
            } else {
                assertThatExceptionOfType(AbstractServerRtException.class).isThrownBy(create::get);
            }
        }

        final JpaAutoAssignment jpaAutoAssignment = (JpaAutoAssignment) autoAssignmentManagement.create(
                Create.builder().name("testAutoAssignActionTypeConvert").targetFilterQuery("name==*")
                        .distributionSet(testdataFactory.createDistributionSet("convert")).build());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> jpaAutoAssignment.setActionType(ActionType.TIMEFORCED));
    }

    /**
     * Referencing a non-existing distribution set when searching auto assignments fails.
     */
    @Test
    void failIfReferNotExistingDistributionSet() {
        verifyThrownExceptionBy(
                () -> autoAssignmentManagement.findByDSAndRsql(NOT_EXIST_IDL, "name==*", PAGE), "DistributionSet");
    }

    private void verifyCreateWithActionType(final String name, final ActionType actionType, final ActionType expectedActionType) {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet(name);
        final AutoAssignment autoAssignment = autoAssignmentManagement.create(
                Create.builder().name(name).targetFilterQuery("name==*").distributionSet(distributionSet).actionType(actionType).build());

        assertThat(autoAssignment.getDistributionSet().getId()).isEqualTo(distributionSet.getId());
        assertThat(autoAssignment.getActionType()).isEqualTo(expectedActionType);
    }

    private void verifyFindByDistributionSetAndRsql(
            final DistributionSet distributionSet, final String rsql, final AutoAssignment... expectedAutoAssignments) {
        final Page<AutoAssignment> autoAssignments = autoAssignmentManagement
                .findByDSAndRsql(distributionSet.getId(), rsql, PageRequest.of(0, 500));

        assertThat(autoAssignments.getTotalElements()).isEqualTo(expectedAutoAssignments.length);
        assertThat(autoAssignments.map(AutoAssignment::getId)).containsExactlyInAnyOrder(
                Arrays.stream(expectedAutoAssignments).map(AutoAssignment::getId).toArray(Long[]::new));
    }
}