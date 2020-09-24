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
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractMetaDataWindowBuilder;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetaDataWindowLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Builder for DistributionSet meta data window
 */
public class DsMetaDataWindowBuilder extends AbstractMetaDataWindowBuilder<Long> {
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;
    private final SpPermissionChecker permChecker;

    private final DistributionSetManagement dsManagement;

    /**
     * Constructor for DsMetaDataWindowBuilder
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param uiNotification
     *            UINotification
     * @param permChecker
     *            SpPermissionChecker
     * @param dsManagement
     *            DistributionSetManagement
     */
    public DsMetaDataWindowBuilder(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification, final SpPermissionChecker permChecker,
            final DistributionSetManagement dsManagement) {
        super(i18n);

        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
        this.permChecker = permChecker;

        this.dsManagement = dsManagement;
    }

    /**
     * Get the distribution set window
     *
     * @param dsId
     *            Distribution set id
     * @param name
     *            Distribution set name
     *
     * @return Dialog window
     */
    public Window getWindowForShowDsMetaData(final Long dsId, final String name) {
        return getWindowForShowDsMetaData(dsId, name, null);
    }

    /**
     * Get the distribution set window
     *
     * @param dsId
     *            Distribution set id
     * @param name
     *            Distribution set name
     * @param proxyMetaData
     *            Meta data
     *
     * @return Dialog window
     */
    public Window getWindowForShowDsMetaData(final Long dsId, final String name, final ProxyMetaData proxyMetaData) {
        return getWindowForShowMetaData(
                new DsMetaDataWindowLayout(i18n, eventBus, permChecker, uiNotification, entityFactory, dsManagement),
                dsId, name, proxyMetaData);
    }
}
