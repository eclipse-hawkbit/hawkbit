/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import com.vaadin.ui.Window;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

/**
 * Builder for target type window
 */
public class TargetTypeWindowBuilder extends AbstractEntityWindowBuilder<ProxyTargetType> {

    private final TargetTypeManagement targetTypeManagement;
    private final DistributionSetTypeManagement dsTypeManagement;
    private final ContextAware contextRunner;

    /**
     * Constructor for TargetTypeWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTypeManagement
     *            TargetTypeManagement
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param contextRunner
     *            ContextRunner
     */
    public TargetTypeWindowBuilder(final CommonUiDependencies uiDependencies,
            final TargetTypeManagement targetTypeManagement, final DistributionSetTypeManagement dsTypeManagement,
            final ContextAware contextRunner) {
        super(uiDependencies);

        this.targetTypeManagement = targetTypeManagement;
        this.dsTypeManagement = dsTypeManagement;
        this.contextRunner = contextRunner;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.TAG_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        CommonDialogWindow window = getWindowForNewEntity(new AddTargetTypeWindowController(uiDependencies,
                targetTypeManagement, new TargetTypeWindowLayout(uiDependencies, dsTypeManagement), contextRunner));
        window.hideMandatoryExplanation();
        return window;
    }

    @Override
    public Window getWindowForUpdate(final ProxyTargetType proxyType) {
        CommonDialogWindow window = getWindowForEntity(proxyType, new UpdateTargetTypeWindowController(uiDependencies,
                targetTypeManagement, new TargetTypeWindowLayout(uiDependencies, dsTypeManagement), contextRunner));
        window.hideMandatoryExplanation();
        return window;
    }
}
