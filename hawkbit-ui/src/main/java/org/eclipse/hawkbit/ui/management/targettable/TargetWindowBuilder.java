/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for target window
 */
public class TargetWindowBuilder extends AbstractEntityWindowBuilder<ProxyTarget> {

    private final TargetManagement targetManagement;

    private final TargetTypeManagement targetTypeManagement;

    private final EventView view;

    /**
     * Constructor for TargetWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param targetTypeManagement
     *            TargetTypeManagement
     * @param view
     *            EventView
     */
    public TargetWindowBuilder(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement,
                               final TargetTypeManagement targetTypeManagement, final EventView view) {
        super(uiDependencies);

        this.targetManagement = targetManagement;
        this.targetTypeManagement = targetTypeManagement;
        this.view = view;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.CREATE_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(
                new AddTargetWindowController(uiDependencies, targetManagement, new TargetWindowLayout(getI18n(), targetTypeManagement), view));

    }

    @Override
    public Window getWindowForUpdate(final ProxyTarget proxyTarget) {
        return getWindowForEntity(proxyTarget,
                new UpdateTargetWindowController(uiDependencies, targetManagement, new TargetWindowLayout(getI18n(), targetTypeManagement)));
    }
}
