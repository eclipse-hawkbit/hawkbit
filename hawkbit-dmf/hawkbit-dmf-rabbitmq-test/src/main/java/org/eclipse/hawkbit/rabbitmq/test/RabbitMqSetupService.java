/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rabbitmq.test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import jakarta.annotation.PreDestroy;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.UserPermissions;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.junit.BrokerRunningSupport;
import org.springframework.util.ObjectUtils;

/**
 * Creates and deletes a new virtual host if the rabbit mq management api is
 * available.
 */
// exception squid:S2068 - Test instance passwd
@SuppressWarnings("squid:S2068")
public class RabbitMqSetupService {

    private final com.rabbitmq.client.ConnectionFactory connectionFactory;
    private final String hostname;
    private final String username;
    private final String password;
    private Client rabbitmqHttpClient;
    private String virtualHost;

    public RabbitMqSetupService() {

        BrokerRunningSupport brokerSupport = BrokerRunningSupport.isRunning();
        connectionFactory = brokerSupport.getConnectionFactory();
        hostname = brokerSupport.getHostName();
        username = brokerSupport.getUser();
        password = brokerSupport.getPassword();
    }

    public String getHttpApiUrl() {
        return "http://" + getHostname() + ":15672/api/";
    }

    public ConnectionFactory newVirtualHostWithConnectionFactory() {
        virtualHost = UUID.randomUUID().toString();
        getRabbitmqHttpClient().createVhost(virtualHost);
        getRabbitmqHttpClient().updatePermissions(virtualHost, getUsername(), createUserPermissionsFullAccess());
        connectionFactory.setVirtualHost(virtualHost);
        return new CachingConnectionFactory(connectionFactory);
    }

    @PreDestroy
    public void deleteVirtualHost() {
        if (ObjectUtils.isEmpty(virtualHost)) {
            return;
        }
        getRabbitmqHttpClient().deleteVhost(virtualHost);
    }

    @SuppressWarnings("java:S112")
    private synchronized Client getRabbitmqHttpClient() {
        if (rabbitmqHttpClient == null) {
            try {
                rabbitmqHttpClient = new Client(new URL(getHttpApiUrl()), getUsername(), getPassword());
            } catch (final MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return rabbitmqHttpClient;
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
        permissions.setVhost(virtualHost);
        permissions.setRead(".*");
        permissions.setConfigure(".*");
        permissions.setWrite(".*");
        return permissions;
    }
}
