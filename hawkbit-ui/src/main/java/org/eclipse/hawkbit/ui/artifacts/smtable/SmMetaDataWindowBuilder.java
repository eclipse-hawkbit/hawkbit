/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractMetaDataWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Builder for Software module meta data windows
 */
public class SmMetaDataWindowBuilder extends AbstractMetaDataWindowBuilder<Long> {
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;
    private final SpPermissionChecker permChecker;

    private final SoftwareModuleManagement smManagement;

    /**
     * Constructor for SmMetaDataWindowBuilder
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
     * @param smManagement
     *            SoftwareModuleManagement
     */
    public SmMetaDataWindowBuilder(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification, final SpPermissionChecker permChecker,
            final SoftwareModuleManagement smManagement) {
        super(i18n);

        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
        this.permChecker = permChecker;

        this.smManagement = smManagement;
    }

    /**
     * Get software module window without proxy meta data
     *
     * @param smId
     *            software module ID
     * @param name
     *            Selected entity name
     *
     * @return software module window
     */
    public Window getWindowForShowSmMetaData(final Long smId, final String name) {
        return getWindowForShowSmMetaData(smId, name, null);
    }

    /**
     * Get software module window with proxy meta data
     *
     * @param smId
     *            software module ID
     * @param name
     *            Selected entity name
     * @param proxyMetaData
     *            ProxyMetaData
     *
     * @return software module window
     */
    public Window getWindowForShowSmMetaData(final Long smId, final String name, final ProxyMetaData proxyMetaData) {
        return getWindowForShowMetaData(
                new SmMetaDataWindowLayout(i18n, eventBus, permChecker, uiNotification, entityFactory, smManagement),
                smId, name, proxyMetaData);
    }
}
