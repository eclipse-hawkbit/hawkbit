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

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Builder
public class HawkbitClient {

    private static final String AUTHORIZATION = "Authorization";
    private static final ErrorDecoder DEFAULT_ERROR_DECODER = new ErrorDecoder.Default();

    private final HawkbitServer hawkBitServerProperties;

    private final Client client;
    private final Encoder encoder;
    private final Decoder decoder;
    private final Contract contract;

    public HawkbitClient(
            final HawkbitServer hawkBitServerProperties,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        this.hawkBitServerProperties = hawkBitServerProperties;
        this.client = client;
        this.encoder = encoder;
        this.decoder = decoder;
        this.contract = contract;
    }

    public <T> T mgmtService(final Class<T> serviceType, final Tenant tenantProperties) {
        return service(serviceType, tenantProperties, null);
    }
    public <T> T ddiService(final Class<T> serviceType, final Tenant tenantProperties, final Controller controller) {
        return service(serviceType, tenantProperties, controller);
    }

    private <T> T service(final Class<T> serviceType, final Tenant tenantProperties, final Controller controller) {
        return Feign.builder().client(client)
                .encoder(encoder)
                .decoder(decoder)
                .errorDecoder((methodKey, response) -> {
                    final Exception e = DEFAULT_ERROR_DECODER.decode(methodKey, response);
                    log.trace("REST API call failed!", e);
                    return e;
                })
                .contract(contract)
                .requestInterceptor(controller == null ?
                        template -> {
                            template.header(AUTHORIZATION,
                            "Basic " +
                                    Base64.getEncoder()
                                            .encodeToString(
                                                    (Objects.requireNonNull(tenantProperties.getUsername(),
                                                            "User is null!") +
                                                    ":" +
                                                    Objects.requireNonNull(tenantProperties.getPassword(),
                                                            "Password is not available!"))
                                            .getBytes(StandardCharsets.ISO_8859_1)));
                        } :
                        template -> {
                            if (ObjectUtils.isEmpty(tenantProperties.getGatewayToken())) {
                                if (!ObjectUtils.isEmpty(controller.getSecurityToken())) {
                                    template.header(AUTHORIZATION, "TargetToken " + controller.getSecurityToken());
                                } // else do not sent authentication
                            } else {
                                template.header(AUTHORIZATION, "GatewayToken " + tenantProperties.getGatewayToken());
                            }
                        })
                .target(serviceType,
                        controller == null ?
                            hawkBitServerProperties.getMgmtUrl() :
                            hawkBitServerProperties.getDdiUrl());
    }
}
