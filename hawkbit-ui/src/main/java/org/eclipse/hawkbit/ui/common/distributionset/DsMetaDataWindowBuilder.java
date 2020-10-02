/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.distributionset;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractMetaDataWindowBuilder;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetaDataWindowLayout;

import com.vaadin.ui.Window;

/**
 * Builder for DistributionSet meta data window
 */
public class DsMetaDataWindowBuilder extends AbstractMetaDataWindowBuilder<Long> {

    private final DistributionSetManagement dsManagement;

    /**
     * Constructor for DsMetaDataWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsManagement
     *            DistributionSetManagement
     */
    public DsMetaDataWindowBuilder(final CommonUiDependencies uiDependencies, final DistributionSetManagement dsManagement) {
        super(uiDependencies);

        this.dsManagement = dsManagement;
    }

    /**
     * Get the distribution set window
     *
     * @param dsId
     *            Distribution set id
     * @param name
     *            Distribution set name
     *
     * @return Dialog window
     */
    public Window getWindowForShowDsMetaData(final Long dsId, final String name) {
        return getWindowForShowDsMetaData(dsId, name, null);
    }

    /**
     * Get the distribution set window
     *
     * @param dsId
     *            Distribution set id
     * @param name
     *            Distribution set name
     * @param proxyMetaData
     *            Meta data
     *
     * @return Dialog window
     */
    public Window getWindowForShowDsMetaData(final Long dsId, final String name, final ProxyMetaData proxyMetaData) {
        return getWindowForShowMetaData(new DsMetaDataWindowLayout(uiDependencies, dsManagement), dsId, name, proxyMetaData);
    }
}
