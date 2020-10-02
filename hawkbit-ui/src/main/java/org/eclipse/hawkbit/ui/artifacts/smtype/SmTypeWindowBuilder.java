/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for software module type window
 */
public class SmTypeWindowBuilder extends AbstractEntityWindowBuilder<ProxyType> {

    private final SoftwareModuleTypeManagement smTypeManagement;

    /**
     * Constructor for SmTypeWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public SmTypeWindowBuilder(final CommonUiDependencies uiDependencies, final SoftwareModuleTypeManagement smTypeManagement) {
        super(uiDependencies);
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
        return getWindowForNewEntity(
                new AddSmTypeWindowController(uiDependencies, smTypeManagement, new SmTypeWindowLayout(uiDependencies)));

    }

    /**
     * Update window for software module type
     *
     * @param proxyType
     *            ProxyType
     *
     * @return Window of Software module type
     */
    @Override
    public Window getWindowForUpdate(final ProxyType proxyType) {
        return getWindowForEntity(proxyType,
                new UpdateSmTypeWindowController(uiDependencies, smTypeManagement, new SmTypeWindowLayout(uiDependencies)));
    }
}
