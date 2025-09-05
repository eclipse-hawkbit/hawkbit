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
import static org.eclipse.hawkbit.security.SecurityContextSerializer.JSON_SERIALIZATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.Callable;

import lombok.SneakyThrows;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Feature: Component Tests - Context runner<br/>
 * Story: Test Context Runner
 */
class ContextAwareTest extends AbstractJpaIntegrationTest {

    private static final List<String> AUTHORITIES = SpPermission.getAllAuthorities();

    @Autowired
    AutoAssignExecutor autoAssignExecutor;

    @Autowired
    ContextAware contextAware;

    @Autowired
    AuditorAware<String> auditorAware;

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
        final SecurityContext securityContext = createContext(0);
        assertThat(securityContext).isNotNull();

        final Rollout exampleRollout = runInContext(securityContext, testdataFactory::createRollout);
        assertThat(exampleRollout.getAccessControlContext())
                .hasValueSatisfying(ctx -> assertEssentialEquals(JSON_SERIALIZATION.deserialize(ctx), securityContext));
    }

    /**
     * Verifies acm context is reused when handling a rollout
     */
    @Test
    void verifyContextIsReusedWhenHandlingRollout() {
        final SecurityContext securityContext = createContext(1);

        // testdataFactory#createRollout will trigger a rollout handling
        runInContext(securityContext, testdataFactory::createRollout);
        verify(contextAware).runInContext(eq(JSON_SERIALIZATION.serialize(securityContext)), any(Runnable.class));
    }

    /**
     * Verifies acm context is persisted when activating auto assignment
     */
    @Test
    void verifyContextIsPersistedInActiveAutoAssignment() {
        final SecurityContext securityContext = createContext(2);

        final TargetFilterQuery targetFilterQuery =
                runInContext(securityContext, testdataFactory::createTargetFilterWithTargetsAndActiveAutoAssignment);
        assertThat(targetFilterQuery.getAccessControlContext())
                .hasValueSatisfying(ctx -> assertEssentialEquals(JSON_SERIALIZATION.deserialize(ctx), securityContext));
    }

    /**
     * Verifies acm context is used when performing auto assign check on all target
     */
    @Test
    void verifyContextIsReusedWhenCheckingForAutoAssignmentAllTargets() {
        final SecurityContext securityContext = createContext(3);

        runInContext(securityContext, testdataFactory::createTargetFilterWithTargetsAndActiveAutoAssignment);
        runInContext(securityContext, () -> {
            autoAssignExecutor.checkAllTargets();
            return null;
        });
        verify(contextAware).runInContext(eq(JSON_SERIALIZATION.serialize(securityContext)), any(Runnable.class));
    }

    /**
     * Verifies acm context is used when performing auto assign check on single target
     */
    @Test
    void verifyContextIsReusedWhenCheckingForAutoAssignmentSingleTarget() {
        final SecurityContext securityContext = createContext(4);

        runInContext(securityContext, testdataFactory::createTargetFilterWithTargetsAndActiveAutoAssignment);
        runInContext(securityContext, () -> {
            autoAssignExecutor.checkSingleTarget(targetManagement.findAll(Pageable.ofSize(1)).getContent().get(0).getControllerId());
            return null;
        });
        verify(contextAware).runInContext(eq(JSON_SERIALIZATION.serialize(securityContext)), any(Runnable.class));
    }

    private static SecurityContext createContext(final int testId) {
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        final UsernamePasswordAuthenticationToken userPassAuthentication = new UsernamePasswordAuthenticationToken(
                "user", null, AUTHORITIES.stream().map(SimpleGrantedAuthority::new).toList());
        final TenantAwareAuthenticationDetails details = new TenantAwareAuthenticationDetails("my_tenant_" + testId, false);
        userPassAuthentication.setDetails(details);
        securityContext.setAuthentication(userPassAuthentication);

        assertThat(securityContext).isNotNull();

        return securityContext;
    }

    @SneakyThrows
    private <T> T runInContext(final SecurityContext securityContext, final Callable<T> runnable) {
        SecurityContextHolder.setContext(securityContext);
        try {
            return runnable.call();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertEssentialEquals(final SecurityContext sc1, final SecurityContext sc2) {
        assertThat(auditor(sc1)).hasToString(auditor(sc2));
        assertThat(sc1.getAuthentication().getAuthorities()).isEqualTo(sc2.getAuthentication().getAuthorities());
        assertThat(sc1.getAuthentication().isAuthenticated()).isEqualTo(sc2.getAuthentication().isAuthenticated());
        assertThat(sc1.getAuthentication().getDetails()).isEqualTo(sc2.getAuthentication().getDetails());
    }

    private String auditor(final SecurityContext securityContext) {
        SecurityContextHolder.setContext(securityContext);
        try {
            return auditorAware.getCurrentAuditor().orElseThrow();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}