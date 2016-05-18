/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.client.authenctication;

/**
 * A factory to create {@link AuthenticationInterceptor}s.
 */
public class AuthenticationInterceptorFactory {

    private AuthenticationInterceptorFactory() {
        // factory class no public constructor
    }

    /**
     * Creates a new {@link AuthenticationInterceptor} to authenticate a
     * Ddi-Client via a target-security-token HTTP authorization header.
     * 
     * @param targetSecurityToken
     *            the target-security-token to be added to the HTTP
     *            'TargetToken' authorization header
     * @return the authentication interceptor which can be used to authenticate
     *         an Ddi-Client
     */
    public static AuthenticationInterceptor createTargetSecurityAuthenticator(final String targetSecurityToken) {
        return new TargetSecurityTokenAuthenticationInterceptor(targetSecurityToken);
    }

}
