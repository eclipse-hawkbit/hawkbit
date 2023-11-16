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

import javax.persistence.criteria.CriteriaQuery;

public class TestAccessControlManger {

    private final Map<AccessRuleId<?>, AccessRule<?>> accessRules = new HashMap<>();

    public void deleteAllRules() {
        accessRules.clear();
    }

    public <T> void defineAccessRule(
            final Class<T> ruleClass, final AccessController.Operation operation,
            final Specification<T> specification, final Predicate<T> check) {
        accessRules.put(new AccessRuleId<T>(ruleClass, operation), new AccessRule<T>(specification, check));
    }

    public <T extends AbstractJpaBaseEntity> Specification<T> getAccessRule(final Class<T> ruleClass, final AccessController.Operation operation) {
        @SuppressWarnings("unchecked")
        final AccessRule<T> accessRule = (AccessRule<T>) accessRules.getOrDefault(new AccessRuleId<T>(ruleClass, operation), null);
        if (accessRule == null) {
            return nop();
        } else {
            return accessRule.specification();
        }
    }
    private static <T extends AbstractJpaBaseEntity> Specification<T> nop() {
        return (targetRoot, query, cb) -> cb.equal(targetRoot.get(AbstractJpaBaseEntity_.id), -1);
    }

    public <T> void assertOperation(final Class<T> ruleClass, final AccessController.Operation operation, final List<T> entities) {
        @SuppressWarnings("unchecked")
        final AccessRule<T> accessRule = (AccessRule<T>) accessRules.getOrDefault(new AccessRuleId<T>(ruleClass, operation), null);
        if (accessRule == null) {
            throw new InsufficientPermissionException("No access define - reject all");
        } else {
            for (final T entity : entities) {
                if (!accessRule.checker.test(entity)) {
                    throw new InsufficientPermissionException(
                            "Access to " + ruleClass.getName() + "/" + entity + " not allowed by checker!");
                }
            }
            return;
        }
    }

    private record AccessRuleId<T>(Class<T> ruleClass, AccessController.Operation operation) {}
    private record AccessRule<T> (Specification<T> specification, Predicate<T> checker) {}
}
