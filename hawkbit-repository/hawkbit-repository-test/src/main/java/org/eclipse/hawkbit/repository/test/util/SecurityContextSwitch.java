/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import static org.eclipse.hawkbit.auth.SpRole.CONTROLLER_ROLE;
import static org.eclipse.hawkbit.auth.SpRole.SYSTEM_ROLE;
import static org.eclipse.hawkbit.auth.SpRole.TENANT_ADMIN;

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public class SecurityContextSwitch {

    private static final SecurityContextSwitch INSTANCE = new SecurityContextSwitch();

    public static final String DEFAULT_TENANT = "DEFAULT";
    private static final WithUser PRIVILEGED_USER = new WithUserImpl(
            DEFAULT_TENANT, "bumlux", new String[] {TENANT_ADMIN, CONTROLLER_ROLE, SYSTEM_ROLE}, false, false);

    private static SystemManagement systemManagement;

    /**
     * @return the singleton {@link SecurityContextSwitch} instance to be injected
     */
    public static SecurityContextSwitch getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("java:S2696") // intentionally, we want, after registering as bean the instance to have injected system management
    @Autowired // spring setter injection
    public void setSystemManagement(final SystemManagement systemManagement) {
        SecurityContextSwitch.systemManagement = systemManagement;
    }

    public static <T> T asPrivileged(final Callable<T> callable) throws Exception {
        createTenant(DEFAULT_TENANT);
        return callAs(PRIVILEGED_USER, callable);
    }

    public static <T> T callAs(final WithUser withUser, final Callable<T> callable) throws Exception {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        setSecurityContext(withUser);
        try {
            if (withUser.autoCreateTenant()) {
                createTenant(withUser.tenantId());
            }
            return callable.call();
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
    }

    public static <T> T getAs(final WithUser withUser, final Supplier<T> supplier) {
        try {
            return callAs(withUser, supplier::get);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to handle all rollouts", e);
        }
    }

    public static void runAs(final WithUser withUser, final Runnable runnable) {
        getAs(withUser, (Supplier<? extends Object>) () -> {
            runnable.run();
            return null;
        });
    }

    public static WithUser withController(final String principal, final String... authorities) {
        return withUserAndTenant(DEFAULT_TENANT, principal, authorities, true, true);
    }

    public static WithUser withUser(final String principal, final String... authorities) {
        return withUserAndTenant(DEFAULT_TENANT, principal, authorities, false, true);
    }

    public static WithUser withUserAndTenantAllSpPermissions(final String principal, final String tenant) {
        return withUserAndTenant(tenant, principal, new String[] { SpRole.TENANT_ADMIN }, false, true);
    }

    public static WithUser withUserAndTenant(
            final String tenant, final String principal, final String[] authorities,
            final boolean controller, final boolean autoCreateTenant) {
        return new WithUserImpl(tenant, principal, authorities, controller, autoCreateTenant);
    }

    private static void setSecurityContext(final WithUser annotation) {
        SecurityContextHolder.setContext(new WithUserSecurityContext(annotation));
    }

    private static void createTenant(final String tenantId) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        setSecurityContext(PRIVILEGED_USER);
        try {
            systemManagement.createTenantMetadata(tenantId);
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
    }

    // should be used only for test purposes and taking in account 'annotation' non-transient field in a Serializable
    @SuppressWarnings("java:S1948") // java:S1948 - see comments into the method
    static class WithUserSecurityContext implements SecurityContext {

        @Serial
        private static final long serialVersionUID = 1L;

        // in some cases it could be serializable, e.g. if got via {@link java.lang.reflect.AnnotatedElement} (see javadoc) or WithUserImpl,
        // and in some cases it used to be serialized, e.g. in {@link SecurityContextSerializer#JavaSerialization.serialize},
        // must not be made transient!
        @SuppressWarnings("serial")
        private final WithUser annotation;

        WithUserSecurityContext(final WithUser annotation) {
            this.annotation = annotation;
            if (annotation.autoCreateTenant()) {
                createTenant(annotation.tenantId());
            }
        }

        @Override
        public Authentication getAuthentication() {
            final TestingAuthenticationToken testingAuthenticationToken = new TestingAuthenticationToken(
                    new TenantAwareUser(annotation.principal(), "***", null, annotation.tenantId()),
                    annotation.credentials(), annotation.authorities());
            testingAuthenticationToken.setDetails(
                    new TenantAwareAuthenticationDetails(annotation.tenantId(), annotation.controller()));
            return testingAuthenticationToken;
        }

        @Override
        public void setAuthentication(final Authentication authentication) {
            // nothing to do
        }

        @Override
        public int hashCode() {
            return annotation.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof WithUserSecurityContext otherSecurityContextWithUser) {
                return Objects.equals(annotation, otherSecurityContextWithUser.annotation);
            } else {
                return false;
            }
        }
    }

    private static class WithUserImpl implements WithUser, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String tenant;
        private final String principal;
        private final String[] authorities;
        private final boolean controller;
        private final boolean autoCreateTenant;

        private WithUserImpl(
                final String tenant, final String principal, final String[] authorities,
                final boolean controller, final boolean autoCreateTenant) {
            this.tenant = tenant;
            this.principal = principal;
            this.authorities = authorities;
            this.controller = controller;
            this.autoCreateTenant = autoCreateTenant;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return WithUser.class;
        }

        @Override
        public String tenantId() {
            return tenant;
        }

        @Override
        public String principal() {
            return principal;
        }

        @Override
        public String credentials() {
            return null;
        }

        @Override
        public String[] authorities() {
            return authorities;
        }

        @Override
        public boolean controller() {
            return controller;
        }

        @Override
        public boolean autoCreateTenant() {
            return autoCreateTenant;
        }
    }
}