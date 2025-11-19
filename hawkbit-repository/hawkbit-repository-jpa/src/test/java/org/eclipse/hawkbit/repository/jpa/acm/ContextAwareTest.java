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
import static org.eclipse.hawkbit.context.SecurityContextSerializer.JSON_SERIALIZATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import lombok.SneakyThrows;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.context.ContextAware;
import org.eclipse.hawkbit.context.SecurityContextSerializer;
import org.eclipse.hawkbit.repository.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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

    private static final Set<String> AUTHORITIES = SpPermission.getAllAuthorities();

    @Autowired
    AutoAssignExecutor autoAssignExecutor;

    @Autowired
    AuditorAware<String> auditorAware;

    @BeforeAll
    static void setup() {
        ContextAware.setSecurityContextSerializer(SecurityContextSerializer.JSON_SERIALIZATION);
    }

    /**
     * Verifies acm context is persisted when creating Rollout
     */
    @Test
    void verifyAcmContextIsPersistedInCreatedRollout() {
        final SecurityContext userContext = createUserContext(0);
        assertThat(userContext).isNotNull();

        final Rollout exampleRollout = runInContext(userContext, testdataFactory::createRollout);
        assertThat(exampleRollout.getAccessControlContext())
                .hasValueSatisfying(ctx -> assertEssentialEquals(JSON_SERIALIZATION.deserialize(ctx), userContext));
    }

    /**
     * Verifies acm context is persisted when activating auto assignment
     */
    @Test
    void verifyContextIsPersistedInActiveAutoAssignment() {
        final SecurityContext userContext = createUserContext(2);

        final TargetFilterQuery targetFilterQuery =
                runInContext(userContext, testdataFactory::createTargetFilterWithTargetsAndActiveAutoAssignment);
        assertThat(targetFilterQuery.getAccessControlContext())
                .hasValueSatisfying(ctx -> assertEssentialEquals(JSON_SERIALIZATION.deserialize(ctx), userContext));
    }

    /**
     * Verifies acm context is used when handling a rollout
     */
    @Test
    void verifyContextIsUsedWhenHandlingRollout() {
        final SecurityContext userContext = createUserContext(1);
        try (final MockedStatic<ContextAware> mocked = mockStatic(ContextAware.class, Mockito.CALLS_REAL_METHODS)) {
            // testdataFactory#createRollout will trigger a rollout handling
            runInContext(userContext, testdataFactory::createRollout);
            mocked.verify(() -> ContextAware.runInContext(eq(JSON_SERIALIZATION.serialize(userContext)), any(Runnable.class)));
        }
    }

    /**
     * Verifies acm context is used when performing auto assign check on all target
     */
    @Test
    void verifyContextIsUsedWhenCheckingForAutoAssignmentAllTargets() {
        final SecurityContext userContext = createUserContext(3);
        try (final MockedStatic<ContextAware> mocked = mockStatic(ContextAware.class, Mockito.CALLS_REAL_METHODS)) {
            runInContext(userContext, testdataFactory::createTargetFilterWithTargetsAndActiveAutoAssignment);
            runInContext(userContext, () -> {
                autoAssignExecutor.checkAllTargets();
                return null;
            });

            mocked.verify(() -> ContextAware.runInContext(eq(JSON_SERIALIZATION.serialize(userContext)), any(Runnable.class)));
        }
    }

    /**
     * Verifies acm context is used when performing auto assign check on single target
     */
    @Test
    void verifyContextIsUsedWhenCheckingForAutoAssignmentSingleTarget() {
        final SecurityContext userContext = createUserContext(4);
        try (final MockedStatic<ContextAware> mocked = mockStatic(ContextAware.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> ContextAware.runInContext(any(SecurityContext.class), (Supplier<?>)any(Supplier.class))).thenCallRealMethod();

            runInContext(userContext, testdataFactory::createTargetFilterWithTargetsAndActiveAutoAssignment);
            runInContext(userContext, () -> {
                autoAssignExecutor.checkSingleTarget(targetManagement.findAll(Pageable.ofSize(1)).getContent().get(0).getControllerId());
                return null;
            });

            mocked.verify(() -> ContextAware.runInContext(eq(JSON_SERIALIZATION.serialize(userContext)), any(Runnable.class)));
        }
    }

    private static SecurityContext createUserContext(final int testId) {
        final SecurityContext userContext = SecurityContextHolder.createEmptyContext();
        final UsernamePasswordAuthenticationToken userPassAuthentication = new UsernamePasswordAuthenticationToken(
                "user", null, AUTHORITIES.stream().map(SimpleGrantedAuthority::new).toList());
        final TenantAwareAuthenticationDetails details = new TenantAwareAuthenticationDetails("my_tenant_" + testId, false);
        userPassAuthentication.setDetails(details);
        userContext.setAuthentication(userPassAuthentication);

        assertThat(userContext).isNotNull();

        return userContext;
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