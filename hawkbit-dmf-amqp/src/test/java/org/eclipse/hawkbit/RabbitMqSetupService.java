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
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Throwables;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.UserPermissions;

/**
 * Creates and deletes a new virtual host if the rabbit mq management api is
 * available.
 * 
 *
 */
public class RabbitMqSetupService {

    private static Client rabbitmqHttpClient;
    private String virtualHost;

    private final String hostname;

    private String username;

    private String password;

    public RabbitMqSetupService(RabbitProperties properties) {
        hostname = properties.getHost();
        username = properties.getUsername();
        password = properties.getPassword();
    }

    private Client getRabbitmqHttpClient() {
        if (rabbitmqHttpClient == null) {
            try {
                rabbitmqHttpClient = new Client("http://" + getHostname() + ":15672/api/", getUsername(),
                        getPassword());
            } catch (MalformedURLException | URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }
        return rabbitmqHttpClient;
    }

    void createVirtualHost() throws JsonProcessingException, AlivenessException {
        if (!getRabbitmqHttpClient().alivenessTest("/")) {
            throw new AlivenessException(getHostname());

        }
        virtualHost = UUID.randomUUID().toString();
        getRabbitmqHttpClient().createVhost(virtualHost);
        getRabbitmqHttpClient().updatePermissions(virtualHost, getUsername(), createUserPermissionsFullAccess());

    }

    @PreDestroy
    void deleteVirtualHost() {
        if (StringUtils.isEmpty(virtualHost)) {
            return;
        }
        getRabbitmqHttpClient().deleteVhost(virtualHost);
    }

    public String getVirtualHost() {
        if (StringUtils.isEmpty(virtualHost)) {
            return "/";
        }
        return virtualHost;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPassword() {
        if (StringUtils.isEmpty(password)) {
            password = "guest";
        }
        return password;
    }

    public String getUsername() {
        if (StringUtils.isEmpty(username)) {
            username = "guest";
        }
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

    static class AlivenessException extends Exception {
        private static final long serialVersionUID = 1L;

        public AlivenessException(String hostname) {
            super("Aliveness test failed for " + hostname
                    + ":15672 guest/quest; rabbit mq management api not available");
        }
    }

}
