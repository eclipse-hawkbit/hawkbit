/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.report.TenantUsage;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for statistics of a single tenant.
 */
@FunctionalInterface
public interface TenantStatsManagement {

    /**
     * Service for stats of the current tenant.
     *
     * @return collected statistics
     */
    @PreAuthorize(
            "hasAuthority('" + SpRole.TENANT_ADMIN + "')" + " or " +
            SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + " or " +
            SpringEvalExpressions.IS_SYSTEM_CODE)
    TenantUsage getStatsOfTenant();
}