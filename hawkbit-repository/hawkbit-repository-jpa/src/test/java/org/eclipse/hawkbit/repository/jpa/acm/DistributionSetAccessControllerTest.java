/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.im.authentication.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Note: Still all test gets READ_REPOSITORY since find methods are inherited with request for READ_REPOSITORY. However,
 * using READ_DISTRIBUTION_SET scoping - the scopes still work.
 * <p/>
 * Feature: Component Tests - Access Control<br/>
 * Story: Test Distribution Set Access Controller
 */
@ContextConfiguration(classes = { AccessControllerConfiguration.class })
@TestPropertySource(properties = "hawkbit.acm.access-controller.enabled=true")
class DistributionSetAccessControllerTest extends AbstractJpaIntegrationTest {

    /**
     * Verifies read access rules for distribution sets
     */
    @Test
    void verifyDistributionSetReadOperations() {
        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();

        final Action permittedAction = testdataFactory.performAssignment(permitted);

        runAs(withUser("user",
                        READ_DISTRIBUTION_SET + "/id==" + permitted.getId(),
                        READ_TARGET +"/controllerId==" + permittedAction.getTarget().getControllerId()), () -> {
            final Long permittedActionId = permitted.getId();

            // verify distributionSetManagement#findAll
            assertThat(distributionSetManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                    .containsOnly(permittedActionId);

            // verify distributionSetManagement#findByRsql
            assertThat(distributionSetManagement.findByRsql("name==*", Pageable.unpaged()).get().map(Identifiable::getId)
                    .toList()).containsOnly(permittedActionId);

            // verify distributionSetManagement#get
            assertThat(distributionSetManagement.find(permittedActionId)).isPresent();
            final Long hiddenId = hidden.getId();
            assertThat(distributionSetManagement.find(hiddenId)).isEmpty();

            // verify distributionSetManagement#getWithDetails
            assertThat(distributionSetManagement.getWithDetails(permittedActionId)).isNotNull();
            assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> distributionSetManagement.getWithDetails(hiddenId));

            // verify distributionSetManagement#get
            final List<Long> allActionIds = Arrays.asList(permittedActionId, hiddenId);
            assertThatThrownBy(() -> distributionSetManagement.get(allActionIds))
                    .as("Fail if request hidden.").isInstanceOf(EntityNotFoundException.class);

            // verify distributionSetManagement#getByNameAndVersion
            assertThat(distributionSetManagement.findByNameAndVersion(permitted.getName(), permitted.getVersion())).isNotNull();
            final String hiddenName = hidden.getName();
            final String hiddenVersion = hidden.getVersion();
            assertThatExceptionOfType(EntityNotFoundException.class)
                    .isThrownBy(() -> distributionSetManagement.findByNameAndVersion(hiddenName, hiddenVersion));
        });
    }

    /**
     * Verifies read access rules for distribution sets
     */
    @Test
    void verifyDistributionSetUpdates() {
        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final String mdPresetKey = "metadata.preset";
        final String mdPresetValue = "presetValue";
        distributionSetManagement.createMetadata(permitted.getId(), Map.of(mdPresetKey, mdPresetValue));
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        distributionSetManagement.createMetadata(readOnly.getId(), Map.of(mdPresetKey, mdPresetValue));
        final DistributionSet hidden = testdataFactory.createDistributionSet();
        distributionSetManagement.createMetadata(hidden.getId(), Map.of(mdPresetKey, mdPresetValue));

        final SoftwareModule swModule = testdataFactory.createSoftwareModuleOs();

        runAs(withUser("user",
                READ_DISTRIBUTION_SET + "/id==" + permitted.getId() + " or id==" + readOnly.getId(),
                UPDATE_DISTRIBUTION_SET + "/id==" + permitted.getId()), () -> {
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
            distributionSetManagement.createMetadata(permitted.getId(), mdPresetKey, newValue);
            assertThatThrownBy(() -> distributionSetManagement.createMetadata(readOnlyId, mdPresetKey, newValue))
                    .as("Distribution set not allowed to me modified.")
                    .isInstanceOf(InsufficientPermissionException.class);
            assertThatThrownBy(() -> distributionSetManagement.createMetadata(hiddenId, mdPresetKey, newValue))
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
        });
    }

    @Test
    void verifyTagFilteringAndManagement() {
        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();
        final Long dsTagId = distributionSetTagManagement.create(
                DistributionSetTagManagement.Create.builder().name("dsTag").build()).getId();
        final Long dsTag2Id = distributionSetTagManagement.create(
                DistributionSetTagManagement.Create.builder().name("dsTag2").build()).getId();

        // perform tag assignment before setting access rules
        distributionSetManagement.assignTag(Arrays.asList(permitted.getId(), readOnly.getId(), hidden.getId()), dsTagId);

        runAs(withUser("user",
                READ_DISTRIBUTION_SET + "/id==" + permitted.getId() + " or id==" + readOnly.getId(),
                UPDATE_DISTRIBUTION_SET + "/id==" + permitted.getId()), () -> {
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
            assertThatThrownBy(() -> distributionSetManagement.assignTag(readOblyList, dsTag2Id))
                    .as("Missing update permissions for target to toggle tag assignment.")
                    .isInstanceOf(InsufficientPermissionException.class);

            // assignment is denied for hiddenTarget since it's hidden
            final List<Long> hiddenList = Collections.singletonList(hidden.getId());
            assertThatThrownBy(() -> distributionSetManagement.unassignTag(hiddenList, dsTagId))
                    .as("Missing update permissions for target to toggle tag assignment.")
                    .isInstanceOf(EntityNotFoundException.class);

            // assignment is denied for hiddenTarget since it's hidden
            assertThatThrownBy(() -> distributionSetManagement.assignTag(hiddenList, dsTagId))
                    .as("Missing update permissions for target to toggle tag assignment.")
                    .isInstanceOf(EntityNotFoundException.class);

            // assignment is denied for hiddenTarget since it's hidden
            final List<Long> hiddenIdList = List.of(hidden.getId());
            assertThatThrownBy(() -> distributionSetManagement.unassignTag(hiddenIdList, dsTagId))
                    .as("Missing update permissions for target to toggle tag assignment.")
                    .isInstanceOf(EntityNotFoundException.class);
        });
    }

    @Test
    void verifyAutoAssignmentUsage() {
        final DistributionSet permitted = testdataFactory.createDistributionSet();
        final DistributionSet readOnly = testdataFactory.createDistributionSet();
        final DistributionSet hidden = testdataFactory.createDistributionSet();
        // has to lock them, otherwise implicit lock shall be made which require DistributionSet update permissions
        distributionSetManagement.lock(permitted);
        distributionSetManagement.lock(readOnly);
        distributionSetManagement.lock(hidden);

        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("test").query("id==*").build());

        runAs(withUser("user",
                READ_DISTRIBUTION_SET + "/id==" + permitted.getId() + " or id==" + readOnly.getId(),
                UPDATE_DISTRIBUTION_SET + "/id==" + permitted.getId(),
                // read / update target needed to update target filter query
                READ_TARGET, UPDATE_TARGET), () -> {
            assertThat(targetFilterQueryManagement
                    .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(permitted.getId())
                            .actionType(Action.ActionType.FORCED).confirmationRequired(false))
                    .getAutoAssignDistributionSet().getId()).isEqualTo(permitted.getId());
            targetFilterQueryManagement
                    .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                            .ds(readOnly.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false))
                    .getAutoAssignDistributionSet().getId();
            final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate =
                    new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                            .ds(hidden.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false);
            assertThatThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate))
                    .isInstanceOf(EntityNotFoundException.class);
        });
    }
}