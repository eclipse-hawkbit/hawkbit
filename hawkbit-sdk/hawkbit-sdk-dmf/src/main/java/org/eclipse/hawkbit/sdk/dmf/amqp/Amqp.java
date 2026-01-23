/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.dmf.amqp;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Tenant.DMF;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.util.ObjectUtils;

/**
 * Abstract class for connecting to AMQP host.
 */
@Slf4j
public class Amqp {

    private final RabbitProperties rabbitProperties;
    private final AmqpProperties amqpProperties;
    private final ConcurrentHashMap<String, VHost> vHosts = new ConcurrentHashMap<>();

    public Amqp(final RabbitProperties rabbitProperties, final AmqpProperties amqpProperties) {
        this.rabbitProperties = rabbitProperties;
        this.amqpProperties = amqpProperties;
    }

    public void stop() {
        vHosts.values().forEach(VHost::stop);
    }

    @SuppressWarnings("java:S3358") // java:S3358
    public VHost getVhost(final DMF dmf, final boolean initVHost) {
        final String vHost = dmf == null || ObjectUtils.isEmpty(dmf.getVirtualHost()) ?
                (rabbitProperties.getVirtualHost() == null ? "/" : rabbitProperties.getVirtualHost()) :
                dmf.getVirtualHost();
        return vHosts.computeIfAbsent(vHost, vh -> new VHost(getConnectionFactory(dmf, vHost), amqpProperties, initVHost));
    }

    @SuppressWarnings("java:S4449") // java:S4449 - setUsername/Password is called with non-null
    private ConnectionFactory getConnectionFactory(final DMF dmf, final String vHost) {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitProperties.getHost());
        connectionFactory.setPort(rabbitProperties.determinePort());
        if (rabbitProperties.getSsl().determineEnabled()) {
            try {
                connectionFactory.getRabbitConnectionFactory().useSslProtocol();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IllegalStateException("Failed to enable ssl!", e);
            }
        }
        connectionFactory.setUsername(
                dmf == null || ObjectUtils.isEmpty(dmf.getUsername()) ? rabbitProperties.getUsername() : dmf.getUsername());
        connectionFactory.setPassword(
                dmf == null || ObjectUtils.isEmpty(dmf.getPassword()) ? rabbitProperties.getPassword() : dmf.getPassword());
        connectionFactory.setVirtualHost(vHost);
        return connectionFactory;
    }
}