/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Support for assigning the {@link ProxyDistributionSet} items to
 * {@link ProxyTag}.
 * 
 */
public class DistributionSetsToTagAssignmentSupport
        extends ToTagAssignmentSupport<ProxyDistributionSet, DistributionSet> {
    private final DistributionSetManagement distributionSetManagement;
    private final UIEventBus eventBus;
    private final SpPermissionChecker permChecker;

    /**
     * Constructor for DistributionSetsToTagAssignmentSupport
     *
     * @param notification
     *            UINotification
     * @param i18n
     *            VaadinMessageSource
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     */
    public DistributionSetsToTagAssignmentSupport(final UINotification notification, final VaadinMessageSource i18n,
            final DistributionSetManagement distributionSetManagement, final UIEventBus eventBus,
            final SpPermissionChecker permChecker) {
        super(notification, i18n);

        this.distributionSetManagement = distributionSetManagement;
        this.eventBus = eventBus;
        this.permChecker = permChecker;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return permChecker.hasUpdateRepositoryPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_REPOSITORY);
    }

    @Override
    protected AbstractAssignmentResult<DistributionSet> toggleTagAssignment(
            final List<ProxyDistributionSet> sourceItems, final String tagName) {
        final Collection<Long> dsIdsToAssign = sourceItems.stream().map(ProxyDistributionSet::getId)
                .collect(Collectors.toList());

        return distributionSetManagement.toggleTagAssignment(dsIdsToAssign, tagName);
    }

    @Override
    protected String getAssignedEntityTypeMsgKey() {
        return "caption.distribution";
    }

    @Override
    protected void publishTagAssignmentEvent(final List<ProxyDistributionSet> sourceItemsToAssign) {
        final List<Long> assignedDsIds = sourceItemsToAssign.stream().map(ProxyIdentifiableEntity::getId)
                .collect(Collectors.toList());
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, assignedDsIds));
    }
}
