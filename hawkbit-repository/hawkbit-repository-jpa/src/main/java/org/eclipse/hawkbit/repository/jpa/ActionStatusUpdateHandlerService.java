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
import java.util.UUID;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.ActionStatusUpdateEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * A service that listens and processes the TargetStatusForDistributionSetEvent
 *
 */
public class ActionStatusUpdateHandlerService {

    private final ControllerManagement controllerManagement;
    private final EntityFactory entityFactory;

    public ActionStatusUpdateHandlerService(final ControllerManagement controllerManagement,
            final EntityFactory entityFactory) {
        this.controllerManagement = controllerManagement;
        this.entityFactory = entityFactory;
    }

    @EventListener(classes = ActionStatusUpdateEvent.class)
    public void handle(final ActionStatusUpdateEvent event) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        setTenantSecurityContext(event.getTenant());
        Optional<Action> action = controllerManagement.findActionWithDetails(event.getActionId());
        try {
            if (action.isPresent() && action.get().isActive()) {
                this.updateStatus(action.get(), event.getMessages(), event.getStatus());
            }
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
    }

    protected void setTenantSecurityContext(final String tenantId) {
        final AnonymousAuthenticationToken authenticationToken = new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), "Target-Status-Controller",
                Collections.singletonList(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
        authenticationToken.setDetails(new TenantAwareAuthenticationDetails(tenantId, true));
        setSecurityContext(authenticationToken);
    }

    private static void setSecurityContext(final Authentication authentication) {
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContextImpl);
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
