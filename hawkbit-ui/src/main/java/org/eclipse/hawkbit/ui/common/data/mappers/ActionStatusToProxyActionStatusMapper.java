/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyActionStatus;

/**
 * Maps {@link ActionStatus} entities, fetched from backend, to the
 * {@link ProxyActionStatus} entities.
 */
public class ActionStatusToProxyActionStatusMapper
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyActionStatus, ActionStatus> {

    @Override
    public ProxyActionStatus map(final ActionStatus actionStatus) {
        final ProxyActionStatus proxyAS = new ProxyActionStatus();

        proxyAS.setCreatedAt(actionStatus.getCreatedAt());
        proxyAS.setStatus(actionStatus.getStatus());
        proxyAS.setId(actionStatus.getId());

        return proxyAS;
    }
}
