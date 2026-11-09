/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.auth.SpPermission.CREATE_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;

import java.util.concurrent.Callable;

import jakarta.validation.ConstraintViolationException;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.ActionStatusCodeManagement;
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Test for {@link ActionStatusCodeManagement}.
 */
class ActionStatusCodeManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    private TestdataFactory testdataFactory;

    @Test
    void createWithDefaultSeverity() {
        final ActionStatusCode created = actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("A")
                        .build()
        );

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getCode()).isEqualTo(1);
        assertThat(created.getName()).isEqualTo("A");
        assertThat(created.getSeverity()).isEqualTo(ActionStatusCode.Severity.INFO);
    }

    @Test
    void createDuplicateCodeFails() {
        actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("FIRST")
                        .build()
        );

        assertThatThrownBy(() -> actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("SECOND")
                        .build()
        )).isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessageContaining("Action status code")
                .hasMessageContaining("1");
    }

    @Test
    void updateChangesOnlyGivenFields() {
        final ActionStatusCode created = actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("A")
                        .description("d")
                        .severity(ActionStatusCode.Severity.WARN)
                        .build()
        );

        final long initialRevision = created.getOptLockRevision();

        final ActionStatusCode updated = actionStatusCodeManagement.update(
                ActionStatusCodeManagement.Update.builder()
                        .id(created.getId())
                        .severity(ActionStatusCode.Severity.ERROR)
                        .build()
        );

        assertThat(updated.getCode()).isEqualTo(1);
        assertThat(updated.getName()).isEqualTo("A");
        assertThat(updated.getDescription()).isEqualTo("d");
        assertThat(updated.getSeverity()).isEqualTo(ActionStatusCode.Severity.ERROR);
        assertThat(updated.getOptLockRevision()).isGreaterThan((int) initialRevision);
    }

    @Test
    void updateUnknownIdFails() {
        assertThatThrownBy(() -> actionStatusCodeManagement.update(
                ActionStatusCodeManagement.Update.builder()
                        .id(999_999L)
                        .severity(ActionStatusCode.Severity.ERROR)
                        .build()
        )).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getUnknownIdFails() {
        assertThatThrownBy(() -> actionStatusCodeManagement.get(999_999L))
                .isInstanceOf(EntityNotFoundException.class);

        assertThat(actionStatusCodeManagement.find(999_999L)).isEmpty();
    }

    @Test
    void getOtherTenantsIdFails() throws Exception {
        final String tenantB = "tenantB";
        final Long idInTenantB = runAsTenant(tenantB, () -> {
            final ActionStatusCode created = actionStatusCodeManagement.create(
                    ActionStatusCodeManagement.Create.builder()
                            .code(1)
                            .name("B")
                            .build()
            );
            return created.getId();
        });

        // Try to access it from the default tenant
        assertThatThrownBy(() -> actionStatusCodeManagement.get(idInTenantB))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByRsqlFiltersBySeverity() {
        actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("One")
                        .severity(ActionStatusCode.Severity.INFO)
                        .build()
        );
        actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(2)
                        .name("Two")
                        .severity(ActionStatusCode.Severity.ERROR)
                        .build()
        );
        actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(3)
                        .name("Three")
                        .severity(ActionStatusCode.Severity.ERROR)
                        .build()
        );

        final Page<? extends ActionStatusCode> errorCodes = actionStatusCodeManagement.findByRsql(
                "severity==ERROR",
                PageRequest.of(0, 100)
        );
        assertThat(errorCodes.getTotalElements()).isEqualTo(2);
        assertThat(errorCodes.getContent()).extracting(ActionStatusCode::getCode).containsExactlyInAnyOrder(2, 3);

        final Page<? extends ActionStatusCode> codeTwo = actionStatusCodeManagement.findByRsql(
                "code=ge=2;code=le=2",
                PageRequest.of(0, 100)
        );
        assertThat(codeTwo.getTotalElements()).isEqualTo(1);
        assertThat(codeTwo.getContent()).extracting(ActionStatusCode::getCode).containsExactly(2);

        final Page<? extends ActionStatusCode> nameA = actionStatusCodeManagement.findByRsql(
                "name==T*",
                PageRequest.of(0, 100)
        );
        assertThat(nameA.getTotalElements()).isEqualTo(2);
        assertThat(nameA.getContent()).extracting(ActionStatusCode::getCode).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void validationRejectsBlankName() {
        assertThatThrownBy(() -> actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("")
                        .build()
        )).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void validationRejectsNullSeverity() {
        assertThatThrownBy(() -> actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("A")
                        .severity(null)
                        .build()
        )).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @WithUser(authorities = READ_TARGET)
    void createWithoutCreateTargetIsDenied() {
        assertThatThrownBy(() -> actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(1)
                        .name("A")
                        .build()
        )).isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    @WithUser(authorities = CREATE_TARGET)
    void findUndocumentedWithoutReadTargetIsDenied() {
        assertThatThrownBy(() -> actionStatusCodeManagement.findUndocumentedCodes(
                PageRequest.of(0, 10)
        )).isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void undocumentedListsReportedCodesWithoutCatalogueEntry() {
        // Create a target and distribution set
        final String controllerId = "target1";
        final Target target = targetManagement.create(Create.builder().controllerId(controllerId).build());
        final DistributionSet ds = testdataFactory.createDistributionSet();

        // Assign distribution set to target
        final long actionId = deploymentManagement.assignDistributionSets(
                List.of(DeploymentRequest.builder(controllerId, ds.getId()).build()), null
        ).get(0).getAssignedEntity().get(0).getId();

        // Report action statuses with codes 300, 100, 200, 100 (duplicate)
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(300)
                        .build()
        );
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(100)
                        .build()
        );
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(200)
                        .build()
        );
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .code(100)
                        .build()
        );

        // Create catalogue entry for code 200
        actionStatusCodeManagement.create(
                ActionStatusCodeManagement.Create.builder()
                        .code(200)
                        .name("Documented")
                        .build()
        );

        // Query undocumented codes
        final Page<Integer> undocumented = actionStatusCodeManagement.findUndocumentedCodes(
                PageRequest.of(0, 10)
        );

        // Should return [100, 300] (distinct, sorted, 200 is documented)
        assertThat(undocumented.getContent()).containsExactly(100, 300);
        assertThat(undocumented.getTotalElements()).isEqualTo(2);
    }

    @Test
    void undocumentedPaging() {
        // Create a target and distribution set
        final String controllerId = "target2";
        final Target target = targetManagement.create(Create.builder().controllerId(controllerId).build());
        final DistributionSet ds = testdataFactory.createDistributionSet();

        final long actionId = deploymentManagement.assignDistributionSets(
                List.of(DeploymentRequest.builder(controllerId, ds.getId()).build()), null
        ).get(0).getAssignedEntity().get(0).getId();

        // Report codes 1, 2, 3, 4, 5
        for (int code = 1; code <= 5; code++) {
            controllerManagement.addUpdateActionStatus(
                    Action.ActionStatusCreate.builder()
                            .actionId(actionId)
                            .status(Action.Status.RUNNING)
                            .code(code)
                            .build()
            );
        }

        // Get page 1, size 2 (should return codes 3, 4)
        final Page<Integer> page = actionStatusCodeManagement.findUndocumentedCodes(
                PageRequest.of(1, 2)
        );

        assertThat(page.getContent()).containsExactly(3, 4);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void undocumentedIsTenantIsolated() throws Exception {
        final String tenantA = "tenantA";
        final String tenantB = "tenantB";

        // Tenant A reports code 700
        runAsTenant(tenantA, () -> {
            final String controllerId = "targetA";
            final Target target = targetManagement.create(Create.builder().controllerId(controllerId).build());
            final DistributionSet ds = testdataFactory.createDistributionSet();
            final long actionId = deploymentManagement.assignDistributionSets(
                    List.of(DeploymentRequest.builder(controllerId, ds.getId()).build()), null
            ).get(0).getAssignedEntity().get(0).getId();

            controllerManagement.addUpdateActionStatus(
                    Action.ActionStatusCreate.builder()
                            .actionId(actionId)
                            .status(Action.Status.RUNNING)
                            .code(700)
                            .build()
            );
            return null;
        });

        // Tenant B reports code 800 and documents code 700
        runAsTenant(tenantB, () -> {
            final String controllerId = "targetB";
            final Target target = targetManagement.create(Create.builder().controllerId(controllerId).build());
            final DistributionSet ds = testdataFactory.createDistributionSet();
            final long actionId = deploymentManagement.assignDistributionSets(
                    List.of(DeploymentRequest.builder(controllerId, ds.getId()).build()), null
            ).get(0).getAssignedEntity().get(0).getId();

            controllerManagement.addUpdateActionStatus(
                    Action.ActionStatusCreate.builder()
                            .actionId(actionId)
                            .status(Action.Status.RUNNING)
                            .code(800)
                            .build()
            );

            // Document code 700 in tenant B
            actionStatusCodeManagement.create(
                    ActionStatusCodeManagement.Create.builder()
                            .code(700)
                            .name("Documented in B")
                            .build()
            );
            return null;
        });

        // Tenant A should see [700] (not affected by B's documentation)
        final Page<Integer> undocumentedA = runAsTenant(tenantA, () ->
                actionStatusCodeManagement.findUndocumentedCodes(PageRequest.of(0, 10))
        );
        assertThat(undocumentedA.getContent()).containsExactly(700);

        // Tenant B should see [800] (700 is documented in B, and A's 700 is not visible)
        final Page<Integer> undocumentedB = runAsTenant(tenantB, () ->
                actionStatusCodeManagement.findUndocumentedCodes(PageRequest.of(0, 10))
        );
        assertThat(undocumentedB.getContent()).containsExactly(800);
    }

    @Test
    void statusesWithoutCodeAreIgnored() {
        final String controllerId = "target3";
        final Target target = targetManagement.create(Create.builder().controllerId(controllerId).build());
        final DistributionSet ds = testdataFactory.createDistributionSet();

        final long actionId = deploymentManagement.assignDistributionSets(
                List.of(DeploymentRequest.builder(controllerId, ds.getId()).build()), null
        ).get(0).getAssignedEntity().get(0).getId();

        // Report status without code
        controllerManagement.addUpdateActionStatus(
                Action.ActionStatusCreate.builder()
                        .actionId(actionId)
                        .status(Action.Status.RUNNING)
                        .build()
        );

        final Page<Integer> undocumented = actionStatusCodeManagement.findUndocumentedCodes(
                PageRequest.of(0, 10)
        );

        assertThat(undocumented.getContent()).isEmpty();
        assertThat(undocumented.getTotalElements()).isEqualTo(0);
    }

    private <T> T runAsTenant(final String tenant, final Callable<T> callable) throws Exception {
        return SecurityContextSwitch.callAs(
                SecurityContextSwitch.withTenantAndUser(tenant, "user",
                        new String[] { SpRole.TENANT_ADMIN, SpRole.CONTROLLER_ROLE, SpRole.SYSTEM_ROLE },
                        false, true),
                callable
        );
    }
}
