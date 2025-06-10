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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.Predicate;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

        final Long permittedActionId = permitted.getId();

        // verify distributionSetManagement#findAll
        assertThat(distributionSetManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                .containsOnly(permittedActionId);

        // verify distributionSetManagement#findByRsql
        assertThat(distributionSetManagement.findByRsql("name==*", Pageable.unpaged()).get().map(Identifiable::getId)
                .toList()).containsOnly(permittedActionId);

        // verify distributionSetManagement#findByCompleted
        assertThat(distributionSetManagement.findByCompleted(true, Pageable.unpaged()).get().map(Identifiable::getId)
                .toList()).containsOnly(permittedActionId);

        // verify distributionSetManagement#findByDistributionSetFilter
        assertThat(distributionSetManagement
                .findByDistributionSetFilter(DistributionSetFilter.builder().isDeleted(false).build(), Pageable.unpaged())
                .get().map(Identifiable::getId).toList()).containsOnly(permittedActionId);

        // verify distributionSetManagement#get
        assertThat(distributionSetManagement.get(permittedActionId)).isPresent();
        final Long hiddenId = hidden.getId();
        assertThat(distributionSetManagement.get(hiddenId)).isEmpty();

        // verify distributionSetManagement#getWithDetails
        assertThat(distributionSetManagement.getWithDetails(permittedActionId)).isPresent();
        assertThat(distributionSetManagement.getWithDetails(hiddenId)).isEmpty();

        // verify distributionSetManagement#get
        assertThat(distributionSetManagement.getValid(permittedActionId).getId()).isEqualTo(permittedActionId);
        assertThatThrownBy(() -> distributionSetManagement.getValid(hiddenId))
                .as("Distribution set should not be found.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#get
        final List<Long> allActionIds = Arrays.asList(permittedActionId, hiddenId);
        assertThatThrownBy(() -> distributionSetManagement.get(allActionIds))
                .as("Fail if request hidden.").isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#getByNameAndVersion
        assertThat(distributionSetManagement.findByNameAndVersion(permitted.getName(), permitted.getVersion())).isPresent();
        assertThat(distributionSetManagement.findByNameAndVersion(hidden.getName(), hidden.getVersion())).isEmpty();

        // verify distributionSetManagement#getByAction
        assertThat(distributionSetManagement.findByAction(permittedAction.getId())).isPresent();
        final Long hiddenActionId = hiddenAction.getId();
        assertThatThrownBy(() -> distributionSetManagement.findByAction(hiddenActionId))
                .as("Action is hidden.").isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    @Description("Verifies read access rules for distribution sets")
    void verifyDistributionSetUpdates() {
        // permit all operations first to prepare test setup
        permitAllOperations(AccessController.Operation.READ);
        permitAllOperations(AccessController.Operation.CREATE);
        permitAllOperations(AccessController.Operation.UPDATE);

        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final String mdPresetKey = "metadata.preset";
        final String mdPresetValue = "presetValue";
        distributionSetManagement.createMetadata(permitted.getId(), Map.of(mdPresetKey, mdPresetValue));
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        distributionSetManagement.createMetadata(readOnly.getId(), Map.of(mdPresetKey, mdPresetValue));
        final DistributionSet hidden = testdataFactory.createDistributionSet();
        distributionSetManagement.createMetadata(hidden.getId(), Map.of(mdPresetKey, mdPresetValue));

        final SoftwareModule swModule = testdataFactory.createSoftwareModuleOs();

        // entities created - reset rules
        testAccessControlManger.deleteAllRules();
        // define access controlling rule
        defineAccess(AccessController.Operation.READ, permitted, readOnly);
        defineAccess(AccessController.Operation.UPDATE, permitted);

        // verify distributionSetManagement#assignSoftwareModules
        final List<Long> singleModuleIdList = Collections.singletonList(swModule.getId());
        assertThat(distributionSetManagement.assignSoftwareModules(permitted.getId(), singleModuleIdList))
                .satisfies(ds -> assertThat(ds.getModules().stream().map(Identifiable::getId).toList()).contains(swModule.getId()));
        final Long readOnlyId = readOnly.getId();
        assertThatThrownBy(() -> distributionSetManagement.assignSoftwareModules(readOnlyId, singleModuleIdList))
                .as("Distribution set not allowed to me modified.")
                .isInstanceOf(InsufficientPermissionException.class);
        final Long hiddenId = hidden.getId();
        assertThatThrownBy(() -> distributionSetManagement.assignSoftwareModules(hiddenId, singleModuleIdList))
                .as("Distribution set should not be visible.")
                .isInstanceOf(EntityNotFoundException.class);

        final Map<String, String> metadata = Map.of("test.create", mdPresetValue);

        // verify distributionSetManagement#createMetaData
        distributionSetManagement.createMetadata(permitted.getId(), metadata);
        assertThatThrownBy(() -> distributionSetManagement.createMetadata(readOnlyId, metadata))
                .as("Distribution set not allowed to be modified.")
                .isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> distributionSetManagement.createMetadata(hiddenId, metadata))
                .as("Distribution set should not be visible.")
                .isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#updateMetaData
        final String newValue = "newValue";
        distributionSetManagement.updateMetadata(permitted.getId(), mdPresetKey, newValue);
        assertThatThrownBy(() -> distributionSetManagement.updateMetadata(readOnlyId, mdPresetKey, newValue))
                .as("Distribution set not allowed to me modified.")
                .isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> distributionSetManagement.updateMetadata(hiddenId, mdPresetKey, newValue))
                .as("Distribution set should not be visible.")
                .isInstanceOf(EntityNotFoundException.class);

        // verify distributionSetManagement#deleteMetaData
        final String metadataKey = metadata.entrySet().stream().findAny().get().getKey();
        distributionSetManagement.deleteMetadata(permitted.getId(), metadataKey);
        assertThatThrownBy(() -> distributionSetManagement.deleteMetadata(readOnlyId, mdPresetKey))
                .as("Distribution set not allowed to me modified.")
                .isInstanceOf(InsufficientPermissionException.class);
        assertThatThrownBy(() -> distributionSetManagement.deleteMetadata(hiddenId, mdPresetKey))
                .as("Distribution set should not be visible.")
                .isInstanceOf(EntityNotFoundException.class);
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
        final Long dsTagId = distributionSetTagManagement.create(entityFactory.tag().create().name("dsTag")).getId();
        final Long dsTag2Id = distributionSetTagManagement.create(entityFactory.tag().create().name("dsTag2")).getId();

        // perform tag assignment before setting access rules
        distributionSetManagement.assignTag(Arrays.asList(permitted.getId(), readOnly.getId(), hidden.getId()),
                dsTagId);
        // entities created - reset rules
        testAccessControlManger.deleteAllRules();

        // define access controlling rule
        defineAccess(AccessController.Operation.READ, permitted, readOnly);

        // allow updating the permitted distributionSet
        defineAccess(AccessController.Operation.UPDATE, permitted);

        assertThat(distributionSetManagement.findByTag(dsTagId, Pageable.unpaged()).get().map(Identifiable::getId)
                .toList()).containsOnly(permitted.getId(), readOnly.getId());

        assertThat(distributionSetManagement.findByRsqlAndTag("name==*", dsTagId, Pageable.unpaged()).get()
                .map(Identifiable::getId).toList()).containsOnly(permitted.getId(), readOnly.getId());

        // verify distributionSetManagement#unassignTag on permitted target
        assertThat(distributionSetManagement
                .unassignTag(Collections.singletonList(permitted.getId()), dsTagId))
                .size()
                .isEqualTo(1);
        // verify distributionSetManagement#assignTag on permitted target
        assertThat(distributionSetManagement.assignTag(Collections.singletonList(permitted.getId()), dsTagId))
                .hasSize(1);
        // verify distributionSetManagement#unAssignTag on permitted target
        assertThat(distributionSetManagement.unassignTag(List.of(permitted.getId()), dsTagId)
                .get(0).getId())
                .isEqualTo(permitted.getId());

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        final List<Long> readOblyList = Collections.singletonList(readOnly.getId());
        assertThatThrownBy(() ->
                distributionSetManagement.unassignTag(readOblyList, dsTagId))
                .as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for readOnlyTarget (read, but no update permissions)
        // dsTag2- since - it is tagged with dsTag and won't do anything if assigning dsTag
        assertThatThrownBy(() -> {
            distributionSetManagement.assignTag(readOblyList, dsTag2Id);
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(InsufficientPermissionException.class);

        // assignment is denied for hiddenTarget since it's hidden
        final List<Long> hiddenList = Collections.singletonList(hidden.getId());
        assertThatThrownBy(() -> distributionSetManagement.unassignTag(hiddenList, dsTagId))
                .as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);

        // assignment is denied for hiddenTarget since it's hidden
        assertThatThrownBy(() -> {
            distributionSetManagement.assignTag(hiddenList, dsTagId);
        }).as("Missing update permissions for target to toggle tag assignment.")
                .isInstanceOf(EntityNotFoundException.class);

        // assignment is denied for hiddenTarget since it's hidden
        final List<Long> hiddenIdList = List.of(hidden.getId());
        assertThatThrownBy(() -> distributionSetManagement.unassignTag(hiddenIdList, dsTagId))
                .as("Missing update permissions for target to toggle tag assignment.")
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
        final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate = new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                .ds(hidden.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false);
        assertThatThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private void defineAccess(final AccessController.Operation operation, final DistributionSet... distributionSets) {
        defineAccess(operation, List.of(distributionSets));
    }

    private void defineAccess(final AccessController.Operation operation, final List<DistributionSet> targets) {
        final List<Long> ids = targets.stream().map(DistributionSet::getId).toList();
        testAccessControlManger.defineAccessRule(
                JpaDistributionSet.class, operation,
                dsByIds(ids),
                distributionSet -> ids.contains(distributionSet.getId()));
    }

    private static Specification<JpaDistributionSet> dsByIds(final Collection<Long> distids) {
        return (dsRoot, query, cb) -> {
            final Predicate predicate = dsRoot.get(JpaDistributionSet_.id).in(distids);
            query.distinct(true);
            return predicate;
        };
    }
}
