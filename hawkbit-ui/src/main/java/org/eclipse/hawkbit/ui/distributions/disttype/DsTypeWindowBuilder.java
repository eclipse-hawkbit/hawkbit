/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for distribution set type window
 */
public class DsTypeWindowBuilder extends AbstractEntityWindowBuilder<ProxyType> {

    private final DistributionSetTypeManagement dsTypeManagement;
    private final DistributionSetManagement dsManagement;
    private final SoftwareModuleTypeManagement smTypeManagement;

    /**
     * Constructor for DsTypeWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param dsManagement
     *            DistributionSetManagement
     * @param smTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public DsTypeWindowBuilder(final CommonUiDependencies uiDependencies, final DistributionSetTypeManagement dsTypeManagement,
            final DistributionSetManagement dsManagement, final SoftwareModuleTypeManagement smTypeManagement) {
        super(uiDependencies);

        this.dsTypeManagement = dsTypeManagement;
        this.dsManagement = dsManagement;
        this.smTypeManagement = smTypeManagement;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.TAG_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(new AddDsTypeWindowController(uiDependencies, dsTypeManagement,
                new DsTypeWindowLayout(uiDependencies, smTypeManagement)));

    }

    @Override
    public Window getWindowForUpdate(final ProxyType proxyType) {
        return getWindowForEntity(proxyType, new UpdateDsTypeWindowController(uiDependencies, dsTypeManagement, dsManagement,
                new DsTypeWindowLayout(uiDependencies, smTypeManagement)));
    }
}
