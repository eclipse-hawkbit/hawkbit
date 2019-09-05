package org.eclipse.hawkbit.dmf.hono;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hawkbit.dmf.hono")
@ComponentScan
public class DmfHonoConfiguration {
    private String tenantListUri;
    private String deviceListUri;
    private String credentialsListUri;
    private String authorizationMethod = "none";
    private String oidcTokenUri = "";
    private String oidcClientId = "";
    private String username = "";
    private String password = "";

    @Bean
    public HonoDeviceSync honoDeviceSync() {
        return new HonoDeviceSync(tenantListUri, deviceListUri, credentialsListUri, authorizationMethod,
                oidcTokenUri, oidcClientId, username, password);
    }

    public String getTenantListUri() {
        return tenantListUri;
    }

    public void setTenantListUri(String tenantListUri) {
        this.tenantListUri = tenantListUri;
    }

    public String getDeviceListUri() {
        return deviceListUri;
    }

    public void setDeviceListUri(String deviceListUri) {
        this.deviceListUri = deviceListUri;
    }

    public String getCredentialsListUri() {
        return credentialsListUri;
    }

    public void setCredentialsListUri(String credentialsListUri) {
        this.credentialsListUri = credentialsListUri;
    }

    public String getAuthorizationMethod() {
        return authorizationMethod;
    }

    public void setAuthorizationMethod(String authorizationMethod) {
        this.authorizationMethod = authorizationMethod;
    }

    public String getOidcTokenUri() {
        return oidcTokenUri;
    }

    public void setOidcTokenUri(String oidcTokenUri) {
        this.oidcTokenUri = oidcTokenUri;
    }

    public String getOidcClientId() {
        return oidcClientId;
    }

    public void setOidcClientId(String oidcClientId) {
        this.oidcClientId = oidcClientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
