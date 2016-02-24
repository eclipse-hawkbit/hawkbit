/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
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
 *
 *
 */
public class MongoDBTestRule implements TestRule {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBTestRule.class);
    private static volatile MongodExecutable mongodExecutable = null;
    private static volatile MongodProcess mongod;
    private final String id = UUID.randomUUID().toString();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return statement(base, description);
    }

    private Statement statement(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before(base, description);
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    private void after() {
        if (mongodExecutable != null) {
            LOG.info("Stop MongoDB...");
            mongodExecutable.stop();
            mongodExecutable = null;
            if (mongod != null) {
                mongod.stop();
                mongod = null;
            }
            LOG.info("MongoDB stopped... {}", id);
        }
    }

    private void before(final Statement base, final Description description) throws UnknownHostException, IOException {
        final Command command = Command.MongoD;

        final RuntimeConfigBuilder runtimeConfig = new RuntimeConfigBuilder().defaults(command);

        int port = -1;
        if (System.getProperty("spring.data.mongodb.port") != null) {
            port = Integer.parseInt(System.getProperty("spring.data.mongodb.port"));
        } else {
            port = new FreePortFileWriter(27017, 27100, "./target/freeports").getPort();
            System.setProperty("spring.data.mongodb.port", String.valueOf(port));
        }

        Version version = Version.V3_0_8;
        if (System.getProperty("inf.mongodb.version") != null) {
            version = Version.valueOf("V" + System.getProperty("inf.mongodb.version").trim().replaceAll("\\.", "_"));
        }

        if (System.getProperty("http.proxyHost") != null) {
            runtimeConfig.artifactStore(new ArtifactStoreBuilder().defaults(command)
                    .download(new DownloadConfigBuilder().defaultsForCommand(command)
                            .proxyFactory(new HttpProxyFactory(System.getProperty("http.proxyHost").trim(),
                                    Integer.valueOf(System.getProperty("http.proxyPort"))))));
        }

        final IMongodConfig mongodConfig = new MongodConfigBuilder().version(version)
                .net(new Net("127.0.0.1", port, Network.localhostIsIPv6())).build();

        final MongodStarter starter = MongodStarter.getInstance(runtimeConfig.build());
        mongodExecutable = starter.prepare(mongodConfig);
        LOG.info("Start MongoDB...");
        mongod = mongodExecutable.start();

        final Net net = mongod.getConfig().net();
        LOG.info("MongoDB started id {} on bind ip :{} Port:{} and version {}", id, net.getBindIp(), net.getPort(),
                mongodConfig.version().toString());
    }
}
