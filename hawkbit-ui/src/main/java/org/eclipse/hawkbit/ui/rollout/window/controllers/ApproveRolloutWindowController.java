/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.controllers;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutWindow;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.rollout.window.RolloutWindowDependencies;
import org.eclipse.hawkbit.ui.rollout.window.layouts.ApproveRolloutWindowLayout;

/**
 * Controller for populating and editing/saving data in Update Rollout Window.
 */
public class ApproveRolloutWindowController extends UpdateRolloutWindowController {
    private final ApproveRolloutWindowLayout layout;

    /**
     * Constructor for ApproveRolloutWindowController
     *
     * @param dependencies
     *          RolloutWindowDependencies
     * @param layout
     *          ApproveRolloutWindowLayout
     */
    public ApproveRolloutWindowController(final RolloutWindowDependencies dependencies,
            final ApproveRolloutWindowLayout layout) {
        super(dependencies, layout);

        this.layout = layout;
    }

    @Override
    protected void adaptLayout(final ProxyRollout proxyEntity) {
        super.adaptLayout(proxyEntity);

        layout.disableRolloutFormLayout();
    }

    @Override
    protected void persistEntity(final ProxyRolloutWindow entity) {
        rolloutManagement.approveOrDeny(entity.getId(), entity.getApprovalDecision(), entity.getApprovalRemark());

        getUiNotification().displaySuccess(getI18n().getMessage("message.update.success", entity.getName()));
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxyRollout.class, entity.getId()));
    }

    @Override
    protected boolean isEntityValid(final ProxyRolloutWindow entity) {
        if (entity.getApprovalDecision() == null) {
            getUiNotification().displayValidationError(getI18n().getMessage("message.rollout.approval.required"));
            return false;
        }

        return super.isEntityValid(entity);
    }
}
