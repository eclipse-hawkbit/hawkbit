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

import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutGroup;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * Maps {@link RolloutGroup} entities, fetched from backend, to the
 * {@link ProxyRolloutGroup} entities.
 */
public class RolloutGroupToProxyRolloutGroupMapper
        extends AbstractNamedEntityToProxyNamedEntityMapper<ProxyRolloutGroup, RolloutGroup> {

    @Override
    public ProxyRolloutGroup map(final RolloutGroup rolloutGroup) {
        final ProxyRolloutGroup proxyRolloutGroup = new ProxyRolloutGroup();

        mapNamedEntityAttributes(rolloutGroup, proxyRolloutGroup);

        proxyRolloutGroup.setStatus(rolloutGroup.getStatus());
        proxyRolloutGroup.setErrorAction(rolloutGroup.getErrorAction());
        proxyRolloutGroup.setErrorActionExp(rolloutGroup.getErrorActionExp());
        proxyRolloutGroup.setErrorCondition(rolloutGroup.getErrorCondition());
        proxyRolloutGroup.setErrorConditionExp(rolloutGroup.getErrorConditionExp());
        proxyRolloutGroup.setSuccessCondition(rolloutGroup.getSuccessCondition());
        proxyRolloutGroup.setSuccessConditionExp(rolloutGroup.getSuccessConditionExp());
        proxyRolloutGroup.setFinishedPercentage(formatFinishedPercentage(rolloutGroup));
        proxyRolloutGroup.setTotalTargetsCount(String.valueOf(rolloutGroup.getTotalTargets()));
        proxyRolloutGroup.setTotalTargetCountStatus(rolloutGroup.getTotalTargetCountStatus());
        proxyRolloutGroup.setConfirmationRequired(rolloutGroup.isConfirmationRequired());

        return proxyRolloutGroup;
    }

    private static String formatFinishedPercentage(final RolloutGroup rolloutGroup) {
        float tmpFinishedPercentage = 0;
        switch (rolloutGroup.getStatus()) {
        case READY:
        case SCHEDULED:
        case ERROR:
            tmpFinishedPercentage = 0.0F;
            break;
        case FINISHED:
            tmpFinishedPercentage = 100.0F;
            break;
        case RUNNING:
            tmpFinishedPercentage = rolloutGroup.getTotalTargetCountStatus().getFinishedPercent();
            break;
        default:
            break;
        }
        return String.format(HawkbitCommonUtil.getCurrentLocale(), "%.1f", tmpFinishedPercentage);
    }
}
