/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bean which holds the necessary properties for configuring the Redis
 * connection.
 *
 */
@ConfigurationProperties("hawkbit.server.redis")
public class RedisProperties {

    /**
     * Redis server hostname.
     */
    private String host;

    /**
     * Redis server port.
     */
    private int port;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port
     *            the port to set
     */
    public void setPort(final int port) {
        this.port = port;
    }
}
