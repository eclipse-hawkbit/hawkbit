/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.suppliers;

import org.eclipse.hawkbit.ui.common.data.filters.TargetManagementFilterParams;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;

/**
 * Interface for target backend data retrieval provider for Management View.
 *
 */
public interface TargetManagementStateDataSupplier extends DataSupplier<ProxyTarget, TargetManagementFilterParams> {

}
