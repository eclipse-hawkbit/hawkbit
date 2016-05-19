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
     * Object to hold the properties for the HTTP protocol.
     */
    public static class Http extends DefaultProtocolProperties {

        /**
         * Constructor.
         */
        public Http() {
            setPattern(
                    "{protocol}://{hostname}:{port}/{tenant}/controller/v1/{targetId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");
        }
    }

    /**
     * Object to hold the properties for the HTTP protocol.
     */
    public static class Https extends DefaultProtocolProperties {

        /**
         * Constructor.
         */
        public Https() {
            setPattern(
                    "{protocol}://{hostname}:{port}/{tenant}/controller/v1/{targetId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}");
        }
    }

    /**
     * Object to hold the properties for the HTTP protocol.
     */
    public static class Coap extends DefaultProtocolProperties {

        /**
         * Constructor.
         */
        public Coap() {
            setPattern("{protocol}://{ip}:{port}/fw/{tenant}/{targetId}/sha1/{artifactSHA1}");
            setPort("5683");
        }
    }

}
