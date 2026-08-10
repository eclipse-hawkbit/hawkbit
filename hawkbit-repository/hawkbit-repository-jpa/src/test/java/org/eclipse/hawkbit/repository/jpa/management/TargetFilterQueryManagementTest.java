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
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Update;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.UpdateCreate;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Test class for {@link TargetFilterQueryManagement}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Target Filter Query Management
 */
class TargetFilterQueryManagementTest extends AbstractRepositoryManagementTest<TargetFilterQuery, UpdateCreate, Update> {

    @Override
    protected Object builderParameterValue(final Method builderSetter) {
        // encrypted true is not supported
        if (builderSetter.getDeclaringClass() == UpdateCreate.UpdateCreateBuilder.class && "query".equals(builderSetter.getName())) {
            return "controllerId==PendingTargets001";
        }

        return super.builderParameterValue(builderSetter);
    }

    @Test
    void findByRsqlTargetFilterQuery() {
        final String filterName = "targetFilterQueryName";
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(UpdateCreate.builder().name(filterName).query("name==PendingTargets001").build());

        targetFilterQueryManagement.create(UpdateCreate.builder().name("someOtherFilter").query("name==PendingTargets002").build());

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
                UpdateCreate.builder().name(filterName).query("name==PendingTargets001").build());
        targetFilterQueryManagement.delete(targetFilterQuery.getId());
        assertFalse(
                targetFilterQueryManagement.find(targetFilterQuery.getId()).isPresent(),
                "Returns null as the target filter is deleted");
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

    /**
     * Renaming a target filter query must keep the linked auto assignment (and its status/approval) intact - only the
     * name is propagated so the name-based link stays valid. The query (scope) is unchanged even though it is resent.
     */
    @Test
    void renameFilterKeepsRunningAutoAssignmentStatus() {
        final String query = "name==reset_target_001";
        final TargetFilterQuery filter = targetFilterQueryManagement.create(
                UpdateCreate.builder().name("reset_filter").query(query).build());
        final AutoAssignment autoAssignment = createRunningAutoAssignment("reset_filter", query);

        // rename only; the (unchanged) query is resent, as REST clients echo the whole resource back
        targetFilterQueryManagement.update(
                Update.builder().id(filter.getId()).name("reset_filter_renamed").query(query).build());

        final AutoAssignment renamed = autoAssignmentManagement.findByName("reset_filter_renamed").orElseThrow();
        assertThat(renamed.getId()).isEqualTo(autoAssignment.getId());
        assertThat(renamed.getStatus()).isEqualTo(RUNNING);
        assertThat(autoAssignmentManagement.findByName("reset_filter")).isNotPresent();
    }

    /**
     * Changing the query (target scope) of a target filter query must re-create the linked auto assignment so that a
     * (possibly approved) one is reset and goes through the approval workflow again for the new scope.
     */
    @Test
    void changingFilterQueryResetsAutoAssignmentStatus() {
        final String query = "name==reset_target_001";
        final TargetFilterQuery filter = targetFilterQueryManagement.create(
                UpdateCreate.builder().name("scope_filter").query(query).build());
        final AutoAssignment autoAssignment = createRunningAutoAssignment("scope_filter", query);

        targetFilterQueryManagement.update(
                Update.builder().id(filter.getId()).name("scope_filter").query("name==reset_target_002").build());

        final AutoAssignment recreated = autoAssignmentManagement.findByName("scope_filter").orElseThrow();
        assertThat(recreated.getId()).isNotEqualTo(autoAssignment.getId());
        assertThat(recreated.getStatus()).isEqualTo(READY);
        assertThat(recreated.getTargetFilterQuery()).isEqualTo("name==reset_target_002");
    }

    /**
     * A no-op update (same name and same query) must not touch the linked auto assignment at all.
     */
    @Test
    void noOpFilterUpdateKeepsRunningAutoAssignmentStatus() {
        final String query = "name==reset_target_001";
        final TargetFilterQuery filter = targetFilterQueryManagement.create(
                UpdateCreate.builder().name("noop_filter").query(query).build());
        final AutoAssignment autoAssignment = createRunningAutoAssignment("noop_filter", query);

        targetFilterQueryManagement.update(
                Update.builder().id(filter.getId()).name("noop_filter").query(query).build());

        final AutoAssignment unchanged = autoAssignmentManagement.get(autoAssignment.getId());
        assertThat(unchanged.getStatus()).isEqualTo(RUNNING);
    }

    private AutoAssignment createRunningAutoAssignment(final String name, final String query) {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final AutoAssignment autoAssignment = autoAssignmentManagement.create(AutoAssignmentManagement.Create.builder()
                .name(name).targetFilterQuery(query).distributionSet(distributionSet).build());
        autoAssignmentManagement.start(autoAssignment.getId());
        assertThat(autoAssignmentManagement.get(autoAssignment.getId()).getStatus()).isEqualTo(RUNNING);
        return autoAssignment;
    }
}