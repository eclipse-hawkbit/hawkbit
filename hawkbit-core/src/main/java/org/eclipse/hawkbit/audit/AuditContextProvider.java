/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.audit;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditContextProvider {

    private final TenantAware.DefaultTenantResolver resolver = new TenantAware.DefaultTenantResolver();

    public AuditContext getAuditContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "system";
        String tenant = resolver.resolveTenant();
        return new AuditContext(tenant, username);
    }
    public record AuditContext(String tenant, String username) {
    }
}
