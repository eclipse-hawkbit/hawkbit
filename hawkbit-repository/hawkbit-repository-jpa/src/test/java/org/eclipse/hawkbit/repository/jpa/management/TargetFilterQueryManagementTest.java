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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
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
class TargetFilterQueryManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies that management get access reacts as specfied on calls for non existing entities by means of Optional not present.
     */
    @Test
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class) })
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(targetFilterQueryManagement.find(NOT_EXIST_IDL)).isNotPresent();
    }

    /**
     * Verifies that management queries react as specfied on calls for non existing entities by means of throwing EntityNotFoundException.
     */
    @Test
    @ExpectEvents({
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1) })
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                Create.builder().name("test filter").query("name==PendingTargets001").build());

        verifyThrownExceptionBy(() -> targetFilterQueryManagement.delete(NOT_EXIST_IDL), "TargetFilterQuery");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.findByAutoAssignDSAndRsql(NOT_EXIST_IDL, "name==*", PAGE),
                "DistributionSet");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.update(Update.builder().id(NOT_EXIST_IDL).build()),
                "TargetFilterQuery");

        verifyThrownExceptionBy(() -> targetFilterQueryManagement.updateAutoAssignDS(
                        new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(NOT_EXIST_IDL)),
                "DistributionSet");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(
                        new AutoAssignDistributionSetUpdate(NOT_EXIST_IDL).ds(set.getId())),
                "TargetFilterQuery");

        verifyThrownExceptionBy(() -> targetFilterQueryManagement.updateAutoAssignDS(
                        new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(NOT_EXIST_IDL)),
                "DistributionSet");
    }

    /**
     * Test creation of target filter query.
     */
    @Test
    void createTargetFilterQuery() {
        final String filterName = "new target filter";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(Create.builder().name(filterName).query("name==PendingTargets001").build());
        assertEquals(targetFilterQuery, targetFilterQueryManagement.find(targetFilterQuery.getId()).get(),
                "Retrieved newly created custom target filter");
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
                .name("testfilter").autoAssignDistributionSet(set).query("name==target*").build();
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetFilterQueryManagement.create(targetFilterQueryCreate));
    }

    /**
     * Test searching a target filter query.
     */
    @Test
    void searchTargetFilterQuery() {
        final String filterName = "targetFilterQueryName";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(Create.builder().name(filterName).query("name==PendingTargets001").build());

        targetFilterQueryManagement.create(Create.builder().name("someOtherFilter").query("name==PendingTargets002").build());

        final List<? extends TargetFilterQuery> results = targetFilterQueryManagement
                .findByRsql("name==" + filterName, PageRequest.of(0, 10)).getContent();
        assertEquals(1, results.size(), "Search result should have 1 result");
        assertEquals(targetFilterQuery, results.get(0), "Retrieved newly created custom target filter");
    }

    /**
     * Test searching a target filter query with an invalid filter.
     */
    @Test
    void searchTargetFilterQueryInvalidField() {
        final PageRequest pageRequest = PageRequest.of(0, 10);
        Assertions.assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> targetFilterQueryManagement.findByRsql("unknownField==testValue", pageRequest));
    }

    /**
     * Checks if the EntityAlreadyExistsException is thrown if a targetFilterQuery with the same name are created more than once.
     */
    @Test
    void createDuplicateTargetFilterQuery() {
        final String filterName = "new target filter duplicate";
        final Create targetFilterQueryCreate = Create.builder().name(filterName).query("name==PendingTargets001").build();

        targetFilterQueryManagement.create(targetFilterQueryCreate);

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as query already exists")
                .isThrownBy(() -> targetFilterQueryManagement.create(targetFilterQueryCreate));
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
     * Test update of a target filter query.
     */
    @Test
    void updateTargetFilterQuery() {
        final String filterName = "target_filter_01";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(Create.builder().name(filterName).query("name==PendingTargets001").build());

        final String newQuery = "name==PendingTargets002";
        targetFilterQueryManagement.update(Update.builder().id(targetFilterQuery.getId()).query(newQuery).build());
        assertEquals(newQuery, targetFilterQueryManagement.find(targetFilterQuery.getId()).get().getQuery(),
                "Returns updated target filter query");
    }

    /**
     * Test assigning a distribution set for auto assignment with different action types
     */
    @Test
    void assignDistributionSet() {
        final String filterName = "target_filter_02";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(Create.builder().name(filterName).query("name==PendingTargets001").build());
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        verifyAutoAssignmentWithDefaultActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithSoftActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithDownloadOnlyActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithInvalidActionType(targetFilterQuery, distributionSet);
        verifyAutoAssignmentWithIncompleteDs(targetFilterQuery);
        verifyAutoAssignmentWithSoftDeletedDs(targetFilterQuery);
    }

    /**
     * Assigns a distribution set to an existing filter query and verifies that the quota 'max targets per auto assignment' is enforced.
     */
    @Test
    void assignDistributionSetToTargetFilterQueryThatExceedsQuota() {
        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        // creation is supposed to work as there is no distribution set
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(Create.builder().name("testfilter").query("name==target*").build());

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
    void updateTargetFilterQueryWithQueryThatExceedsQuota() {
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
        TargetFilterQuery tfq = targetFilterQueryManagement.find(targetFilterQuery.getId()).get();
        assertEquals(distributionSet, tfq.getAutoAssignDistributionSet(), "Returns correct distribution set");
        assertEquals(ActionType.FORCED, tfq.getAutoAssignActionType(), "Return correct action type");

        distributionSetManagement.delete(distributionSet.getId());

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.find(targetFilterQuery.getId()).get();
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
        TargetFilterQuery tfq = targetFilterQueryManagement.find(filterId).get();
        assertEquals(distributionSet, tfq.getAutoAssignDistributionSet(), "Returns correct distribution set");
        assertEquals(ActionType.FORCED, tfq.getAutoAssignActionType(), "Return correct action type");

        distributionSetManagement.delete(distributionSet.getId());

        // Check if distribution set is still in the database with deleted flag
        assertTrue(distributionSetManagement.find(distributionSet.getId()).get().isDeleted(), "Distribution set should be deleted");

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.find(filterId).get();
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
     * Creating or updating a target filter query with autoassignment and no-value weight when multi assignment in enabled.
     */
    @Test
    void weightNotRequiredInMultiAssignmentMode() {
        enableMultiAssignments();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("a").query("name==*").build()).getId();

        assertThat(
                targetFilterQueryManagement.create(Create.builder().name("b").query("name==*").autoAssignDistributionSet(ds).build()))
                .isNotNull();
        assertThat(
                targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId())))
                .isNotNull();
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

    /**
     * Auto assignment can be removed from filter when multi assignment in enabled.
     */
    @Test
    void removeDsFromFilterWhenMultiAssignmentModeNotEnabled() {
        enableMultiAssignments();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(
                Create.builder().name("a").query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(23).build()).getId();
        assertThat(targetFilterQueryManagement.updateAutoAssignDS(
                new AutoAssignDistributionSetUpdate(filterId).ds(null).weight(null)))
                .isNotNull();
    }

    /**
     * Weight is validated and saved to the Filter.
     */
    @Test
    void weightValidatedAndSaved() {
        enableMultiAssignments();
        final DistributionSet ds = testdataFactory.createDistributionSet();

        final Create targetFilterQueryCreate = Create.builder().name("a")
                .query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(Action.WEIGHT_MAX + 1).build();
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
                () -> targetFilterQueryManagement.create(targetFilterQueryCreate));

        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("a")
                .query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(Action.WEIGHT_MAX).build()).getId();
        assertThat(targetFilterQueryManagement.find(filterId).get().getAutoAssignWeight()).contains(Action.WEIGHT_MAX);

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
        assertThat(targetFilterQueryManagement.find(filterId).get().getAutoAssignWeight()).contains(Action.WEIGHT_MIN);
    }

    /**
     * Verifies that an exception is thrown when trying to create a target filter with an invalidated distribution set.
     */
    @Test
    void createTargetFilterWithInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        final Create targetFilterQueryCreate = Create.builder()
                .name("createTargetFilterWithInvalidDistributionSet").query("name==*").autoAssignDistributionSet(distributionSet)
                .build();
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.create(targetFilterQueryCreate));
    }

    /**
     * Verifies that an exception is thrown when trying to create a target filter with an incomplete distribution set.
     */
    @Test
    void createTargetFilterWithIncompleteDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        final Create targetFilterQueryCreate = Create.builder()
                .name("createTargetFilterWithIncompleteDistributionSet").query("name==*")
                .autoAssignDistributionSet(distributionSet)
                .build();
        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.create(targetFilterQueryCreate));
    }

    /**
     * Verifies that an exception is thrown when trying to update a target filter with an invalidated distribution set.
     */
    @Test
    void updateAutoAssignDsWithInvalidDistributionSet() {
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

    /**
     * Verifies that an exception is thrown when trying to update a target filter with an incomplete distribution set.
     */
    @Test
    void updateAutoAssignDsWithIncompleteDistributionSet() {
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
     * Tests the auto assign action type mapping.
     */
    @Test
    void testAutoAssignActionTypeConvert() {
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
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                jpaTargetFilterQuery.setAutoAssignActionType(ActionType.TIMEFORCED));
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
        final TargetFilterQuery tfq = targetFilterQueryManagement.find(filterId).get();

        assertEquals(distributionSet, tfq.getAutoAssignDistributionSet(), "Returns correct distribution set");
        assertEquals(actionType, tfq.getAutoAssignActionType(), "Return correct action type");
    }
}