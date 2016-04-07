/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;

/**
 * A Filter for device which download via coap.
 * 
 *
 *
 */
public class CoapAnonymousPreAuthenticatedFilter implements PreAuthenficationFilter {

    @Override
    public HeaderAuthentication getPreAuthenticatedPrincipal(final TenantSecurityToken secruityToken) {
        return new HeaderAuthentication(secruityToken.getControllerId(), TenantSecurityToken.COAP_TOKEN_VALUE);
    }

    @Override
    public HeaderAuthentication getPreAuthenticatedCredentials(final TenantSecurityToken secruityToken) {
        return new HeaderAuthentication(secruityToken.getControllerId(), TenantSecurityToken.COAP_TOKEN_VALUE);
    }

    @Override
    public boolean isEnable(final TenantSecurityToken secruityToken) {
        final String authHeader = secruityToken.getHeader(TenantSecurityToken.COAP_AUTHORIZATION_HEADER);
        return TenantSecurityToken.COAP_TOKEN_VALUE.equals(authHeader);
    }

}
