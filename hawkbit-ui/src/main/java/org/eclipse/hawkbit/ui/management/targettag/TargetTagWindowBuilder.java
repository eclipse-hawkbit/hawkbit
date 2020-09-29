/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for target tag window
 */
public class TargetTagWindowBuilder extends AbstractEntityWindowBuilder<ProxyTag> {

    private final TargetTagManagement targetTagManagement;

    /**
     * Constructor for TargetTagWindowBuilder
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param targetTagManagement
     *            TargetTagManagement
     */
    public TargetTagWindowBuilder(final UIConfiguration uiConfig, final TargetTagManagement targetTagManagement) {
        super(uiConfig);

        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.TAG_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(new AddTargetTagWindowController(uiConfig, targetTagManagement,
                new TagWindowLayout<ProxyTag>(uiConfig)));

    }

    @Override
    public Window getWindowForUpdate(final ProxyTag proxyTag) {
        return getWindowForEntity(proxyTag, new UpdateTargetTagWindowController(uiConfig, targetTagManagement,
                new TagWindowLayout<ProxyTag>(uiConfig)));
    }
}
