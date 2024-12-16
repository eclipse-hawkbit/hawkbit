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
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
public class Jpa {

    public enum JpaVendor {
        ECLIPSELINK,
        HIBERNATE
    }

    public static final JpaVendor JPA_VENDOR = JpaVendor.ECLIPSELINK;
    static {
        log.info("JPA vendor: {}", JPA_VENDOR);
    }

    public static final char NATIVE_QUERY_PARAMETER_PREFIX  = '?';

    public static <T> String formatNativeQueryInClause(final String name, final List<T> list) {
        return formatEclipseLinkNativeQueryInClause(IntStream.range(0, list.size()).mapToObj(i -> name + "_" + i).toList());
    }

    public static <T> void setNativeQueryInParameter(final Query deleteQuery, final String name, final List<T> list) {
        for (int i = 0, len = list.size(); i < len; i++) {
            deleteQuery.setParameter(name + "_" + i, list.get(i));
        }
    }

    private static String formatEclipseLinkNativeQueryInClause(final Collection<String> elements) {
        return "?" + String.join(",?", elements);
    }
}