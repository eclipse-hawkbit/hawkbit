/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.Lists;

/**
 * Artifact handler properties class for holding all supported protocols with
 * host, ip, port and download pattern.
 * 
 * @see PropertyBasedArtifactUrlHandler
 */
@ConfigurationProperties("hawkbit.artifact.url")
public class ArtifactUrlHandlerProperties {

    private final List<UrlProtocol> protocols = new ArrayList<>();

    public static class UrlProtocol {

        /**
         * Hypermedia rel value for this protocol.
         */
        private String rel = "download";

        /**
         * Hypermedia ref pattern for this protocol. Supported place holders are
         * protocol,controllerId,targetId,targetIdBase62,ip,port,hostname,
         * artifactFileName,artifactSHA1,
         * artifactIdBase62,artifactId,tenant,softwareModuleId,
         * softwareModuleIdBase62.
         * 
         * The update server itself supportes
         */
        private String ref = "{protocol}://{hostname}:{port}/{tenant}/controller/v1/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}";

        /**
         * Protocol name placeholder that can be used in ref pattern.
         */
        private String protocol = "http";

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
        private Integer port;

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

        public Integer getPort() {
            return port;
        }

        public void setPort(final Integer port) {
            this.port = port;
        }

        public List<APIType> getSupports() {
            return supports;
        }

        public void setSupports(final List<APIType> supports) {
            this.supports = supports;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(final String protocol) {
            this.protocol = protocol;
        }

    }

    public List<UrlProtocol> getProtocols() {
        return protocols;
    }

}
