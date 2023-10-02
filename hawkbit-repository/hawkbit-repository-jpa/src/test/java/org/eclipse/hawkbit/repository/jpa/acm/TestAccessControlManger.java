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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.controller.AccessController;
import org.springframework.data.jpa.domain.Specification;

public class TestAccessControlManger {

    final List<AccessRule<?>> accessRules = new ArrayList<>();
    final List<OperationDescriber<?>> operationDescribers = new ArrayList<>();

    public void deleteAllRules() {
        accessRules.clear();
        operationDescribers.clear();
    }

    public <T> Specification<T> getAccessRule(final Class<T> ruleClass, final AccessController.Operation operation) {
        return accessRules.stream()
                .filter(rule -> rule.getRuleClass().isAssignableFrom(ruleClass)
                        && rule.getOperation().equals(operation))
                .map(AccessRule::getSpecification).map(s -> (Specification<T>) s).findFirst()
                .orElseGet(() -> Specification.where(null));
    }

    public <T> void defineAccessRule(final Class<T> ruleClass, final AccessController.Operation operation,
            final Specification<T> specification) {
        final List<AccessRule<?>> list = accessRules.stream().filter(
                rule -> rule.getRuleClass().isAssignableFrom(ruleClass) && rule.getOperation().equals(operation))
                .toList();
        list.forEach(accessRules::remove);
        accessRules.add(new AccessRule<T>(ruleClass, operation, specification));
    }

    public <T> void assertOperation(final AccessController.Operation operation, final List<T> entities) {
        final boolean verificationResult = entities.stream().allMatch(entity -> {
            return operationDescribers.stream().filter(rule -> rule.getOperation().equals(operation))
                    .filter(rule -> rule.getEntity().isAssignableFrom(entity.getClass()))
                    .anyMatch(rule -> ((Predicate<T>) rule.getEntityIdentifier()).test(entity));
        });
        if (!verificationResult) {
            throw new InsufficientPermissionException();
        }
    }

    public <T> void permitOperation(final Class<T> ruleClass, final AccessController.Operation operation,
            final Predicate<T> entityIdentifier) {
        final List<OperationDescriber<?>> list = operationDescribers.stream()
                .filter(rule -> rule.getOperation().equals(operation)).toList();
        list.forEach(operationDescribers::remove);
        operationDescribers.add(new OperationDescriber<T>(ruleClass, entityIdentifier, operation));
    }

    public static class AccessRule<T> {
        private final Class<T> ruleClass;
        private final AccessController.Operation operation;
        private final Specification<T> specification;

        public AccessRule(final Class<T> ruleClass, final AccessController.Operation operation,
                final Specification<T> specification) {
            this.ruleClass = ruleClass;
            this.operation = operation;
            this.specification = specification;
        }

        public Class<T> getRuleClass() {
            return ruleClass;
        }

        public AccessController.Operation getOperation() {
            return operation;
        }

        public Specification<T> getSpecification() {
            return specification;
        }
    }

    public static class OperationDescriber<T> {
        private final Class<T> entity;
        private final Predicate<T> entityIdentifier;
        private final AccessController.Operation operation;

        public OperationDescriber(final Class<T> entity, final Predicate<T> entityIdentifier,
                final AccessController.Operation operation) {
            this.entity = entity;
            this.entityIdentifier = entityIdentifier;
            this.operation = operation;
        }

        public Class<T> getEntity() {
            return entity;
        }

        public Predicate<T> getEntityIdentifier() {
            return entityIdentifier;
        }

        public AccessController.Operation getOperation() {
            return operation;
        }

    }

}
