/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.swagger;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.rest.resource.RestConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger configuration for RESTful SP server APIs.
 *
 *
 *
 *
 */

@Configuration
@EnableSwagger2
public class SwaggerApiDocAutoConfiguration {

    /**
     * Create the Springfox Docket, which generates the Information for the
     * REST-Swagger-UI. Rest paths are: /rest/v1/. and ./controller/v1/.
     * 
     * @see springfox.documentation.spring.web.plugins.Docket
     * 
     * @return the v1 docket
     */
    @Bean
    public Docket customImplementation() {
        return createDocket();
    }

    private Predicate<String> selectApiPaths() {
        return Predicates.or(PathSelectors.regex("/rest/v1/.*"), PathSelectors.regex("/.*/controller/v1/.*"));
    }

    private Docket createDocket() {
        final List<ResponseMessage> authorizationMessages = globalAuhtorizationMessages();
        return new Docket(DocumentationType.SWAGGER_2).select().paths(selectApiPaths()).build()
                .useDefaultResponseMessages(true).globalResponseMessage(RequestMethod.GET, authorizationMessages)
                .globalResponseMessage(RequestMethod.POST, authorizationMessages)
                .globalResponseMessage(RequestMethod.PUT, authorizationMessages)
                .globalResponseMessage(RequestMethod.DELETE, authorizationMessages).apiInfo(apiInfo());
    }

    private List<ResponseMessage> globalAuhtorizationMessages() {
        final List<ResponseMessage> messageList = new ArrayList<>();
        messageList.add(new ResponseMessage(200, "Request sucessfull", new ModelRef("com.")));
        messageList.add(new ResponseMessage(400, "Bad Request - e.g. invalid parameters", null));
        messageList.add(new ResponseMessage(401, "Unauthorized - The request requires user authentication.",

                null));
        messageList.add(
                new ResponseMessage(403, "Forbidden - Insufficient permissions or data volume restriction applies.",
                        new ModelRef("ExceptionInfo")));
        messageList.add(new ResponseMessage(405, "Method Not Allowed", null));
        messageList.add(new ResponseMessage(406,
                "Not Acceptable - In case accept header is specified and not application/json", null));
        messageList.add(new ResponseMessage(429,
                "Too many requests. The server will refuse further attemps and the client has to wait another second.",
                null));
        return messageList;
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Software Provisioning API Descriptions").version(RestConstants.API_VERSION)
                .build();
    }

}
