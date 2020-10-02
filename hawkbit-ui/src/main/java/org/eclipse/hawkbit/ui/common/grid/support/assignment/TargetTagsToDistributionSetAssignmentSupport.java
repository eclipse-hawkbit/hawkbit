/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * Support for assigning target tags to distribution set.
 *
 */
public class TargetTagsToDistributionSetAssignmentSupport extends AssignmentSupport<ProxyTag, ProxyDistributionSet> {
    private final TargetsToDistributionSetAssignmentSupport targetsToDistributionSetAssignmentSupport;
    private final TargetManagement targetManagement;

    /**
     * Constructor for TargetTagsToDistributionSetAssignmentSupport
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param targetsToDistributionSetAssignmentSupport
     *            TargetsToDistributionSetAssignmentSupport
     */
    public TargetTagsToDistributionSetAssignmentSupport(final CommonUiDependencies uiDependencies,
            final TargetManagement targetManagement,
            final TargetsToDistributionSetAssignmentSupport targetsToDistributionSetAssignmentSupport) {
        super(uiDependencies.getUiNotification(), uiDependencies.getI18n());

        this.targetManagement = targetManagement;
        this.targetsToDistributionSetAssignmentSupport = targetsToDistributionSetAssignmentSupport;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return targetsToDistributionSetAssignmentSupport.getMissingPermissionsForDrop();
    }

    @Override
    protected void performAssignment(final List<ProxyTag> sourceItemsToAssign, final ProxyDistributionSet targetItem) {
        // we are taking first tag because multi-tag assignment is
        // not supported
        final String tagName = sourceItemsToAssign.get(0).getName();
        final Long tagId = sourceItemsToAssign.get(0).getId();

        final List<Target> targetsToAssign = getTargetsAssignedToTag(tagId);

        if (targetsToAssign.isEmpty()) {
            notification.displayValidationError(i18n.getMessage("message.no.targets.assiged.fortag", tagName));
            return;
        }

        targetsToDistributionSetAssignmentSupport.performAssignment(mapTargetsToProxyTargets(targetsToAssign),
                targetItem);
    }

    private List<Target> getTargetsAssignedToTag(final Long tagId) {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(query -> targetManagement.findByTag(query, tagId));
    }

    private static List<ProxyTarget> mapTargetsToProxyTargets(final List<Target> targetsToAssign) {
        // it is redundant to use TargetToProxyTargetMapper here
        return targetsToAssign.stream().map(target -> {
            final ProxyTarget proxyTarget = new ProxyTarget();

            proxyTarget.setId(target.getId());
            proxyTarget.setControllerId(target.getControllerId());
            proxyTarget.setName(target.getName());

            return proxyTarget;
        }).collect(Collectors.toList());
    }
}
