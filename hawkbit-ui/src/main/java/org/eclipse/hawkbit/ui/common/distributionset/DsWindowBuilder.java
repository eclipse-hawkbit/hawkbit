/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.distributionset;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.distributions.dstable.AddDsWindowController;
import org.eclipse.hawkbit.ui.distributions.dstable.DsWindowLayout;
import org.eclipse.hawkbit.ui.distributions.dstable.UpdateDsWindowController;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for distribution set window
 */
public class DsWindowBuilder extends AbstractEntityWindowBuilder<ProxyDistributionSet> {
    private final SystemManagement systemManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final TenantConfigurationManagement tenantConfigurationManagement;

    private final DistributionSetManagement dsManagement;
    private final DistributionSetTypeManagement dsTypeManagement;

    private final EventView view;

    /**
     * Constructor for DsWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param systemManagement
     *            SystemManagement
     * @param systemSecurityContext
     *            SystemSecurityContext
     * @param tenantConfigurationManagement
     *            TenantConfigurationManagement
     * @param dsManagement
     *            DistributionSetManagement
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param view
     *            EventView
     */
    public DsWindowBuilder(final CommonUiDependencies uiDependencies, final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final DistributionSetManagement dsManagement, final DistributionSetTypeManagement dsTypeManagement,
            final EventView view) {
        super(uiDependencies);

        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantConfigurationManagement = tenantConfigurationManagement;

        this.dsManagement = dsManagement;
        this.dsTypeManagement = dsTypeManagement;

        this.view = view;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.CREATE_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(new AddDsWindowController(uiDependencies, systemManagement, dsManagement,
                new DsWindowLayout(uiDependencies.getI18n(), systemSecurityContext, tenantConfigurationManagement,
                        dsTypeManagement),
                view));

    }

    @Override
    public Window getWindowForUpdate(final ProxyDistributionSet proxyDs) {
        return getWindowForEntity(proxyDs, new UpdateDsWindowController(uiDependencies, dsManagement,
                new DsWindowLayout(getI18n(), systemSecurityContext, tenantConfigurationManagement, dsTypeManagement)));
    }
}
