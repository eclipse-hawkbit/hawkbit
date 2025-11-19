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

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.context.ContextAware;
import org.eclipse.hawkbit.repository.qfields.DistributionSetFields;
import org.eclipse.hawkbit.repository.qfields.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.eclipse.hawkbit.repository.qfields.TargetTypeFields;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.context.SecurityContextSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.expression.EvaluationContext;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.function.SingletonSupplier;

@Configuration
@ConditionalOnProperty(name = "hawkbit.acm.access-controller.enabled", havingValue = "true")
public class AccessControllerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SecurityContextSerializer securityContextSerializer() {
        final SecurityContextSerializer serializer = SecurityContextSerializer.JSON_SERIALIZATION;
        ContextAware.setSecurityContextSerializer(serializer);
        return serializer;
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.target.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaTarget> targetAccessController() {
        return new DefaultAccessController<>(TargetFields.class, SpPermission.TARGET);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.action.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaAction> actionAccessController(final AccessController<JpaTarget> targetAccessController) {
        return new AccessController<>() {

            @Override
            @SuppressWarnings("unchecked")
            public Optional<Specification<JpaAction>> getAccessRules(final Operation operation) {
                return targetAccessController.getAccessRules(map(operation)).map(targetSpec -> (actionRoot, query, cb) -> {
                    final Join<JpaAction, JpaTarget> targetJoin = actionRoot.join(JpaAction_.target);
                    final EntityType<JpaTarget> targetModel = query.from(JpaTarget.class).getModel();
                    final Root<JpaTarget> targetRoot = (Root<JpaTarget>) Proxy.newProxyInstance(
                            actionRoot.getClass().getClassLoader(),
                            new Class[] { Root.class },
                            (proxy, method, args) -> {
                                if (method.getName().equals("getModel") && method.getParameterCount() == 0) {
                                    return targetModel;
                                } else if (method.getDeclaringClass().isAssignableFrom(From.class)) {
                                    return method.invoke(targetJoin, args);
                                } else {
                                    return method.invoke(this, args);
                                }
                            });
                    return targetSpec.toPredicate(targetRoot, query, cb);
                });
            }

            @Override
            public void assertOperationAllowed(final Operation operation, final JpaAction entity) throws InsufficientPermissionException {
                targetAccessController.assertOperationAllowed(map(operation), entity.getTarget());
            }

            // all CREATE/UPDATE/DELETE action operations are mapped to UPDATE_TARGET permissions / actions
            private static Operation map(final Operation actionOperation) {
                return switch (actionOperation) {
                    case READ -> Operation.READ;
                    case CREATE, UPDATE, DELETE -> Operation.UPDATE;
                };
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.target-type.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaTargetType> targetTypeAccessController() {
        return new DefaultAccessController<>(TargetTypeFields.class, SpPermission.TARGET_TYPE);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.software-module.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaSoftwareModule> softwareModuleAccessController() {
        return new DefaultAccessController<>(SoftwareModuleFields.class, SpPermission.SOFTWARE_MODULE);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.software-module-type.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaSoftwareModuleType> softwareModuleTypeAccessController() {
        return new DefaultAccessController<>(SoftwareModuleTypeFields.class, SpPermission.SOFTWARE_MODULE_TYPE);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.distribution-set.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaDistributionSet> distributionSetAccessController() {
        return new DefaultAccessController<>(DistributionSetFields.class, SpPermission.DISTRIBUTION_SET);
    }

    @Bean
    @ConditionalOnProperty(name = "hawkbit.acm.access-controller.distribution-set-type.enabled", havingValue = "true", matchIfMissing = true)
    AccessController<JpaDistributionSetType> distributionSetTypeAccessController() {
        return new DefaultAccessController<>(DistributionSetTypeFields.class, SpPermission.DISTRIBUTION_SET_TYPE);
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
