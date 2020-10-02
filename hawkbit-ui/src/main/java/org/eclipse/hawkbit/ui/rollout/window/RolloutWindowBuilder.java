/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window;

import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.rollout.window.controllers.AddRolloutWindowController;
import org.eclipse.hawkbit.ui.rollout.window.controllers.ApproveRolloutWindowController;
import org.eclipse.hawkbit.ui.rollout.window.controllers.CopyRolloutWindowController;
import org.eclipse.hawkbit.ui.rollout.window.controllers.UpdateRolloutWindowController;
import org.eclipse.hawkbit.ui.rollout.window.layouts.AddRolloutWindowLayout;
import org.eclipse.hawkbit.ui.rollout.window.layouts.ApproveRolloutWindowLayout;
import org.eclipse.hawkbit.ui.rollout.window.layouts.UpdateRolloutWindowLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for Add/Approve/Update/Copy Rollout windows.
 */
public final class RolloutWindowBuilder extends AbstractEntityWindowBuilder<ProxyRollout> {

    private final RolloutWindowDependencies dependencies;

    /**
     * Constructor for RolloutWindowBuilder
     *
     * @param rolloutWindowDependencies
     *            RolloutWindowDependencies
     */
    public RolloutWindowBuilder(final RolloutWindowDependencies rolloutWindowDependencies) {
        super(rolloutWindowDependencies.getuiDependencies());

        this.dependencies = rolloutWindowDependencies;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.ROLLOUT_POPUP_ID;
    }

    @Override
    protected String getHelpLink() {
        return dependencies.getUiProperties().getLinks().getDocumentation().getRolloutView();
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(
                new AddRolloutWindowController(dependencies, new AddRolloutWindowLayout(dependencies)));
    }

    /**
     * Gets the copy rollout window
     *
     * @param proxyRollout
     *            ProxyRollout
     *
     * @return Common dialog window to copy rollout
     */
    public Window getWindowForCopyRollout(final ProxyRollout proxyRollout) {
        return getWindowForEntity(proxyRollout,
                new CopyRolloutWindowController(dependencies, new AddRolloutWindowLayout(dependencies)));
    }

    @Override
    public Window getWindowForUpdate(final ProxyRollout proxyRollout) {
        return getWindowForEntity(proxyRollout,
                new UpdateRolloutWindowController(dependencies, new UpdateRolloutWindowLayout(dependencies)));
    }

    /**
     * Gets the approval rollout window
     *
     * @param proxyRollout
     *            ProxyRollout
     *
     * @return Common dialog window to approve rollout
     */
    public Window getWindowForApproveRollout(final ProxyRollout proxyRollout) {
        return getWindowForEntity(proxyRollout,
                new ApproveRolloutWindowController(dependencies, new ApproveRolloutWindowLayout(dependencies)));
    }
}
