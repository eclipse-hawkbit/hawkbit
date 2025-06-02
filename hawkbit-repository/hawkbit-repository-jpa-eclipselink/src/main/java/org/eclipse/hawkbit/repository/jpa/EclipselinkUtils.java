/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import jakarta.persistence.Query;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.JpaQuery;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
public class EclipselinkUtils {

    public static String toSql(final Query query) {
        query.setParameter(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, "DEFAULT");
        // executes the query - otherwise the SQL string is not generated
        query.getResultList();
        return query.unwrap(JpaQuery.class).getDatabaseQuery().getSQLString();
    }
}