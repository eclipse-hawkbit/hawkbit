/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import com.vaadin.ui.Window;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

/**
 * Builder for distribution set type window
 */
public class TargetTypeWindowBuilder extends AbstractEntityWindowBuilder<ProxyType> {

    private final TargetTypeManagement targetTypeManagement;
    private final TargetManagement targetManagement;
    private final DistributionSetTypeManagement dsTypeManagement;

    /**
     * Constructor for TargetTypeWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param targetManagement
     *            DistributionSetManagement
     * @param targetTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public TargetTypeWindowBuilder(final CommonUiDependencies uiDependencies, final TargetTypeManagement targetTypeManagement,
                                   final TargetManagement targetManagement, final DistributionSetTypeManagement dsTypeManagement) {
        super(uiDependencies);

        this.targetTypeManagement = targetTypeManagement;
        this.targetManagement = targetManagement;
        this.dsTypeManagement = dsTypeManagement;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.TAG_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(new AddTargetTypeWindowController(uiDependencies, targetTypeManagement,
                new TargetTypeWindowLayout(uiDependencies, dsTypeManagement)));
//        return getWindowForNewEntity(new AddDsTypeWindowController(uiDependencies, dsTypeManagement,
//                new DsTypeWindowLayout(uiDependencies, smTypeManagement)));

    }

    @Override
    public Window getWindowForUpdate(final ProxyType proxyType) {
        return getWindowForEntity(proxyType, new UpdateTargetTypeWindowController(uiDependencies, targetTypeManagement, targetManagement,
                new TargetTypeWindowLayout(uiDependencies, dsTypeManagement)));
    }
}
