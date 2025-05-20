/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.security;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ToString
@ConfigurationProperties("hawkbit.server.security")
public class OidcClientProperties {

    private final OidcClientProperties.Oauth2 oauth2 = new OidcClientProperties.Oauth2();

    @Data
    public static class Oauth2 {

        private final OidcClientProperties.Oauth2.Client client = new OidcClientProperties.Oauth2.Client();

        @Data
        public static class Client {

            private boolean enabled = false;
        }
    }
}