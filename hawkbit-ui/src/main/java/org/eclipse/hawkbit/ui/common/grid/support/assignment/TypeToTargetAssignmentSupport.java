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
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.vaadin.spring.events.EventBus;

/**
 * Support for assigning the {@link ProxyTargetType} items to {@link ProxyTarget}.
 *
 */
public class TypeToTargetAssignmentSupport  extends AssignmentSupport<ProxyTargetType, ProxyTarget>  {
    
    private final TargetManagement targetManagement;
    private final EventBus.UIEventBus eventBus;
    private final SpPermissionChecker permChecker;

    private static final String CAPTION_TYPE = "caption.type";
    private static final String CAPTION_TARGET = "caption.target";

    /**
     * Constructor for TypeToTargetAssignmentSupport
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     */
    public TypeToTargetAssignmentSupport(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement) {
        super(uiDependencies.getUiNotification(), uiDependencies.getI18n());
        this.eventBus = uiDependencies.getEventBus();
        this.permChecker = uiDependencies.getPermChecker();
        this.targetManagement = targetManagement;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return permChecker.hasUpdateTargetPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_TARGET);
    }

    @Override
    protected void performAssignment(List<ProxyTargetType> sourceItemsToAssign, ProxyTarget targetItem) {
        final String controllerId = targetItem.getControllerId();
        // multi-type assignment is not supported
        final Long typeId = sourceItemsToAssign.get(0).getId();
        TargetTypeAssignmentResult targetTypeAssignmentResult = initiateTargetTypeAssignment(typeId, controllerId);

        final String assignmentMsg = createAssignmentMessage(targetTypeAssignmentResult, i18n.getMessage(CAPTION_TARGET),
                i18n.getMessage(CAPTION_TYPE), sourceItemsToAssign.get(0).getName());
        notification.displaySuccess(assignmentMsg);

        publishTypeAssignmentEvent(sourceItemsToAssign);
    }

    protected TargetTypeAssignmentResult initiateTargetTypeAssignment(final Long targetType, final String controllerId) {
        return targetManagement.assignType(Collections.singletonList(controllerId), targetType);
    }

    protected void publishTypeAssignmentEvent(final List<ProxyTargetType> sourceItemsToAssign) {
        final List<Long> assignedTargetIds = sourceItemsToAssign.stream().map(ProxyIdentifiableEntity::getId)
                .collect(Collectors.toList());
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventPayload.EntityModifiedEventType.ENTITY_UPDATED, ProxyTarget.class, assignedTargetIds));
    }
}
