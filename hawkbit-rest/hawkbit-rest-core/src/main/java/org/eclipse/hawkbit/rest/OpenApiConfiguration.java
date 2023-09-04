package org.eclipse.hawkbit.rest;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.springframework.context.annotation.Bean;

public class OpenApiConfiguration {

  private static final String DESCRIPTION = """
        The Management API is a RESTful API that enables to perform Create/Read/Update/Delete operations for provisioning targets
        (i.e. devices) and repository content (i.e. software). Based on the Management API you can manage and monitor software update
         operations via HTTP/HTTPS. The Management API supports JSON payload with hypermedia as well as filtering, sorting and paging.
        Furthermore the Management API provides permission based access control and standard roles as well as custom role creation.
        The API is protected and needs authentication and authorization based on the security concept.
        """;

  @Bean
  public OpenAPI customOpenApi() {
    final String apiTitle = "hawkBit Management REST API";

    final String basiAuthSecSchemeName = "basicAuth";
    final String oauth2SecSchemeName = "OAuth2";
    final String bearerAuthenticationSchemeName = "Bearer Authentication";

    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList(basiAuthSecSchemeName))
        .addSecurityItem(new SecurityRequirement().addList(oauth2SecSchemeName))
        .addSecurityItem(new SecurityRequirement().addList(bearerAuthenticationSchemeName))
        .components(
            new Components()
                .addSecuritySchemes(basiAuthSecSchemeName,
                    new SecurityScheme()
                        .name(basiAuthSecSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .in(SecurityScheme.In.HEADER)
                        .scheme("basic")
                )
                .addSecuritySchemes(oauth2SecSchemeName,
                    new SecurityScheme()
                        .name(oauth2SecSchemeName)
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                            .authorizationUrl("${spring.security.oauth2.client.provider.suite.authorization-uri}")
                            .tokenUrl("${spring.security.oauth2.client.provider.suite.token-uri}")
                            .refreshUrl("${spring.security.oauth2.client.provider.suite.token-uri}")
                            .scopes(new Scopes()
                                      .addString("openid", "openid")
                                      .addString("offline_access", "offline_access"))))
                )
                .addSecuritySchemes(bearerAuthenticationSchemeName,
                    new SecurityScheme()
                        .name(bearerAuthenticationSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .bearerFormat("JWT")
                        .scheme("bearer")))
        .info(new Info().title(apiTitle).description(DESCRIPTION).version(MgmtRestConstants.API_VERSION));
  }
}
