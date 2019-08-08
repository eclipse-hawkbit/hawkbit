/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An pre-authenticated processing filter which extracts (if enabled through
 * configuration) the possibility to authenticate a target based on its target
 * security-token with the {@code Authorization} HTTP header.
 * {@code Example Header: Authorization: HonoToken
 * 5d8fSD54fdsFG98DDsa.}
 */
public class ControllerPreAuthenticatedHonoFilter extends AbstractControllerAuthenticationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerPreAuthenticatedHonoFilter.class);
    private static final String TARGET_SECURITY_TOKEN_AUTH_SCHEME = "HonoToken ";
    private static final int OFFSET_TARGET_TOKEN = TARGET_SECURITY_TOKEN_AUTH_SCHEME.length();

    private final ControllerManagement controllerManagement;
    private final String honoCredentialsEndpoint;

    /**
     * Constructor.
     *
     * @param tenantConfigurationManagement
     *            the tenant management service to retrieve configuration
     *            properties
     * @param controllerManagement
     *            the controller management to retrieve the specific target
     *            security token to verify
     * @param tenantAware
     *            the tenant aware service to get configuration for the specific
     *            tenant
     * @param systemSecurityContext
     *            the system security context to get access to tenant
     *            configuration
     */
    public ControllerPreAuthenticatedHonoFilter(
            final TenantConfigurationManagement tenantConfigurationManagement,
            final ControllerManagement controllerManagement, final TenantAware tenantAware,
            final SystemSecurityContext systemSecurityContext, final String honoCredentialsEndpoint) {
        super(tenantConfigurationManagement, tenantAware, systemSecurityContext);
        this.controllerManagement = controllerManagement;
        this.honoCredentialsEndpoint = honoCredentialsEndpoint;
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedPrincipal(final DmfTenantSecurityToken secruityToken) {
        final String controllerId = resolveControllerId(secruityToken);
        final String authHeader = secruityToken.getHeader(DmfTenantSecurityToken.AUTHORIZATION_HEADER);
        if ((authHeader != null) && authHeader.startsWith(TARGET_SECURITY_TOKEN_AUTH_SCHEME)) {
            LOGGER.debug("found authorization header with scheme {} using target security token for authentication",
                    TARGET_SECURITY_TOKEN_AUTH_SCHEME);
            return new HeaderAuthentication(controllerId, authHeader.substring(OFFSET_TARGET_TOKEN));
        }
        LOGGER.debug(
                "security token filter is enabled but request does not contain either the necessary path variables {} or the authorization header with scheme {}",
                secruityToken, TARGET_SECURITY_TOKEN_AUTH_SCHEME);
        return null;
    }

    @Override
    public Collection<HonoCredentials> getPreAuthenticatedCredentials(final DmfTenantSecurityToken securityToken) {
        Object response;
        try {
            URL url = new URL(honoCredentialsEndpoint
                    .replace("$tenantId", securityToken.getTenant())
                    .replace("$deviceId", resolveControllerId(securityToken)));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                return null;
            }
            JSONParser parser = new JSONParser();
            response = parser.parse(connection.getInputStream());
        }
        catch (IOException | ParseException e) {
            return null;
        }

        ArrayList<HonoCredentials> honoCredentials = new ArrayList<>();

        if (response instanceof JSONArray) {
            JSONArray credentialsArray = (JSONArray) response;
            for (Object object : credentialsArray) {
                JSONObject credentials = (JSONObject) object;

                String deviceId = credentials.getAsString("device-id");
                String type = credentials.getAsString("type");
                String authId = credentials.getAsString("auth-id");
                Boolean enabled = (Boolean) credentials.getOrDefault("enabled", true);
                JSONArray secrets = (JSONArray) credentials.get("secrets");

                ArrayList<HonoSecret> honoSecrets = new ArrayList<>();
                switch (type) {
                    case "hashed-password":
                        for (Object secretObject : secrets) {
                            JSONObject secret = (JSONObject) secretObject;
                            String notAfter = secret.getAsString("not-after");
                            String notBefore = secret.getAsString("not-before");
                            String pwdHash = secret.getAsString("pwd-hash");
                            String salt = secret.getAsString("salt");
                            String hashFunction = (String) secret.getOrDefault("hash-function", "sha-256");

                            honoSecrets.add(new HonoPasswordSecret(notBefore, notAfter, hashFunction, salt, pwdHash));
                        }
                        break;

                    case "psk":
                        for (Object secretObject : secrets) {
                            JSONObject secret = (JSONObject) secretObject;
                            String notAfter = secret.getAsString("not-after");
                            String notBefore = secret.getAsString("not-before");
                            String key = secret.getAsString("key");

                            honoSecrets.add(new HonoPreSharedKey(notBefore, notAfter, key));
                        }
                        break;

                    case "x509-cert":
                        for (Object secretObject : secrets) {
                            JSONObject secret = (JSONObject) secretObject;
                            String notAfter = secret.getAsString("not-after");
                            String notBefore = secret.getAsString("not-before");

                            honoSecrets.add(new HonoX509Certificate(notBefore, notAfter));
                        }

                    default:
                        // skip this credentials entry as it is not supported
                        continue;
                }
                honoCredentials.add(new HonoCredentials(deviceId, type, authId, enabled, honoSecrets));
            }
        }

        return honoCredentials;
    }

    private String resolveControllerId(final DmfTenantSecurityToken securityToken) {
        if (securityToken.getControllerId() != null) {
            return securityToken.getControllerId();
        }
        final Optional<Target> foundTarget = systemSecurityContext.runAsSystemAsTenant(
                () -> controllerManagement.get(securityToken.getTargetId()), securityToken.getTenant());
        return foundTarget.map(Target::getControllerId).orElse(null);
    }

    @Override
    protected String getTenantConfigurationKey() {
        return TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED;
    }
}
