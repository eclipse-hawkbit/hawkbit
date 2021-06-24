/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewBeforeLeaveEvent;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;

/**
 * Abstract class managing event listeners aware layouts.
 *
 */
public abstract class AbstractEventListenersAwareView extends VerticalLayout implements View, ViewNameAware {
    private static final long serialVersionUID = 1L;

    private final transient Set<EventListenersAwareLayout> eventAwareLayouts = new HashSet<>();
    private boolean initial;

    protected void addEventAwareLayout(final EventListenersAwareLayout eventAwareLayout) {
        if (eventAwareLayout != null) {
            eventAwareLayouts.add(eventAwareLayout);
        }
    }

    protected void addEventAwareLayouts(final Collection<EventListenersAwareLayout> eventAwareLayouts) {
        eventAwareLayouts.forEach(this::addEventAwareLayout);
    }

    @PostConstruct
    protected void init() {
        buildLayout();
        initial = true;
    }

    protected abstract void buildLayout();

    @Override
    public void enter(final ViewChangeEvent event) {
        subscribeListeners();

        if (initial) {
            restoreState();
            initial = false;
            return;
        }

        updateLayoutsOnViewEnter();
    }

    protected void subscribeListeners() {
        eventAwareLayouts.forEach(EventListenersAwareLayout::subscribeListeners);
    }

    protected void restoreState() {
        eventAwareLayouts.forEach(EventListenersAwareLayout::restoreState);
    }

    protected void updateLayoutsOnViewEnter() {
        eventAwareLayouts.forEach(EventListenersAwareLayout::onViewEnter);
    }

    @Override
    public void beforeLeave(final ViewBeforeLeaveEvent event) {
        unsubscribeListeners();
        event.navigate();
    }

    protected void unsubscribeListeners() {
        eventAwareLayouts.forEach(EventListenersAwareLayout::unsubscribeListeners);
    }

    @PreDestroy
    public void destroy() {
        unsubscribeListeners();
    }
}
