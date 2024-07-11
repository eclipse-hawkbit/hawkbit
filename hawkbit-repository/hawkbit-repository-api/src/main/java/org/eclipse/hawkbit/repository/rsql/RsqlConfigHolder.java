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
     * If RSQL operands and string values shall ignore case. If ignore case operators as
     * "aND" will be accepted and "x == ax" will match "x == aX"
     */
    @Value("${hawkbit.rsql.ignoreCase:true}")
    private boolean ignoreCase;
    @Autowired
    private RsqlVisitorFactory rsqlVisitorFactory;

    /**
     * @return The holder singleton instance.
     */
    public static RsqlConfigHolder getInstance() {
        return SINGLETON;
    }
}