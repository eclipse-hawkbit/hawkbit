/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import java.util.List;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.acm.controller.TargetTypeAccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { AbstractAccessControllingTest.AccessControlTestConfig.class })
public abstract class AbstractAccessControllingTest extends AbstractJpaIntegrationTest {

    @Autowired
    protected TestAccessControlManger testAccessControlManger;

    @AfterEach
    void cleanup() {
        testAccessControlManger.deleteAllRules();
    }

    public static class AccessControlTestConfig {

        @Bean
        public TestAccessControlManger accessControlTestManger() {
            return new TestAccessControlManger();
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

    }

}
