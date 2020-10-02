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
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractMetaDataWindowBuilder;

import com.vaadin.ui.Window;

/**
 * Builder for target meta data window
 */
public class TargetMetaDataWindowBuilder extends AbstractMetaDataWindowBuilder<String> {

    private final TargetManagement targetManagement;

    /**
     * Constructor for TargetMetaDataWindowBuilder
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     */
    public TargetMetaDataWindowBuilder(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement) {
        super(uiDependencies);

        this.targetManagement = targetManagement;
    }

    /**
     * Get the target meta data window
     *
     * @param controllerId
     *            Controller id
     * @param name
     *            Entity name
     *
     * @return Common dialog window with target meta data
     */
    public Window getWindowForShowTargetMetaData(final String controllerId, final String name) {
        return getWindowForShowTargetMetaData(controllerId, name, null);
    }

    /**
     * Get the target meta data window
     *
     * @param controllerId
     *            Controller id
     * @param name
     *            Entity name
     * @param proxyMetaData
     *            ProxyMetaData
     *
     * @return Common dialog window with target meta data
     */
    public Window getWindowForShowTargetMetaData(final String controllerId, final String name,
            final ProxyMetaData proxyMetaData) {
        return getWindowForShowMetaData(new TargetMetaDataWindowLayout(uiDependencies, targetManagement), controllerId, name,
                proxyMetaData);
    }
}
