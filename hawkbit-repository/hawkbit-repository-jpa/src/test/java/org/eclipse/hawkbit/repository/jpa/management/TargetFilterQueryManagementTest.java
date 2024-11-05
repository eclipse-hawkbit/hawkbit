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

import jakarta.validation.ConstraintViolationException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
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
 */
@Feature("Component Tests - Repository")
@Story("Target Filter Query Management")
public class TargetFilterQueryManagementTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 0) })
    public void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(targetFilterQueryManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(targetFilterQueryManagement.getByName(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({ @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1) })
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("test filter").query("name==PendingTargets001"));

        verifyThrownExceptionBy(() -> targetFilterQueryManagement.delete(NOT_EXIST_IDL), "TargetFilterQuery");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.findByAutoAssignDSAndRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                "DistributionSet");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.update(entityFactory.targetFilterQuery().update(NOT_EXIST_IDL)),
                "TargetFilterQuery");

        verifyThrownExceptionBy(() -> targetFilterQueryManagement.updateAutoAssignDS(
                        entityFactory.targetFilterQuery().updateAutoAssign(targetFilterQuery.getId()).ds(NOT_EXIST_IDL)),
                "DistributionSet");

        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(
                        entityFactory.targetFilterQuery().updateAutoAssign(NOT_EXIST_IDL).ds(set.getId())),
                "TargetFilterQuery");

        verifyThrownExceptionBy(() -> targetFilterQueryManagement.updateAutoAssignDS(
                        entityFactory.targetFilterQuery().updateAutoAssign(targetFilterQuery.getId()).ds(NOT_EXIST_IDL)),
                "DistributionSet");
    }

    @Test
    @Description("Test creation of target filter query.")
    public void createTargetFilterQuery() {
        final String filterName = "new target filter";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));
        assertEquals(targetFilterQuery, targetFilterQueryManagement.getByName(filterName).get(),
                "Retrieved newly created custom target filter");
    }

    @Test
    @Description("Create a target filter query with an auto-assign distribution set and a query string that addresses too many targets.")
    public void createTargetFilterQueryThatExceedsQuota() {

        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet set = testdataFactory.createDistributionSet();

        // creation is supposed to work as there is no distribution set
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create()
                        .name("testfilter").autoAssignDistributionSet(set.getId()).query("name==target*")));
    }

    @Test
    @Description("Test searching a target filter query.")
    public void searchTargetFilterQuery() {
        final String filterName = "targetFilterQueryName";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));

        targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("someOtherFilter").query("name==PendingTargets002"));

        final List<TargetFilterQuery> results = targetFilterQueryManagement
                .findByRsql(PageRequest.of(0, 10), "name==" + filterName).getContent();
        assertEquals(1, results.size(), "Search result should have 1 result");
        assertEquals(targetFilterQuery, results.get(0), "Retrieved newly created custom target filter");
    }

    @Test
    @Description("Test searching a target filter query with an invalid filter.")
    public void searchTargetFilterQueryInvalidField() {
        Assertions.assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> targetFilterQueryManagement
                        .findByRsql(PageRequest.of(0, 10), "unknownField==testValue").getContent());
    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if a targetfilterquery with the same name are created more than once.")
    public void createDuplicateTargetFilterQuery() {
        final String filterName = "new target filter duplicate";
        targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("should not have worked as query already exists")
                .isThrownBy(() -> targetFilterQueryManagement.create(
                        entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001")));
    }

    @Test
    @Description("Test deletion of target filter query.")
    public void deleteTargetFilterQuery() {
        final String filterName = "delete_target_filter_query";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));
        targetFilterQueryManagement.delete(targetFilterQuery.getId());
        assertFalse(targetFilterQueryManagement.get(targetFilterQuery.getId()).isPresent(),
                "Returns null as the target filter is deleted");
    }

    @Test
    @Description("Test updation of target filter query.")
    public void updateTargetFilterQuery() {
        final String filterName = "target_filter_01";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));

        final String newQuery = "status==UNKNOWN";
        targetFilterQueryManagement
                .update(entityFactory.targetFilterQuery().update(targetFilterQuery.getId()).query(newQuery));
        assertEquals(newQuery, targetFilterQueryManagement.getByName(filterName).get().getQuery(),
                "Returns updated target filter query");
    }

    @Test
    @Description("Test assigning a distribution set for auto assignment with different action types")
    public void assignDistributionSet() {
        final String filterName = "target_filter_02";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        verifyAutoAssignmentWithDefaultActionType(filterName, targetFilterQuery, distributionSet);

        verifyAutoAssignmentWithSoftActionType(filterName, targetFilterQuery, distributionSet);

        verifyAutoAssignmentWithDownloadOnlyActionType(filterName, targetFilterQuery, distributionSet);

        verifyAutoAssignmentWithInvalidActionType(targetFilterQuery, distributionSet);

        verifyAutoAssignmentWithIncompleteDs(targetFilterQuery);

        verifyAutoAssignmentWithSoftDeletedDs(targetFilterQuery);
    }

    @Test
    @Description("Assigns a distribution set to an existing filter query and verifies that the quota 'max targets per auto assignment' is enforced.")
    public void assignDistributionSetToTargetFilterQueryThatExceedsQuota() {

        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        // creation is supposed to work as there is no distribution set
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("testfilter").query("name==target*"));

        // assigning a distribution set is supposed to fail as the query
        // addresses too many targets

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                        .updateAutoAssign(targetFilterQuery.getId()).ds(distributionSet.getId())));
    }

    @Test
    @Description("Updates an existing filter query with a query string that addresses too many targets.")
    public void updateTargetFilterQueryWithQueryThatExceedsQuota() {

        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target%s");
        final DistributionSet set = testdataFactory.createDistributionSet();

        // creation is supposed to work as the query does not exceed the quota
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(entityFactory.targetFilterQuery()
                .create().name("testfilter").autoAssignDistributionSet(set.getId()).query("name==foo"));

        // update with a query string that addresses too many targets
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> targetFilterQueryManagement
                .update(entityFactory.targetFilterQuery().update(targetFilterQuery.getId()).query("name==target*")));
    }

    @Test
    @Description("Test removing distribution set while it has a relation to a target filter query")
    public void removeAssignDistributionSet() {
        final String filterName = "target_filter_03";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                .updateAutoAssign(targetFilterQuery.getId()).ds(distributionSet.getId()));
        implicitLock(distributionSet);

        // Check if target filter query is there
        TargetFilterQuery tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertEquals(distributionSet, tfq.getAutoAssignDistributionSet(), "Returns correct distribution set");
        assertEquals(ActionType.FORCED, tfq.getAutoAssignActionType(), "Return correct action type");

        distributionSetManagement.delete(distributionSet.getId());

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertNotNull(tfq, "Returns target filter query");
        assertNull(tfq.getAutoAssignDistributionSet(), "Returns distribution set as null");
        assertNull(tfq.getAutoAssignActionType(), "Returns action type as null");
    }

    @Test
    @Description("Test to implicitly remove the auto assign distribution set when the ds is soft deleted")
    public void implicitlyRemoveAssignDistributionSet() {
        final String filterName = "target_filter_03";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dist_set");
        final Target target = testdataFactory.createTarget();

        // Assign the distribution set to an target, to force a soft delete in a
        // later step
        assignDistributionSet(distributionSet.getId(), target.getControllerId());

        final Long filterId = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"))
                .getId();
        targetFilterQueryManagement.updateAutoAssignDS(
                entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(distributionSet.getId()));
        implicitLock(distributionSet);

        // Check if target filter query is there with the distribution set
        TargetFilterQuery tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertEquals(distributionSet, tfq.getAutoAssignDistributionSet(), "Returns correct distribution set");
        assertEquals(ActionType.FORCED, tfq.getAutoAssignActionType(), "Return correct action type");

        distributionSetManagement.delete(distributionSet.getId());

        // Check if distribution set is still in the database with deleted flag
        assertTrue(distributionSetManagement.get(distributionSet.getId()).get().isDeleted(),
                "Distribution set should be deleted");

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertNotNull(tfq, "Returns target filter query");
        assertNull(tfq.getAutoAssignDistributionSet(), "Returns distribution set as null");
        assertNull(tfq.getAutoAssignActionType(), "Returns action type as null");
    }

    @Test
    @Description("Test finding and auto assign distribution set")
    public void findFiltersWithDistributionSet() {
        final String filterName = "d";
        assertEquals(0L, targetFilterQueryManagement.count());
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("a").query("name==*"));
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("b").query("name==*"));
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet("2");

        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("c").query("name==x")
                        .autoAssignDistributionSet(distributionSet).autoAssignActionType(ActionType.SOFT));
        final TargetFilterQuery tfq2 = targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create()
                .name(filterName).query("name==z*").autoAssignDistributionSet(distributionSet2));
        assertEquals(4L, targetFilterQueryManagement.count());

        // check if find works
        verifyFindByDistributionSetAndRsql(distributionSet, null, tfq);

        targetFilterQueryManagement.updateAutoAssignDS(
                entityFactory.targetFilterQuery().updateAutoAssign(tfq2.getId()).ds(distributionSet.getId()));

        // check if find works for two
        verifyFindByDistributionSetAndRsql(distributionSet, null, tfq, tfq2);

        // check if find works with name filter
        verifyFindByDistributionSetAndRsql(distributionSet, "name==" + filterName, tfq2);

        verifyFindForAllWithAutoAssignDs(tfq, tfq2);
    }

    @Test
    @Description("Creating or updating a target filter query with autoassignment and no-value weight when multi assignment in enabled.")
    public void weightNotRequiredInMultiAssignmentMode() {
        enableMultiAssignments();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("a").query("name==*")).getId();

        targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("b").query("name==*").autoAssignDistributionSet(ds));
        targetFilterQueryManagement
                .updateAutoAssignDS(entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(ds.getId()));
    }

    @Test
    @Description("Creating or updating a target filter query with autoassignment with a weight causes an error when multi assignment in disabled.")
    public void weightAllowedWhenMultiAssignmentModeNotEnabled() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("a").query("name==*")).getId();

        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create()
                .name("b").query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(342));
        targetFilterQueryManagement.updateAutoAssignDS(
                entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(ds.getId()).weight(343));
    }

    @Test
    @Description("Auto assignment can be removed from filter when multi assignment in enabled.")
    public void removeDsFromFilterWhenMultiAssignmentModeNotEnabled() {
        enableMultiAssignments();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("a")
                .query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(23)).getId();
        targetFilterQueryManagement
                .updateAutoAssignDS(entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(null).weight(null));
    }

    @Test
    @Description("Weight is validated and saved to the Filter.")
    public void weightValidatedAndSaved() {
        enableMultiAssignments();
        final DistributionSet ds = testdataFactory.createDistributionSet();

        Assertions.assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(
                () -> targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("a")
                        .query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(Action.WEIGHT_MAX + 1)));

        final Long filterId = targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("a")
                .query("name==*").autoAssignDistributionSet(ds).autoAssignWeight(Action.WEIGHT_MAX)).getId();
        assertThat(targetFilterQueryManagement.get(filterId).get().getAutoAssignWeight().get())
                .isEqualTo(Action.WEIGHT_MAX);

        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                        .updateAutoAssign(filterId).ds(ds.getId()).weight(Action.WEIGHT_MAX + 1)));
        Assertions.assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                        .updateAutoAssign(filterId).ds(ds.getId()).weight(Action.WEIGHT_MIN - 1)));
        targetFilterQueryManagement.updateAutoAssignDS(
                entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(ds.getId()).weight(Action.WEIGHT_MAX));
        targetFilterQueryManagement.updateAutoAssignDS(
                entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(ds.getId()).weight(Action.WEIGHT_MIN));
        assertThat(targetFilterQueryManagement.get(filterId).get().getAutoAssignWeight().get())
                .isEqualTo(Action.WEIGHT_MIN);
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to create a target filter with an invalidated distribution set.")
    public void createTargetFilterWithInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.create(
                        entityFactory.targetFilterQuery().create().name("createTargetFilterWithInvalidDistributionSet")
                                .query("name==*").autoAssignDistributionSet(distributionSet)));
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to create a target filter with an incomplete distribution set.")
    public void createTargetFilterWithIncompleteDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create()
                        .name("createTargetFilterWithIncompleteDistributionSet").query("name==*")
                        .autoAssignDistributionSet(distributionSet)));
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to update a target filter with an invalidated distribution set.")
    public void updateAutoAssignDsWithInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("updateAutoAssignDsWithInvalidDistributionSet")
                        .query("name==*").autoAssignDistributionSet(distributionSet));
        final DistributionSet invalidDistributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement
                        .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                                .ds(invalidDistributionSet.getId())));
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to update a target filter with an incomplete distribution set.")
    public void updateAutoAssignDsWithIncompleteDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("updateAutoAssignDsWithIncompleteDistributionSet")
                        .query("name==*").autoAssignDistributionSet(distributionSet));
        final DistributionSet incompleteDistributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> targetFilterQueryManagement
                        .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                                .ds(incompleteDistributionSet.getId())));
    }

    @Step
    private void verifyAutoAssignmentWithDefaultActionType(final String filterName,
            final TargetFilterQuery targetFilterQuery, final DistributionSet distributionSet) {
        targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                .updateAutoAssign(targetFilterQuery.getId()).ds(distributionSet.getId()));
        implicitLock(distributionSet);
        verifyAutoAssignDsAndActionType(filterName, distributionSet, ActionType.FORCED);
    }

    @Step
    private void verifyAutoAssignmentWithSoftActionType(final String filterName,
            final TargetFilterQuery targetFilterQuery, final DistributionSet distributionSet) {
        targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                .updateAutoAssign(targetFilterQuery.getId()).ds(distributionSet.getId()).actionType(ActionType.SOFT));
        verifyAutoAssignDsAndActionType(filterName, distributionSet, ActionType.SOFT);
    }

    @Step
    private void verifyAutoAssignmentWithDownloadOnlyActionType(final String filterName,
            final TargetFilterQuery targetFilterQuery, final DistributionSet distributionSet) {
        targetFilterQueryManagement
                .updateAutoAssignDS(entityFactory.targetFilterQuery().updateAutoAssign(targetFilterQuery.getId())
                        .ds(distributionSet.getId()).actionType(ActionType.DOWNLOAD_ONLY));
        verifyAutoAssignDsAndActionType(filterName, distributionSet, ActionType.DOWNLOAD_ONLY);
    }

    @Step
    private void verifyAutoAssignmentWithInvalidActionType(final TargetFilterQuery targetFilterQuery,
            final DistributionSet distributionSet) {
        // assigning a distribution set with TIMEFORCED action is supposed to
        // fail as only FORCED and SOFT action types are allowed
        assertThatExceptionOfType(InvalidAutoAssignActionTypeException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(
                        entityFactory.targetFilterQuery().updateAutoAssign(targetFilterQuery.getId())
                                .ds(distributionSet.getId()).actionType(ActionType.TIMEFORCED)));
    }

    @Step
    private void verifyAutoAssignmentWithIncompleteDs(final TargetFilterQuery targetFilterQuery) {
        final DistributionSet incompleteDistributionSet = distributionSetManagement
                .create(entityFactory.distributionSet().create().name("incomplete").version("1")
                        .type(testdataFactory.findOrCreateDefaultTestDsType()));

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                        .updateAutoAssign(targetFilterQuery.getId()).ds(incompleteDistributionSet.getId())));
    }

    @Step
    private void verifyAutoAssignmentWithSoftDeletedDs(final TargetFilterQuery targetFilterQuery) {
        final DistributionSet softDeletedDs = testdataFactory.createDistributionSet("softDeleted");
        assignDistributionSet(softDeletedDs, testdataFactory.createTarget("forSoftDeletedDs"));
        distributionSetManagement.delete(softDeletedDs.getId());

        assertThatExceptionOfType(DeletedException.class)
                .isThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                        .updateAutoAssign(targetFilterQuery.getId()).ds(softDeletedDs.getId())));
    }

    @Step
    private void verifyFindByDistributionSetAndRsql(final DistributionSet distributionSet, final String rsql,
            final TargetFilterQuery... expectedFilterQueries) {
        final Page<TargetFilterQuery> tfqList = targetFilterQueryManagement
                .findByAutoAssignDSAndRsql(PageRequest.of(0, 500), distributionSet.getId(), rsql);

        assertThat(tfqList.getTotalElements()).isEqualTo(expectedFilterQueries.length);
        verifyExpectedFilterQueriesInList(tfqList, expectedFilterQueries);
    }

    private void verifyExpectedFilterQueriesInList(final Slice<TargetFilterQuery> tfqList,
            final TargetFilterQuery... expectedFilterQueries) {
        assertThat(tfqList.map(TargetFilterQuery::getId)).containsExactly(
                Arrays.stream(expectedFilterQueries).map(TargetFilterQuery::getId).toArray(Long[]::new));
    }

    @Step
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

    private void verifyAutoAssignDsAndActionType(final String filterName, final DistributionSet distributionSet,
            final ActionType actionType) {
        final TargetFilterQuery tfq = targetFilterQueryManagement.getByName(filterName).get();

        assertEquals(distributionSet, tfq.getAutoAssignDistributionSet(), "Returns correct distribution set");
        assertEquals(actionType, tfq.getAutoAssignActionType(), "Return correct action type");
    }
}