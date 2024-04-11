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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.ConcurrentHashMap;

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

    public VHost getVhost(final Tenant.DMF dmf) {
        final String vHost = ObjectUtils.isEmpty(dmf.getVirtualHost()) ?
                rabbitProperties.getVirtualHost() : dmf.getVirtualHost();
        return vHosts.computeIfAbsent(vHost, vh -> {
            final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitProperties.getHost());
            connectionFactory.setUsername(
                    ObjectUtils.isEmpty(dmf.getUsername()) ?
                            rabbitProperties.getUsername() : dmf.getUsername());
            connectionFactory.setPassword(
                    ObjectUtils.isEmpty(dmf.getPassword()) ?
                            rabbitProperties.getPassword() : dmf.getPassword());
            connectionFactory.setVirtualHost(vHost);
            return new VHost(connectionFactory, amqpProperties);
        });
    }
}