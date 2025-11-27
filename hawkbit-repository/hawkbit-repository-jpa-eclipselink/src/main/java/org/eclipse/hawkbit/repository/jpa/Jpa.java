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
import java.util.Iterator;
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

    // intentional, if it is a constant the compiler will inline it, we want to be changed with changing the JPA vendor lib
    @SuppressWarnings("java:S3400")
    public static char nativeQueryParamPrefix() {
        return '?';
    }

    public static <T> String formatNativeQueryInClause(final String name, final Collection<T> collection) {
        return formatEclipseLinkNativeQueryInClause(IntStream.range(0, collection.size()).mapToObj(i -> name + "_" + i).toList());
    }

    public static <T> void setNativeQueryInParameter(final Query query, final String name, final Collection<T> collection) {
        int i = 0;
        for (final Iterator<T> iterator = collection.iterator(); iterator.hasNext(); i++) {
            query.setParameter(name + "_" + i, iterator.next());
        }
    }

    private static String formatEclipseLinkNativeQueryInClause(final Collection<String> elements) {
        return "?" + String.join(",?", elements);
    }
}