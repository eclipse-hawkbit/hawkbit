/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;

/**
 * Support for assigning the distribution set tags to distribution set.
 *
 */
public class DsTagsToDistributionSetAssignmentSupport
        extends TagsAssignmentSupport<ProxyDistributionSet, DistributionSet> {
    private final DistributionSetManagement distributionSetManagement;
    private final CommonUiDependencies uiDependencies;

    /**
     * Constructor for DsTagsToDistributionSetAssignmentSupport
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param distributionSetManagement
     *            DistributionSetManagement
     */
    public DsTagsToDistributionSetAssignmentSupport(final CommonUiDependencies uiDependencies,
            final DistributionSetManagement distributionSetManagement) {
        super(uiDependencies);
        this.uiDependencies = uiDependencies;

        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return uiDependencies.getPermChecker().hasUpdateRepositoryPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_REPOSITORY);
    }

    @Override
    protected AbstractAssignmentResult<DistributionSet> toggleTagAssignment(final String tagName,
            final ProxyDistributionSet targetItem) {
        return distributionSetManagement.toggleTagAssignment(Collections.singletonList(targetItem.getId()), tagName);
    }

    @Override
    protected String getAssignedEntityTypeMsgKey() {
        return "caption.distribution";
    }

    @Override
    protected void publishTagAssignmentEvent(final ProxyDistributionSet targetItem) {
        uiDependencies.getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, targetItem.getId()));

    }
}
