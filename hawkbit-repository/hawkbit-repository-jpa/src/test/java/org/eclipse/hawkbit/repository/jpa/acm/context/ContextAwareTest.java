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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.security.SecurityContextSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Feature("Component Tests - Context runner")
@Story("Test Context Runner")
class ContextAwareTest extends AbstractJpaIntegrationTest {

    @Autowired
    AutoAssignExecutor autoAssignExecutor;

    @Autowired
    ContextAware contextAware;

    private static final SecurityContextSerializer SECURITY_CONTEXT_SERIALIZER =
            new SecurityContextSerializer.JavaSerialization();

    @BeforeEach
    @AfterEach
    void before() {
        reset(contextAware);
    }

    @Test
    @Description("Verifies acm context is persisted when creating Rollout")
    void verifyAcmContextIsPersistedInCreatedRollout() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        final Rollout exampleRollout = testdataFactory.createRollout();
        assertThat(exampleRollout.getAccessControlContext())
                .hasValueSatisfying(ctx ->
                        assertThat(SECURITY_CONTEXT_SERIALIZER.deserialize(ctx)).isEqualTo(securityContext));
    }

    @Test
    @Description("Verifies acm context is reused when handling a rollout")
    void verifyContextIsReusedWhenHandlingRollout() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        // testdataFactory#createRollout will trigger a rollout handling
        testdataFactory.createRollout();
        verify(contextAware).runInContext(eq(SECURITY_CONTEXT_SERIALIZER.serialize(securityContext)), any(Runnable.class));
    }

    @Test
    @Description("Verifies acm context is persisted when activating auto assignment")
    void verifyContextIsPersistedInActiveAutoAssignment() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        final TargetFilterQuery targetFilterQuery = testdataFactory.createTargetFilterWithTargetsAndActiveAutoAssignment();
        assertThat(targetFilterQuery.getAccessControlContext())
                .hasValueSatisfying(ctx ->
                        assertThat(SECURITY_CONTEXT_SERIALIZER.deserialize(ctx)).isEqualTo(securityContext));
    }

    @Test
    @Description("Verifies acm context is used when performing auto assign check on all target")
    void verifyContextIsReusedWhenCheckingForAutoAssignmentAllTargets() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        testdataFactory.createTargetFilterWithTargetsAndActiveAutoAssignment();
        autoAssignExecutor.checkAllTargets();
        verify(contextAware).runInContext(eq(SECURITY_CONTEXT_SERIALIZER.serialize(securityContext)), any(Runnable.class));
    }

    @Test
    @Description("Verifies acm context is used when performing auto assign check on single target")
    void verifyContextIsReusedWhenCheckingForAutoAssignmentSingleTarget() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        testdataFactory.createTargetFilterWithTargetsAndActiveAutoAssignment();
        autoAssignExecutor
                .checkSingleTarget(targetManagement.findAll(Pageable.ofSize(1)).getContent().get(0).getControllerId());
        verify(contextAware).runInContext(eq(SECURITY_CONTEXT_SERIALIZER.serialize(securityContext)), any(Runnable.class));
    }
}
