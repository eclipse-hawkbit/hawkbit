/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Builder for target window
 */
public class TargetWindowBuilder extends AbstractEntityWindowBuilder<ProxyTarget> {
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;

    private final TargetManagement targetManagement;

    private final EventView view;

    /**
     * Constructor for TargetWindowBuilder
     *
     * @param i18n
     *          VaadinMessageSource
     * @param entityFactory
     *          EntityFactory
     * @param eventBus
     *          UIEventBus
     * @param uiNotification
     *          UINotification
     * @param targetManagement
     *          TargetManagement
     * @param view
     *          EventView
     */
    public TargetWindowBuilder(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification, final TargetManagement targetManagement,
            final EventView view) {
        super(i18n);

        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;

        this.targetManagement = targetManagement;

        this.view = view;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.CREATE_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(new AddTargetWindowController(i18n, entityFactory, eventBus, uiNotification,
                targetManagement, new TargetWindowLayout(i18n), view));

    }

    @Override
    public Window getWindowForUpdate(final ProxyTarget proxyTarget) {
        return getWindowForEntity(proxyTarget, new UpdateTargetWindowController(i18n, entityFactory, eventBus,
                uiNotification, targetManagement, new TargetWindowLayout(i18n)));
    }
}
