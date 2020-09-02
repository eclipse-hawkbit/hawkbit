/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.HorizontalLayout;

/**
 * Layout responsible for action-history-grid and the corresponding header.
 */
public class ActionHistoryLayout extends HorizontalLayout {
    private static final long serialVersionUID = 1L;

    private final ActionHistoryGridLayout actionHistoryGridLayout;
    private final ActionStatusGridLayout actionStatusLayout;
    private final ActionStatusMsgGridLayout actionStatusMsgLayout;

    /**
     * Constructor for ActionHistoryLayout
     *
     * @param i18n
     *          VaadinMessageSource
     * @param deploymentManagement
     *          DeploymentManagement
     * @param eventBus
     *          UIEventBus
     * @param notification
     *          UINotification
     * @param permChecker
     *          SpPermissionChecker
     * @param actionHistoryGridLayoutUiState
     *          ActionHistoryGridLayoutUiState
     */
    public ActionHistoryLayout(final VaadinMessageSource i18n, final DeploymentManagement deploymentManagement,
            final UIEventBus eventBus, final UINotification notification, final SpPermissionChecker permChecker,
            final ActionHistoryGridLayoutUiState actionHistoryGridLayoutUiState) {

        this.actionHistoryGridLayout = new ActionHistoryGridLayout(i18n, deploymentManagement, eventBus, notification,
                permChecker, actionHistoryGridLayoutUiState);

        this.actionStatusLayout = new ActionStatusGridLayout(i18n, eventBus, deploymentManagement);
        this.actionStatusMsgLayout = new ActionStatusMsgGridLayout(i18n, eventBus, deploymentManagement);

        init();
        buildLayout();
    }

    private void init() {
        setSizeFull();
        setMargin(false);
        setSpacing(true);
    }

    private void buildLayout() {
        addComponent(actionHistoryGridLayout);
        setExpandRatio(actionHistoryGridLayout, 0.55F);

        actionStatusLayout.setVisible(false);
        addComponent(actionStatusLayout);
        setExpandRatio(actionStatusLayout, 0.18F);

        actionStatusMsgLayout.setVisible(false);
        addComponent(actionStatusMsgLayout);
        setExpandRatio(actionStatusMsgLayout, 0.27F);
    }

    /**
     * Restore the action history grid layout
     */
    public void restoreState() {
        actionHistoryGridLayout.restoreState();
    }

    /**
     * Maximize the action history grid layout
     */
    public void maximize() {
        actionStatusLayout.enableSelection();
        actionStatusMsgLayout.enableSelection();

        actionHistoryGridLayout.maximize();

        actionStatusLayout.setVisible(true);
        actionStatusMsgLayout.setVisible(true);
    }

    /**
     * Minimize the action history grid layout
     */
    public void minimize() {
        actionStatusLayout.disableSelection();
        actionStatusMsgLayout.disableSelection();

        actionHistoryGridLayout.minimize();

        actionStatusLayout.setVisible(false);
        actionStatusMsgLayout.setVisible(false);
    }

    /**
     * Unsubscribe the changed listener
     */
    public void unsubscribeListener() {
        actionHistoryGridLayout.unsubscribeListener();
        actionStatusLayout.unsubscribeListener();
        actionStatusMsgLayout.unsubscribeListener();
    }
}
