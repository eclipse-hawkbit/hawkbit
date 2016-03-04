/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.rollout.RolloutListView;
import org.eclipse.hawkbit.ui.rollout.rolloutgroup.RolloutGroupsListView;
import org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets.RolloutGroupTargetsListView;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.VerticalLayout;

/**
 * Rollout management view.
 */
@SpringView(name = RolloutView.VIEW_NAME, ui = HawkbitUI.class)
@ViewScope
public class RolloutView extends VerticalLayout implements View {

    private static final long serialVersionUID = -6199789714170913988L;

    public static final String VIEW_NAME = "rollout";

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private RolloutListView rolloutListView;

    @Autowired
    private RolloutGroupsListView rolloutGroupsListView;

    @Autowired
    private RolloutGroupTargetsListView rolloutGroupTargetsListView;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Override
    public void enter(final ViewChangeEvent event) {
        setSizeFull();
        if (!(rolloutUIState.isShowRollOuts() || rolloutUIState.isShowRolloutGroups() || rolloutUIState
                .isShowRolloutGroupTargets())) {
            rolloutUIState.setShowRollOuts(true);
        }
        buildLayout();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final RolloutEvent event) {
        if (event == RolloutEvent.SHOW_ROLLOUTS) {
            rolloutUIState.setShowRollOuts(true);
            rolloutUIState.setShowRolloutGroups(false);
            rolloutUIState.setShowRolloutGroupTargets(false);
            buildLayout();
        } else if (event == RolloutEvent.SHOW_ROLLOUT_GROUPS) {
            rolloutUIState.setShowRollOuts(false);
            rolloutUIState.setShowRolloutGroups(true);
            rolloutUIState.setShowRolloutGroupTargets(false);
            buildLayout();
        } else if (event == RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS) {
            rolloutUIState.setShowRollOuts(false);
            rolloutUIState.setShowRolloutGroups(false);
            rolloutUIState.setShowRolloutGroupTargets(true);
            buildLayout();
        }
    }

    private void buildLayout() {
        if (permChecker.hasRolloutReadPermission() && rolloutUIState.isShowRollOuts()) {
            showRolloutListView();
        } else if (permChecker.hasRolloutReadPermission() && rolloutUIState.isShowRolloutGroups()) {
            showRolloutGroupListView();
        } else if (permChecker.hasRolloutTargetsReadPermission() && rolloutUIState.isShowRolloutGroupTargets()) {
            showRolloutGroupTargetsListView();
        }
    }

    /**
     * 
     */
    private void showRolloutGroupTargetsListView() {
        rolloutGroupTargetsListView.setVisible(true);
        if (rolloutListView.isVisible()) {
            rolloutListView.setVisible(false);
        }
        if (rolloutGroupsListView.isVisible()) {
            rolloutGroupsListView.setVisible(false);
        }
        addComponent(rolloutGroupTargetsListView);
        setExpandRatio(rolloutGroupTargetsListView, 1.0f);
    }

    /**
     * 
     */
    private void showRolloutGroupListView() {
        rolloutGroupsListView.setVisible(true);
        if (rolloutListView.isVisible()) {
            rolloutListView.setVisible(false);
        }
        if (rolloutGroupTargetsListView.isVisible()) {
            rolloutGroupTargetsListView.setVisible(false);
        }
        addComponent(rolloutGroupsListView);
        setExpandRatio(rolloutGroupsListView, 1.0f);
    }

    /**
     * 
     */
    private void showRolloutListView() {
        rolloutListView.setVisible(true);
        if (rolloutGroupsListView.isVisible()) {
            rolloutGroupsListView.setVisible(false);
        }
        if (rolloutGroupTargetsListView.isVisible()) {
            rolloutGroupTargetsListView.setVisible(false);
        }
        addComponent(rolloutListView);
        setExpandRatio(rolloutListView, 1.0f);
    }

}
