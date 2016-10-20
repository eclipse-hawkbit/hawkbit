/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
    @Description("Test creation of target filter query.")
    public void createTargetFilterQuery() {
        final String filterName = "new target filter";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));
        assertEquals("Retrieved newly created custom target filter", targetFilterQuery,
                targetFilterQueryManagement.findTargetFilterQueryByName(filterName));
    }

    @Test
    @Description("Test searching a target filter query.")
    public void searchTargetFilterQuery() {
        final String filterName = "targetFilterQueryName";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));

        targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery("someOtherFilter", "name==PendingTargets002"));

        List<TargetFilterQuery> results = targetFilterQueryManagement
                .findTargetFilterQueryByFilter(new PageRequest(0, 10), "name==" + filterName).getContent();
        assertEquals("Search result should have 1 result", 1, results.size());
        assertEquals("Retrieved newly created custom target filter", targetFilterQuery, results.get(0));
    }

    @Test(expected = RSQLParameterUnsupportedFieldException.class)
    @Description("Test searching a target filter query with an invalid filter.")
    public void searchTargetFilterQueryInvalidField() {
        // Should throw an exception
        targetFilterQueryManagement.findTargetFilterQueryByFilter(new PageRequest(0, 10), "unknownField==testValue")
                .getContent();

    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if a targetfilterquery with the same name are created more than once.")
    public void createDuplicateTargetFilterQuery() {
        final String filterName = "new target filter duplicate";
        targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));

        try {
            targetFilterQueryManagement
                    .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));
            fail("should not have worked as query already exists");
        } catch (final EntityAlreadyExistsException e) {

        }
    }

    @Test
    @Description("Test deletion of target filter query.")
    public void deleteTargetFilterQuery() {
        final String filterName = "delete_target_filter_query";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));
        targetFilterQueryManagement.deleteTargetFilterQuery(targetFilterQuery.getId());
        assertEquals("Returns null as the target filter is deleted", null,
                targetFilterQueryManagement.findTargetFilterQueryById(targetFilterQuery.getId()));

    }

    @Test
    @Description("Test updation of target filter query.")
    public void updateTargetFilterQuery() {
        final String filterName = "target_filter_01";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));

        final String newQuery = "status==UNKNOWN";
        targetFilterQuery.setQuery(newQuery);
        targetFilterQueryManagement.updateTargetFilterQuery(targetFilterQuery);
        assertEquals("Returns updated target filter query", newQuery,
                targetFilterQueryManagement.findTargetFilterQueryByName(filterName).getQuery());

    }

    @Test
    @Description("Test assigning a distribution set")
    public void assignDistributionSet() {
        final String filterName = "target_filter_02";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));

        final DistributionSet distributionSet = distributionSetManagement.createDistributionSet(new JpaDistributionSet(
                "dist_Set_01", "0.1", "", null, null
        ));

        targetFilterQuery.setAutoAssignDistributionSet(distributionSet);
        targetFilterQueryManagement.updateTargetFilterQuery(targetFilterQuery);

        TargetFilterQuery tfq = targetFilterQueryManagement.findTargetFilterQueryByName(filterName);

        assertEquals("Returns correct distribution set", distributionSet,
                tfq.getAutoAssignDistributionSet());

    }

    @Test
    @Description("Test removing distribution set while it has a relation to a target filter query")
    public void removeAssignDistributionSet() {
        final String filterName = "target_filter_03";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .createTargetFilterQuery(new JpaTargetFilterQuery(filterName, "name==PendingTargets001"));

        final DistributionSet distributionSet = distributionSetManagement.createDistributionSet(new JpaDistributionSet(
                "dist_Set_02", "0.1", "", null, null
        ));

        targetFilterQuery.setAutoAssignDistributionSet(distributionSet);
        targetFilterQueryManagement.updateTargetFilterQuery(targetFilterQuery);

        // Check if target filter query is there
        TargetFilterQuery tfq = targetFilterQueryManagement.findTargetFilterQueryByName(filterName);
        assertEquals("Returns correct distribution set", distributionSet,
                tfq.getAutoAssignDistributionSet());

        distributionSetManagement.deleteDistributionSet(distributionSet);

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.findTargetFilterQueryByName(filterName);
        assertNotNull("Returns target filter query", tfq);
        assertNull("Returns distribution set as null", tfq.getAutoAssignDistributionSet());

    }

    @Test
    @Description("Test to implicitly remove the auto assign distribution set when the ds is soft deleted")
    public void implicitlyRemoveAssignDistributionSet() {
        final String filterName = "target_filter_03";
        DistributionSet distributionSet = testdataFactory.createDistributionSet("dist_set");
        Target target = testdataFactory.createTarget();

        // Assign the distribution set to an target, to force a soft delete in a
        // later step
        deploymentManagement.assignDistributionSet(distributionSet.getId(), target.getControllerId());

        targetFilterQueryManagement.createTargetFilterQuery(
                new JpaTargetFilterQuery(filterName, "name==PendingTargets001", (JpaDistributionSet) distributionSet));

        // Check if target filter query is there with the distribution set
        TargetFilterQuery tfq = targetFilterQueryManagement.findTargetFilterQueryByName(filterName);
        assertEquals("Returns correct distribution set", distributionSet, tfq.getAutoAssignDistributionSet());

        distributionSetManagement.deleteDistributionSet(distributionSet);

        // Check if distribution set is still in the database with deleted flag
        assertTrue("Distribution set should be deleted",
                distributionSetManagement.findDistributionSetById(distributionSet.getId()).isDeleted());

        // Check if auto assign distribution set is null
        tfq = targetFilterQueryManagement.findTargetFilterQueryByName(filterName);
        assertNotNull("Returns target filter query", tfq);
        assertNull("Returns distribution set as null", tfq.getAutoAssignDistributionSet());

    }

    @Test
    @Description("Test finding and auto assign distribution set")
    public void findFiltersWithDistributionSet() {

        final String filterName = "d";

        assertEquals(0L, targetFilterQueryManagement.countAllTargetFilterQuery().longValue());

        targetFilterQueryManagement.createTargetFilterQuery(new JpaTargetFilterQuery("a", "name==*"));
        targetFilterQueryManagement.createTargetFilterQuery(new JpaTargetFilterQuery("b", "name==*"));

        final DistributionSet distributionSet = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("dist_Set_01", "0.1", "", null, null));
        final DistributionSet distributionSet2 = distributionSetManagement
                .createDistributionSet(new JpaDistributionSet("dist_Set_02", "0.1", "", null, null));

        final TargetFilterQuery tfq = targetFilterQueryManagement.createTargetFilterQuery(
                new JpaTargetFilterQuery("c", "name==x", (JpaDistributionSet) distributionSet));

        final TargetFilterQuery tfq2 = targetFilterQueryManagement.createTargetFilterQuery(
                new JpaTargetFilterQuery(filterName, "name==z*", (JpaDistributionSet) distributionSet2));

        assertEquals(4L, targetFilterQueryManagement.countAllTargetFilterQuery().longValue());

        // check if find works
        Page<TargetFilterQuery> tfqList = targetFilterQueryManagement
                .findTargetFilterQueryByAutoAssignDS(new PageRequest(0, 500), distributionSet);
        assertThat(1L).as("Target filter query").isEqualTo(tfqList.getTotalElements());

        assertEquals("Returns correct target filter query", tfq.getId(), tfqList.iterator().next().getId());

        tfq2.setAutoAssignDistributionSet(distributionSet);
        targetFilterQueryManagement.updateTargetFilterQuery(tfq2);

        // check if find works for two
        tfqList = targetFilterQueryManagement.findTargetFilterQueryByAutoAssignDS(new PageRequest(0, 500),
                distributionSet);
        assertThat(2L).as("Target filter query count").isEqualTo(tfqList.getTotalElements());
        Iterator<TargetFilterQuery> iterator = tfqList.iterator();
        assertEquals("Returns correct target filter query 1", tfq.getId(), iterator.next().getId());
        assertEquals("Returns correct target filter query 2", tfq2.getId(), iterator.next().getId());

        // check if find works with name filter
        tfqList = targetFilterQueryManagement.findTargetFilterQueryByAutoAssignDS(new PageRequest(0, 500),
                distributionSet, "name==" + filterName);
        assertThat(1L).as("Target filter query count").isEqualTo(tfqList.getTotalElements());

        assertEquals("Returns correct target filter query", tfq2.getId(), tfqList.iterator().next().getId());

        // check if find works for all with auto assign DS
        tfqList = targetFilterQueryManagement.findTargetFilterQueryWithAutoAssignDS(new PageRequest(0, 500));
        assertThat(2L).as("Target filter query count").isEqualTo(tfqList.getTotalElements());
        iterator = tfqList.iterator();
        assertEquals("Returns correct target filter query 1", tfq.getId(), iterator.next().getId());
        assertEquals("Returns correct target filter query 2", tfq2.getId(), iterator.next().getId());

    }

}
