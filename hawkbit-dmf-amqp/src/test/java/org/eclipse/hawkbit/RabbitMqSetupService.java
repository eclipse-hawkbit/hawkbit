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
import org.eclipse.hawkbit.amqp.AmqpAuthenticationMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.UserPermissions;

/**
 * Creates and deletes a new virtual host if the rabbit mq management api is
 * available.
 * 
 *
 */
public class RabbitMqSetupService {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpAuthenticationMessageHandler.class);

    private static Client rabbitmqHttpClient;
    private String virtualHost;

    private final String hostname;

    public RabbitMqSetupService(String hostname) {
        this.hostname = hostname;
    }

    protected Client getRabbitmqHttpClient() {
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
        try {
            if (!getRabbitmqHttpClient().alivenessTest("/")) {
                throw new AlivenessException(getHostname());

            }
        } catch (final AlivenessException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        } catch (final Exception e) {
            LOG.error("Connection to management rejected. Maybe no broker is avaiable at host {}", hostname);
            return;
        }

        virtualHost = RandomStringUtils.random(7, "abcdefghijklmnopqrstuvwxyz");
        try {
            getRabbitmqHttpClient().createVhost(virtualHost);
            getRabbitmqHttpClient().updatePermissions(virtualHost, "guest", createUserPermissionsFullAccess());
        } catch (final JsonProcessingException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }

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

    private static class AlivenessException extends RuntimeException {
        public AlivenessException(String hostname) {
            super("Aliveness test failed for " + hostname
                    + ":15672 guest/quest; rabbit mq management api not available");
        }
    }

}
