/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.rsql;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Helper class providing static access to the RSQL configuration as managed the ignoreCase and
 * the {@link RsqlVisitorFactory} bean.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Getter
public final class RsqlConfigHolder {

    private static final RsqlConfigHolder SINGLETON = new RsqlConfigHolder();

    /**
     * If RSQL comparison operators shall ignore the case. If ignore case is <code>true</code>
     * "x == ax" will match "x == aX"
     */
    @Value("${hawkbit.rsql.ignoreCase:true}")
    private boolean ignoreCase;
    /**
     * Declares if the database is case-insensitive, by default assumes <code>false</code>. In case it is case-sensitive and,
     * {@link #ignoreCase} is set to <code>true</code> the SQL queries use upper case comparisons to ignore case.
     *
     * If the database is declared as case-sensitive and ignoreCase is set to <code>false</code> the RSQL queries shall use strict
     * syntax - i.e. 'and' instead of 'AND' / 'aND'. Otherwise, the queries would be case-insensitive regarding operators.
     */
    @Value("${hawkbit.rsql.caseInsensitiveDB:false}")
    private boolean caseInsensitiveDB;
    @Autowired
    private RsqlVisitorFactory rsqlVisitorFactory;

    @Deprecated
    @Value("${hawkbit.rsql.legacyRsqlVisitor:false}")
    private boolean legacyRsqlVisitor;

    /**
     * @return The holder singleton instance.
     */
    public static RsqlConfigHolder getInstance() {
        return SINGLETON;
    }
}