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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { AbstractAccessControllerTest.AccessControlTestConfig.class })
public abstract class AbstractAccessControllerTest extends AbstractJpaIntegrationTest {

    @Autowired
    protected TestAccessControlManger testAccessControlManger;

    @BeforeEach
    void cleanup() {
        testAccessControlManger.deleteAllRules();
    }

    protected void permitAllOperations(final AccessController.Operation operation) {
        testAccessControlManger.defineAccessRule(
                JpaTarget.class, operation, Specification.where(null), type -> true);
        testAccessControlManger.defineAccessRule(
                JpaTargetType.class, operation, Specification.where(null), type -> true);
        testAccessControlManger.defineAccessRule(
                JpaDistributionSet.class, operation, Specification.where(null), type -> true);
    }

    public static class AccessControlTestConfig {

        @Bean
        public TestAccessControlManger accessControlTestManger() {
            return new TestAccessControlManger();
        }

        @Bean
        public AccessController<JpaTarget, Long> targetAccessController(final TestAccessControlManger testAccessControlManger) {
            return new AccessControllerBase<>() {

                @Override
                public void assertOperationAllowed(final Operation operation) throws InsufficientPermissionException {
                  // TODO
                }

                @Override
                public Optional<Specification<JpaTarget>> getAccessRules(final Operation operation) {
                    return Optional.ofNullable(testAccessControlManger.getAccessRule(JpaTarget.class, operation));
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final Supplier<JpaTarget> entitySupplier)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(JpaTarget.class, operation, List.of(entitySupplier.get()));
                }
            };
        }

        @Bean
        public AccessController<JpaTargetType, Long> targetTypeAccessController(
                final TestAccessControlManger testAccessControlManger) {
            return new AccessControllerBase<>() {
                @Override
                public void assertOperationAllowed(final Operation operation) throws InsufficientPermissionException {
                    // TODO
                }

                @Override
                public Optional<Specification<JpaTargetType>> getAccessRules(final Operation operation) {
                    return Optional.ofNullable(testAccessControlManger.getAccessRule(JpaTargetType.class, operation));
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final Supplier<JpaTargetType> entitySupplier)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(JpaTargetType.class, operation, List.of(entitySupplier.get()));
                }
            };
        }

        @Bean
        public AccessController<JpaDistributionSet, Long> distributionSetAccessController(
                final TestAccessControlManger testAccessControlManger) {
            return new AccessControllerBase<>() {
                @Override
                public void assertOperationAllowed(final Operation operation) throws InsufficientPermissionException {
                    // TODO
                }

                @Override
                public Optional<Specification<JpaDistributionSet>> getAccessRules(final Operation operation) {
                    return Optional.ofNullable( testAccessControlManger.getAccessRule(JpaDistributionSet.class, operation));
                }

                @Override
                public void assertOperationAllowed(final Operation operation, final Supplier<JpaDistributionSet> entitySupplier)
                        throws InsufficientPermissionException {
                    testAccessControlManger.assertOperation(JpaDistributionSet.class, operation, List.of(entitySupplier.get()));
                }
            };
        }
    }

    protected static <T> List<T> merge(final List<T> lists0, final List<T> list1) {
        final List<T> merge = new ArrayList<>(lists0);
        merge.addAll(list1);
        return merge;
    }

    private static abstract class AccessControllerBase<T extends Identifiable<Long>> implements AccessController<T, Long> {

        @Override
        public  void assertOperationAllowed(final AccessController.Operation operation, final Iterable<? extends T> entities) throws InsufficientPermissionException {
            final List<Long> entityIds = new ArrayList<>();
            final Map<Long, T> idToEntity = new HashMap<>();
            for (final T entity : entities) {
                entityIds.add(entity.getId());
                idToEntity.put(entity.getId(), entity);
            }
            assertOperationAllowed(operation, entityIds, idToEntity::get);
        }
    }
}
