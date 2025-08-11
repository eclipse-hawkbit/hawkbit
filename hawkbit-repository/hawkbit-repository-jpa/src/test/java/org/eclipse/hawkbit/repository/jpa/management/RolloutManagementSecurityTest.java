/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutManagement.Create;
import org.eclipse.hawkbit.repository.RolloutManagement.GroupCreate;
import org.eclipse.hawkbit.repository.RolloutManagement.Update;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Feature: SecurityTests - RolloutManagement<br/>
 * Story: SecurityTests RolloutManagement
 */
@Slf4j
class RolloutManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Autowired
    private TestdataFactory testdataFactory;

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.get(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByNamePermissionsCheck() {
        assertPermissions(() -> rolloutManagement.getByName("name"), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.getWithDetailedStatus(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void approveOrDenyPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED), List.of(SpPermission.APPROVE_ROLLOUT));
        assertPermissions(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED, "comment"),
                List.of(SpPermission.APPROVE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void pauseRolloutPermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.pauseRollout(1L);
            return null;
        }, List.of(SpPermission.HANDLE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void resumeRolloutPermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.resumeRollout(1L);
            return null;
        }, List.of(SpPermission.HANDLE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActiveRolloutsPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findActiveRollouts(), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void cancelRolloutsForDistributionSetPermissionsCheck() {
        final DistributionSetTypeManagement.Create key = DistributionSetTypeManagement.Create.builder().name("type").key("type").build();
        distributionSetTypeManagement.create(key);
        final DistributionSetManagement.Create dsCreate =
                DistributionSetManagement.Create.builder().type(defaultDsType()).name("name").version("1.0.0").build();
        final DistributionSet ds = distributionSetManagement.create(dsCreate);
        assertPermissions(() -> {
            rolloutManagement.cancelRolloutsForDistributionSet(ds);
            return null;
        }, List.of(SpPermission.UPDATE_ROLLOUT, SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.count(), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByDistributionSetIdAndRolloutIsStoppablePermissionsCheck() {
        assertPermissions(() -> rolloutManagement.countByDistributionSetIdAndRolloutIsStoppable(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createPermissionsCheck() {
        testdataFactory.createTarget(); // to have matching
        final DistributionSet ds = testdataFactory.createDistributionSetLocked("createPermissionsCheck");
        final List<String> permissions = List.of(SpPermission.CREATE_ROLLOUT, SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY);
        assertPermissions(() -> rolloutManagement.create(
                        Create.builder().name("createPermissionsCheck").distributionSet(ds).targetFilterQuery("controllerid==*").build(),
                        1, false, new RolloutGroupConditionBuilder().withDefaults().build()),
                permissions);
        assertPermissions(() -> rolloutManagement.create(
                        Create.builder().name("createPermissionsCheck2").distributionSet(ds).targetFilterQuery("controllerid==*").dynamic(true).build(),
                        1, false, new RolloutGroupConditionBuilder().withDefaults().build(), RolloutManagement.DynamicRolloutGroupTemplate.builder().build()),
                permissions);
        assertPermissions(
                () -> rolloutManagement.create(
                        Create.builder().name("createPermissionsCheck3").distributionSet(ds).targetFilterQuery("controllerid==*").build(),
                        List.of(GroupCreate.builder().name("group").build()), new RolloutGroupConditionBuilder().withDefaults().build()),
                permissions);
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findAllPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findAll(false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findByRsql("id==1", false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findAllWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findAllWithDetailedStatus(false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlWithDetailedStatusPermissionsCheck() {
        assertPermissions(() ->
                rolloutManagement.findByRsqlWithDetailedStatus("name==*", false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void startPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.start(1L), List.of(SpPermission.HANDLE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updatePermissionsCheck() {
        assertPermissions(() -> rolloutManagement.update(Update.builder().id(1L).build()), List.of(SpPermission.UPDATE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.delete(1L);
            return null;
        }, List.of(SpPermission.DELETE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void triggerNextGroupPermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.triggerNextGroup(1L);
            return null;
        }, List.of(SpPermission.UPDATE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutAndRsqlWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE),
                List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRolloutWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }
}