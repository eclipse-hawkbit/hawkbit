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

    public static final JpaVendor JPA_VENDOR = JpaVendor.HIBERNATE;
    static {
        log.info("JPA Vendor: {}", JPA_VENDOR);
    }

    // intentional, if it is a constant the compiler will inline it, we want to be changed with changing the JPA vendor lib
    @SuppressWarnings("java:S3400")
    public static char nativeQueryParamPrefix() {
        return ':';
    }

    @SuppressWarnings("java:S1172") // intentionally - it shall follow the common "interface"/signature of the method for all JPA providers
    public static <T> String formatNativeQueryInClause(final String name, final Collection<T> collection) {
        return ":" + name;
    }

    public static <T> void setNativeQueryInParameter(final Query query, final String name, final Collection<T> collection) {
        query.setParameter(name, collection);
    }
}