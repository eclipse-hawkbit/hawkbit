/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.net.UnknownHostException;
import java.util.Arrays;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;

/**
 * {@link AbstractMongoConfiguration} that uses {@link MongoClientURI} even when
 * port is configured for NON {@link Cloud} use cases.
 *
 */
@Configuration
@EnableConfigurationProperties(MongoProperties.class)
@ConditionalOnClass(Mongo.class)
@ConditionalOnMissingBean(type = "org.springframework.data.mongodb.MongoDbFactory")
@Profile({ "!cloud" })
public class MongoConfiguration extends AbstractMongoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(MongoConfiguration.class);

    @Autowired
    private MongoProperties properties;

    @Autowired(required = false)
    private MongoClientOptions options;

    private Mongo mongo;

    @Override
    public String getDatabaseName() {
        return properties.getMongoClientDatabase();
    }

    /**
     * Closes mongo client when destroyd.
     */
    @PreDestroy
    public void close() {
        if (this.mongo != null) {
            this.mongo.close();
        }
    }

    @Override
    @Bean
    @ConditionalOnMissingBean
    public Mongo mongo() throws UnknownHostException {
        final MongoClientURI uri = new MongoClientURI(properties.getUri(), createBuilderOutOfOptions(options));

        if (properties.getPort() != null) {
            LOG.debug("Create mongo by properties (host: {}, port: {})", uri.getHosts().get(0), properties.getPort());
            this.mongo = new MongoClient(Arrays.asList(new ServerAddress(uri.getHosts().get(0), properties.getPort())),
                    uri.getOptions());
        } else {
            LOG.debug("Create mongo by URI : {}", uri);
            this.mongo = new MongoClient(uri);
        }

        return this.mongo;
    }

    /*
     * Creates {@link MongoClientOptions} builder out of existing options as the
     * {@link MongoClientURI} expects a builder.
     *
     * Based on MongoProperties#builder method.
     */
    private Builder createBuilderOutOfOptions(final MongoClientOptions options) {
        final Builder builder = MongoClientOptions.builder();
        if (options != null) {
            builder.alwaysUseMBeans(options.isAlwaysUseMBeans());
            builder.connectionsPerHost(options.getConnectionsPerHost());
            builder.connectTimeout(options.getConnectTimeout());
            builder.cursorFinalizerEnabled(options.isCursorFinalizerEnabled());
            builder.dbDecoderFactory(options.getDbDecoderFactory());
            builder.dbEncoderFactory(options.getDbEncoderFactory());
            builder.description(options.getDescription());
            builder.maxWaitTime(options.getMaxWaitTime());
            builder.readPreference(options.getReadPreference());
            builder.serverSelectionTimeout(options.getServerSelectionTimeout());
            builder.socketFactory(options.getSocketFactory());
            builder.socketKeepAlive(options.isSocketKeepAlive());
            builder.socketTimeout(options.getSocketTimeout());
            builder.threadsAllowedToBlockForConnectionMultiplier(options
                    .getThreadsAllowedToBlockForConnectionMultiplier());
            builder.writeConcern(options.getWriteConcern());
        }
        return builder;
    }
}
