/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.report.model.TenantUsage;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for statistics of a single tenant.
 *
 */
public interface TenantStatsManagement {

    /**
     * Service for stats of a single tenant. Opens a new transaction and as a
     * result can an be used for multiple tenants, i.e. to allow in one session
     * to collect data of all tenants in the system.
     *
     * @param tenant
     *            to collect for
     * @return collected statistics
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    TenantUsage getStatsOfTenant(String tenant);

}