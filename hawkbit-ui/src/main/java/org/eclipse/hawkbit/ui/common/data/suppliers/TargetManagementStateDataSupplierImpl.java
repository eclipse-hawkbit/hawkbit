/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.suppliers;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.common.data.filters.TargetManagementFilterParams;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetManagementStateDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;

import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.data.provider.DataProvider;

public class TargetManagementStateDataSupplierImpl implements TargetManagementStateDataSupplier {
    private final TargetManagementStateDataProvider dataProvider;
    private final DataCommunicator<ProxyTarget> dataCommunicator;

    public TargetManagementStateDataSupplierImpl(final TargetManagement targetManagement,
            final TargetToProxyTargetMapper targetToProxyTargetMapper) {
        this.dataProvider = new TargetManagementStateDataProvider(targetManagement, targetToProxyTargetMapper);
        this.dataCommunicator = new DataCommunicator<>();
    }

    @Override
    public DataProvider<ProxyTarget, TargetManagementFilterParams> dataProvider() {
        return dataProvider;
    }

    @Override
    public DataCommunicator<ProxyTarget> dataCommunicator() {
        return dataCommunicator;
    }

}
