/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.spring.events.EventBus;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

/**
 * A {@link Runnable} implementation for the {@link UI#access(Runnable)} to
 * dispatch events to the UI in the UI thread.
 * 
 *
 *
 *
 */
public class DispatcherRunnable implements Runnable {

    private final SecurityContext userContext;
    private final TenantAwareEvent event;
    private final VaadinSession session;
    private final EventBus eventBus;

    /**
     * @param eventBus
     *            the event bus to distribute the event to
     * @param session
     *            the current Vaadin session
     * @param userContext
     *            the context of the currently logged in user to distribute the
     *            event to
     * @param event
     *            the event which is distributed to the UI.
     */
    public DispatcherRunnable(final EventBus eventBus, final VaadinSession session, final SecurityContext userContext,
            final TenantAwareEvent event) {
        this.eventBus = eventBus;
        this.session = session;
        this.userContext = userContext;
        this.event = event;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        CurrentInstance.setCurrent(session.getUIs().iterator().next());
        CurrentInstance.setCurrent(session);
        SecurityContextHolder.setContext(userContext);
        eventBus.publish(this, event);
    }
}
