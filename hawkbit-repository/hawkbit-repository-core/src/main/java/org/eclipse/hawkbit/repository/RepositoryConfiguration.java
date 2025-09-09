/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.hawkbit.im.authentication.Hierarchy;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.expression.EvaluationContext;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.DenyAllPermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.ObjectUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Default configuration that is common to all repository implementations.
 */
@Slf4j
@Configuration
@EnableMethodSecurity(proxyTargetClass = true, securedEnabled = true)
@EnableConfigurationProperties({ RepositoryProperties.class, ControllerPollProperties.class, TenantConfigurationProperties.class })
@PropertySource("classpath:/hawkbit-repository-defaults.properties")
public class RepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("java:S3358") // java:S3358 better readable this way
    RoleHierarchy roleHierarchy(
            // if configured replaces the hierarchy completely
            @Value("${org.eclipse.hawkbit.hierarchy:}") final String hierarchy,
            // if the "hierarchy" property is empty, and this property is configured it is appended to the default hierarchy
            @Value("${org.eclipse.hawkbit.hierarchy.ext:}") final String hierarchyExt) {
        return RoleHierarchyImpl.fromHierarchy(
                ObjectUtils.isEmpty(hierarchy)
                        ? (ObjectUtils.isEmpty(hierarchyExt) ? Hierarchy.DEFAULT : Hierarchy.DEFAULT + hierarchyExt)
                        : hierarchy);
    }

    @Bean
    PermissionEvaluator permissionEvaluator(final RoleHierarchy roleHierarchy) {
        return new DenyAllPermissionEvaluator() {

            @Override
            public boolean hasPermission(final Authentication authentication, final Object targetDomainObject, final Object permission) {
                if (targetDomainObject instanceof MethodSecurityExpressionOperations root &&
                        root.getThis() instanceof PermissionSupport permissionSupport) {
                    final String neededPermission = permission + "_" + permissionSupport.permissionGroup();

                    // do permissions check
                    final boolean hasPermission = roleHierarchy.getReachableGrantedAuthorities(authentication.getAuthorities()).stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(authority -> authority.equals(neededPermission));
                    if (!hasPermission) {
                        log.debug(
                                "User {} does not have permission {} for target {}",
                                authentication.getName(), neededPermission, targetDomainObject);
                    }

                    return hasPermission;
                } else {
                    return super.hasPermission(authentication, targetDomainObject, permission);
                }
            }
        };
    }

    @Bean
    @Primary
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            final RoleHierarchy roleHierarchy, final PermissionEvaluator permissionEvaluator,
            final Optional<ApplicationContext> applicationContext) {
        final DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler = new DefaultMethodSecurityExpressionHandler() {

            @Override
            public EvaluationContext createEvaluationContext(final Supplier<Authentication> authentication, final MethodInvocation mi) {
                return super.createEvaluationContext(SingletonSupplier.of(() -> new RawAuthoritiesAuthentication(authentication.get())), mi);
            }

            @Override
            protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
                    final Authentication authentication, final MethodInvocation mi) {
                return super.createSecurityExpressionRoot(new RawAuthoritiesAuthentication(authentication), mi);
            }
        };
        methodSecurityExpressionHandler.setRoleHierarchy(roleHierarchy);
        methodSecurityExpressionHandler.setPermissionEvaluator(permissionEvaluator);
        applicationContext.ifPresent(methodSecurityExpressionHandler::setApplicationContext);
        return methodSecurityExpressionHandler;
    }

    private static class RawAuthoritiesAuthentication implements Authentication {

        private final Authentication authentication;
        private final transient SingletonSupplier<List<? extends GrantedAuthority>> rawAuthoritiesSupplier;

        public RawAuthoritiesAuthentication(final Authentication authentication) {
            this.authentication = authentication;
            rawAuthoritiesSupplier = SingletonSupplier.of(
                    () -> authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)// get the authority
                            .map(authority -> {
                                // permissions are in the format UPDATE_TARGET(/<rsql query>).
                                // here we remove the rsql query - not supported by expression evaluation
                                // the rsql evaluation will be done later by the access controller
                                final int index = authority.indexOf('/');
                                return index < 0 ? authority : authority.substring(0, index);
                            })
                            .distinct() // remove duplicates if any
                            .map(SimpleGrantedAuthority::new)
                            .toList());
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return rawAuthoritiesSupplier.get();
        }

        @Override
        public Object getCredentials() {
            return authentication.getCredentials();
        }

        @Override
        public Object getDetails() {
            return authentication.getDetails();
        }

        @Override
        public Object getPrincipal() {
            return authentication.getPrincipal();
        }

        @Override
        public boolean isAuthenticated() {
            return authentication.isAuthenticated();
        }

        @Override
        public void setAuthenticated(final boolean isAuthenticated) throws IllegalArgumentException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return authentication.getName();
        }
    }
}