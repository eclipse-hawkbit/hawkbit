/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { AbstractAccessControllerTest.AccessControlTestConfig.class })
public abstract class AbstractAccessControllerTest extends AbstractJpaIntegrationTest {

    @Autowired
    protected TestAccessControlManger testAccessControlManger;

    protected static <T> List<T> merge(final List<T> lists0, final List<T> list1) {
        final List<T> merge = new ArrayList<>(lists0);
        merge.addAll(list1);
        return merge;
    }

    protected void permitAllOperations(final AccessController.Operation operation) {
        testAccessControlManger.defineAccessRule(JpaTarget.class, operation, Specification.where(null), type -> true);
        testAccessControlManger.defineAccessRule(JpaTargetType.class, operation, Specification.where(null), type -> true);
        testAccessControlManger.defineAccessRule(JpaDistributionSet.class, operation, Specification.where(null), type -> true);
    }

    @BeforeEach
    void beforeEach() {
        testAccessControlManger.deleteAllRules();
    }

    @AfterEach
    void afterEach() {
        testAccessControlManger.deleteAllRules();
    }

    public static class AccessControlTestConfig {

        private final ContextAware contextAware = new SecurityContextTenantAware((tenant, username) -> List.of());

        @Bean
        public ContextAware contextAware() {
            return contextAware;
        }

        @Bean
        public TestAccessControlManger accessControlTestManger() {
            return new TestAccessControlManger();
        }

        @Bean
        public AccessController<JpaTarget> targetAccessController(final TestAccessControlManger testAccessControlManger) {
            return new AccessController<>() {

                @Override
                public Optional<Specification<JpaTarget>> getAccessRules(final Operation operation) {
                    if (contextAware.getCurrentTenant() != null
                            && SecurityContextTenantAware.SYSTEM_USER.equals(contextAware.getCurrentUsername())) {
                        // as tenant, no restrictions
                        return Optional.empty();
                    }

                    return Optional.ofNullable(testAccessControlManger.getAccessRule(JpaTarget.class, operation));
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final JpaTarget entity) throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(JpaTarget.class, operation, List.of(entity));
                }

                @Override
                public String toString() {
                    return AccessController.class.getSimpleName() + '<' + JpaTarget.class.getSimpleName() + '>';
                }
            };
        }

        @Bean
        public AccessController<JpaTargetType> targetTypeAccessController(final TestAccessControlManger testAccessControlManger) {
            return new AccessController<>() {

                @Override
                public Optional<Specification<JpaTargetType>> getAccessRules(final Operation operation) {
                    if (contextAware.getCurrentTenant() != null
                            && SecurityContextTenantAware.SYSTEM_USER.equals(contextAware.getCurrentUsername())) {
                        // as tenant, no restrictions
                        return Optional.empty();
                    }

                    return Optional.ofNullable(testAccessControlManger.getAccessRule(JpaTargetType.class, operation));
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final JpaTargetType entity)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(JpaTargetType.class, operation, List.of(entity));
                }

                @Override
                public String toString() {
                    return AccessController.class.getSimpleName() + '<' + JpaTargetType.class.getSimpleName() + '>';
                }
            };
        }

        @Bean
        public AccessController<JpaDistributionSet> distributionSetAccessController(final TestAccessControlManger testAccessControlManger) {
            return new AccessController<>() {

                @Override
                public Optional<Specification<JpaDistributionSet>> getAccessRules(final Operation operation) {
                    if (contextAware.getCurrentTenant() != null
                            && SecurityContextTenantAware.SYSTEM_USER.equals(contextAware.getCurrentUsername())) {
                        // as tenant, no restrictions
                        return Optional.empty();
                    }

                    return Optional.ofNullable(testAccessControlManger.getAccessRule(JpaDistributionSet.class, operation));
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final JpaDistributionSet entity)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(JpaDistributionSet.class, operation, List.of(entity));
                }

                @Override
                public String toString() {
                    return AccessController.class.getSimpleName() + '<' + JpaDistributionSet.class.getSimpleName() + '>';
                }
            };
        }
    }
}