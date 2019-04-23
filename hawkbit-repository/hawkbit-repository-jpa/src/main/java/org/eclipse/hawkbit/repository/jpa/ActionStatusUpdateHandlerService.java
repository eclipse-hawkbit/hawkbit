/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.ActionStatusUpdateEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * A service that listens and processes the TargetStatusForDistributionSetEvent
 *
 */
public class ActionStatusUpdateHandlerService {

    private final ControllerManagement controllerManagement;
    private final EntityFactory entityFactory;
    private final SystemSecurityContext securityContext;

    public ActionStatusUpdateHandlerService(final ControllerManagement controllerManagement,
            final EntityFactory entityFactory, final SystemSecurityContext securityContext) {
        this.controllerManagement = controllerManagement;
        this.entityFactory = entityFactory;
        this.securityContext = securityContext;
    }

    @EventListener(classes = ActionStatusUpdateEvent.class)
    public void handle(final ActionStatusUpdateEvent event) {
        List<SimpleGrantedAuthority> authorities = Collections
                .singletonList(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS));
        securityContext.runWithAuthority(() -> {
            Optional<Action> action = controllerManagement.findActionWithDetails(event.getActionId());
            if (action.isPresent() && action.get().isActive()) {
                return this.updateStatus(action.get(), event.getMessages(), event.getStatus());
            }
            return action;
        }, authorities, event.getTenant());
    }

    private Action updateStatus(final Action action, final List<String> messages, final Status status) {
        final ActionStatusCreate actionStatus = entityFactory.actionStatus().create(action.getId()).status(status)
                .messages(messages);
        if (Status.CANCELED.equals(status)) {
            return controllerManagement.addCancelActionStatus(actionStatus);
        } else {
            return controllerManagement.addUpdateActionStatus(actionStatus);
        }
    }

}
