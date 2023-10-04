/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Access Control")
@Story("Test Distribution Set Access Controlling")
class DistributionSetAccessControllingTest extends AbstractAccessControllingTest {

    @Test
    @Description("Verifies read access rules for distribution sets")
    void verifyDistributionSetReadOperations() {
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();

        final Action permittedAction = testdataFactory.performAssignment(permitted);
        final Action hiddenAction = testdataFactory.performAssignment(hidden);

        testAccessControlManger.deleteAllRules();
        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaDistributionSet.class, AccessController.Operation.READ,
                DistributionSetSpecification.byId(permitted.getId()));

        // verify distributionSetManagement#findAll
        assertThat(distributionSetManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                .containsOnly(permitted.getId());

        // verify distributionSetManagement#findByRsql
        assertThat(distributionSetManagement.findByRsql(Pageable.unpaged(), "name==*").get().map(Identifiable::getId)
                .toList()).containsOnly(permitted.getId());

        // verify distributionSetManagement#findByCompleted
        assertThat(distributionSetManagement.findByCompleted(Pageable.unpaged(), true).get().map(Identifiable::getId)
                .toList()).containsOnly(permitted.getId());

        // verify distributionSetManagement#findByDistributionSetFilter
        assertThat(distributionSetManagement
                .findByDistributionSetFilter(Pageable.unpaged(),
                        new DistributionSetFilter.DistributionSetFilterBuilder().setIsDeleted(false).build())
                .get().map(Identifiable::getId).toList()).containsOnly(permitted.getId());

        // verify distributionSetManagement#get
        assertThat(distributionSetManagement.get(permitted.getId())).isPresent();
        assertThat(distributionSetManagement.get(hidden.getId())).isEmpty();

        // verify distributionSetManagement#getWithDetails
        assertThat(distributionSetManagement.getWithDetails(permitted.getId())).isPresent();
        assertThat(distributionSetManagement.getWithDetails(hidden.getId())).isEmpty();

        // verify distributionSetManagement#get
        assertThat(distributionSetManagement.getValid(permitted.getId()).getId()).isEqualTo(permitted.getId());
        assertThatThrownBy(() -> {
            assertThat(distributionSetManagement.getValid(hidden.getId()));
        }).as("Distribution set should not be found.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#get
        assertThat(distributionSetManagement.get(Arrays.asList(permitted.getId(), hidden.getId())).stream()
                .map(Identifiable::getId).toList()).containsOnly(permitted.getId());

        // verify distributionSetManagement#getByNameAndVersion
        assertThat(distributionSetManagement.getByNameAndVersion(permitted.getName(), permitted.getVersion()))
                .isPresent();
        assertThat(distributionSetManagement.getByNameAndVersion(hidden.getName(), hidden.getVersion())).isEmpty();

        // verify distributionSetManagement#getByAction
        assertThat(distributionSetManagement.getByAction(permittedAction.getId())).isPresent();
        assertThat(distributionSetManagement.getByAction(hiddenAction.getId())).isEmpty();
    }

}
