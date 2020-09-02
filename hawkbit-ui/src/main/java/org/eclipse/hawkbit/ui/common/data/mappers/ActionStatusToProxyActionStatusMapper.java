/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
