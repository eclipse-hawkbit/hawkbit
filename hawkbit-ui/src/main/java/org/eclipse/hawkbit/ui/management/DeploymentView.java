/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.management.actionhistory.ActionHistoryComponent;
import org.eclipse.hawkbit.ui.management.dstable.DistributionTableLayout;
import org.eclipse.hawkbit.ui.management.dstag.DistributionTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.footer.DeleteActionsLayout;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.targettable.TargetTableLayout;
import org.eclipse.hawkbit.ui.management.targettag.TargetTagFilterLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.Page.BrowserWindowResizeEvent;
import com.vaadin.server.Page.BrowserWindowResizeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 *
 *
 */
@SpringView(name = DeploymentView.VIEW_NAME, ui = HawkbitUI.class)
@ViewScope
public class DeploymentView extends VerticalLayout implements View, BrowserWindowResizeListener {

    public static final String VIEW_NAME = "deployment";
    private static final long serialVersionUID = 1847434723456644998L;

    @Autowired
    private transient EventBus.SessionEventBus eventbus;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private ActionHistoryComponent actionHistoryComponent;

    @Autowired
    private TargetTagFilterLayout targetTagFilterLayout;

    @Autowired
    private TargetTableLayout targetTableLayout;

    @Autowired
    private DistributionTagLayout distributionTagLayout;

    @Autowired
    private DistributionTableLayout distributionTableLayoutNew;

    @Autowired
    private DeleteActionsLayout deleteAndActionsLayout;

    private GridLayout mainLayout;

    @Override
    public void enter(final ViewChangeEvent event) {
        buildLayout();
        restoreState();
        checkNoDataAvaialble();
        eventbus.subscribe(this);
        Page.getCurrent().addBrowserWindowResizeListener(this);
        showOrHideFilterButtons(Page.getCurrent().getBrowserWindowWidth());
        eventbus.publish(this, ManagementUIEvent.SHOW_COUNT_MESSAGE);
    }

    @PreDestroy
    void destroy() {
        eventbus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent event) {
        if (event.getDistributionComponentEvent() == DistributionTableEvent.DistributionComponentEvent.MINIMIZED) {
            minimizeDistTable();
        } else if (event
                .getDistributionComponentEvent() == DistributionTableEvent.DistributionComponentEvent.MAXIMIZED) {
            maximizeDistTable();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent event) {
        if (event.getTargetComponentEvent() == TargetTableEvent.TargetComponentEvent.MINIMIZED) {
            minimizeTargetTable();
        } else if (event.getTargetComponentEvent() == TargetTableEvent.TargetComponentEvent.MAXIMIZED) {
            maximizeTargetTable();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent mgmtUIEvent) {
        if (mgmtUIEvent == ManagementUIEvent.MAX_ACTION_HISTORY) {
            UI.getCurrent().access(() -> maximizeActionHistory());
        }
        if (mgmtUIEvent == ManagementUIEvent.MIN_ACTION_HISTORY) {
            UI.getCurrent().access(() -> minimizeActionHistory());
        }
    }

    private void restoreState() {
        if (managementUIState.isTargetTableMaximized()) {
            maximizeTargetTable();
        }
        if (managementUIState.isDsTableMaximized()) {
            maximizeDistTable();
        }
        if (managementUIState.isActionHistoryMaximized()) {
            maximizeActionHistory();
        }
    }

    private void buildLayout() {
        // Build only if user has both permissions
        if (permChecker.hasTargetReadPermission() || permChecker.hasReadDistributionPermission()) {
            setSizeFull();
            createMainLayout();
            addComponents(mainLayout);
            setExpandRatio(mainLayout, 1);
            hideDropHints();
        }
    }

    private void createMainLayout() {
        mainLayout = new GridLayout();
        layoutWidgets();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);
        mainLayout.setRowExpandRatio(0, 1f);
    }

    private void layoutWidgets() {
        mainLayout.removeAllComponents();
        if (permChecker.hasReadDistributionPermission() && permChecker.hasTargetReadPermission()) {
            displayAllWidgets();
        } else if (permChecker.hasReadDistributionPermission()) {
            displayDistributionWidgetsOnly();
        } else if (permChecker.hasTargetReadPermission()) {
            displayTargetWidgetsOnly();
        }
    }

    private void displayAllWidgets() {
        mainLayout.setColumns(5);
        mainLayout.setRows(2);
        mainLayout.addComponent(targetTagFilterLayout, 0, 0);
        mainLayout.addComponent(targetTableLayout, 1, 0);
        mainLayout.addComponent(actionHistoryComponent, 4, 0);
        mainLayout.addComponent(distributionTableLayoutNew, 2, 0);
        mainLayout.addComponent(distributionTagLayout, 3, 0);
        mainLayout.setColumnExpandRatio(1, 0.275f);
        mainLayout.setColumnExpandRatio(2, 0.275f);
        mainLayout.setColumnExpandRatio(4, 0.45f);
        if (showFooterLayout()) {
            mainLayout.addComponent(deleteAndActionsLayout, 1, 1, 2, 1);
            mainLayout.setComponentAlignment(deleteAndActionsLayout, Alignment.BOTTOM_CENTER);
        }
    }

    private void displayDistributionWidgetsOnly() {
        mainLayout.setColumns(2);
        mainLayout.setRows(2);
        mainLayout.addComponent(distributionTableLayoutNew, 0, 0);
        mainLayout.addComponent(distributionTagLayout, 1, 0);
        mainLayout.setColumnExpandRatio(0, 1f);
        if (showFooterLayout()) {
            mainLayout.addComponent(deleteAndActionsLayout, 0, 1);
            mainLayout.setComponentAlignment(deleteAndActionsLayout, Alignment.BOTTOM_CENTER);
        }
    }

    private Boolean showFooterLayout() {
        if (permChecker.hasTargetReadPermission()
                || (permChecker.hasDeleteDistributionPermission() || permChecker.hasDeleteTargetPermission())
                || hasDeploymentPermission()) {
            return true;
        }
        return false;
    }

    private boolean hasDeploymentPermission() {
        return permChecker.hasReadDistributionPermission() && permChecker.hasUpdateTargetPermission();
    }

    private void displayTargetWidgetsOnly() {
        mainLayout.setColumns(3);
        mainLayout.setRows(2);
        mainLayout.addComponent(targetTagFilterLayout, 0, 0);
        mainLayout.addComponent(targetTableLayout, 1, 0);
        mainLayout.addComponent(actionHistoryComponent, 2, 0);
        mainLayout.setColumnExpandRatio(1, 0.4f);
        mainLayout.setColumnExpandRatio(2, 0.6f);
        if (showFooterLayout()) {
            mainLayout.addComponent(deleteAndActionsLayout, 1, 1);
            mainLayout.setComponentAlignment(deleteAndActionsLayout, Alignment.BOTTOM_CENTER);
        }
    }

    private void hideDropHints() {
        UI.getCurrent().addClickListener(new ClickListener() {
            @Override
            public void click(final com.vaadin.event.MouseEvents.ClickEvent event) {
                eventbus.publish(this, DragEvent.HIDE_DROP_HINT);
            }
        });
    }

    private void maximizeTargetTable() {
        if (permChecker.hasReadDistributionPermission()) {
            mainLayout.removeComponent(distributionTableLayoutNew);
            mainLayout.removeComponent(distributionTagLayout);
        }
        mainLayout.removeComponent(actionHistoryComponent);
        mainLayout.removeComponent(deleteAndActionsLayout);
        mainLayout.setColumnExpandRatio(1, 1f);
        mainLayout.setColumnExpandRatio(2, 0f);
        mainLayout.setColumnExpandRatio(3, 0f);
        mainLayout.setColumnExpandRatio(4, 0f);
    }

    private void maximizeDistTable() {
        if (permChecker.hasTargetReadPermission()) {
            mainLayout.removeComponent(targetTagFilterLayout);
            mainLayout.removeComponent(targetTableLayout);
            mainLayout.removeComponent(actionHistoryComponent);
        }
        mainLayout.removeComponent(deleteAndActionsLayout);
        mainLayout.setColumnExpandRatio(0, 0f);
        mainLayout.setColumnExpandRatio(1, 0f);
        mainLayout.setColumnExpandRatio(2, 1f);
        mainLayout.setColumnExpandRatio(4, 0f);
    }

    private void maximizeActionHistory() {
        mainLayout.setSpacing(false);
        mainLayout.removeComponent(targetTagFilterLayout);
        mainLayout.removeComponent(targetTableLayout);
        if (permChecker.hasReadDistributionPermission()) {
            mainLayout.removeComponent(distributionTableLayoutNew);
            mainLayout.removeComponent(distributionTagLayout);
        }
        mainLayout.setColumnExpandRatio(0, 0f);
        mainLayout.setColumnExpandRatio(1, 0f);
        mainLayout.setColumnExpandRatio(2, 0f);
        mainLayout.setColumnExpandRatio(3, 0f);
        mainLayout.setColumnExpandRatio(4, 1f);
        mainLayout.removeComponent(deleteAndActionsLayout);
        mainLayout.setComponentAlignment(actionHistoryComponent, Alignment.TOP_LEFT);
    }

    private void minimizeTargetTable() {
        layoutWidgets();
    }

    private void minimizeDistTable() {
        layoutWidgets();
    }

    private void minimizeActionHistory() {
        layoutWidgets();
        mainLayout.setSpacing(true);
    }

    private void checkNoDataAvaialble() {
        if (managementUIState.isNoDataAvilableTarget() && managementUIState.isNoDataAvailableDistribution()) {
            uiNotification.displayValidationError(i18n.get("message.no.data"));
        }
    }

    @Override
    public void browserWindowResized(final BrowserWindowResizeEvent event) {
        final int browserWidth = event.getWidth();
        showOrHideFilterButtons(browserWidth);
    }

    private void showOrHideFilterButtons(final int browserWidth) {
        if (browserWidth < SPUIDefinitions.REQ_MIN_BROWSER_WIDTH) {
            targetTagFilterLayout.setVisible(false);
            targetTableLayout.setShowFilterButtonVisible(true);
            distributionTagLayout.setVisible(false);
            distributionTableLayoutNew.setShowFilterButtonVisible(true);
        } else {
            if (!managementUIState.isTargetTagFilterClosed()) {
                targetTagFilterLayout.setVisible(true);
                targetTableLayout.setShowFilterButtonVisible(false);

            }
            if (!managementUIState.isDistTagFilterClosed()) {
                distributionTagLayout.setVisible(true);
                distributionTableLayoutNew.setShowFilterButtonVisible(false);
            }
        }
    }

}
