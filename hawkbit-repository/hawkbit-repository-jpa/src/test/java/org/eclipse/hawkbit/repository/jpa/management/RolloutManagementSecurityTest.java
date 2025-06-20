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

import jakarta.validation.ConstraintDeclarationException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DynamicRolloutGroupTemplate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
/**
 * Feature: SecurityTests - RolloutManagement<br/>
 * Story: SecurityTests RolloutManagement
 */
class RolloutManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void getPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.get(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void getByNamePermissionsCheck() {
        assertPermissions(() -> rolloutManagement.getByName("name"), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void getWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.getWithDetailedStatus(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void approveOrDenyPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED), List.of(SpPermission.APPROVE_ROLLOUT));
        assertPermissions(() -> rolloutManagement.approveOrDeny(1L, Rollout.ApprovalDecision.APPROVED, "comment"),
                List.of(SpPermission.APPROVE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void pauseRolloutPermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.pauseRollout(1L);
            return null;
        }, List.of(SpPermission.HANDLE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void resumeRolloutPermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.resumeRollout(1L);
            return null;
        }, List.of(SpPermission.HANDLE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findActiveRolloutsPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findActiveRollouts(), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void cancelRolloutsForDistributionSetPermissionsCheck() {
        final DistributionSetTypeCreate key = entityFactory.distributionSetType().create().name("type").key("type");
        distributionSetTypeManagement.create(key);
        final DistributionSetCreate dsCreate = entityFactory.distributionSet().create().name("name").version("1.0.0").type("type");
        final DistributionSet ds = distributionSetManagement.create(dsCreate);
        assertPermissions(() -> {
            rolloutManagement.cancelRolloutsForDistributionSet(ds);
            return null;
        }, List.of(SpPermission.UPDATE_ROLLOUT, SpPermission.READ_REPOSITORY, SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void countPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.count(), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void countByDistributionSetIdAndRolloutIsStoppablePermissionsCheck() {
        assertPermissions(() -> rolloutManagement.countByDistributionSetIdAndRolloutIsStoppable(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void createPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L), 1, false,
                new RolloutGroupConditionBuilder().withDefaults().build()), List.of(SpPermission.CREATE_ROLLOUT, SpPermission.READ_REPOSITORY));
        assertPermissions(() -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L), 1, false,
                        new RolloutGroupConditionBuilder().withDefaults().build(), DynamicRolloutGroupTemplate.builder().build()),
                List.of(SpPermission.CREATE_ROLLOUT, SpPermission.READ_REPOSITORY));
        assertPermissions(
                () -> rolloutManagement.create(entityFactory.rollout().create().distributionSetId(1L),
                        List.of(entityFactory.rolloutGroup().create()),
                        new RolloutGroupConditionBuilder().withDefaults().build()),
                List.of(SpPermission.CREATE_ROLLOUT, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findAllPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findAll(false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findByRsql("id==1", false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findAllWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.findAllWithDetailedStatus(false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRsqlWithDetailedStatusPermissionsCheck() {
        assertPermissions(() ->
                rolloutManagement.findByRsqlWithDetailedStatus("name==*", false, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void startPermissionsCheck() {
        assertPermissions(() -> rolloutManagement.start(1L), List.of(SpPermission.HANDLE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void updatePermissionsCheck() {
        assertPermissions(() -> rolloutManagement.update(entityFactory.rollout().update(1L)), List.of(SpPermission.UPDATE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void deletePermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.delete(1L);
            return null;
        }, List.of(SpPermission.DELETE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void triggerNextGroupPermissionsCheck() {
        assertPermissions(() -> {
            rolloutManagement.triggerNextGroup(1L);
            return null;
        }, List.of(SpPermission.UPDATE_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test//    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.CREATE_ROLLOUT, SpPermission.READ_ROLLOUT,
//            SpPermission.READ_TARGET })
    void validateTargetsInGroupsPermissionsCheck() {
        try {
            assertPermissions(
                    () -> rolloutManagement.validateTargetsInGroups(List.of(entityFactory.rolloutGroup().create()), "name==dummy", 1L, 1L),
                    List.of(SpPermission.READ_ROLLOUT, SpPermission.READ_TARGET));
        } catch (Error e) {
            if (e.getCause() instanceof ConstraintDeclarationException) {
                log.info("ConstraintDeclarationException thrown expected");
            } else {
                throw e;
            }
        }
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutAndRsqlWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE),
                List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRolloutWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }
}
