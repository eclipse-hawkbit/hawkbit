/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui;

import org.eclipse.hawkbit.ui.view.util.Utils;
import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.FeignException;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import lombok.Getter;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

import static feign.Util.ISO_8859_1;

@Getter
public class HawkbitClient {

    private static final RequestInterceptor AUTHORIZATION = requestTemplate -> {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        requestTemplate.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (Objects.requireNonNull(authentication.getPrincipal(), "User is null!") + ":" + Objects.requireNonNull(
                        authentication.getCredentials(), "Password is not available!")).getBytes(ISO_8859_1)));
    };

    private final String hawkbitUrl;
    private final MgmtSoftwareModuleRestApi softwareModuleRestApi;
    private final MgmtSoftwareModuleTypeRestApi softwareModuleTypeRestApi;
    private final MgmtDistributionSetRestApi distributionSetRestApi;
    private final MgmtDistributionSetTypeRestApi distributionSetTypeRestApi;
    private final MgmtDistributionSetTagRestApi distributionSetTagRestApi;
    private final MgmtTargetRestApi targetRestApi;
    private final MgmtTargetTypeRestApi targetTypeRestApi;
    private final MgmtTargetTagRestApi targetTagRestApi;
    private final MgmtTargetFilterQueryRestApi targetFilterQueryRestApi;
    private final MgmtRolloutRestApi rolloutRestApi;

    HawkbitClient(final String hawkbitUrl,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        this.hawkbitUrl = hawkbitUrl;

        softwareModuleRestApi = service(MgmtSoftwareModuleRestApi .class, client, encoder, decoder, contract);
        softwareModuleTypeRestApi = service(MgmtSoftwareModuleTypeRestApi.class, client, encoder, decoder, contract);
        distributionSetRestApi = service(MgmtDistributionSetRestApi.class, client, encoder, decoder, contract);
        distributionSetTypeRestApi = service(MgmtDistributionSetTypeRestApi.class, client, encoder, decoder, contract);
        distributionSetTagRestApi = service(MgmtDistributionSetTagRestApi.class, client, encoder, decoder, contract);
        targetRestApi = service(MgmtTargetRestApi.class, client, encoder, decoder, contract);
        targetTypeRestApi = service(MgmtTargetTypeRestApi.class, client, encoder, decoder, contract);
        targetTagRestApi = service(MgmtTargetTagRestApi.class, client, encoder, decoder, contract);
        targetFilterQueryRestApi = service(MgmtTargetFilterQueryRestApi.class, client, encoder, decoder, contract);
        rolloutRestApi = service(MgmtRolloutRestApi.class, client, encoder, decoder, contract);
    }

    boolean hasSoftwareModulesRead() {
        return hasRead(() -> softwareModuleRestApi.getSoftwareModule(-1L));
    }

    boolean hasRolloutRead() {
        return hasRead(() -> rolloutRestApi.getRollout(-1L));
    }

    boolean hasDistributionSetRead() {
        return hasRead(() -> distributionSetRestApi.getDistributionSet(-1L));
    }

    boolean hasTargetRead() {
        return hasRead(() -> targetRestApi.getTarget("_#ETE$ER"));
    }

    private boolean hasRead(final Supplier<ResponseEntity<?>> doCall) {
        try {
            final int statusCode = doCall.get().getStatusCode().value();
            return statusCode != 401 && statusCode != 403;
        } catch (final FeignException e) {
            return !(e instanceof FeignException.Unauthorized) && !(e instanceof FeignException.Forbidden);
        }
    }

    private static final ErrorDecoder DEFAULT_ERROR_DECODER = new ErrorDecoder.Default();
    private <T> T service(final Class<T> serviceType,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        return Feign.builder().client(client)
                .encoder(encoder)
                .decoder(decoder)
                .errorDecoder((methodKey, response) -> {
                    final Exception e = DEFAULT_ERROR_DECODER.decode(methodKey, response);
                    Utils.errorNotification(e);
                    return e;
                })
                .contract(contract)
                .requestInterceptor(AUTHORIZATION)
                .target(serviceType, hawkbitUrl);
    }
}
