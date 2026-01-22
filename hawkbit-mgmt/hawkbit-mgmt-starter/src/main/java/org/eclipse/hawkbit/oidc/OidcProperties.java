/**
 * Copyright (c) 2025 blue-zone GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.oidc;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for hawkBit oidc resource server
 */
@Data
@ToString
@ConfigurationProperties("hawkbit.server.security")
public class OidcProperties {

    private final OidcProperties.Oauth2 oauth2 = new OidcProperties.Oauth2();

    @Data
    public static class Oauth2 {

        private final OidcProperties.Oauth2.ResourceServer resourceserver = new OidcProperties.Oauth2.ResourceServer();

        @Data
        public static class ResourceServer {

            private final OidcProperties.Oauth2.ResourceServer.Jwt jwt = new OidcProperties.Oauth2.ResourceServer.Jwt();

            /**
             * Indicates whether the default OAuth2 resource server configuration is enabled.
             * Defaults to false. If false either no Oauth2 resource server is active or a hawkbitOAuth2ResourceServerCustomizer component can be used to define custom OAuth2 resource server behaviour.
             * If true, the default spring OAuth2 resource server configuration is activated.
             *
             * @see <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#_specifying_the_authorization_server">Spring Documentation</a>
             */
            private boolean enabled = false;

            @Data
            public static class Jwt {

                private final OidcProperties.Oauth2.ResourceServer.Jwt.Claim claim = new OidcProperties.Oauth2.ResourceServer.Jwt.Claim();

                @Data
                public static class Claim {

                    /**
                     * Defines the claim within the JWT token that supplies the hawkbit username.
                     */
                    private String username = "preferred_username";

                    /**
                     * Defines the claim within the JWT token that supplies the hawkbit authorities.
                     */
                    private String roles = "roles";

                    /**
                     * Defines the claim within the JWT token that supplies the hawkbit tenant.
                     * If null, the DEFAULT tenant is used for every user.
                     */
                    private String tenant = null;
                }
            }
        }
    }
}