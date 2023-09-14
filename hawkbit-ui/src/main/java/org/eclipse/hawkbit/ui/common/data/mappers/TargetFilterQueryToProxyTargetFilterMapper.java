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

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSetInfo;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;

/**
 * Maps {@link TargetFilterQuery} entities, fetched from backend, to the
 * {@link ProxyTargetFilterQuery} entities.
 */
public class TargetFilterQueryToProxyTargetFilterMapper
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyTargetFilterQuery, TargetFilterQuery> {

    @Override
    public ProxyTargetFilterQuery map(final TargetFilterQuery targetFilterQuery) {
        final ProxyTargetFilterQuery proxyTargetFilter = new ProxyTargetFilterQuery();

        proxyTargetFilter.setName(targetFilterQuery.getName());
        proxyTargetFilter.setId(targetFilterQuery.getId());
        proxyTargetFilter.setCreatedDate(SPDateTimeUtil.getFormattedDate(targetFilterQuery.getCreatedAt()));
        proxyTargetFilter.setCreatedBy(UserDetailsFormatter.loadAndFormatCreatedBy(targetFilterQuery));
        proxyTargetFilter.setModifiedDate(SPDateTimeUtil.getFormattedDate(targetFilterQuery.getLastModifiedAt()));
        proxyTargetFilter.setLastModifiedBy(UserDetailsFormatter.loadAndFormatLastModifiedBy(targetFilterQuery));
        proxyTargetFilter.setQuery(targetFilterQuery.getQuery());

        final DistributionSet distributionSet = targetFilterQuery.getAutoAssignDistributionSet();
        if (distributionSet != null) {
            proxyTargetFilter.setAutoAssignmentEnabled(true);
            proxyTargetFilter.setDistributionSetInfo(new ProxyDistributionSetInfo(distributionSet.getId(),
                    distributionSet.getName(), distributionSet.getVersion(), distributionSet.isValid()));
            proxyTargetFilter.setAutoAssignActionType(targetFilterQuery.getAutoAssignActionType());
            proxyTargetFilter.setConfirmationRequired(targetFilterQuery.isConfirmationRequired());
        }

        return proxyTargetFilter;
    }

}
