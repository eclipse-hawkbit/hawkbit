/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MgmtOpenApi30Config
{
    @Bean
    public OpenAPI customOpenAPI()
    {
        // Globally add the basicAuth security scheme.
        // This ensures that it gets added to the OpenAPI specification file and add
        // the "Authorize" button in swagger-ui.
        // Without this, there is no defined security scheme.
        final String securitySchemeName = "basicAuth";
        final String apiTitle = "hawkBit Management REST API";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                                    new SecurityScheme()
                                                            .name(securitySchemeName)
                                                            .type(SecurityScheme.Type.HTTP)
                                                            .in(SecurityScheme.In.HEADER)
                                                            .scheme("basic")
                                )
                )
                .info(new Info().title(apiTitle).version(MgmtRestConstants.API_VERSION));
    }
}
