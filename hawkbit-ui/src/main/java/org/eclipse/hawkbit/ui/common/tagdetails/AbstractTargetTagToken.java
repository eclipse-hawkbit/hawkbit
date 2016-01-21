/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.eventbus.event.TargetTagCreatedBulkEvent;
import org.eclipse.hawkbit.eventbus.event.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.UI;

/**
 * Abstract class for target tag token layout.
 */
public abstract class AbstractTargetTagToken extends AbstractTagToken {

    private static final long serialVersionUID = 7772876588903171201L;
    protected UI ui;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    @Autowired
    protected I18N i18n;

    @Autowired
    protected transient TagManagement tagManagement;

    @Autowired
    protected SpPermissionChecker checker;

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        ui = UI.getCurrent();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEventTargetTagCreated(final TargetTagCreatedBulkEvent event) {
        for (final TargetTag tag : event.getEntities()) {
            setContainerPropertValues(tag.getId(), tag.getName(), tag.getColour());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetDeletedEvent(final TargetTagDeletedEvent event) {
        final Long deletedTagId = getTagIdByTagName(event.getEntity().getName());
        removeTagFromCombo(deletedTagId);
    }

}
