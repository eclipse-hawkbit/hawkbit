/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rabbitmq.test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.junit.BrokerRunningSupport;
import org.springframework.util.StringUtils;

import com.google.common.base.Throwables;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.UserPermissions;

/**
 * Creates and deletes a new virtual host if the rabbit mq management api is
 * available.
 */
// exception squid:S2068 - Test instance passwd
@SuppressWarnings("squid:S2068")
public class RabbitMqSetupService {

    private Client rabbitmqHttpClient;

    private final com.rabbitmq.client.ConnectionFactory connectionFactory;

    private static final String VIRTUAL_HOST = UUID.randomUUID().toString();

    private final String hostname;

    private String username;

    private String password;

    public RabbitMqSetupService() {

        BrokerRunningSupport brokerSupport = BrokerRunningSupport.isRunning();
        connectionFactory = brokerSupport.getConnectionFactory();
        hostname = brokerSupport.getHostName();
        username = brokerSupport.getUser();
        password = brokerSupport.getPassword();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteVirtualHost()));
    }

    private synchronized Client getRabbitmqHttpClient() {
        if (rabbitmqHttpClient == null) {
            try {
                rabbitmqHttpClient = new Client(getHttpApiUrl(), getUsername(), getPassword());
            } catch (MalformedURLException | URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }
        return rabbitmqHttpClient;
    }

    private String getHttpApiUrl() {
        return "http://" + getHostname() + ":15672/api/";
    }

    public ConnectionFactory newVirtualHostWithConnectionFactory() {
        getRabbitmqHttpClient().createVhost(VIRTUAL_HOST);
        getRabbitmqHttpClient().updatePermissions(VIRTUAL_HOST, getUsername(), createUserPermissionsFullAccess());
        connectionFactory.setVirtualHost(VIRTUAL_HOST);
        return new CachingConnectionFactory(connectionFactory);
    }

    @PreDestroy
    public void deleteVirtualHost() {
        if (StringUtils.isEmpty(VIRTUAL_HOST)) {
            return;
        }
        getRabbitmqHttpClient().deleteVhost(VIRTUAL_HOST);
    }

    private String getHostname() {
        return hostname;
    }

    private String getPassword() {
        return password;
    }

    private String getUsername() {
        return username;
    }

    private UserPermissions createUserPermissionsFullAccess() {
        final UserPermissions permissions = new UserPermissions();
        permissions.setVhost(VIRTUAL_HOST);
        permissions.setRead(".*");
        permissions.setConfigure(".*");
        permissions.setWrite(".*");
        return permissions;
    }
}
