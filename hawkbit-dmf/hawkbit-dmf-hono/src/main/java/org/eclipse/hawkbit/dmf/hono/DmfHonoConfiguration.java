package org.eclipse.hawkbit.dmf.hono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class DmfHonoConfiguration {

    @Value("${hawkbit.server.repository.hono-sync.tenant-list-uri}")
    private String honoTenantListUri;

    @Value("${hawkbit.server.repository.hono-sync.device-list-uri}")
    private String honoDeviceListUri;

    @Value("${hawkbit.server.repository.hono-sync.authorization-method:none}")
    private String authorizationMethod;

    @Value("${hawkbit.server.repository.hono-sync.oidc-token-uri:}")
    private String oidcTokenUri;

    @Value("${hawkbit.server.repository.hono-sync.oidc-client-id:}")
    private String oidcClientId;

    @Value("${hawkbit.server.repository.hono-sync.user.name:}")
    private String username;

    @Value("${hawkbit.server.repository.hono-sync.user.password:}")
    private String password;

    @Bean
    public HonoDeviceSync honoDeviceSync() {
        return new HonoDeviceSync(honoTenantListUri, honoDeviceListUri, authorizationMethod, oidcTokenUri, oidcClientId,
                username, password);
    }
}
