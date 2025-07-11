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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Feature: Component Tests - Context runner<br/>
 * Story: Test Context Runner
 */
class ContextAwareTest extends AbstractJpaIntegrationTest {

    @Autowired
    AutoAssignExecutor autoAssignExecutor;

    @Autowired
    ContextAware contextAware;

    private static final SecurityContextSerializer SECURITY_CONTEXT_SERIALIZER =
            SecurityContextSerializer.JAVA_SERIALIZATION;

    @BeforeEach
    @AfterEach
    void before() {
        reset(contextAware);
    }

    /**
     * Verifies acm context is persisted when creating Rollout
     */
    @Test
    void verifyAcmContextIsPersistedInCreatedRollout() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        final Rollout exampleRollout = testdataFactory.createRollout();
        assertThat(exampleRollout.getAccessControlContext())
                .hasValueSatisfying(ctx ->
                        assertThat(SECURITY_CONTEXT_SERIALIZER.deserialize(ctx)).isEqualTo(securityContext));
    }

    /**
     * Verifies acm context is reused when handling a rollout
     */
    @Test
    void verifyContextIsReusedWhenHandlingRollout() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        // testdataFactory#createRollout will trigger a rollout handling
        testdataFactory.createRollout();
        verify(contextAware).runInContext(eq(SECURITY_CONTEXT_SERIALIZER.serialize(securityContext)), any(Runnable.class));
    }

    /**
     * Verifies acm context is persisted when activating auto assignment
     */
    @Test
    void verifyContextIsPersistedInActiveAutoAssignment() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        final TargetFilterQuery targetFilterQuery = testdataFactory.createTargetFilterWithTargetsAndActiveAutoAssignment();
        assertThat(targetFilterQuery.getAccessControlContext())
                .hasValueSatisfying(ctx ->
                        assertThat(SECURITY_CONTEXT_SERIALIZER.deserialize(ctx)).isEqualTo(securityContext));
    }

    /**
     * Verifies acm context is used when performing auto assign check on all target
     */
    @Test
    void verifyContextIsReusedWhenCheckingForAutoAssignmentAllTargets() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        testdataFactory.createTargetFilterWithTargetsAndActiveAutoAssignment();
        autoAssignExecutor.checkAllTargets();
        verify(contextAware).runInContext(eq(SECURITY_CONTEXT_SERIALIZER.serialize(securityContext)), any(Runnable.class));
    }

    /**
     * Verifies acm context is used when performing auto assign check on single target
     */
    @Test
    void verifyContextIsReusedWhenCheckingForAutoAssignmentSingleTarget() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext).isNotNull();

        testdataFactory.createTargetFilterWithTargetsAndActiveAutoAssignment();
        autoAssignExecutor
                .checkSingleTarget(targetManagement.findAll(Pageable.ofSize(1)).getContent().get(0).getControllerId());
        verify(contextAware).runInContext(eq(SECURITY_CONTEXT_SERIALIZER.serialize(securityContext)), any(Runnable.class));
    }
}
