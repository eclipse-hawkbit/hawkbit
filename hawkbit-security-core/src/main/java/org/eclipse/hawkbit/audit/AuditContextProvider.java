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

import java.util.Optional;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public class AuditContextProvider {

    private static final AuditContextProvider INSTANCE = new AuditContextProvider();

    private TenantAware.TenantResolver resolver;
    private AuditorAware<String> auditorAware;

    public static AuditContextProvider getInstance() {
        return INSTANCE;
    }

    @Autowired
    public void setTenantResolver(final TenantAware.TenantResolver resolver) {
        this.resolver = resolver;
    }

    @Autowired
    public void setAuditorAware(final AuditorAware<String> auditorAware) {
        this.auditorAware = auditorAware;
    }

    public AuditContext getAuditContext() {
        return new AuditContext(
                Optional.ofNullable(resolver.resolveTenant()).orElse("n/a"),
                Optional.ofNullable(auditorAware).flatMap(AuditorAware::getCurrentAuditor).orElse("system"));
    }

    public record AuditContext(String tenant, String username) {}
}