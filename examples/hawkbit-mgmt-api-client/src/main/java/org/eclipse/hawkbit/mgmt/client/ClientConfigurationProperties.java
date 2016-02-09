/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration bean which holds the configuration of the client e.g. the base
 * URL of the hawkbit-server and the credentials to use the RESTful Management
 * API.
 */
@ConfigurationProperties(prefix = "hawkbit")
public class ClientConfigurationProperties {

    private String url = "localhost:8080";
    private String username = "admin";
    private String password = "admin"; // NOSONAR this password is only used for
                                       // examples

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
