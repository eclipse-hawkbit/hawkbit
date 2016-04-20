/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ddi.client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.ddi.client.resource.RootControllerResourceClient;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.http.ResponseEntity;

import feign.Feign;
import feign.Logger;
import feign.Logger.Level;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

public class DdiClient {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    private final String controllerId;
    private final String name;
    private final String description;
    private RootControllerResourceClient rootControllerResourceClient;
    private String rootControllerResourcePath;

    public DdiClient(final String rolloutsUrl, final String controllerId, final String name, final String description,
            final String tenant) {
        super();
        this.controllerId = controllerId;
        this.name = name;
        this.description = description;
        rootControllerResourcePath = rolloutsUrl + DdiRestConstants.BASE_V1_REQUEST_MAPPING;
        rootControllerResourcePath = rootControllerResourcePath.replace("{tenant}", tenant);

        createFeignClient();
    }

    private void createFeignClient() {

        // BasicAuthRequestInterceptor TODO

        final Feign.Builder feignBuilder = Feign.builder()
                .contract(new IgnoreMultipleConsumersProducersSpringMvcContract())
                .requestInterceptor(new ApplicationJsonRequestHeaderInterceptor()).logLevel(Level.FULL)
                .logger(new Logger.ErrorLogger()).encoder(new JacksonEncoder())
                .decoder(new ResponseEntityDecoder(new JacksonDecoder()));
        // TODO implement feign client encoder to handle MultiPartFile
        // .requestInterceptor(new BasicAuthRequestInterceptor(tenant + "\\" +
        // user, password))

        rootControllerResourceClient = feignBuilder.target(RootControllerResourceClient.class,
                rootControllerResourcePath);

    }

    public void startDdiClient() {

        // final HttpServletRequest request;

        // final HttpSession mySession = request.getSession();

        // final HttpServletRequest request = new;

        final ResponseEntity<DdiControllerBase> response = rootControllerResourceClient.getControllerBase("test",
                request);
        final DdiControllerBase controllerBase = response.getBody();

        // TODO notify every 10 seconds on the rollout server

        // TODO if new update available -> start download and installation
        // process
        // report status messages

    }

}
