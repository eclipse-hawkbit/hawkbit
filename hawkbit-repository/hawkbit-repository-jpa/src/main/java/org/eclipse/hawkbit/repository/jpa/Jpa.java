/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.persistence.Query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Jpa {

    public enum JpaVendor {
        ECLIPSELINK,
        HIBERNATE // NOT SUPPORTED!
    }

    public static final JpaVendor JPA_VENDOR = JpaVendor.ECLIPSELINK;

    public static char NATIVE_QUERY_PARAMETER_PREFIX = switch (JPA_VENDOR) {
        case ECLIPSELINK -> '?';
        case HIBERNATE -> ':';
    };

    public static <T> String formatNativeQueryInClause(final String name, final List<T> list) {
        return switch (Jpa.JPA_VENDOR) {
            case ECLIPSELINK -> formatEclipseLinkNativeQueryInClause(IntStream.range(0, list.size()).mapToObj(i -> name + "_" + i).toList());
            case HIBERNATE -> ":" + name;
        };
    }

    public static <T> void setNativeQueryInParameter(final Query deleteQuery, final String name, final List<T> list) {
        if (Jpa.JPA_VENDOR == Jpa.JpaVendor.ECLIPSELINK) {
            for (int i = 0, len = list.size(); i < len; i++) {
                deleteQuery.setParameter(name + "_" + i, list.get(i));
            }
        } else if (Jpa.JPA_VENDOR == Jpa.JpaVendor.HIBERNATE) {
            deleteQuery.setParameter(name, list);
        }
    }

    private static String formatEclipseLinkNativeQueryInClause(final Collection<String> elements) {
        return "?" + String.join(",?", elements);
    }
}
