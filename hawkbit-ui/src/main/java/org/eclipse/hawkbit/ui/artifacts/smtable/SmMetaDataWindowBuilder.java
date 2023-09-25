/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractMetaDataWindowBuilder;

import com.vaadin.ui.Window;

/**
 * Builder for Software module meta data windows
 */
public class SmMetaDataWindowBuilder extends AbstractMetaDataWindowBuilder<Long> {

    private final SoftwareModuleManagement smManagement;

    /**
     * Constructor for SmMetaDataWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smManagement
     *            SoftwareModuleManagement
     */
    public SmMetaDataWindowBuilder(final CommonUiDependencies uiDependencies, final SoftwareModuleManagement smManagement) {
        super(uiDependencies);

        this.smManagement = smManagement;
    }

    /**
     * Get software module window without proxy meta data
     *
     * @param smId
     *            software module ID
     * @param name
     *            Selected entity name
     *
     * @return software module window
     */
    public Window getWindowForShowSmMetaData(final Long smId, final String name) {
        return getWindowForShowSmMetaData(smId, name, null);
    }

    /**
     * Get software module window with proxy meta data
     *
     * @param smId
     *            software module ID
     * @param name
     *            Selected entity name
     * @param proxyMetaData
     *            ProxyMetaData
     *
     * @return software module window
     */
    public Window getWindowForShowSmMetaData(final Long smId, final String name, final ProxyMetaData proxyMetaData) {
        return getWindowForShowMetaData(new SmMetaDataWindowLayout(uiDependencies, smManagement), smId, name, proxyMetaData);
    }
}
