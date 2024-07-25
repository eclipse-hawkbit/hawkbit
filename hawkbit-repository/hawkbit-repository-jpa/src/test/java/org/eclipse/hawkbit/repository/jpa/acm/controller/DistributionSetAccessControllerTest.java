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
import java.util.List;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
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
        permitAllOperations(AccessController.Operation.READ);
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();

        final Action permittedAction = testdataFactory.performAssignment(permitted);
        final Action hiddenAction = testdataFactory.performAssignment(hidden);

        testAccessControlManger.deleteAllRules();

        // define access controlling rule
        defineAccess(AccessController.Operation.READ, permitted);
        testAccessControlManger.defineAccessRule(
                JpaTarget.class, AccessController.Operation.READ,
                TargetSpecifications.hasId(permittedAction.getTarget().getId()),
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
                        DistributionSetFilter.builder().isDeleted(false).build())
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
        }).as("Action is hidden.").isInstanceOf(InsufficientPermissionException.class);
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
        defineAccess(AccessController.Operation.READ, permitted, readOnly);

        // allow updating the permitted distributionSet
        defineAccess(AccessController.Operation.READ, permitted);
        defineAccess(AccessController.Operation.UPDATE, permitted);

        // verify distributionSetManagement#assignSoftwareModules
        assertThat(distributionSetManagement.assignSoftwareModules(permitted.getId(),
                Collections.singletonList(swModule.getId()))).satisfies(ds -> {
                    assertThat(ds.getModules().stream().map(Identifiable::getId).toList()).contains(swModule.getId());
                });
        assertThatThrownBy(() -> {
            distributionSetManagement.assignSoftwareModules(readOnly.getId(),
                    Collections.singletonList(swModule.getId()));
        }).as("Distribution set not allowed to me modified.").isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.assignSoftwareModules(hidden.getId(),
                    Collections.singletonList(swModule.getId()));
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);

        final JpaDistributionSetMetadata metadata = new JpaDistributionSetMetadata("test", "test");

        // verify distributionSetManagement#createMetaData
        distributionSetManagement.createMetaData(permitted.getId(), Collections.singletonList(metadata));
        assertThatThrownBy(() -> {
            distributionSetManagement.createMetaData(readOnly.getId(), Collections.singletonList(metadata));
        }).as("Distribution set not allowed to me modified.").isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.createMetaData(hidden.getId(), Collections.singletonList(metadata));
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#updateMetaData
        distributionSetManagement.updateMetaData(permitted.getId(), metadata);
        assertThatThrownBy(() -> {
            distributionSetManagement.updateMetaData(readOnly.getId(), metadata);
        }).as("Distribution set not allowed to me modified.").isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.updateMetaData(hidden.getId(), metadata);
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#deleteMetaData
        distributionSetManagement.deleteMetaData(permitted.getId(), metadata.getKey());
        assertThatThrownBy(() -> {
            distributionSetManagement.deleteMetaData(readOnly.getId(), metadata.getKey());
        }).as("Distribution set not allowed to me modified.").isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> {
            distributionSetManagement.deleteMetaData(hidden.getId(), metadata.getKey());
        }).as("Distribution set should not be visible.").isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void verifyTagFilteringAndManagement() {
        // permit all operations first to prepare test setup
        permitAllOperations(AccessController.Operation.READ);
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();
        final DistributionSetTag dsTag = distributionSetTagManagement.create(entityFactory.tag().create().name("dsTag"));

        // perform tag assignment before setting access rules
        distributionSetManagement.assignTag(Arrays.asList(permitted.getId(), readOnly.getId(), hidden.getId()),
                dsTag.getId());
        // entities created - reset rules
        testAccessControlManger.deleteAllRules();

        // define access controlling rule
        defineAccess(AccessController.Operation.READ, permitted, readOnly);

        // allow updating the permitted distributionSet
        defineAccess(AccessController.Operation.UPDATE, permitted);

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
        assertThat(distributionSetManagement.unassignTag(permitted.getId(), dsTag.getId()).getId())
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
            distributionSetManagement.unassignTag(readOnly.getId(), dsTag.getId());
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
            distributionSetManagement.unassignTag(hidden.getId(), dsTag.getId());
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void verifyAutoAssignmentUsage() {
        // permit all operations first to prepare test setup
        permitAllOperations(AccessController.Operation.READ);
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();
        // has to lock them, otherwise implicit lock shall be made which require DistributionSet update permissions
        distributionSetManagement.lock(permitted.getId());
        distributionSetManagement.lock(readOnly.getId());
        distributionSetManagement.lock(hidden.getId());

        // entities created - reset rules
        testAccessControlManger.deleteAllRules();
        // define read access
        defineAccess(AccessController.Operation.READ, permitted, readOnly);
        // permit update operation
        defineAccess(AccessController.Operation.UPDATE, permitted);

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name("test").query("id==*"));

        assertThat(targetFilterQueryManagement
                .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(permitted.getId())
                        .actionType(Action.ActionType.FORCED).confirmationRequired(false))
                .getAutoAssignDistributionSet().getId()).isEqualTo(permitted.getId());
        targetFilterQueryManagement
                .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                        .ds(readOnly.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false))
                .getAutoAssignDistributionSet().getId();
        assertThatThrownBy(() -> {
            targetFilterQueryManagement
                    .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                            .ds(hidden.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false))
                    .getAutoAssignDistributionSet().getId();
        }).isInstanceOf(EntityNotFoundException.class);
    }


    private void defineAccess(final AccessController.Operation operation, final DistributionSet... distributionSets) {
        defineAccess(operation, List.of(distributionSets));
    }

    private void defineAccess(final AccessController.Operation operation, final List<DistributionSet> targets) {
        final List<Long> ids = targets.stream().map(DistributionSet::getId).toList();
        testAccessControlManger.defineAccessRule(
                JpaDistributionSet.class, operation,
                DistributionSetSpecification.byIds(ids),
                distributionSet -> ids.contains(distributionSet.getId()));
    }
}
