/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security.uaa;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;

/**
 * Implementation which maps principals from the OAUTH2 access tokens to the
 * hawkBit {@link UserPrincipal}, so hawkBit is able to work with the user and
 * tenant information. Additional extracting the {@code scope}-list from the JWT
 * into {@link GrantedAuthority}.
 * 
 * This implementation concentrates all necessary authentication, principal and
 * authorities mapping within the OAUTH2 workflow, e.g. using redirect login
 * form or using the REST-API with a access bearer token.
 */
public class UserPrincipalInfoTokenServices extends UserInfoTokenServices
        implements UserAuthenticationConverter, AuthoritiesExtractor {

    private final OAuth2ClientContext oauth2ClientContext;
    private final JsonParser jsonParser = JsonParserFactory.create();

    /**
     * Constructor.
     * 
     * @param userInfoEndpointUrl
     *            the OAUTH2 info endpoint to retrieve user information
     * @param clientId
     *            the OAUTH2 client-id to execute the user info endpoint
     * @param oauth2ClientContext
     *            the spring {@link OAuth2ClientContext}
     */
    public UserPrincipalInfoTokenServices(final String userInfoEndpointUrl, final String clientId,
            final OAuth2ClientContext oauth2ClientContext) {
        super(userInfoEndpointUrl, clientId);
        this.oauth2ClientContext = oauth2ClientContext;
    }

    @Override
    protected Object getPrincipal(final Map<String, Object> map) {
        final String username = String.valueOf(map.get("user_name"));
        final String firstname = String.valueOf(map.get("given_name"));
        final String lastname = String.valueOf(map.get("family_name"));
        final String email = String.valueOf(map.get("email"));
        final String zoneId = String.valueOf(getAccessTokenMap().get("zid"));
        return new UserPrincipal(username, firstname, lastname, username, email, zoneId);
    }

    @Override
    public Map<String, ?> convertUserAuthentication(final Authentication userAuthentication) {
        throw new UnsupportedOperationException("converting an authentication object to a map is not implemented");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Authentication extractAuthentication(final Map<String, ?> map) {
        final Object principal = getPrincipal((Map<String, Object>) map);
        return new UsernamePasswordAuthenticationToken(principal, "N/A", extractAuthorities((Map<String, Object>) map));
    }

    @Override
    public List<GrantedAuthority> extractAuthorities(final Map<String, Object> map) {
        final Map<String, Object> accessTokenMap;
        if (map.containsKey("scope")) {
            accessTokenMap = map;
        } else {
            accessTokenMap = getAccessTokenMap();
        }
        @SuppressWarnings("unchecked")
        final List<String> scopes = (List<String>) accessTokenMap.get("scope");
        return scopes.stream().map(scope -> new SimpleGrantedAuthority(scope)).collect(Collectors.toList());

    }

    private Map<String, Object> getAccessTokenMap() {
        final Map<String, Object> accessTokenMap;
        final OAuth2AccessToken accessToken = oauth2ClientContext.getAccessToken();
        final Jwt decode = JwtHelper.decode(accessToken.getValue());
        accessTokenMap = jsonParser.parseMap(decode.getClaims());
        return accessTokenMap;
    }
}
