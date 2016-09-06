/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.Lists;

/**
 * Artifact handler properties class for holding all supported protocols with
 * host, ip, port and download pattern.
 */
@ConfigurationProperties("hawkbit.artifact.url")
public class ArtifactUrlHandlerProperties {

    private final Map<String, UrlProtocol> protocols = new HashMap<>();

    /**
     * @author kaizimmerm
     *
     */
    /**
     * @author kaizimmerm
     *
     */
    public static class UrlProtocol {
        /**
         * Hypermedia rel value for this protocol.
         */
        private String rel = "download-http";

        /**
         * Hypermedia ref pattern for this protocol. Supported place holders are
         * protocol,controllerId,ip,port,hostname,artifactFileName,artifactSHA1,
         * artifactIdBase62,artifactId,tenant,softwareModuleId;
         */
        private String ref = "{protocol}://{hostname}:{port}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}";

        /**
         * Protocol name placeholder that can be used in ref pattern.
         */
        private String name = "https";

        /**
         * Hostname placeholder that can be used in ref pattern.
         */
        private String hostname = "localhost";

        /**
         * IP address placeholder that can be used in ref pattern.
         */
        private String ip = "127.0.0.1";

        /**
         * Port placeholder that can be used in ref pattern.
         */
        private int port = 8080;

        /**
         * Support for the following hawkBit API.
         */
        private List<APIType> supports = Lists.newArrayList(APIType.DDI, APIType.DMF);

        public String getRel() {
            return rel;
        }

        public void setRel(final String rel) {
            this.rel = rel;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(final String ref) {
            this.ref = ref;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(final String hostname) {
            this.hostname = hostname;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(final String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(final int port) {
            this.port = port;
        }

        public List<APIType> getSupports() {
            return supports;
        }

        public void setSupports(final List<APIType> supports) {
            this.supports = supports;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

    }

    public Map<String, UrlProtocol> getProtocols() {
        return protocols;
    }

}
