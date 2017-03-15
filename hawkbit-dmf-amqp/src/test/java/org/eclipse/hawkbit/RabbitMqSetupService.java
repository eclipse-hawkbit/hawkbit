/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.UserPermissions;

public class RabbitMqSetupService {

    public static final String RABBITMQ_HOSTNAME_ENV = "RABBITMQ_HOSTNAME";

    private static Client rabbitmqHttpClient;
    private String virtualHost;

    protected static Client getRabbitmqHttpClient() {
        if (rabbitmqHttpClient == null) {
            try {
                rabbitmqHttpClient = new Client("http://" + getHostname() + ":15672/api/", "guest", "guest");
            } catch (MalformedURLException | URISyntaxException e) {
                ReflectionUtils.rethrowRuntimeException(e);
            }
        }
        return rabbitmqHttpClient;
    }

    @PostConstruct
    void createVirtualHost() {
        virtualHost = RandomStringUtils.random(7, "abcdefghijklmnopqrstuvwxyz");

        try {
            getRabbitmqHttpClient().createVhost(virtualHost);
        } catch (final JsonProcessingException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        getRabbitmqHttpClient().updatePermissions(virtualHost, "guest", createUserPermissionsFullAccess());
    }

    @PreDestroy
    void deleteVirtualHost() {
        getRabbitmqHttpClient().deleteVhost(virtualHost);
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public static String getHostname() {
        final String hostname = System.getenv(RABBITMQ_HOSTNAME_ENV);
        if (StringUtils.isEmpty(hostname)) {
            return "localhost";
        }
        return hostname;
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
