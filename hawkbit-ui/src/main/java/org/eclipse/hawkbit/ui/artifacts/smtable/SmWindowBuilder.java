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
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Builder for Software module windows
 */
public class SmWindowBuilder extends AbstractEntityWindowBuilder<ProxySoftwareModule> {
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;

    private final SoftwareModuleManagement smManagement;
    private final SoftwareModuleTypeManagement smTypeManagement;

    private final EventView view;

    /**
     * Constructor for SmWindowBuilder
     *
     * @param i18n
     *          VaadinMessageSource
     * @param entityFactory
     *          EntityFactory
     * @param eventBus
     *          UIEventBus
     * @param uiNotification
     *          UINotification
     * @param smManagement
     *          SoftwareModuleManagement
     * @param smTypeManagement
     *          SoftwareModuleTypeManagement
     * @param view
     *          EventView
     */
    public SmWindowBuilder(final VaadinMessageSource i18n, final EntityFactory entityFactory, final UIEventBus eventBus,
            final UINotification uiNotification, final SoftwareModuleManagement smManagement,
            final SoftwareModuleTypeManagement smTypeManagement, final EventView view) {
        super(i18n);

        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;

        this.smManagement = smManagement;
        this.smTypeManagement = smTypeManagement;

        this.view = view;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.CREATE_POPUP_ID;
    }

    /**
     * @return add window for software module
     */
    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(new AddSmWindowController(i18n, entityFactory, eventBus, uiNotification,
                smManagement, new SmWindowLayout(i18n, smTypeManagement), view));

    }

    /**
     * @param proxySm
     *          ProxySoftwareModule
     *
     * @return update window for software module
     */
    @Override
    public Window getWindowForUpdate(final ProxySoftwareModule proxySm) {
        return getWindowForEntity(proxySm, new UpdateSmWindowController(i18n, entityFactory, eventBus, uiNotification,
                smManagement, new SmWindowLayout(i18n, smTypeManagement)));
    }
}
