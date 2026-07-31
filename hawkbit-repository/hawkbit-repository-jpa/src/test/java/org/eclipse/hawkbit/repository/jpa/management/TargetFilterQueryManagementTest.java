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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Update;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.UpdateCreate;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
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
}