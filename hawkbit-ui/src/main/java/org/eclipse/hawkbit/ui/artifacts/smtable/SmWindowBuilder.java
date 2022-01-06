/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for Software module windows
 */
public class SmWindowBuilder extends AbstractEntityWindowBuilder<ProxySoftwareModule> {

    private final SoftwareModuleManagement smManagement;
    private final SoftwareModuleTypeManagement smTypeManagement;

    private final EventView view;

    /**
     * Constructor for SmWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smManagement
     *            SoftwareModuleManagement
     * @param smTypeManagement
     *            SoftwareModuleTypeManagement
     * @param view
     *            EventView
     */
    public SmWindowBuilder(final CommonUiDependencies uiDependencies, final SoftwareModuleManagement smManagement,
            final SoftwareModuleTypeManagement smTypeManagement, final EventView view) {
        super(uiDependencies);

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
        return getWindowForNewEntity(new AddSmWindowController(uiDependencies, smManagement,
                new SmWindowLayout(getI18n(), smTypeManagement), view));

    }

    /**
     * @param proxySm
     *            ProxySoftwareModule
     *
     * @return update window for software module
     */
    @Override
    public Window getWindowForUpdate(final ProxySoftwareModule proxySm) {
        return getWindowForEntity(proxySm, new UpdateSmWindowController(uiDependencies, smManagement,
                new SmWindowLayout(getI18n(), smTypeManagement)));
    }
}
