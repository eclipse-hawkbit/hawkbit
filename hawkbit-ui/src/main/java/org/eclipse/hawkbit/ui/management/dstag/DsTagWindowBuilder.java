/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for distribution set tag window
 */
public class DsTagWindowBuilder extends AbstractEntityWindowBuilder<ProxyTag> {

    private final DistributionSetTagManagement dsTagManagement;

    /**
     * Constructor for DsTagWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsTagManagement
     *            DistributionSetTagManagement
     */
    public DsTagWindowBuilder(final CommonUiDependencies uiDependencies, final DistributionSetTagManagement dsTagManagement) {
        super(uiDependencies);

        this.dsTagManagement = dsTagManagement;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.TAG_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(
                new AddDsTagWindowController(uiDependencies, dsTagManagement, new TagWindowLayout<ProxyTag>(uiDependencies)));

    }

    @Override
    public Window getWindowForUpdate(final ProxyTag proxyTag) {
        return getWindowForEntity(proxyTag,
                new UpdateDsTagWindowController(uiDependencies, dsTagManagement, new TagWindowLayout<ProxyTag>(uiDependencies)));
    }
}
