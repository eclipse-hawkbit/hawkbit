/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Artifact handler properties class for holding all supported protocols with
 * host, ip, port and download pattern.
 */
@ConfigurationProperties("hawkbit.artifact.url")
public class ArtifactUrlHandlerProperties {
    private static final String DEFAULT_IP_LOCALHOST = "127.0.0.1";
    private static final String LOCALHOST = "localhost";

    private final Http http = new Http();
    private final Https https = new Https();
    private final Coap coap = new Coap();

    public Http getHttp() {
        return http;
    }

    public Https getHttps() {
        return https;
    }

    public Coap getCoap() {
        return coap;
    }

    /**
     * @param protocol
     *            the protocol schema to retrieve the properties.
     * @return the properties to a protocol or {@code null} if protocol does not
     *         have properties or protocol not supported
     */
    public ProtocolProperties getProperties(final String protocol) {
        switch (protocol) {
        case "http":
            return getHttp();
        case "https":
            return getHttps();
        case "coap":
            return getCoap();
        default:
            return null;
        }
    }

    /**
     * Interface for declaring common properties through all supported protocols
     * pattern.
     */
    public interface ProtocolProperties {
        /**
         * @return the hostname value to resolve in the pattern.
         */
        String getHostname();

        /**
         * @return the IP address value to resolve in the pattern.
         */
        String getIp();

        /**
         * @return the port value to resolve in the pattern.
         */
        String getPort();

        /**
         * @return the pattern to build the URL.
         */
        String getPattern();
    }

    /**
     * Object to hold the properties for the HTTP protocol.
     */
    public static class Http implements ProtocolProperties {
        private String hostname = LOCALHOST;
        private String ip = DEFAULT_IP_LOCALHOST;
        private String port = "";
        /**
         * An ant-URL pattern with placeholder to build the URL on. The URL can
         * have specific artifact placeholder.
         */
        private String pattern = "{protocol}://{hostname}:{port}/{tenant}/controller/v1/{targetId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}";

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

    /**
     * Object to hold the properties for the HTTP protocol.
     */
    public static class Https implements ProtocolProperties {
        private String hostname = LOCALHOST;
        private String ip = DEFAULT_IP_LOCALHOST;
        private String port = "";
        /**
         * An ant-URL pattern with placeholder to build the URL on. The URL can
         * have specific artifact placeholder.
         */
        private String pattern = "{protocol}://{hostname}:{port}/{tenant}/controller/v1/{targetId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}";

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

    /**
     * Object to hold the properties for the HTTP protocol.
     */
    public static class Coap implements ProtocolProperties {
        private String hostname = LOCALHOST;
        private String ip = DEFAULT_IP_LOCALHOST;
        private String port = "5683";
        /**
         * An ant-URL pattern with placeholder to build the URL on. The URL can
         * have specific artifact placeholder.
         */
        private String pattern = "{protocol}://{ip}:{port}/fw/{tenant}/{targetId}/sha1/{artifactSHA1}";

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

}
