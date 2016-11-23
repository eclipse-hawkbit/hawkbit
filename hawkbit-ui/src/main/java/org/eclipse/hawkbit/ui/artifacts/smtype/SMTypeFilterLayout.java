/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Software module type filter buttons layout.
 * 
 *
 * 
 */
@SpringComponent
@UIScope
public class SMTypeFilterLayout extends AbstractFilterLayout {

    private static final long serialVersionUID = 1581066345157393665L;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private SMTypeFilterHeader smTypeFilterHeader;

    @Autowired
    private SMTypeFilterButtons smTypeFilterButtons;

    @Autowired
    private SMTypeFilterButtonClick smTypeFilterButtonClick;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    void init() {
        super.init(smTypeFilterHeader, smTypeFilterButtons, smTypeFilterButtonClick);
        eventbus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.HIDE_FILTER_BY_TYPE) {
            setVisible(false);
        }
        if (event == UploadArtifactUIEvent.SHOW_FILTER_BY_TYPE) {
            setVisible(true);
        }
    }

    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventbus.unsubscribe(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.filterlayout.SPFilterLayout#
     * onLoadIsTypeFilterIsClosed()
     */
    @Override
    public Boolean onLoadIsTypeFilterIsClosed() {
        return artifactUploadState.isSwTypeFilterClosed();
    }

}
