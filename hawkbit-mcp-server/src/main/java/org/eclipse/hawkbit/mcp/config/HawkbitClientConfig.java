package org.eclipse.hawkbit.mcp.config;

import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.mgmt.AuthenticationSetupHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;

@Configuration
public class HawkbitClientConfig {

    @Bean
    public HawkbitClient hawkbitClient(final HawkbitServer hawkbitServer, final Encoder encoder, final Decoder decoder,
            final Contract contract) {
        return new HawkbitClient(hawkbitServer, encoder, decoder, contract);
    }

    @Bean
    AuthenticationSetupHelper mgmtApi(final Tenant tenant, final HawkbitClient hawkbitClient) {
        return new AuthenticationSetupHelper(tenant, hawkbitClient);
    }
}
