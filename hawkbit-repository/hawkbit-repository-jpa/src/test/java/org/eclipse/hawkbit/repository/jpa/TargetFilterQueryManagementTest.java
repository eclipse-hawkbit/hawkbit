/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test class for {@link TargetFilterQueryManagement}.
 * 
 */
@Features("Component Tests - Repository")
@Stories("Target Filter Query Management")
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
        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(targetFilterQuery.getId(), NOT_EXIST_IDL),
                "DistributionSet");
        verifyThrownExceptionBy(() -> targetFilterQueryManagement.updateAutoAssignDS(1234L, set.getId()),
                "TargetFilterQuery");
        verifyThrownExceptionBy(
                () -> targetFilterQueryManagement.updateAutoAssignDS(targetFilterQuery.getId(), NOT_EXIST_IDL),
                "DistributionSet");
    }

    @Test
    @Description("Test creation of target filter query.")
    public void createTargetFilterQuery() {
        final String filterName = "new target filter";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));
        assertEquals("Retrieved newly created custom target filter", targetFilterQuery,
                targetFilterQueryManagement.getByName(filterName).get());
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
                .findByRsql(new PageRequest(0, 10), "name==" + filterName).getContent();
        assertEquals("Search result should have 1 result", 1, results.size());
        assertEquals("Retrieved newly created custom target filter", targetFilterQuery, results.get(0));
    }

    @Test(expected = RSQLParameterUnsupportedFieldException.class)
    @Description("Test searching a target filter query with an invalid filter.")
    public void searchTargetFilterQueryInvalidField() {
        // Should throw an exception
        targetFilterQueryManagement.findByRsql(new PageRequest(0, 10), "unknownField==testValue").getContent();

    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if a targetfilterquery with the same name are created more than once.")
    public void createDuplicateTargetFilterQuery() {
        final String filterName = "new target filter duplicate";
        targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));

        try {
            targetFilterQueryManagement.create(
                    entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));
            fail("should not have worked as query already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Test deletion of target filter query.")
    public void deleteTargetFilterQuery() {
        final String filterName = "delete_target_filter_query";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));
        targetFilterQueryManagement.delete(targetFilterQuery.getId());
        assertFalse("Returns null as the target filter is deleted",
                targetFilterQueryManagement.get(targetFilterQuery.getId()).isPresent());

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
        assertEquals("Returns updated target filter query", newQuery,
                targetFilterQueryManagement.getByName(filterName).get().getQuery());

    }

    @Test
    @Description("Test assigning a distribution set")
    public void assignDistributionSet() {
        final String filterName = "target_filter_02";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.updateAutoAssignDS(targetFilterQuery.getId(), distributionSet.getId());

        final TargetFilterQuery tfq = targetFilterQueryManagement.getByName(filterName).get();

        assertEquals("Returns correct distribution set", distributionSet, tfq.getAutoAssignDistributionSet());

    }

    @Test
    @Description("Test removing distribution set while it has a relation to a target filter query")
    public void removeAssignDistributionSet() {
        final String filterName = "target_filter_03";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"));

        final DistributionSet distributionSet = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.updateAutoAssignDS(targetFilterQuery.getId(), distributionSet.getId());

        // Check if target filter query is there
        TargetFilterQuery tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertEquals("Returns correct distribution set", distributionSet, tfq.getAutoAssignDistributionSet());

        distributionSetManagement.delete(distributionSet.getId());

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertNotNull("Returns target filter query", tfq);
        assertNull("Returns distribution set as null", tfq.getAutoAssignDistributionSet());

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

        targetFilterQueryManagement.updateAutoAssignDS(targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==PendingTargets001"))
                .getId(), distributionSet.getId());

        // Check if target filter query is there with the distribution set
        TargetFilterQuery tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertEquals("Returns correct distribution set", distributionSet, tfq.getAutoAssignDistributionSet());

        distributionSetManagement.delete(distributionSet.getId());

        // Check if distribution set is still in the database with deleted flag
        assertTrue("Distribution set should be deleted",
                distributionSetManagement.get(distributionSet.getId()).get().isDeleted());

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.getByName(filterName).get();
        assertNotNull("Returns target filter query", tfq);
        assertNull("Returns distribution set as null", tfq.getAutoAssignDistributionSet());

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

        final TargetFilterQuery tfq = targetFilterQueryManagement.updateAutoAssignDS(
                targetFilterQueryManagement
                        .create(entityFactory.targetFilterQuery().create().name("c").query("name==x")).getId(),
                distributionSet.getId());

        final TargetFilterQuery tfq2 = targetFilterQueryManagement.updateAutoAssignDS(
                targetFilterQueryManagement
                        .create(entityFactory.targetFilterQuery().create().name(filterName).query("name==z*")).getId(),
                distributionSet2.getId());

        assertEquals(4L, targetFilterQueryManagement.count());

        // check if find works
        Page<TargetFilterQuery> tfqList = targetFilterQueryManagement.findByAutoAssignDSAndRsql(new PageRequest(0, 500),
                distributionSet.getId(), null);
        assertThat(1L).as("Target filter query").isEqualTo(tfqList.getTotalElements());

        assertEquals("Returns correct target filter query", tfq.getId(), tfqList.iterator().next().getId());

        targetFilterQueryManagement.updateAutoAssignDS(tfq2.getId(), distributionSet.getId());

        // check if find works for two
        tfqList = targetFilterQueryManagement.findByAutoAssignDSAndRsql(new PageRequest(0, 500),
                distributionSet.getId(), null);
        assertThat(2L).as("Target filter query count").isEqualTo(tfqList.getTotalElements());
        Iterator<TargetFilterQuery> iterator = tfqList.iterator();
        assertEquals("Returns correct target filter query 1", tfq.getId(), iterator.next().getId());
        assertEquals("Returns correct target filter query 2", tfq2.getId(), iterator.next().getId());

        // check if find works with name filter
        tfqList = targetFilterQueryManagement.findByAutoAssignDSAndRsql(new PageRequest(0, 500),
                distributionSet.getId(), "name==" + filterName);
        assertThat(1L).as("Target filter query count").isEqualTo(tfqList.getTotalElements());

        assertEquals("Returns correct target filter query", tfq2.getId(), tfqList.iterator().next().getId());

        // check if find works for all with auto assign DS
        tfqList = targetFilterQueryManagement.findWithAutoAssignDS(new PageRequest(0, 500));
        assertThat(2L).as("Target filter query count").isEqualTo(tfqList.getTotalElements());
        iterator = tfqList.iterator();
        assertEquals("Returns correct target filter query 1", tfq.getId(), iterator.next().getId());
        assertEquals("Returns correct target filter query 2", tfq2.getId(), iterator.next().getId());

    }

}
