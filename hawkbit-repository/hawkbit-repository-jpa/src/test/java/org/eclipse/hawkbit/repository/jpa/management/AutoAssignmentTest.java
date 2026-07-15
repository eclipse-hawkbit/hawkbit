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
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignApprovalDecision.APPROVED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignApprovalDecision.DENIED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.APPROVAL_DENIED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.PAUSED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.RUNNING;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.WAITING_FOR_APPROVAL;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTO_ASSIGNMENT_APPROVAL_ENABLED;

import java.time.Duration;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.Create;
import org.eclipse.hawkbit.repository.exception.AutoAssignmentIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.scheduler.JpaAutoAssignHandler;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
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
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("ds").query("name==*").build()).getId();

        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()));

        // approval is disabled, so the auto assignment becomes READY directly
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(READY);
    }

    /**
     * Auto assignment created while approval is enabled, becomes WAITING_FOR_APPROVAL, then is denied
     */
    @Test
    void autoAssignmentLifecycleWithApprovalThenDenied() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("ds").query("name==*").build()).getId();

        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()));

        // approval is enabled, so the auto assignment waits for approval
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        targetFilterQueryManagement.approveOrDeny(filterId, DENIED, "denied");

        // the decision, the deciding actor and the remark are persisted
        final TargetFilterQuery query = targetFilterQueryManagement.get(filterId);
        assertThat(query.getAutoAssignStatus()).isEqualTo(APPROVAL_DENIED);
        assertThat(query.getApprovalDecidedBy()).isEqualTo(AccessContext.actor());
        assertThat(query.getApprovalRemark()).isEqualTo("denied");
    }

    /**
     * Auto assignment created while approval is enabled, becomes WAITING_FOR_APPROVAL, then is approved
     */
    @Test
    void autoAssignmentLifecycleWithApprovalThenApproved() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("ds").query("name==*").build()).getId();

        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()));

        // approval is enabled, so the auto assignment waits for approval
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        targetFilterQueryManagement.approveOrDeny(filterId, APPROVED, "approved");

        // the decision, the deciding actor and the remark are persisted
        final TargetFilterQuery query = targetFilterQueryManagement.get(filterId);
        assertThat(query.getAutoAssignStatus()).isEqualTo(READY);
        assertThat(query.getApprovalDecidedBy()).isEqualTo(AccessContext.actor());
        assertThat(query.getApprovalRemark()).isEqualTo("approved");

        // an already decided auto assignment cannot be approved or denied again
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> targetFilterQueryManagement.approveOrDeny(filterId, APPROVED, "again"));
    }

    /**
     * Auto assignment can be started, paused and resumed and rejects illegal state transitions
     */
    @Test
    void autoAssignmentPauseResume() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("ds").query("name==*").build()).getId();

        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()));
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(READY);

        // READY -> RUNNING, but a running auto assignment cannot be started again
        targetFilterQueryManagement.startAutoAssignDS(filterId);
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(RUNNING);
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> targetFilterQueryManagement.startAutoAssignDS(filterId));

        // RUNNING -> PAUSED, but a paused auto assignment cannot be paused again
        targetFilterQueryManagement.pauseAutoAssignDS(filterId);
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(PAUSED);
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> targetFilterQueryManagement.pauseAutoAssignDS(filterId));

        // a paused auto assignment is ignored by the scheduler
        autoAssignHandler.handleAll();
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(PAUSED);

        // PAUSED -> RUNNING, but a running auto assignment cannot be resumed again
        targetFilterQueryManagement.resumeAutoAssignDS(filterId);
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(RUNNING);
        assertThatExceptionOfType(AutoAssignmentIllegalStateException.class)
                .isThrownBy(() -> targetFilterQueryManagement.resumeAutoAssignDS(filterId));
    }

    /**
     * Auto assignment with startAt set to the current time is started by the scheduler
     */
    @Test
    void autoAssignmentSchedulerTestCurrentTime() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("dsCurrent").query("name==*").build()).getId();

        final long currentTime = System.currentTimeMillis();
        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()).startAt(currentTime));

        assertThat(targetFilterQueryManagement.get(filterId).getStartAt()).isEqualTo(currentTime);
        // approval is disabled, so the auto assignment is READY
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(READY);

        // startAt is due, so the scheduler starts it
        autoAssignHandler.handleAll();
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(RUNNING);
    }

    /**
     * Auto assignment with startAt in the future is not started by the scheduler and stays READY
     */
    @Test
    void autoAssignmentSchedulerTestFutureTime() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("dsCurrent").query("name==*").build()).getId();

        final long futureTime = System.currentTimeMillis() + Duration.ofHours(1).toMillis();
        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()).startAt(futureTime));

        assertThat(targetFilterQueryManagement.get(filterId).getStartAt()).isEqualTo(futureTime);
        // approval is disabled, so the auto assignment is READY
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(READY);

        // startAt is in the future, so the scheduler leaves it READY
        autoAssignHandler.handleAll();
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(READY);
    }

    /**
     * Auto assignment with no startAt is started by the scheduler
     */
    @Test
    void autoAssignmentSchedulerTestNoTimeSet() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("dsCurrent").query("name==*").build()).getId();

        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()).startAt(null));

        assertThat(targetFilterQueryManagement.get(filterId).getStartAt()).isNull();
        // approval is disabled, so the auto assignment is READY
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(READY);

        // no startAt is set, so the scheduler starts it immediately
        autoAssignHandler.handleAll();
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(RUNNING);
    }

    /**
     * Auto assignments that are neither READY nor RUNNING are ignored by the scheduler
     */
    @Test
    void autoAssignmentSchedulerSkipNotReady() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, true);

        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long filterId = targetFilterQueryManagement.create(Create.builder().name("dsCurrent").query("name==*").build()).getId();

        targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(filterId).ds(ds.getId()));
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        // a WAITING_FOR_APPROVAL auto assignment is ignored by the scheduler
        autoAssignHandler.handleAll();
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(WAITING_FOR_APPROVAL);

        targetFilterQueryManagement.approveOrDeny(filterId, DENIED);
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(APPROVAL_DENIED);

        // an APPROVAL_DENIED auto assignment is ignored by the scheduler
        autoAssignHandler.handleAll();
        assertThat(targetFilterQueryManagement.get(filterId).getAutoAssignStatus()).isEqualTo(APPROVAL_DENIED);
    }

    @BeforeEach
    void reset() {
        tenantConfigurationManagement().addOrUpdateConfiguration(AUTO_ASSIGNMENT_APPROVAL_ENABLED, false);
    }
}