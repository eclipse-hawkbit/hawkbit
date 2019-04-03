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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(ActionStatusUpdateHandlerService.class);

    private final ControllerManagement controllerManagement;
    private final EntityFactory entityFactory;

    public ActionStatusUpdateHandlerService(final ControllerManagement controllerManagement,
            final EntityFactory entityFactory) {
        this.controllerManagement = controllerManagement;
        this.entityFactory = entityFactory;
    }

    @EventListener(classes = ActionStatusUpdateEvent.class)
    public void handle(final ActionStatusUpdateEvent event) {
        Optional<Action> action = controllerManagement.findActiveActionsByTargetAndDistributionSet(event.getTargetControllerId(),
                event.getDistributionSetId());
        if (action.isPresent()) {
            final SecurityContext oldContext = SecurityContextHolder.getContext();
            try {
                this.updateStatus(action.get(), event.getTenant(), event.getMessages(), event.getStatus());
            } finally {
                SecurityContextHolder.setContext(oldContext);
            }
        }
    }

    protected void setTenantSecurityContext(String tenantId) {
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

    private Action updateStatus(Action action, String tenant, List<String> messages, Status status) {
        // attempt to avoid permission issue. Not happening
        setTenantSecurityContext(tenant);

        final ActionStatusCreate actionStatus = entityFactory.actionStatus().create(action.getId()).status(status)
                .messages(messages);
        if (Status.CANCELED.equals(status)) {
            return controllerManagement.addCancelActionStatus(actionStatus);
        } else {
            return controllerManagement.addUpdateActionStatus(actionStatus);
        }
    }

}
