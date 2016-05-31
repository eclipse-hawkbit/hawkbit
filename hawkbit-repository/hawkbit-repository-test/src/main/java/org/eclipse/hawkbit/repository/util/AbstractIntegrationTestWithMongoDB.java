/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.store.HttpProxyFactory;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * Test class that contains MonfoDb start and stop for the test
 *
 *
 *
 *
 */
public abstract class AbstractIntegrationTestWithMongoDB extends AbstractIntegrationTest {

    protected static volatile MongodExecutable mongodExecutable = null;
    private static final AtomicInteger mongoLease = new AtomicInteger(0);
    private static volatile Integer port;

    @Autowired
    protected GridFsOperations operations;

    @BeforeClass
    public static void setupMongo() throws UnknownHostException, IOException {
        mongoLease.incrementAndGet();
        if (mongodExecutable == null) {
            final Command command = Command.MongoD;

            final RuntimeConfigBuilder runtimeConfig = new RuntimeConfigBuilder().defaults(command);

            if (port == null) {
                port = new FreePortFileWriter(28017, 28090, "./target/freeports").getPort();
                System.setProperty("spring.data.mongodb.port", String.valueOf(port));
            }

            Version version = Version.V3_0_8;
            if (System.getProperty("inf.mongodb.version") != null) {
                version = Version
                        .valueOf("V" + System.getProperty("inf.mongodb.version").trim().replaceAll("\\.", "_"));
            }

            if (System.getProperty("http.proxyHost") != null) {
                runtimeConfig
                        .artifactStore(
                                new ArtifactStoreBuilder().defaults(command)
                                        .download(new DownloadConfigBuilder().defaultsForCommand(command)
                                                .proxyFactory(new HttpProxyFactory(
                                                        System.getProperty("http.proxyHost").trim(), Integer
                                                                .valueOf(System.getProperty("http.proxyPort"))))));
            }

            final IMongodConfig mongodConfig = new MongodConfigBuilder().version(version)
                    .net(new Net("127.0.0.1", port, Network.localhostIsIPv6())).build();

            final MongodStarter starter = MongodStarter.getInstance(runtimeConfig.build());
            mongodExecutable = starter.prepare(mongodConfig);
            mongodExecutable.start();
        }

    }

    @After
    public void cleanCurrentCollection() {
        operations.delete(new Query());
    }

    public static void internalShutDownMongo() {
        if (mongodExecutable != null && mongoLease.decrementAndGet() <= 0) {
            mongodExecutable.stop();
            mongodExecutable = null;
        }
    }

    @AfterClass
    public static void shutdownMongo() throws UnknownHostException, IOException {
        if (mongodExecutable != null && mongoLease.decrementAndGet() <= 0) {
            mongodExecutable.stop();
            mongodExecutable = null;
        }
        port = null;
    }
}
