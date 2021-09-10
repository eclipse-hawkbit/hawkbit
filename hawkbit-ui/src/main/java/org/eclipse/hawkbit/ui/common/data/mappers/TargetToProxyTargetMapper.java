/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import java.util.TimeZone;

import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Maps {@link Target} entities, fetched from backend, to the
 * {@link ProxyTarget} entities.
 */
public class TargetToProxyTargetMapper extends AbstractNamedEntityToProxyNamedEntityMapper<ProxyTarget, Target> {

    private final VaadinMessageSource i18n;

    /**
     * Constructor for TargetToProxyTargetMapper
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public TargetToProxyTargetMapper(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    public ProxyTarget map(final Target target) {
        final ProxyTarget proxyTarget = new ProxyTarget();

        mapNamedEntityAttributes(target, proxyTarget);

        proxyTarget.setControllerId(target.getControllerId());
        proxyTarget.setInstallationDate(target.getInstallationDate());
        proxyTarget.setAddress(target.getAddress());
        proxyTarget.setLastTargetQuery(target.getLastTargetQuery());
        proxyTarget.setUpdateStatus(target.getUpdateStatus());
        proxyTarget.setPollStatusToolTip(getPollStatusToolTip(target.getPollStatus()));
        proxyTarget.setSecurityToken(target.getSecurityToken());
        proxyTarget.setRequestAttributes(target.isRequestControllerAttributes());
        if (target.getTargetType() != null){
            TargetType type = target.getTargetType();
            proxyTarget.setTypeInfo(new ProxyTypeInfo(type.getId(), type.getName(), type.getKey()));
        }

        return proxyTarget;
    }

    private String getPollStatusToolTip(final PollStatus pollStatus) {
        if (pollStatus != null && pollStatus.getLastPollDate() != null && pollStatus.isOverdue()) {
            final TimeZone tz = SPDateTimeUtil.getBrowserTimeZone();
            return i18n.getMessage(UIMessageIdProvider.TOOLTIP_OVERDUE, SPDateTimeUtil.getDurationFormattedString(
                    pollStatus.getOverdueDate().atZone(SPDateTimeUtil.getTimeZoneId(tz)).toInstant().toEpochMilli(),
                    pollStatus.getCurrentDate().atZone(SPDateTimeUtil.getTimeZoneId(tz)).toInstant().toEpochMilli(),
                    i18n));
        }
        return null;
    }
}
