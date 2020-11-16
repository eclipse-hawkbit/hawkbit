/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.io.Serializable;

public class ProxySystemConfigDsType extends ProxySystemConfigWindow implements Serializable {
    private static final long serialVersionUID = 1L;

    private ProxyTypeInfo dsTypeInfo;

    public ProxyTypeInfo getDsTypeInfo() {
        return dsTypeInfo;
    }

    public void setDsTypeInfo(final ProxyTypeInfo dsTypeInfo) {
        this.dsTypeInfo = dsTypeInfo;
    }

}
