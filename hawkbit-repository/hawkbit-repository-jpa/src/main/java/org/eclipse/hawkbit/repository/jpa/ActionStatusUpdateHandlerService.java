/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.ActionStatusUpdateEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.context.event.EventListener;

/**
 * A service that listens and processes the {@link ActionStatusUpdateEvent}
 *
 */
public class ActionStatusUpdateHandlerService {

    private final ControllerManagement controllerManagement;
    private final EntityFactory entityFactory;
    private final SystemSecurityContext securityContext;

    ActionStatusUpdateHandlerService(final ControllerManagement controllerManagement, final EntityFactory entityFactory,
            final SystemSecurityContext securityContext) {
        this.controllerManagement = controllerManagement;
        this.entityFactory = entityFactory;
        this.securityContext = securityContext;
    }

    @EventListener(classes = ActionStatusUpdateEvent.class)
    void handle(final ActionStatusUpdateEvent event) {
        securityContext.runAsControllerAsTenant(event.getTenant(),
                () -> controllerManagement.findActionWithDetails(event.getActionId()).filter(Action::isActive)
                        .map(action -> updateStatus(action, event.getMessages(), event.getStatus())));
    }

    private Action updateStatus(final Action action, final List<String> messages, final Status status) {
        final ActionStatusCreate actionStatus = entityFactory.actionStatus().create(action.getId()).status(status)
                .messages(messages);
        if (Status.CANCELED.equals(status)) {
            return controllerManagement.addCancelActionStatus(actionStatus);
        }
        return controllerManagement.addUpdateActionStatus(actionStatus);
    }

}
