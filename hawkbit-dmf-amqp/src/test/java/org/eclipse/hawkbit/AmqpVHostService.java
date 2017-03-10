/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.UserPermissions;

public class AmqpVHostService {

    private static String vhost;
    private static Client httpClient;

    public String generateNewVHost(final String host, final String username, final String password) {

        // TODO use variables later
        try {
            httpClient = new Client("http://" + host + ":15672/api/", username, password);
        } catch (MalformedURLException | URISyntaxException e1) {
            fail();
        }

        if (vhost != null) {
            deleteCurrentVhost();
        }

        vhost = RandomStringUtils.random(7, "abcdefghijklmnopqrstuvwxyz");

        try {
            httpClient.createVhost(vhost);
        } catch (final JsonProcessingException e) {
            fail("Could not create vhost");
        }
        httpClient.updatePermissions(vhost, username, createUserPermissionsFullAccess(vhost));
        return vhost;
    }

    public static void deleteCurrentVhost() {
        httpClient.deleteVhost(vhost);
    }

    public String getCurrentVhost() {
        return vhost;
    }

    private UserPermissions createUserPermissionsFullAccess(final String vhost) {
        final UserPermissions permissions = new UserPermissions();
        permissions.setVhost(vhost);
        permissions.setRead(".*");
        permissions.setConfigure(".*");
        permissions.setWrite(".*");
        return permissions;
    }

}
