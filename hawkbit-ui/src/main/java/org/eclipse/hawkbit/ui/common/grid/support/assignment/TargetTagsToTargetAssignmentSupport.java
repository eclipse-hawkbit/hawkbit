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
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;

/**
 * Support for assigning target tags to target.
 *
 */
public class TargetTagsToTargetAssignmentSupport extends TagsAssignmentSupport<ProxyTarget, Target> {
    private final TargetManagement targetManagement;
    private final UIConfiguration uiConfig;

    /**
     * Constructor for TargetTagsToTargetAssignmentSupport
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param targetManagement
     *            TargetManagement
     */
    public TargetTagsToTargetAssignmentSupport(final UIConfiguration uiConfig,
            final TargetManagement targetManagement) {
        super(uiConfig);
        this.uiConfig = uiConfig;

        this.targetManagement = targetManagement;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return uiConfig.getPermChecker().hasUpdateTargetPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_TARGET);
    }

    @Override
    protected AbstractAssignmentResult<Target> toggleTagAssignment(final String tagName, final ProxyTarget targetItem) {
        return targetManagement.toggleTagAssignment(Collections.singletonList(targetItem.getControllerId()), tagName);
    }

    @Override
    protected String getAssignedEntityTypeMsgKey() {
        return "caption.target";
    }

    @Override
    protected void publishTagAssignmentEvent(final ProxyTarget targetItem) {
        uiConfig.getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxyTarget.class, targetItem.getId()));
    }
}
