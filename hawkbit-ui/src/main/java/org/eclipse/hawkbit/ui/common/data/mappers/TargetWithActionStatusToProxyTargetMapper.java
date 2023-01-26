/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;

/**
 * Maps {@link TargetWithActionStatus} entities, fetched from backend, to the
 * {@link ProxyTarget} entities.
 */
public class TargetWithActionStatusToProxyTargetMapper
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyTarget, TargetWithActionStatus> {

    @Override
    public ProxyTarget map(final TargetWithActionStatus targetWithActionStatus) {
        final ProxyTarget proxyTarget = new ProxyTarget();
        final Target target = targetWithActionStatus.getTarget();

        proxyTarget.setId(target.getId());
        proxyTarget.setName(target.getName());
        proxyTarget.setDescription(target.getDescription());
        proxyTarget.setControllerId(target.getControllerId());
        proxyTarget.setInstallationDate(target.getInstallationDate());
        proxyTarget.setAddress(target.getAddress());
        proxyTarget.setLastTargetQuery(target.getLastTargetQuery());
        proxyTarget.setModifiedDate(SPDateTimeUtil.getFormattedDate(target.getLastModifiedAt()));
        proxyTarget.setLastModifiedBy(UserDetailsFormatter.loadAndFormatLastModifiedBy(target));
        proxyTarget.setCreatedDate(SPDateTimeUtil.getFormattedDate(target.getCreatedAt()));
        proxyTarget.setCreatedBy(UserDetailsFormatter.loadAndFormatCreatedBy(target));
        proxyTarget.setLastTargetQuery(target.getLastTargetQuery());
        proxyTarget.setLastActionStatusCode(targetWithActionStatus.getLastActionStatusCode());
        proxyTarget.setStatus(targetWithActionStatus.getStatus());

        return proxyTarget;
    }

}
