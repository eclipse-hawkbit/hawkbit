/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetQueryExecutionManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;

/**
 * Parameter struct used to pass the services, needed by different UI components, through the constructors.
 */
public class RolloutServiceContext {
    private final RolloutManagement rolloutManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final TargetManagement targetManagement;
    private final TargetQueryExecutionManagement targetQueryExecutionManagement;
    private final EntityFactory entityFactory;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final QuotaManagement quotaManagement;
    private final TenantConfigurationManagement tenantConfigManagement;

    public RolloutServiceContext(final RolloutManagement rolloutManagement,
            final RolloutGroupManagement rolloutGroupManagement, final TargetManagement targetManagement,
            final TargetQueryExecutionManagement targetQueryExecutionManagement, final EntityFactory entityFactory,
            final TargetFilterQueryManagement targetFilterQueryManagement, final QuotaManagement quotaManagement,
            final TenantConfigurationManagement tenantConfigManagement) {
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.targetManagement = targetManagement;
        this.targetQueryExecutionManagement = targetQueryExecutionManagement;
        this.entityFactory = entityFactory;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.quotaManagement = quotaManagement;
        this.tenantConfigManagement = tenantConfigManagement;
    }

    public RolloutManagement getRolloutManagement() {
        return rolloutManagement;
    }

    public RolloutGroupManagement getRolloutGroupManagement() {
        return rolloutGroupManagement;
    }

    public TargetManagement getTargetManagement() {
        return targetManagement;
    }

    public TargetQueryExecutionManagement getTargetQueryExecutionManagement() {
        return targetQueryExecutionManagement;
    }

    public EntityFactory getEntityFactory() {
        return entityFactory;
    }

    public TargetFilterQueryManagement getTargetFilterQueryManagement() {
        return targetFilterQueryManagement;
    }

    public QuotaManagement getQuotaManagement() {
        return quotaManagement;
    }

    public TenantConfigurationManagement getTenantConfigManagement() {
        return tenantConfigManagement;
    }
}
