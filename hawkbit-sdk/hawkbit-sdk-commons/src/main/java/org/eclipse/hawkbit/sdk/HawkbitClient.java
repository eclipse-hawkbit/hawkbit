/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.function.BiFunction;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

@Slf4j
@Builder
public class HawkbitClient {

    private static final String AUTHORIZATION = "Authorization";
    public static final BiFunction<Tenant, Controller, RequestInterceptor> DEFAULT_REQUEST_INTERCEPTOR_FN =
            (tenant, controller) ->
                    controller == null
                            ? template ->
                                    template.header(
                                            AUTHORIZATION,

                                            "Basic " +
                                                    Base64.getEncoder()
                                                            .encodeToString(
                                                                    (Objects.requireNonNull(tenant.getUsername(), "User is null!") +
                                                                            ":" +
                                                                            Objects.requireNonNull(tenant.getPassword(),
                                                                                    "Password is not available!"))
                                                                            .getBytes(StandardCharsets.ISO_8859_1)))
                            :
                                    template -> {
                                        if (ObjectUtils.isEmpty(tenant.getGatewayToken())) {
                                            if (!ObjectUtils.isEmpty(controller.getSecurityToken())) {
                                                template.header(AUTHORIZATION, "TargetToken " + controller.getSecurityToken());
                                            } // else do not sent authentication
                                        } else {
                                            template.header(AUTHORIZATION, "GatewayToken " + tenant.getGatewayToken());
                                        }
                                    };
    private static final ErrorDecoder DEFAULT_ERROR_DECODER_0 = new ErrorDecoder.Default();
    public static final ErrorDecoder DEFAULT_ERROR_DECODER = (methodKey, response) -> {
        final Exception e = DEFAULT_ERROR_DECODER_0.decode(methodKey, response);
        log.trace("REST API call failed!", e);
        return e;
    };
    private final HawkbitServer hawkBitServer;

    private final Client client;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Contract contract;

    private final ErrorDecoder errorDecoder;
    private final BiFunction<Tenant, Controller, RequestInterceptor> requestInterceptorFn;

    public HawkbitClient(
            final HawkbitServer hawkBitServer,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        this(hawkBitServer, client, encoder, decoder, contract, null, null);
    }

    /**
     * Customizers gets default ones and could
     */
    public HawkbitClient(
            final HawkbitServer hawkBitServer,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract,
            final ErrorDecoder errorDecoder,
            final BiFunction<Tenant, Controller, RequestInterceptor> requestInterceptorFn) {
        this.hawkBitServer = hawkBitServer;
        this.client = client;
        this.encoder = encoder;
        this.decoder = decoder;
        this.contract = contract;

        this.errorDecoder = errorDecoder == null ? DEFAULT_ERROR_DECODER : errorDecoder;
        this.requestInterceptorFn = requestInterceptorFn == null ? DEFAULT_REQUEST_INTERCEPTOR_FN : requestInterceptorFn;
    }

    public <T> T mgmtService(final Class<T> serviceType, final Tenant tenantProperties) {
        return service(serviceType, tenantProperties, null);
    }

    public <T> T ddiService(final Class<T> serviceType, final Tenant tenantProperties, final Controller controller) {
        return service(serviceType, tenantProperties, controller);
    }

    private <T> T service(final Class<T> serviceType, final Tenant tenant, final Controller controller) {
        return Feign.builder().client(client)
                .encoder(encoder)
                .decoder(decoder)
                .errorDecoder(errorDecoder)
                .contract(contract)
                .requestInterceptor(requestInterceptorFn.apply(tenant, controller))
                .target(serviceType,
                        controller == null ?
                                hawkBitServer.getMgmtUrl() :
                                hawkBitServer.getDdiUrl());
    }
}