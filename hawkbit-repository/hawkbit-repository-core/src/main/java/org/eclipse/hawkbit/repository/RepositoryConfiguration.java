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

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.Hierarchy;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
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
import org.springframework.util.ObjectUtils;

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
            @Value("${hawkbit.hierarchy:}") final String hierarchy,
            // if the "hierarchy" property is empty, and this property is configured it is appended to the default hierarchy
            @Value("${hawkbit.hierarchy.ext:}") final String hierarchyExt) {
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
    @ConditionalOnMissingBean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            final RoleHierarchy roleHierarchy, final PermissionEvaluator permissionEvaluator,
            final Optional<ApplicationContext> applicationContext) {
        final DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler = new DefaultMethodSecurityExpressionHandler() {};
        methodSecurityExpressionHandler.setRoleHierarchy(roleHierarchy);
        methodSecurityExpressionHandler.setPermissionEvaluator(permissionEvaluator);
        applicationContext.ifPresent(methodSecurityExpressionHandler::setApplicationContext);
        return methodSecurityExpressionHandler;
    }
}