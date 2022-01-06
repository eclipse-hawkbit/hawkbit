/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.vaadin.spring.events.EventBus;

/**
 * Support for assigning/un-assigning targets to target type.
 *
 */
public abstract class AbstractTargetsToTargetTypeAssignmentSupport extends AssignmentSupport<ProxyTarget, ProxyTargetType> {
    private final SpPermissionChecker permChecker;
    private final EventBus.UIEventBus eventBus;

    protected AbstractTargetsToTargetTypeAssignmentSupport(final CommonUiDependencies uiDependencies) {
        super(uiDependencies.getUiNotification(), uiDependencies.getI18n());
        this.permChecker = uiDependencies.getPermChecker();
        this.eventBus = uiDependencies.getEventBus();
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return permChecker.hasUpdateTargetPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_TARGET);
    }

    protected void publishTypeAssignmentEvent(final List<ProxyTarget> sourceItemsToAssign) {
        final List<Long> assignedTargetIds = sourceItemsToAssign.stream().map(ProxyIdentifiableEntity::getId)
                .collect(Collectors.toList());
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this,
                new EntityModifiedEventPayload(EntityModifiedEventPayload.EntityModifiedEventType.ENTITY_UPDATED,
                        ProxyTarget.class, assignedTargetIds));
    }
}
