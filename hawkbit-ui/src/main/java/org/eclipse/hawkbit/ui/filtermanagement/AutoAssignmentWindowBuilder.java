/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;
import org.eclipse.hawkbit.utils.TenantConfigHelper;

/**
 * Builder for auto assignment window
 */
public class AutoAssignmentWindowBuilder extends AbstractEntityWindowBuilder<ProxyTargetFilterQuery> {

    private final TargetManagement targetManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final DistributionSetManagement dsManagement;
    private final TenantConfigHelper tenantConfigHelper;
    private final TenantAware tenantAware;

    /**
     * Constructor for AutoAssignmentWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param targetFilterQueryManagement
     *            TargetFilterQueryManagement
     * @param dsManagement
     *            DistributionSetManagement
     */
    public AutoAssignmentWindowBuilder(final CommonUiDependencies uiDependencies,
            final TargetManagement targetManagement, final TargetFilterQueryManagement targetFilterQueryManagement,
            final DistributionSetManagement dsManagement, final TenantConfigHelper tenantConfigHelper,
            final TenantAware tenantAware) {
        super(uiDependencies);

        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.dsManagement = dsManagement;
        this.tenantConfigHelper = tenantConfigHelper;
        this.tenantAware = tenantAware;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.DIST_SET_SELECT_WINDOW_ID;
    }

    /**
     * Gets the auto assigment window
     *
     * @param proxyTargetFilter
     *            ProxyTargetFilterQuery
     *
     * @return Common dialog window
     */
    public Window getWindowForAutoAssignment(final ProxyTargetFilterQuery proxyTargetFilter) {
        return getWindowForEntity(proxyTargetFilter,
                new AutoAssignmentWindowController(uiDependencies, targetManagement, targetFilterQueryManagement,
                        tenantConfigHelper, tenantAware,
                        new AutoAssignmentWindowLayout(getI18n(), dsManagement, tenantConfigHelper)));
    }

    @Override
    public Window getWindowForAdd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Window getWindowForUpdate(final ProxyTargetFilterQuery entity) {
        throw new UnsupportedOperationException();
    }
}
