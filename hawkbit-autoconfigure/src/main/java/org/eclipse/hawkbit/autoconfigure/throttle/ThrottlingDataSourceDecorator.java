/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.throttle;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import javax.sql.DataSource;

import org.eclipse.hawkbit.throttle.ConnectionThrottleGate;
import org.eclipse.hawkbit.throttle.TenantThrottle;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Layer-2 enforcement point (): thin {@link DelegatingDataSource} that routes
 * every connection acquisition through {@link ConnectionThrottleGate}. All logic (tenant exemption,
 * reentrancy bypass, permit acquire/release tied to {@link Connection#close()}) lives in the gate and
 * is unit-tested in hawkbit-core; this class is only the Spring/JDBC glue.
 */
public class ThrottlingDataSourceDecorator extends DelegatingDataSource {

    private final TenantThrottle throttle;
    private final Duration timeout;

    public ThrottlingDataSourceDecorator(final DataSource targetDataSource, final TenantThrottle throttle,
            final Duration timeout) {
        super(targetDataSource);
        this.throttle = throttle;
        this.timeout = timeout;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ConnectionThrottleGate.acquire(throttle, timeout, super::getConnection);
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return ConnectionThrottleGate.acquire(throttle, timeout, () -> super.getConnection(username, password));
    }
}
