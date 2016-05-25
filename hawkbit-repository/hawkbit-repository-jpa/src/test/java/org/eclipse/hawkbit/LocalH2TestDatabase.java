/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 */
public class LocalH2TestDatabase implements Testdatabase {

    private final static Logger LOG = LoggerFactory.getLogger(LocalH2TestDatabase.class);
    private final int port;
    private Server h2server;
    private boolean dbStarted;
    private String uri;

    public LocalH2TestDatabase(final int port) {
        super();
        this.port = port;
        createUri();
        initSystemProperties();
    }

    private final void initSystemProperties() {
        System.setProperty("spring.datasource.driverClassName", getDriverClassName());
        System.setProperty("spring.datasource.username", "");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("hawkbit.server.database", "H2");
    }

    private void dropAllObjects() {
        try (Connection connection = DriverManager.getConnection(uri)) {
            connection.prepareCall("DROP ALL OBJECTS;").execute();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void before() {
        try {
            startDatabase();
        } catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void after() {
        try {
            stopDatabase();
        } catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void startDatabase() throws SQLException, ClassNotFoundException, IOException {
        if (dbStarted) {
            return;
        }

        // Start H2 database for OpenFire
        h2server = Server
                .createTcpServer(
                        new String[] { "-tcpPort", String.valueOf(port), "-tcpAllowOthers", "-tcpShutdownForce" })
                .start();
        dbStarted = true;
        LOG.info("H2 Database started on port {} and uri {}", port, uri);
        dropAllObjects();
    }

    private final void createUri() {
        this.uri = "jdbc:h2:tcp://localhost:" + port + "/mem:SP" + UUID.randomUUID().toString() + ";MVCC=TRUE;"
                + "DB_CLOSE_DELAY=-1";
        System.setProperty("spring.datasource.url", uri);
    }

    private void stopDatabase() throws SQLException, ClassNotFoundException, IOException {
        if (!dbStarted) {
            return;
        }

        h2server.stop();
        h2server = null;
        dbStarted = false;
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
        }
    }

    @Override
    public String getDriverClassName() {
        return "org.h2.Driver";
    }

    @Override
    public String getUri() {
        return uri;
    }

}
