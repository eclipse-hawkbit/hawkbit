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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignApprovalDecision.APPROVED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignApprovalDecision.DENIED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.APPROVAL_DENIED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.PAUSED;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.RUNNING;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.WAITING_FOR_APPROVAL;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTO_ASSIGNMENT_APPROVAL_ENABLED;

import java.time.Duration;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement.Create;
import org.eclipse.hawkbit.repository.exception.AutoAssignmentIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.scheduler.JpaAutoAssignHandler;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Junit test for auto assignments
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Auto Assignments
 */
class AutoAssignmentTest extends AbstractJpaIntegrationTest {

    @Autowired
    private JpaAutoAssignHandler autoAssignHandler;

    /**
     * Auto assignment created while approval is disabled, becomes READY directly
     */
    @Test
    void autoAssignmentLifecycleWithoutApproval() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("ds").targetFilterQuery("name==*").distributionSet(ds).build()).getId();

        // approval is disabled, so the auto assignment becomes READY directly
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);
    }

    /**
     * Auto assignment created while approval is enabled, becomes WAITING_FOR_APPROVAL, then is denied
     */
    @Test
    void autoAssignmentLifecycleWithApprovalThenDenied() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("ds").targetFilterQuery("name==*").distributionSet(ds).build()).getId();

        // approval is enabled, so the auto assignment waits for approval
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        autoAssignmentManagement.approveOrDeny(autoAssignmentId, DENIED, "denied");

        // the decision, the deciding actor and the remark are persisted
        final AutoAssignment autoAssignment = autoAssignmentManagement.get(autoAssignmentId);
        assertThat(autoAssignment.getStatus()).isEqualTo(APPROVAL_DENIED);
        assertThat(autoAssignment.getApprovalDecidedBy()).isEqualTo(AccessContext.actor());
        assertThat(autoAssignment.getApprovalRemark()).isEqualTo("denied");
    }

    /**
     * Auto assignment created while approval is enabled, becomes WAITING_FOR_APPROVAL, then is approved
     */
    @Test
    void autoAssignmentLifecycleWithApprovalThenApproved() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("ds").targetFilterQuery("name==*").distributionSet(ds).build()).getId();

        // approval is enabled, so the auto assignment waits for approval
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        autoAssignmentManagement.approveOrDeny(autoAssignmentId, APPROVED, "approved");

        // the decision, the deciding actor and the remark are persisted
        final AutoAssignment autoAssignment = autoAssignmentManagement.get(autoAssignmentId);
        assertThat(autoAssignment.getStatus()).isEqualTo(READY);
        assertThat(autoAssignment.getApprovalDecidedBy()).isEqualTo(AccessContext.actor());
        assertThat(autoAssignment.getApprovalRemark()).isEqualTo("approved");

        // an already decided auto assignment cannot be approved or denied again
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> autoAssignmentManagement.approveOrDeny(autoAssignmentId, APPROVED, "again"));
    }

    /**
     * Auto assignment can be started, paused and resumed and rejects illegal state transitions
     */
    @Test
    void autoAssignmentPauseResume() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("ds").targetFilterQuery("name==*").distributionSet(ds).build()).getId();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);

        // READY -> RUNNING, but a running auto assignment cannot be started again
        autoAssignmentManagement.start(autoAssignmentId);
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> autoAssignmentManagement.start(autoAssignmentId));

        // RUNNING -> PAUSED, but a paused auto assignment cannot be paused again
        autoAssignmentManagement.pause(autoAssignmentId);
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(PAUSED);
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> autoAssignmentManagement.pause(autoAssignmentId));

        // a paused auto assignment is ignored by the scheduler
        autoAssignHandler.handleAll();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(PAUSED);

        // PAUSED -> RUNNING, but a running auto assignment cannot be resumed again
        autoAssignmentManagement.resume(autoAssignmentId);
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> autoAssignmentManagement.resume(autoAssignmentId));
    }

    /**
     * Auto assignment with startAt set to the current time is started by the scheduler
     */
    @Test
    void autoAssignmentSchedulerTestCurrentTime() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final long currentTime = System.currentTimeMillis();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("dsCurrent").targetFilterQuery("name==*").distributionSet(ds).startAt(currentTime).build()).getId();

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStartAt()).isEqualTo(currentTime);
        // approval is disabled, so the auto assignment is READY
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);

        // startAt is due, so the scheduler starts it
        autoAssignHandler.handleAll();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);
    }

    /**
     * Auto assignment with startAt in the future is not started by the scheduler and stays READY
     */
    @Test
    void autoAssignmentSchedulerTestFutureTime() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final long futureTime = System.currentTimeMillis() + Duration.ofHours(1).toMillis();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("dsCurrent").targetFilterQuery("name==*").distributionSet(ds).startAt(futureTime).build()).getId();

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStartAt()).isEqualTo(futureTime);
        // approval is disabled, so the auto assignment is READY
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);

        // startAt is in the future, so the scheduler leaves it READY
        autoAssignHandler.handleAll();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);
    }

    /**
     * Auto assignment with no startAt is started by the scheduler
     */
    @Test
    void autoAssignmentSchedulerTestNoTimeSet() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("dsCurrent").targetFilterQuery("name==*").distributionSet(ds).startAt(null).build()).getId();

        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStartAt()).isNull();
        // approval is disabled, so the auto assignment is READY
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(READY);

        // no startAt is set, so the scheduler starts it immediately
        autoAssignHandler.handleAll();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(RUNNING);
    }

    /**
     * Auto assignments that are neither READY nor RUNNING are ignored by the scheduler
     */
    @Test
    void autoAssignmentSchedulerSkipNotReady() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long autoAssignmentId = autoAssignmentManagement.create(
                Create.builder().name("dsCurrent").targetFilterQuery("name==*").distributionSet(ds).build()).getId();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        // a WAITING_FOR_APPROVAL auto assignment is ignored by the scheduler
        autoAssignHandler.handleAll();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        autoAssignmentManagement.approveOrDeny(autoAssignmentId, DENIED);
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(APPROVAL_DENIED);

        // an APPROVAL_DENIED auto assignment is ignored by the scheduler
        autoAssignHandler.handleAll();
        assertThat(autoAssignmentManagement.get(autoAssignmentId).getStatus()).isEqualTo(APPROVAL_DENIED);
    }

    @BeforeEach
    void reset() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, false);
    }
}