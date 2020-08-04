/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * Maps {@link Rollout} entities, fetched from backend, to the
 * {@link ProxyRollout} entities.
 */
public class RolloutToProxyRolloutMapper extends AbstractNamedEntityToProxyNamedEntityMapper<ProxyRollout, Rollout> {

    /**
     * Maps the rollout to Proxy rollout
     *
     * @param rollout
     *            Rollout
     *
     * @return ProxyRollout
     */
    public static ProxyRollout mapRollout(final Rollout rollout) {
        return new RolloutToProxyRolloutMapper().map(rollout);
    }

    @Override
    public ProxyRollout map(final Rollout rollout) {
        final ProxyRollout proxyRollout = new ProxyRollout();

        mapNamedEntityAttributes(rollout, proxyRollout);

        final DistributionSet distributionSet = rollout.getDistributionSet();
        proxyRollout.setDistributionSetNameVersion(
                HawkbitCommonUtil.getFormattedNameVersion(distributionSet.getName(), distributionSet.getVersion()));
        proxyRollout.setDistributionSetId(distributionSet.getId());

        proxyRollout
                .setNumberOfGroups(rollout.getRolloutGroupsCreated() > 0 ? rollout.getRolloutGroupsCreated() : null);
        proxyRollout.setForcedTime(rollout.getForcedTime() > 0 ? rollout.getForcedTime() : null);
        proxyRollout.setStatus(rollout.getStatus());
        proxyRollout.setStatusTotalCountMap(rollout.getTotalTargetCountStatus().getStatusTotalCountMap());
        proxyRollout.setTotalTargets(rollout.getTotalTargets());
        proxyRollout.setApprovalDecidedBy(rollout.getApprovalDecidedBy());
        proxyRollout.setApprovalRemark(rollout.getApprovalRemark());
        proxyRollout.setActionType(rollout.getActionType());
        proxyRollout.setTargetFilterQuery(rollout.getTargetFilterQuery());
        proxyRollout.setStartAt(rollout.getStartAt());

        return proxyRollout;
    }
}
