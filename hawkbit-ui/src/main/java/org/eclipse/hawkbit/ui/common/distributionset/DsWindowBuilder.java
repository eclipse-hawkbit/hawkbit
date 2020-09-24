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
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.distributions.dstable.AddDsWindowController;
import org.eclipse.hawkbit.ui.distributions.dstable.DsWindowLayout;
import org.eclipse.hawkbit.ui.distributions.dstable.UpdateDsWindowController;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Builder for distribution set window
 */
public class DsWindowBuilder extends AbstractEntityWindowBuilder<ProxyDistributionSet> {
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;
    private final SystemManagement systemManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final TenantConfigurationManagement tenantConfigurationManagement;

    private final DistributionSetManagement dsManagement;
    private final DistributionSetTypeManagement dsTypeManagement;

    private final EventView view;

    /**
     * Constructor for DsWindowBuilder
     *
     * @param i18n
     *          VaadinMessageSource
     * @param entityFactory
     *          EntityFactory
     * @param eventBus
     *          UIEventBus
     * @param uiNotification
     *          UINotification
     * @param systemManagement
     *          SystemManagement
     * @param systemSecurityContext
     *          SystemSecurityContext
     * @param tenantConfigurationManagement
     *          TenantConfigurationManagement
     * @param dsManagement
     *          DistributionSetManagement
     * @param dsTypeManagement
     *          DistributionSetTypeManagement
     * @param view
     *          EventView
     */
    public DsWindowBuilder(final VaadinMessageSource i18n, final EntityFactory entityFactory, final UIEventBus eventBus,
            final UINotification uiNotification, final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final DistributionSetManagement dsManagement, final DistributionSetTypeManagement dsTypeManagement,
            final EventView view) {
        super(i18n);

        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
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
        return getWindowForNewEntity(new AddDsWindowController(i18n, entityFactory, eventBus, uiNotification,
                systemManagement, dsManagement,
                new DsWindowLayout(i18n, systemSecurityContext, tenantConfigurationManagement, dsTypeManagement),
                view));

    }

    @Override
    public Window getWindowForUpdate(final ProxyDistributionSet proxyDs) {
        return getWindowForEntity(proxyDs, new UpdateDsWindowController(i18n, entityFactory, eventBus, uiNotification,
                dsManagement,
                new DsWindowLayout(i18n, systemSecurityContext, tenantConfigurationManagement, dsTypeManagement)));
    }
}
