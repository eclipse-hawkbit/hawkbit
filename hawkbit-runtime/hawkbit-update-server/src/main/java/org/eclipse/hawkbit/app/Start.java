/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.eclipse.hawkbit.autoconfigure.security.EnableHawkbitManagedSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A {@link SpringBootApplication} annotated class with a main method to start.
 * The minimal configuration for the stand alone hawkBit server.
 *
 */
@SpringBootApplication
@EnableHawkbitManagedSecurityConfiguration
@OpenAPIDefinition(info = @Info(title = "HawkBit Management API", version = "1.0",
    description = """
        The Management API is a RESTful API that enables to perform Create/Read/Update/Delete operations for provisioning targets
        (i.e. devices) and repository content (i.e. software). Based on the Management API you can manage and monitor software update
         operations via HTTP/HTTPS. The Management API supports JSON payload with hypermedia as well as filtering, sorting and paging.
        Furthermore the Management API provides permission based access control and standard roles as well as custom role creation.
        The API is protected and needs authentication and authorization based on the security concept.
        """
    ),
    security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "OAuth2"), @SecurityRequirement(name = "Bearer Authentication")})
@SecuritySchemes({
    @SecurityScheme(name = "basicAuth", scheme = "basic", type = SecuritySchemeType.HTTP),
    @SecurityScheme(name = "Bearer Authentication", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer"),
    @SecurityScheme(name = "OAuth2", type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(authorizationUrl = "${spring.security.oauth2.client.provider.suite.authorization-uri}",
            tokenUrl = "${spring.security.oauth2.client.provider.suite.token-uri}",
            refreshUrl = "${spring.security.oauth2.client.provider.suite.token-uri}",
            scopes = {@OAuthScope(name = "openid", description = "openid"),
                @OAuthScope(name = "offline_access", description = "offline_access"),
            })))
})
// Exception squid:S1118 - Spring boot standard behavior
@SuppressWarnings({ "squid:S1118" })
public class Start {

    /**
     * Main method to start the spring-boot application.
     *
     * @param args
     *            the VM arguments.
     */
    // Exception squid:S2095 - Spring boot standard behavior
    @SuppressWarnings({ "squid:S2095" })
    public static void main(final String[] args) {
        SpringApplication.run(Start.class, args);
    }
}
