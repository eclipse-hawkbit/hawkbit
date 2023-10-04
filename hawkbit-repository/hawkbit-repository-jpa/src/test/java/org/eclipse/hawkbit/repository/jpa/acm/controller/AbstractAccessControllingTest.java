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

import java.util.List;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { AbstractAccessControllingTest.AccessControlTestConfig.class })
public abstract class AbstractAccessControllingTest extends AbstractJpaIntegrationTest {

    @Autowired
    protected TestAccessControlManger testAccessControlManger;

    @BeforeEach
    void cleanup() {
        testAccessControlManger.deleteAllRules();
    }

    protected void permitAllOperations(final AccessController.Operation operation) {
        testAccessControlManger.permitOperation(JpaTarget.class, operation, type -> true);
        testAccessControlManger.permitOperation(JpaTargetType.class, operation, type -> true);
        testAccessControlManger.permitOperation(JpaDistributionSet.class, operation, type -> true);
    }

    public static class AccessControlTestConfig {

        @Bean
        public TestAccessControlManger accessControlTestManger() {
            return new TestAccessControlManger();
        }

        @Bean
        public TargetAccessController targetAccessController(final TestAccessControlManger testAccessControlManger) {
            return new TargetAccessController() {
                @Override
                public Specification<JpaTarget> getAccessRules(final Operation operation) {
                    return testAccessControlManger.getAccessRule(JpaTarget.class, operation);
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final List<JpaTarget> entities)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(operation, entities);
                }
            };
        }

        @Bean
        public TargetTypeAccessController targetTypeAccessController(
                final TestAccessControlManger testAccessControlManger) {
            return new TargetTypeAccessController() {
                @Override
                public Specification<JpaTargetType> getAccessRules(final Operation operation) {
                    return testAccessControlManger.getAccessRule(JpaTargetType.class, operation);
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final List<JpaTargetType> entities)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(operation, entities);
                }
            };
        }

        @Bean
        public DistributionSetAccessController distributionSetAccessController(
                final TestAccessControlManger testAccessControlManger) {
            return new DistributionSetAccessController() {
                @Override
                public Specification<JpaDistributionSet> getAccessRules(final Operation operation) {
                    return testAccessControlManger.getAccessRule(JpaDistributionSet.class, operation);
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final List<JpaDistributionSet> entities)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(operation, entities);
                }
            };
        }

    }

}
