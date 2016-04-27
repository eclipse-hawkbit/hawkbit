/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client.authenctication;

import com.google.common.net.HttpHeaders;

import feign.RequestTemplate;

/**
 * Implementation of the {@link AuthenticationInterceptor} to add a given
 * target-security-token to the HTTP authorization header.
 */
class TargetSecurityTokenAuthenticationInterceptor implements AuthenticationInterceptor {

    private final String targetSecurityToken;

    /**
     * @param targetSecurityToken
     *            the security token to add to the authorization header
     */
    TargetSecurityTokenAuthenticationInterceptor(final String targetSecurityToken) {
        this.targetSecurityToken = targetSecurityToken;
    }

    @Override
    public void apply(final RequestTemplate template) {
        template.header(HttpHeaders.AUTHORIZATION, "TargetToken " + targetSecurityToken);
    }
}
