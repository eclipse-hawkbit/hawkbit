/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

/**
 * {@link CurrentTenantIdentifierResolver} and {@link HibernatePropertiesCustomizer} that resolves the
 * {@link TenantAware#getCurrentTenant()} for hibernate.
 */
class TenantIdentifier implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        // on bootstrapping hibernate requests tenant and want to be non-null
        return Optional.ofNullable(TenantAware.getCurrentTenant()).map(String::toUpperCase).orElse("");
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}