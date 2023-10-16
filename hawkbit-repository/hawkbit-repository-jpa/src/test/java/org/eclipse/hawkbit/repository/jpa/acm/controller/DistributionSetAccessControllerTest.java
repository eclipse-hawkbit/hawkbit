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
import java.util.Collections;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Access Control")
@Story("Test Distribution Set Access Controller")
class DistributionSetAccessControllerTest extends AbstractAccessControllerTest {

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
        testAccessControlManger.permitOperation(JpaTarget.class, AccessController.Operation.READ,
                target -> target.getId().equals(permittedAction.getTarget().getId()));

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
        assertThatThrownBy(() -> {
            distributionSetManagement.get(Arrays.asList(permitted.getId(), hidden.getId()));
        }).as("Fail if request hidden.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#getByNameAndVersion
        assertThat(distributionSetManagement.getByNameAndVersion(permitted.getName(), permitted.getVersion()))
                .isPresent();
        assertThat(distributionSetManagement.getByNameAndVersion(hidden.getName(), hidden.getVersion())).isEmpty();

        // verify distributionSetManagement#getByAction
        assertThat(distributionSetManagement.getByAction(permittedAction.getId())).isPresent();
        assertThatThrownBy(() -> {
            distributionSetManagement.getByAction(hiddenAction.getId());
        }).as("Action is hidden.").isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Description("Verifies read access rules for distribution sets")
    void verifyDistributionSetUpdates() {
        // permit all operations first to prepare test setup
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();

        final SoftwareModule swModule = testdataFactory.createSoftwareModuleOs();

        // entities created - reset rules
        testAccessControlManger.deleteAllRules();
        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaDistributionSet.class, AccessController.Operation.READ,
                DistributionSetSpecification.byIds(Arrays.asList(permitted.getId(), readOnly.getId())));

        // allow updating the permitted distributionSet
        testAccessControlManger.permitOperation(JpaDistributionSet.class, AccessController.Operation.READ,
                ds -> ds.getId().equals(permitted.getId()));
        testAccessControlManger.permitOperation(JpaDistributionSet.class, AccessController.Operation.UPDATE,
                ds -> ds.getId().equals(permitted.getId()));

        // verify distributionSetManagement#assignSoftwareModules
        assertThat(distributionSetManagement.assignSoftwareModules(permitted.getId(),
                Collections.singletonList(swModule.getId()))).satisfies(ds -> {
                    assertThat(ds.getModules().stream().map(Identifiable::getId).toList()).contains(swModule.getId());
                });
        assertThatThrownBy(() -> {
            distributionSetManagement.assignSoftwareModules(readOnly.getId(),
                    Collections.singletonList(swModule.getId()));
        }).as("Distribution set not allowed to me modified.").isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.assignSoftwareModules(hidden.getId(),
                    Collections.singletonList(swModule.getId()));
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);

        final JpaDistributionSetMetadata metadata = new JpaDistributionSetMetadata("test", "test");

        // verify distributionSetManagement#createMetaData
        distributionSetManagement.createMetaData(permitted.getId(), Collections.singletonList(metadata));
        assertThatThrownBy(() -> {
            distributionSetManagement.createMetaData(readOnly.getId(), Collections.singletonList(metadata));
        }).as("Distribution set not allowed to me modified.").isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.createMetaData(hidden.getId(), Collections.singletonList(metadata));
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#updateMetaData
        distributionSetManagement.updateMetaData(permitted.getId(), metadata);
        assertThatThrownBy(() -> {
            distributionSetManagement.updateMetaData(readOnly.getId(), metadata);
        }).as("Distribution set not allowed to me modified.").isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.updateMetaData(hidden.getId(), metadata);
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#deleteMetaData
        distributionSetManagement.deleteMetaData(permitted.getId(), metadata.getKey());
        assertThatThrownBy(() -> {
            distributionSetManagement.deleteMetaData(readOnly.getId(), metadata.getKey());
        }).as("Distribution set not allowed to me modified.").isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.deleteMetaData(hidden.getId(), metadata.getKey());
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void verifyTagFilteringAndManagement() {
        // permit all operations first to prepare test setup
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();

        final DistributionSetTag dsTag = distributionSetTagManagement
                .create(entityFactory.tag().create().name("dsTag"));

        // perform tag assignment before setting access rules
        distributionSetManagement.assignTag(Arrays.asList(permitted.getId(), readOnly.getId(), hidden.getId()),
                dsTag.getId());

        // entities created - reset rules
        testAccessControlManger.deleteAllRules();
        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaDistributionSet.class, AccessController.Operation.READ,
                DistributionSetSpecification.byIds(Arrays.asList(permitted.getId(), readOnly.getId())));

        // allow updating the permitted distributionSet
        testAccessControlManger.permitOperation(JpaDistributionSet.class, AccessController.Operation.UPDATE,
                ds -> ds.getId().equals(permitted.getId()));

        assertThat(distributionSetManagement.findByTag(Pageable.unpaged(), dsTag.getId()).get().map(Identifiable::getId)
                .toList()).containsOnly(permitted.getId(), readOnly.getId());

        assertThat(distributionSetManagement.findByRsqlAndTag(Pageable.unpaged(), "id==*", dsTag.getId()).get()
                .map(Identifiable::getId).toList()).containsOnly(permitted.getId(), readOnly.getId());

        // verify distributionSetManagement#toggleTagAssignment on permitted target
        assertThat(distributionSetManagement
                .toggleTagAssignment(Collections.singletonList(permitted.getId()), dsTag.getName()).getUnassigned())
                .isEqualTo(1);
        // verify distributionSetManagement#assignTag on permitted target
        assertThat(distributionSetManagement.assignTag(Collections.singletonList(permitted.getId()), dsTag.getId()))
                .hasSize(1);
        // verify distributionSetManagement#unAssignTag on permitted target
        assertThat(distributionSetManagement.unAssignTag(permitted.getId(), dsTag.getId()).getId())
                .isEqualTo(permitted.getId());

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        assertThatThrownBy(() -> {
            distributionSetManagement.toggleTagAssignment(Collections.singletonList(readOnly.getId()), dsTag.getName())
                    .getUnassigned();
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        assertThatThrownBy(() -> {
            distributionSetManagement.assignTag(Collections.singletonList(readOnly.getId()), dsTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        assertThatThrownBy(() -> {
            distributionSetManagement.unAssignTag(readOnly.getId(), dsTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for hiddenTarget since it's hidden
        assertThatThrownBy(() -> {
            distributionSetManagement.toggleTagAssignment(Collections.singletonList(hidden.getId()), dsTag.getName())
                    .getUnassigned();
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);

        // assignment is denied for hiddenTarget since it's hidden
        assertThatThrownBy(() -> {
            distributionSetManagement.assignTag(Collections.singletonList(hidden.getId()), dsTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);

        // assignment is denied for hiddenTarget since it's hidden
        assertThatThrownBy(() -> {
            distributionSetManagement.unAssignTag(hidden.getId(), dsTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void verifyAutoAssignmentUsage() {
        // permit all operations first to prepare test setup
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();

        // entities created - reset rules
        testAccessControlManger.deleteAllRules();
        // define read access
        testAccessControlManger.defineAccessRule(JpaDistributionSet.class, AccessController.Operation.READ,
                DistributionSetSpecification.byIds(Arrays.asList(permitted.getId(), readOnly.getId())));
        // permit update operation
        testAccessControlManger.permitOperation(JpaDistributionSet.class, AccessController.Operation.UPDATE,
                ds -> ds.getId().equals(permitted.getId()));

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("test").query("id==*"));

        assertThat(targetFilterQueryManagement
                .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(permitted.getId())
                        .actionType(Action.ActionType.FORCED).confirmationRequired(false))
                .getAutoAssignDistributionSet().getId()).isEqualTo(permitted.getId());
        assertThatThrownBy(() -> {
            targetFilterQueryManagement
                    .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                            .ds(readOnly.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false))
                    .getAutoAssignDistributionSet().getId();
        }).isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> {
            targetFilterQueryManagement
                    .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                            .ds(hidden.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false))
                    .getAutoAssignDistributionSet().getId();
        }).isInstanceOf(EntityNotFoundException.class);
    }

}
