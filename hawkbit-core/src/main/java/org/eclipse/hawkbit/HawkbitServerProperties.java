/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the server e.g. the server's URL which must be configured.
 * 
 */
@ConfigurationProperties("hawkbit.server")
public class HawkbitServerProperties {
    /**
     * Defines under which URI the update server can be reached. Used to
     * calculate download URLs for DMF transmitted update actions.
     */
    private String url = "http://localhost:8080";

    private final Build build = new Build();

    public Build getBuild() {
        return build;
    }

    /**
     * Build information of the hawkBit instance. Influenced by maven.
     *
     */
    public static class Build {
        /**
         * Project artifact ID.
         */
        private String artifact = "";

        /**
         * Project name.
         */
        private String name = "";

        /**
         * Project description.
         */
        private String description = "";

        /**
         * Project version.
         */
        private String version = "";

        public String getArtifact() {
            return artifact;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getVersion() {
            return version;
        }

        public void setArtifact(final String artifact) {
            this.artifact = artifact;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public void setVersion(final String version) {
            this.version = version;
        }

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
