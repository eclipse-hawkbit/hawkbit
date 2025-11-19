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

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.auth.SpPermission;
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
    private static final WithUser PRIVILEGED_USER = createWithUser(
            "bumlux", DEFAULT_TENANT, false, true, false, "ROLE_CONTROLLER", "ROLE_SYSTEM_CODE");

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

    public static <T> T callAsPrivileged(final Callable<T> callable) throws Exception {
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
        return withUserAndTenant(principal, DEFAULT_TENANT, true, false, true, authorities);
    }

    public static WithUser withUser(final String principal, final String... authorities) {
        return withUserAndTenant(principal, DEFAULT_TENANT, true, false, false, authorities);
    }

    public static WithUser withUserAndTenantAllSpPermissions(final String principal, final String tenant) {
        return withUserAndTenant(principal, tenant, true, true, false);
    }

    public static WithUser withUserAndTenant(final String principal, final String tenant,
            final boolean autoCreateTenant, final boolean allSpPermission, final boolean controller,
            final String... authorities) {
        return createWithUser(principal, tenant, autoCreateTenant, allSpPermission, controller, authorities);
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

    private static WithUser createWithUser(
            final String principal, final String tenant, final boolean autoCreateTenant,
            final boolean allSpPermission, final boolean controller, final String... authorities) {
        return new WithUserImpl(principal, tenant, autoCreateTenant, allSpPermission, controller, authorities);
    }

    // should be used only for test purposes and taking in account 'annotation' non-transient field in a Serializable
    @SuppressWarnings("java:S1948") // java:S1948 - see comments into the method
    static class WithUserSecurityContext implements SecurityContext {

        @Serial
        private static final long serialVersionUID = 1L;

        // in some cases it could be serializable, e.g. if got via {@link java.lang.reflect.AnnotatedElement} (see javadoc) or WithUserImpl,
        // and in some cases it used to be serialized, e.g. in {@link SecurityContextSerializer#JavaSerialization.serialize},
        // must not be made transient!
        private final WithUser annotation;

        WithUserSecurityContext(final WithUser annotation) {
            this.annotation = annotation;
            if (annotation.autoCreateTenant()) {
                createTenant(annotation.tenantId());
            }
        }

        @Override
        public Authentication getAuthentication() {
            final String[] authorities;
            if (annotation.allSpPermissions()) {
                authorities = getAllAuthorities(annotation.authorities(), annotation.removeFromAllPermission());
            } else {
                authorities = annotation.authorities();
            }
            final TestingAuthenticationToken testingAuthenticationToken = new TestingAuthenticationToken(
                    new TenantAwareUser(annotation.principal(), "***", null, annotation.tenantId()),
                    annotation.credentials(), authorities);
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

        private String[] getAllAuthorities(final String[] additionalAuthorities, final String[] notInclude) {
            final List<String> permissions = new ArrayList<>(SpPermission.getAllAuthorities()); // list is unmodifiable
            if (notInclude != null) {
                permissions.removeAll(Arrays.asList(notInclude));
            }
            if (additionalAuthorities != null) {
                permissions.addAll(Arrays.asList(additionalAuthorities));
            }
            return permissions.toArray(new String[0]);
        }
    }

    private static class WithUserImpl implements WithUser, Serializable {

        private final String principal;
        private final String tenant;
        private final boolean autoCreateTenant;
        private final boolean allSpPermission;
        private final boolean controller;
        private final String[] authorities;

        private WithUserImpl(
                final String principal, final String tenant, final boolean autoCreateTenant,
                final boolean allSpPermission, final boolean controller, final String... authorities) {
            this.principal = principal;
            this.tenant = tenant;
            this.autoCreateTenant = autoCreateTenant;
            this.allSpPermission = allSpPermission;
            this.controller = controller;
            this.authorities = authorities;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return WithUser.class;
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
        public String tenantId() {
            return tenant;
        }

        @Override
        public boolean autoCreateTenant() {
            return autoCreateTenant;
        }

        @Override
        public String[] authorities() {
            return authorities;
        }

        @Override
        public boolean allSpPermissions() {
            return allSpPermission;
        }

        @Override
        public String[] removeFromAllPermission() {
            return new String[0];
        }

        @Override
        public boolean controller() {
            return controller;
        }
    }
}