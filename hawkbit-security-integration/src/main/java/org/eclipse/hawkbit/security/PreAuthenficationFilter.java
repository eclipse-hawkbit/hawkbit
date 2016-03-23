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
 * Interface for Pre Authenfication.
 * 
 *
 *
 */
public interface PreAuthenficationFilter {

    /**
     * Check if the filter is enabled.
     * 
     * @param secruityToken
     *            the secruity info
     * @return <true> is enabled <false> diabled
     */
    boolean isEnable(TenantSecurityToken secruityToken);

    /**
     * Extract the principal information from the current secruityToken.
     * 
     * @param secruityToken
     *            the secruityToken
     * @return the extracted tenant and controller id
     */
    HeaderAuthentication getPreAuthenticatedPrincipal(TenantSecurityToken secruityToken);

    /**
     * Extract the principal credentials from the current secruityToken.
     * 
     * @param secruityToken
     *            the secruityToken
     * @return the extracted tenant and controller id
     */
    HeaderAuthentication getPreAuthenticatedCredentials(TenantSecurityToken secruityToken);

}
