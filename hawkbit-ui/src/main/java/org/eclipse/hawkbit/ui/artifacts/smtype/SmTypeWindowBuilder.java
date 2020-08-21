/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Builder for software module type window
 */
public class SmTypeWindowBuilder extends AbstractEntityWindowBuilder<ProxyType> {
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;

    private final SoftwareModuleTypeManagement smTypeManagement;

    /**
     * Constructor for SmTypeWindowBuilder
     *
     * @param i18n
     *          VaadinMessageSource
     * @param entityFactory
     *          EntityFactory
     * @param eventBus
     *          UIEventBus
     * @param uiNotification
     *          UINotification
     * @param smTypeManagement
     *          SoftwareModuleTypeManagement
     */
    public SmTypeWindowBuilder(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification,
            final SoftwareModuleTypeManagement smTypeManagement) {
        super(i18n);

        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;

        this.smTypeManagement = smTypeManagement;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.TAG_POPUP_ID;
    }

    /**
     * Add window for software module type
     *
     * @return Window of Software module type
     */
    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(new AddSmTypeWindowController(i18n, entityFactory, eventBus, uiNotification,
                smTypeManagement, new SmTypeWindowLayout(i18n, uiNotification)));

    }

    /**
     * Update window for software module type
     *
     * @param proxyType
     *          ProxyType
     *
     * @return Window of Software module type
     */
    @Override
    public Window getWindowForUpdate(final ProxyType proxyType) {
        return getWindowForEntity(proxyType, new UpdateSmTypeWindowController(i18n, entityFactory, eventBus,
                uiNotification, smTypeManagement, new SmTypeWindowLayout(i18n, uiNotification)));
    }
}
