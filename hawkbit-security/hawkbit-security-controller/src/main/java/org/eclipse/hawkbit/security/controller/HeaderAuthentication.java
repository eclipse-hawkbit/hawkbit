/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security.controller;

/**
 * The authentication principal and credentials object which holds the
 * controller-id and the authority name from the http-headers as principal or
 * from the http-url and tenant configuration for the credentials.
 */
final class HeaderAuthentication {

    private final String controllerId;
    private final String headerAuth;

    HeaderAuthentication(final String controllerId, final String headerAuth) {
        this.controllerId = controllerId;
        this.headerAuth = headerAuth;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((controllerId == null) ? 0 : controllerId.hashCode());
        result = prime * result + ((headerAuth == null) ? 0 : headerAuth.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HeaderAuthentication other = (HeaderAuthentication) obj;
        if (controllerId == null) {
            if (other.controllerId != null) {
                return false;
            }
        } else if (!controllerId.equals(other.controllerId)) {
            return false;
        }
        if (headerAuth == null) {
            if (other.headerAuth != null) {
                return false;
            }
        } else if (!headerAuth.equals(other.headerAuth)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        // only the controller ID because the principal is stored as string for
        // audit information
        // etc.
        return controllerId;
    }

}
