/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the server e.g. the server's URL which must be configured.
 */
@Getter
@ConfigurationProperties("hawkbit.server")
public class HawkbitServerProperties {

    private final Anonymous anonymous = new Anonymous();
    private final Build build = new Build();
    /**
     * Defines under which URI the update server can be reached. Used to
     * calculate download URLs for DMF transmitted update actions.
     */
    private String url = "http://localhost:8080";

    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Properties for anonymous API access by Devices/Controllers.
     */
    @Getter
    public static class Anonymous {

        private final Download download = new Download();

        /**
         * Properties for artifact download under anonymous API access by
         * Devices/Controllers.
         */
        @Data
        public static class Download {

            /**
             * Unauthenticated artifact download possible if true.
             */
            private boolean enabled;
        }
    }

    /**
     * Build information of the hawkBit instance. Influenced by maven.
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
}
