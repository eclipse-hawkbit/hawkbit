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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.ALLOWED_AUTO_ASSIGN_ACTION_TYPES;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolationException;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Create;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Update;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.UpdateCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Test class for {@link TargetFilterQueryManagement}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Target Filter Query Management
 */
class TargetFilterQueryManagementTest extends AbstractRepositoryManagementTest<TargetFilterQuery, Create, Update> {

    @SuppressWarnings("unchecked")
    @Override
    protected <O> O forType(final Class<O> type) {
        if (DistributionSet.class.isAssignableFrom(type)) {
            // need to be completed in order to be assigned
            incrementEvents(DistributionSet.class, EventType.CREATED);
            incrementEvents(SoftwareModule.class, EventType.CREATED, 3);
            return (O) testdataFactory.createDistributionSet();
        } else if (type == Action.ActionType.class) {
            return (O) ALLOWED_AUTO_ASSIGN_ACTION_TYPES.toArray()[RND.nextInt(ALLOWED_AUTO_ASSIGN_ACTION_TYPES.size())];
        }

        return super.forType(type);
    }

    @Override
    protected Object builderParameterValue(final Method builderSetter) {
        // encrypted true is not supported
        if (builderSetter.getDeclaringClass() == UpdateCreate.UpdateCreateBuilder.class && "query".equals(builderSetter.getName())) {
            return "controllerId==PendingTargets001";
        }

        return super.builderParameterValue(builderSetter);
    }

    /**
     * Create a target filter query with an auto-assign distribution set and a query string that addresses too many targets.
     */
    @Test
    void createTargetFilterQueryThatExceedsQuota() {
        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet set = testdataFactory.createDistributionSet();

        // creation is supposed to work as there is no distribution set
        final Create targetFilterQueryCreate = Create.builder()
                .name("testFilter").autoAssignDistributionSet(set).query("name==target*").build();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetFilterQueryManagement.create(targetFilterQueryCreate));
    }

    @Test
    void findByRsqlTargetFilterQuery() {
        final String filterName = "targetFilterQueryName";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(Create.builder().name(filterName).query("name==PendingTargets001").build());

        targetFilterQueryManagement.create(Create.builder().name("someOtherFilter").query("name==PendingTargets002").build());

        final List<? extends TargetFilterQuery> results = targetFilterQueryManagement
                .findByRsql("name==" + filterName, PageRequest.of(0, 10)).getContent();
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(targetFilterQuery);
    }

    /**
     * Test deletion of target filter query.
     */
    @Test
    void deleteTargetFilterQuery() {
        final String filterName = "delete_target_filter_query";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name(filterName).query("name==PendingTargets001").build());
        targetFilterQueryManagement.delete(targetFilterQuery.getId());
        assertFalse(
                targetFilterQueryManagement.find(targetFilterQuery.getId()).isPresent(),
                "Returns null as the target filter is deleted");
    }

    /**
     * Test assigning a distribution set for auto assignment with different action types
     */
    @Test
    void assignDistributionSet() {
        final String filterName = "target_filter_02";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name(filterName).query("name==PendingTargets001").build());
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        verifyAutoAssignmentWithDefaultActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithSoftActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithDownloadOnlyActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithInvalidActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithIncompleteDs(targetFilterQuery);
        verifyAutoAssignmentWithSoftDeletedDs(targetFilterQuery);
    }

    /**
     * Test removing distribution set while it has a relation to a target filter query
     */
    @Test
    void removeAssignDistributionSet() {
        final String filterName = "target_filter_03";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(Create.builder().name(filterName).query("name==PendingTargets001").build());

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(distributionSet.getId()));
        implicitLock(distributionSet);

        // Check if target filter query is there
        verifyAutoAssignDsAndActionType(targetFilterQuery.getId(), distributionSet, ActionType.FORCED);

        distributionSetManagement.delete(distributionSet.getId());

        // Check if auto assign distribution set is null
        final TargetFilterQuery tfq = targetFilterQueryManagement.get(targetFilterQuery.getId());
        assertNotNull(tfq, "Returns target filter query");
        assertNull(tfq.getAutoAssignDistributionSet(), "Returns distribution set as null");
        assertNull(tfq.getAutoAssignActionType(), "Returns action type as null");
    }

    /**
     * Test to implicitly remove the auto assign distribution set when the ds is soft deleted
     */
    @Test
    void implicitlyRemoveAssignDistributionSet() {
        final String filterName = "target_filter_03";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dist_set");
        final Target target = testdataFactory.createTarget();

        // Assign the distribution set to an target, to force a soft delete in a later step
        assignDistributionSet(distributionSet.getId(), target.getControllerId());

        final Long filterId = targetFilterQueryManagement
                .create(Create.builder().name(filterName).query("name==PendingTargets001").build())
                .getId();
        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(filterId).ds(distributionSet.getId()));
        implicitLock(distributionSet);

        // Check if target filter query is there with the distribution set
        verifyAutoAssignDsAndActionType(filterId, distributionSet, ActionType.FORCED);

        distributionSetManagement.delete(distributionSet.getId());

        // Check if distribution set is still in the database with deleted flag
        assertTrue(distributionSetManagement.get(distributionSet.getId()).isDeleted(), "Distribution set should be deleted");

        // Check if auto assign distribution set is null
        final TargetFilterQuery tfq = targetFilterQueryManagement.get(filterId);
        assertNotNull(tfq, "Returns target filter query");
        assertNull(tfq.getAutoAssignDistributionSet(), "Returns distribution set as null");
        assertNull(tfq.getAutoAssignActionType(), "Returns action type as null");
    }

    /**
     * Test finding and auto assign distribution set
     */
    @Test
    void findFiltersWithDistributionSet() {
        final String filterName = "d";
        assertEquals(0L, targetFilterQueryManagement.count());
        targetFilterQueryManagement.create(Create.builder().name("a").query("name==*").build());
        targetFilterQueryManagement.create(Create.builder().name("b").query("name==*").build());
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet("2");

        final TargetFilterQuery tfq = targetFilterQueryManagement.create(
                Create.builder()
                        .name("c").query("name==x").autoAssignDistributionSet(distributionSet).autoAssignActionType(ActionType.SOFT)
                        .build());
        final TargetFilterQuery tfq2 = targetFilterQueryManagement.create(
                Create.builder().name(filterName).query("name==z*").autoAssignDistributionSet(distributionSet2).build());
        assertEquals(4L, targetFilterQueryManagement.count());

        // check if find works
        verifyFindByDistributionSetAndRsql(distributionSet, null, tfq);

        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(tfq2.getId()).ds(distributionSet.getId()));

        // check if find works for two
        verifyFindByDistributionSetAndRsql(distributionSet, null, tfq, tfq2);
        // check if find works with name filter
        verifyFindByDistributionSetAndRsql(distributionSet, "name==" + filterName, tfq2);
        verifyFindForAllWithAutoAssignDs(tfq, tfq2);
    }

    /**
     * Creating or updating a target filter query with autoassignment with a weight causes an error when multi assignment in disabled.
     */
    @Test
    void weightAllowedWhenMultiAssignmentModeNotEnabled() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("a").query("name==*").build()).getId();

        assertThat(
                targetFilterQueryManagement.create(
                        Create.builder().name("b").query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(342).build()))
                .isNotNull();
        assertThat(
                targetFilterQueryManagement.updateAutoAssignDS(
                        new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()).weight(343)))
                .isNotNull();
    }

    @Test
    void weightValidatedAndSaved() {
        final DistributionSet ds = testdataFactory.createDistributionSet();

        final Create targetFilterQueryCreate = Create.builder().name("a")
                .query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(Action.WEIGHT_MAX + 1).build();
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
                () -> targetFilterQueryManagement.create(targetFilterQueryCreate));

        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("a")
                .query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(Action.WEIGHT_MAX).build()).getId();
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignWeight()).contains(Action.WEIGHT_MAX);

        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate = new AutoAssignDistributionSetUpdate(filterId)
                .ds(ds.getId()).weight(Action.WEIGHT_MAX + 1);
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate));
        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate2 =
                new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()).weight(Action.WEIGHT_MIN - 1);
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate2));
        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()).weight(Action.WEIGHT_MAX));
        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()).weight(Action.WEIGHT_MIN));
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignWeight()).contains(Action.WEIGHT_MIN);
    }

    /**
     * Tests the auto assign action type mapping.
     */
    @Test
    void autoAssignActionTypeConvert() {
        for (final ActionType actionType : ActionType.values()) {
            final Supplier<Long> create = () ->
                    targetFilterQueryManagement.create(
                                    Create.builder()
                                            .name("testAutoAssignActionTypeConvert_" + actionType)
                                            .query("name==*")
                                            .autoAssignActionType(actionType)
                                            .build())
                            .getId();
            if (actionType == ActionType.TIMEFORCED) {
                assertThatExceptionOfType(AbstractServerRtException.class).isThrownBy(create::get);
            } else {
                assertThat(targetFilterQueryManagement.find(create.get()).orElseThrow().getAutoAssignActionType()).isEqualTo(actionType);
            }
        }

        final JpaTargetFilterQuery jpaTargetFilterQuery = (JpaTargetFilterQuery) targetFilterQueryManagement.create(
                Create.builder().name("testAutoAssignActionTypeConvert").query("name==*").build());
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> jpaTargetFilterQuery.setAutoAssignActionType(ActionType.TIMEFORCED));
    }

    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1) })
    void failIfReferNotExistingEntity() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name("test filter").query("name==PendingTargets001").build());

        verifyThrownExceptionBy(() -> targetFilterQueryManagement.delete(NOT_EXIST_IDL), "TargetFilterQuery");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.findByAutoAssignDSAndRsql(NOT_EXIST_IDL, "name==*", PAGE), "DistributionSet");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.update(Update.builder().id(NOT_EXIST_IDL).build()), "TargetFilterQuery");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(
                        new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(NOT_EXIST_IDL)),
                "DistributionSet");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(NOT_EXIST_IDL).ds(set.getId())),
                "TargetFilterQuery");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(
                        new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(NOT_EXIST_IDL)),
                "DistributionSet");
    }

    /**
     * Test searching a target filter query with an invalid filter.
     */
    @Test
    void failToFindTargetFilterQueryByInvalidField() {
        final PageRequest pageRequest = PageRequest.of(0, 10);
        Assertions.assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> targetFilterQueryManagement.findByRsql("unknownField==testValue", pageRequest));
    }

    @Test
    void failToCreateTargetFilterWithInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        final Create targetFilterQueryCreate = Create.builder()
                .name("createTargetFilterWithInvalidDistributionSet").query("name==*").autoAssignDistributionSet(distributionSet)
                .build();
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.create(targetFilterQueryCreate));
    }

    @Test
    void failToCreateTargetFilterWithIncompleteDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        final Create targetFilterQueryCreate = Create.builder()
                .name("createTargetFilterWithIncompleteDistributionSet").query("name==*")
                .autoAssignDistributionSet(distributionSet)
                .build();
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.create(targetFilterQueryCreate));
    }

    @Test
    void failToUpdateAutoAssignDsWithInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(Create.builder()
                .name("updateAutoAssignDsWithInvalidDistributionSet").query("name==*").autoAssignDistributionSet(distributionSet).build());
        final DistributionSet invalidDistributionSet = testdataFactory.createAndInvalidateDistributionSet();

        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate = new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                .ds(invalidDistributionSet.getId());
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate));
    }

    @Test
    void failToUpdateAutoAssignDsWithIncompleteDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name("updateAutoAssignDsWithIncompleteDistributionSet")
                        .query("name==*").autoAssignDistributionSet(distributionSet).build());
        final DistributionSet incompleteDistributionSet = testdataFactory.createIncompleteDistributionSet();

        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate = new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                .ds(incompleteDistributionSet.getId());
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate));
    }

    /**
     * Assigns a distribution set to an existing filter query and verifies that the quota 'max targets per auto assignment' is enforced.
     */
    @Test
    void failToAssignDistributionSetToTargetFilterQueryOnQuotaExceeded() {
        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        // creation is supposed to work as there is no distribution set
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name("testFilter").query("name==target*").build());

        // assigning a distribution set is supposed to fail as the query
        // addresses too many targets
        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate =
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(distributionSet.getId());
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate));
    }

    /**
     * Updates an existing filter query with a query string that addresses too many targets.
     */
    @Test
    void failToUpdateTargetFilterQueryOnQuotaExceeded() {
        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet set = testdataFactory.createDistributionSet();

        // creation is supposed to work as the query does not exceed the quota
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name("testfilter").autoAssignDistributionSet(set).query("name==foo").build());

        // update with a query string that addresses too many targets
        final Update targetFilterQueryUpdate = Update.builder().id(targetFilterQuery.getId()).query("name==target*").build();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetFilterQueryManagement.update(targetFilterQueryUpdate));
    }

    private void verifyAutoAssignmentWithDefaultActionType(final TargetFilterQuery targetFilterQuery, final DistributionSet distributionSet) {
        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(distributionSet.getId()));
        implicitLock(distributionSet);
        verifyAutoAssignDsAndActionType(targetFilterQuery.getId(), distributionSet, ActionType.FORCED);
    }

    private void verifyAutoAssignmentWithSoftActionType(final TargetFilterQuery targetFilterQuery, final DistributionSet distributionSet) {
        targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(distributionSet.getId()).actionType(ActionType.SOFT));
        verifyAutoAssignDsAndActionType(targetFilterQuery.getId(), distributionSet, ActionType.SOFT);
    }

    private void verifyAutoAssignmentWithDownloadOnlyActionType(final TargetFilterQuery targetFilterQuery, final DistributionSet distributionSet) {
        targetFilterQueryManagement
                .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                        .ds(distributionSet.getId()).actionType(ActionType.DOWNLOAD_ONLY));
        verifyAutoAssignDsAndActionType(targetFilterQuery.getId(), distributionSet, ActionType.DOWNLOAD_ONLY);
    }

    private void verifyAutoAssignmentWithInvalidActionType(final TargetFilterQuery targetFilterQuery,
            final DistributionSet distributionSet) {
        // assigning a distribution set with TIMEFORCED action is supposed to fail as only FORCED and SOFT action types are allowed
        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate =
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(distributionSet.getId()).actionType(ActionType.TIMEFORCED);
        assertThatExceptionOfType(InvalidAutoAssignActionTypeException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate));
    }

    private void verifyAutoAssignmentWithIncompleteDs(final TargetFilterQuery targetFilterQuery) {
        final DistributionSet incompleteDistributionSet = distributionSetManagement
                .create(DistributionSetManagement.Create.builder()
                        .type(testdataFactory.findOrCreateDefaultTestDsType())
                        .name("incomplete").version("1")
                        .build());

        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate =
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(incompleteDistributionSet.getId());
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate));
    }

    private void verifyAutoAssignmentWithSoftDeletedDs(final TargetFilterQuery targetFilterQuery) {
        final DistributionSet softDeletedDs = testdataFactory.createDistributionSet("softDeleted");
        assignDistributionSet(softDeletedDs, testdataFactory.createTarget("forSoftDeletedDs"));
        distributionSetManagement.delete(softDeletedDs.getId());

        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate =
                new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(softDeletedDs.getId());
        assertThatExceptionOfType(DeletedException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate));
    }

    private void verifyFindByDistributionSetAndRsql(
            final DistributionSet distributionSet, final String rsql, final TargetFilterQuery... expectedFilterQueries) {
        final Page<TargetFilterQuery> tfqList = targetFilterQueryManagement
                .findByAutoAssignDSAndRsql(distributionSet.getId(), rsql, PageRequest.of(0, 500));

        assertThat(tfqList.getTotalElements()).isEqualTo(expectedFilterQueries.length);
        verifyExpectedFilterQueriesInList(tfqList, expectedFilterQueries);
    }

    private void verifyExpectedFilterQueriesInList(final Slice<TargetFilterQuery> tfqList,
            final TargetFilterQuery... expectedFilterQueries) {
        assertThat(tfqList.map(TargetFilterQuery::getId)).containsExactly(
                Arrays.stream(expectedFilterQueries).map(TargetFilterQuery::getId).toArray(Long[]::new));
    }

    private void verifyFindForAllWithAutoAssignDs(final TargetFilterQuery... expectedFilterQueries) {
        final Slice<TargetFilterQuery> tfqList = targetFilterQueryManagement
                .findWithAutoAssignDS(PageRequest.of(0, 500));

        assertThat(tfqList.getNumberOfElements()).isEqualTo(expectedFilterQueries.length);
        verifyExpectedFilterQueriesInList(tfqList, expectedFilterQueries);
    }

    private void verifyExpectedFilterQueriesInList(final Page<TargetFilterQuery> tfqList,
            final TargetFilterQuery... expectedFilterQueries) {
        assertThat(expectedFilterQueries).as("Target filter query count").hasSize((int) tfqList.getTotalElements());

        assertThat(tfqList.map(TargetFilterQuery::getId)).containsExactly(
                Arrays.stream(expectedFilterQueries).map(TargetFilterQuery::getId).toArray(Long[]::new));
    }

    private void verifyAutoAssignDsAndActionType(final Long filterId, final DistributionSet distributionSet, final ActionType actionType) {
        final TargetFilterQuery tfq = targetFilterQueryManagement.get(filterId);
        assertThat(tfq.getAutoAssignDistributionSet()).isEqualTo(distributionSet);
        assertThat(tfq.getAutoAssignActionType()).isEqualTo(actionType);
    }
}