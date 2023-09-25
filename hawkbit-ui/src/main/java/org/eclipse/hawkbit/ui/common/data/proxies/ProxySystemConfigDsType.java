/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

/**
 * Proxy for the DistributionSetType view of system config window
 */
public class ProxySystemConfigDsType extends ProxySystemConfigWindow {
    private static final long serialVersionUID = 1L;

    private ProxyTypeInfo dsTypeInfo;

    public ProxyTypeInfo getDsTypeInfo() {
        return dsTypeInfo;
    }

    public void setDsTypeInfo(final ProxyTypeInfo dsTypeInfo) {
        this.dsTypeInfo = dsTypeInfo;
    }

}
