/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security.uaa;

import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;

/**
 * Configuration properties for setting the UAA OAUTH2 client-properties and
 * resource-properties.
 * 
 * <pre>
    uaa.client.clientId=app
    uaa.client.clientSecret=appsecret
    uaa.client.accessTokenUri=http://localhost:8080/uaa/oauth/token
    uaa.client.userAuthorizationUri=http://localhost:8080/uaa/oauth/authorize
    uaa.client.clientAuthenticationScheme=form
    uaa.resource.userInfoUri=http://localhost:8080/uaa/userinfo
    uaa.resource.jwt.keyValue=abc
 * </pre>
 */
@ConfigurationProperties("uaa")
public class UaaClientProperties {

    @NestedConfigurationProperty
    private final AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();

    @NestedConfigurationProperty
    private final ResourceServerProperties resource = new ResourceServerProperties();

    public AuthorizationCodeResourceDetails getClient() {
        return client;
    }

    public ResourceServerProperties getResource() {
        return resource;
    }
}
