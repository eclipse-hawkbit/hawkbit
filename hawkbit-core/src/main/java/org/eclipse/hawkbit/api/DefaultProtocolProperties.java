/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

/**
 * Object to hold the properties for the base protocols.
 */
public class DefaultProtocolProperties implements ProtocolProperties {
    // The IP address is not hardcoded. It's the default value, if the IP
    // address is not configured.
    @SuppressWarnings("squid:S1313")
    private static final String DEFAULT_IP_LOCALHOST = "127.0.0.1";
    private static final String LOCALHOST = "localhost";

    private String hostname = LOCALHOST;
    private String ip = DEFAULT_IP_LOCALHOST;
    private String port = "";
    /**
     * An ant-URL pattern with placeholder to build the URL on. The URL can have
     * specific artifact placeholder.
     */
    private String pattern;

    /**
     * Enables protocol.
     */
    private boolean enabled = true;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String urlPattern) {
        this.pattern = urlPattern;
    }

    @Override
    public String getPort() {
        return port;
    }

    public void setPort(final String port) {
        this.port = port;
    }
}
