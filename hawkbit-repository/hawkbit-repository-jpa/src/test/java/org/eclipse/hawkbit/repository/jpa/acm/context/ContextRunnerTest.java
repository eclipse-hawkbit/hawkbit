/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.repository.acm.context.ContextRunner;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Context runner")
@Story("Test Context Runner")
class ContextRunnerTest extends AbstractJpaIntegrationTest {

    @MockBean
    ContextRunner testContextRunner;

    @Autowired
    AutoAssignExecutor autoAssignExecutor;

    private static final String EXAMPLE_CONTEXT = "example_context";

    @BeforeEach
    void setContext() {
        when(testContextRunner.getCurrentContext()).thenReturn(EXAMPLE_CONTEXT);
    }

    @Test
    @Description("Verifies acm context is persisted when creating Rollout")
    void verifyAcmContextIsPersistedInCreatedRollout() {
        final Rollout exampleRollout = testdataFactory.createRollout();

        assertThat(exampleRollout.getAccessControlContext())
                .hasValueSatisfying(ctx -> assertThat(ctx).isEqualTo(EXAMPLE_CONTEXT));
    }

    @Test
    @Description("Verifies acm context is reused when handling a rollout")
    void verifyContextIsReusedWhenHandlingRollout() {
        // testdataFactory#createRollout will trigger a rollout handling
        testdataFactory.createRollout();

        verify(testContextRunner).runInContext(eq(EXAMPLE_CONTEXT), any(Runnable.class));
    }

    @Test
    @Description("Verifies acm context is persisted when activating auto assignment")
    void verifyContextIsPersistedInActiveAutoAssignment() {
        final TargetFilterQuery targetFilterQuery = testdataFactory.createTargetFilterWithActiveAutoAssignment();

        assertThat(targetFilterQuery.getAccessControlContext())
                .hasValueSatisfying(ctx -> assertThat(ctx).isEqualTo(EXAMPLE_CONTEXT));
    }

    @Test
    @Description("Verifies acm context is used when performing auto assign check on all target")
    void verifyContextIsReusedWhenCheckingForAutoAssignmentAllTargets() {
        testdataFactory.createTargetFilterWithActiveAutoAssignment();

        autoAssignExecutor.checkAllTargets();

        verify(testContextRunner).runInContext(eq(EXAMPLE_CONTEXT), any(Runnable.class));
    }

    @Test
    @Description("Verifies acm context is used when performing auto assign check on single target")
    void verifyContextIsReusedWhenCheckingForAutoAssignmentSingleTarget() {
        testdataFactory.createTargetFilterWithActiveAutoAssignment();

        autoAssignExecutor
                .checkSingleTarget(targetManagement.findAll(Pageable.ofSize(1)).getContent().get(0).getControllerId());
        verify(testContextRunner).runInContext(eq(EXAMPLE_CONTEXT), any(Runnable.class));
    }
}
