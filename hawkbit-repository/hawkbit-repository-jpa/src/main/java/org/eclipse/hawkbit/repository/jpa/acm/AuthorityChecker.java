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

import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.qfields.DistributionSetFields;
import org.eclipse.hawkbit.repository.qfields.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.eclipse.hawkbit.repository.qfields.TargetTagFields;

// utility class to validate authorities when ACM is enabled
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthorityChecker {

    private static final Set<String> ALL_AUTHORITIES = SpPermission.getAllTenantAuthorities();

    public static String[] validateAuthorities(final String... authorities) {
        for (final String authority : authorities) {
            validateAuthority(authority);
        }
        return authorities;
    }

    public static void validateAuthority(final String authority) {
        final int index = authority.indexOf('/');
        final String unscopedPermission = index > 0 ? authority.substring(0, index) : authority;
        if (index > 0) {
            validateScope(group(unscopedPermission), authority.substring(index + 1), authority);
        }
        if (!ALL_AUTHORITIES.contains(unscopedPermission)) {
            throw new IllegalArgumentException(
                    "Unknown permission: " + unscopedPermission + (index > 0 ? " (unscoped of " + authority + ")" : ""));
        }
    }

    private static String group(final String unscopedPermission) {
        if (unscopedPermission.startsWith(SpPermission.CREATE_PREFIX)) {
            return unscopedPermission.substring(SpPermission.CREATE_PREFIX.length());
        } else if (unscopedPermission.startsWith(SpPermission.READ_PREFIX)) {
            return unscopedPermission.substring(SpPermission.READ_PREFIX.length());
        } else if (unscopedPermission.startsWith(SpPermission.UPDATE_PREFIX)) {
            return unscopedPermission.substring(SpPermission.UPDATE_PREFIX.length());
        } else if (unscopedPermission.startsWith(SpPermission.DELETE_PREFIX)) {
            return unscopedPermission.substring(SpPermission.DELETE_PREFIX.length());
        } else {
            throw new IllegalArgumentException(unscopedPermission + " doesn't support targetTypeScope");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void validateScope(final String permission, final String rsql, final String authority) {
        // validate RSQL
        final Class<?> rsqlQueryFieldType;
        final Class<?> jpaType;
        switch (permission) {
            case SpPermission.TARGET -> {
                rsqlQueryFieldType = TargetFields.class;
                jpaType = JpaTarget.class;
            }
            case SpPermission.TARGET_TYPE -> {
                rsqlQueryFieldType = TargetTagFields.class;
                jpaType = JpaTarget.class;
            }
            case SpPermission.SOFTWARE_MODULE -> {
                rsqlQueryFieldType = SoftwareModuleFields.class;
                jpaType = JpaSoftwareModule.class;
            }
            case SpPermission.SOFTWARE_MODULE_TYPE -> {
                rsqlQueryFieldType = SoftwareModuleTypeFields.class;
                jpaType = JpaSoftwareModuleType.class;
            }
            case SpPermission.DISTRIBUTION_SET -> {
                rsqlQueryFieldType = DistributionSetFields.class;
                jpaType = JpaDistributionSet.class;
            }
            case SpPermission.DISTRIBUTION_SET_TYPE -> {
                rsqlQueryFieldType = DistributionSetTypeFields.class;
                jpaType = JpaTarget.class;
            }
            default -> throw new IllegalArgumentException(permission + " doesn't support targetTypeScope");
        }
        try {
            QLSupport.getInstance().validate(rsql, (Class) rsqlQueryFieldType, jpaType);
        } catch (final RuntimeException e) {
            throw new IllegalArgumentException(
                    "Scope of " + authority + " is not a valid RSQL for " + permission + ": " + e.getMessage(), e);
        }
    }
}