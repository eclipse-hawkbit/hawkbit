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

    private final Anonymous anonymous = new Anonymous();

    private final Build build = new Build();

    public Anonymous getAnonymous() {
        return anonymous;
    }

    public Build getBuild() {
        return build;
    }

    /**
     * Properties for anonymous API access by Devices/Controllers.
     *
     */
    public static class Anonymous {
        private final Download download = new Download();

        public Download getDownload() {
            return download;
        }

        /**
         * Properties for artifact download under anonymous API access by
         * Devices/Controllers.
         *
         */
        public static class Download {

            /**
             * Unauthenticated artifact download possible if true.
             */
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(final boolean enabled) {
                this.enabled = enabled;
            }

        }
    }

    /**
     * Build information of the hawkBit instance. Influenced by maven.
     *
     */
    public static class Build {

        /**
         * Project version.
         */
        private String version = "";

        public String getVersion() {
            return version;
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
