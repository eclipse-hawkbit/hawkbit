/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * A {@link DataSource} wrapper that records every SQL statement executed through it into a {@link QueryCount}.
 * <p>
 * Works at the JDBC layer via dynamic proxies on {@link Connection} and {@link Statement}, so it is completely
 * JPA-provider agnostic (both EclipseLink and Hibernate issue their SQL through this data source). The SQL of a
 * {@link PreparedStatement}/{@link CallableStatement} is captured at prepare time; the SQL of a plain
 * {@link Statement} is captured from its {@code execute*(sql)} argument.
 */
public class QueryCountingDataSource extends DelegatingDataSource {

    private final QueryCount queryCount;

    public QueryCountingDataSource(final DataSource targetDataSource, final QueryCount queryCount) {
        super(targetDataSource);
        this.queryCount = queryCount;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return proxyConnection(super.getConnection());
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return proxyConnection(super.getConnection(username, password));
    }

    private Connection proxyConnection(final Connection connection) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class },
                new ConnectionHandler(connection));
    }

    private class ConnectionHandler implements InvocationHandler {

        private final Connection connection;

        private ConnectionHandler(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final Object result = invokeTarget(connection, method, args);
            if (result instanceof Statement statement) {
                // prepareStatement/prepareCall carry the SQL as first arg; createStatement has none (SQL at execute time)
                final String preparedSql = (args != null && args.length > 0 && args[0] instanceof String sql) ? sql : null;
                return Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[] { statementInterface(method) }, new StatementHandler(statement, preparedSql));
            }
            return result;
        }

        private Class<?> statementInterface(final Method factoryMethod) {
            final String name = factoryMethod.getName();
            if (name.equals("prepareCall")) {
                return CallableStatement.class;
            }
            return name.equals("prepareStatement") ? PreparedStatement.class : Statement.class;
        }
    }

    private class StatementHandler implements InvocationHandler {

        private final Statement statement;
        private final String preparedSql;

        private StatementHandler(final Statement statement, final String preparedSql) {
            this.statement = statement;
            this.preparedSql = preparedSql;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.getName().startsWith("execute")) {
                // plain Statement passes the SQL as first arg; PreparedStatement uses the SQL captured at prepare time
                final String sql = (args != null && args.length > 0 && args[0] instanceof String s) ? s : preparedSql;
                queryCount.record(sql);
            }
            return invokeTarget(statement, method, args);
        }
    }

    private static Object invokeTarget(final Object target, final Method method, final Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
