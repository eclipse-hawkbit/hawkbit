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

import org.eclipse.hawkbit.repository.jpa.acm.controller.AccessController;
import org.springframework.data.jpa.domain.Specification;

public class TestAccessControlManger {

    final List<AccessRule<?>> accessRules = new ArrayList<>();

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

}
