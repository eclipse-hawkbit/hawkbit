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
import org.eclipse.hawkbit.ui.common.data.mappers.TargetToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetFilterStateDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;

import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.data.provider.DataProvider;

public class TargetFilterStateDataSupplierImpl implements TargetFilterStateDataSupplier {
    private final TargetFilterStateDataProvider dataProvider;
    private final DataCommunicator<ProxyTarget> dataCommunicator;

    public TargetFilterStateDataSupplierImpl(final TargetManagement targetManagement,
            final TargetToProxyTargetMapper targetToProxyTargetMapper) {
        this.dataProvider = new TargetFilterStateDataProvider(targetManagement, targetToProxyTargetMapper);
        this.dataCommunicator = new DataCommunicator<>();
    }

    @Override
    public DataProvider<ProxyTarget, String> dataProvider() {
        return dataProvider;
    }

    @Override
    public DataCommunicator<ProxyTarget> dataCommunicator() {
        return dataCommunicator;
    }

}
