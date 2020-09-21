/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.aware;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQueryInfo;

/**
 * Element is used for target filter query combobox
 */
public interface TargetFilterQueryAware {
    void setTargetFilterQueryInfo(ProxyTargetFilterQueryInfo tfqInfo);

    ProxyTargetFilterQueryInfo getTargetFilterQueryInfo();
}
