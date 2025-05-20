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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.springframework.data.jpa.domain.Specification;

public class TestAccessControlManger {

    private final Map<AccessRuleId<?>, AccessRule<?>> accessRules = new HashMap<>();

    public void deleteAllRules() {
        accessRules.clear();
    }

    public <T> void defineAccessRule(
            final Class<T> ruleClass, final AccessController.Operation operation,
            final Specification<T> specification, final Predicate<T> check) {
        defineAccessRule(ruleClass, operation, specification, check, false);
    }

    public <T> void overwriteAccessRule(
            final Class<T> ruleClass, final AccessController.Operation operation,
            final Specification<T> specification, final Predicate<T> check) {
        defineAccessRule(ruleClass, operation, specification, check, true);
    }

    private <T> void defineAccessRule(
            final Class<T> ruleClass, final AccessController.Operation operation,
            final Specification<T> specification, final Predicate<T> check, final boolean overwrite) {
        final AccessRuleId<T> ruleId = new AccessRuleId<>(ruleClass, operation);
        if (!overwrite && accessRules.containsKey(ruleId)) {
            throw new IllegalStateException("Access rule already defined for " + ruleId + "! You should explicitly set overwrite to true.");
        }
        accessRules.put(ruleId, new AccessRule<>(specification, check));
    }

    public <T extends AbstractJpaBaseEntity> Specification<T> getAccessRule(final Class<T> ruleClass,
            final AccessController.Operation operation) {
        @SuppressWarnings("unchecked")
        final AccessRule<T> accessRule = (AccessRule<T>) accessRules.getOrDefault(new AccessRuleId<>(ruleClass, operation), null);
        if (accessRule == null) {
            return nop();
        } else {
            return accessRule.specification();
        }
    }

    public <T> void assertOperation(final Class<T> ruleClass, final AccessController.Operation operation, final List<T> entities) {
        @SuppressWarnings("unchecked")
        final AccessRule<T> accessRule = (AccessRule<T>) accessRules.getOrDefault(new AccessRuleId<>(ruleClass, operation), null);
        if (accessRule == null) {
            throw new InsufficientPermissionException("No access define - reject all");
        } else {
            for (final T entity : entities) {
                if (!accessRule.checker.test(entity)) {
                    throw new InsufficientPermissionException("Access to " + ruleClass.getName() + "/" + entity + " not allowed by checker!");
                }
            }
        }
    }

    private static <T extends AbstractJpaBaseEntity> Specification<T> nop() {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(AbstractJpaBaseEntity_.id), -1);
    }

    private record AccessRuleId<T>(Class<T> ruleClass, AccessController.Operation operation) {}

    private record AccessRule<T>(Specification<T> specification, Predicate<T> checker) {}
}