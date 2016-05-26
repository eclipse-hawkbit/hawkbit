/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.jpa.repository.model.helper.SystemManagementHolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class WithSpringAuthorityRule implements TestRule {

    /*
     * (non-Javadoc)
     * 
     * @see org.junit.rules.TestRule#apply(org.junit.runners.model.Statement,
     * org.junit.runner.Description)
     */
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final SecurityContext oldContext = before(description);
                try {
                    base.evaluate();
                } finally {
                    after(oldContext);
                }
            }
        };
    }

    private SecurityContext before(final Description description) throws Throwable {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        WithUser annotation = description.getAnnotation(WithUser.class);
        if (annotation == null) {
            annotation = description.getTestClass().getAnnotation(WithUser.class);
        }
        if (annotation != null) {
            setSecurityContext(annotation);
            if (annotation.autoCreateTenant()) {
                SystemManagementHolder.getInstance().getSystemManagement().getTenantMetadata(annotation.tenantId());
            }
        }
        return oldContext;
    }

    /**
     * @param annotation
     */
    private void setSecurityContext(final WithUser annotation) {
        SecurityContextHolder.setContext(new SecurityContext() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void setAuthentication(final Authentication authentication) {
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
                        annotation.principal(), annotation.credentials(), authorities);
                testingAuthenticationToken
                        .setDetails(new TenantAwareAuthenticationDetails(annotation.tenantId(), false));
                return testingAuthenticationToken;
            }

            private String[] getAllAuthorities(final String[] additionalAuthorities, final String[] notInclude) {
                final List<String> allPermissions = new ArrayList<>();
                final Field[] declaredFields = SpPermission.class.getDeclaredFields();
                for (final Field field : declaredFields) {
                    if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        try {
                            boolean addPermission = true;
                            final String permissionName = (String) field.get(null);
                            if (notInclude != null) {
                                for (final String notInlcudePerm : notInclude) {
                                    if (permissionName.equals(notInlcudePerm)) {
                                        addPermission = false;
                                        break;
                                    }
                                }
                            }
                            if (addPermission) {
                                allPermissions.add(permissionName);
                            }
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            // nope
                        }
                    }
                }
                for (final String authority : additionalAuthorities) {
                    allPermissions.add(authority);
                }
                return allPermissions.toArray(new String[allPermissions.size()]);
            }
        });
    }

    private void after(final SecurityContext oldContext) {
        SecurityContextHolder.setContext(oldContext);
    }

    /**
     * @param callable
     * @return
     * @throws Exception
     */
    public <T> T runAsPrivileged(final Callable<T> callable) throws Exception {
        return runAs(privilegedUser(), callable);
    }

    /**
     * 
     * @param withUser
     * @param callable
     * @return
     * @throws Exception
     */
    public <T> T runAs(final WithUser withUser, final Callable<T> callable) throws Exception {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        setSecurityContext(withUser);
        if (withUser.autoCreateTenant()) {
            SystemManagementHolder.getInstance().getSystemManagement().getTenantMetadata(withUser.tenantId());
        }
        try {
            return callable.call();
        } finally {
            after(oldContext);
        }
    }

    public static WithUser withUser(final String principal, final String... authorities) {
        return withUserAndTenant(principal, "default", true, true, authorities);
    }
    
    public static WithUser withUser(final String principal, final boolean allSpPermision, final String... authorities) {
        return withUserAndTenant(principal, "default", true, allSpPermision, authorities);
    }

    public static WithUser withUser(final boolean autoCreateTenant) {
        return withUserAndTenant("bumlux", "default", autoCreateTenant, true, new String[] {});
    }

    public static WithUser withUserAndTenant(final String principal, final String tenant, final String... authorities) {
        return withUserAndTenant(principal, tenant, true, true, new String[] {});
    }

    public static WithUser withUserAndTenant(final String principal, final String tenant,
            final boolean autoCreateTenant, final boolean allSpPermission, final String... authorities) {
        return new WithUser() {

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
            public String[] authorities() {
                return authorities;
            }

            @Override
            public boolean allSpPermissions() {
                return allSpPermission;
            }

            @Override
            public String[] removeFromAllPermission() {
                return null;
            }

            @Override
            public String tenantId() {
                return tenant;
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.hawkbit.WithUser#autoCreateTenant()
             */
            @Override
            public boolean autoCreateTenant() {
                return autoCreateTenant;
            }
        };
    }

    private static WithUser privilegedUser() {
        return new WithUser() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return WithUser.class;
            }

            @Override
            public String principal() {
                return "bumlux";
            }

            @Override
            public String credentials() {
                return null;
            }

            @Override
            public String[] authorities() {
                return new String[] { "ROLE_CONTROLLER" };
            }

            @Override
            public boolean allSpPermissions() {
                return true;
            }

            @Override
            public String[] removeFromAllPermission() {
                return null;
            }

            @Override
            public String tenantId() {
                return "default";
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.hawkbit.WithUser#autoCreateTenant()
             */
            @Override
            public boolean autoCreateTenant() {
                return true;
            }
        };
    }
}
