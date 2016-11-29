/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.actionhistory.ActionHistoryComponent;
import org.eclipse.hawkbit.ui.management.dstable.DistributionTableLayout;
import org.eclipse.hawkbit.ui.management.dstag.DistributionTagLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.footer.DeleteActionsLayout;
import org.eclipse.hawkbit.ui.management.state.DistributionTableFilters;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.targettable.TargetTable;
import org.eclipse.hawkbit.ui.management.targettable.TargetTableLayout;
import org.eclipse.hawkbit.ui.management.targettag.CreateUpdateTargetTagLayoutWindow;
import org.eclipse.hawkbit.ui.management.targettag.TargetTagFilterLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.Page.BrowserWindowResizeEvent;
import com.vaadin.server.Page.BrowserWindowResizeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Target status and deployment management view.
 *
 */
@UIScope
@SpringView(name = DeploymentView.VIEW_NAME, ui = HawkbitUI.class)
public class DeploymentView extends VerticalLayout implements View, BrowserWindowResizeListener {

    public static final String VIEW_NAME = "deployment";
    private static final long serialVersionUID = 1847434723456644998L;

    private final transient EventBus.UIEventBus eventbus;

    private final SpPermissionChecker permChecker;

    private final I18N i18n;

    private final UINotification uiNotification;

    private final ManagementUIState managementUIState;

    private final ActionHistoryComponent actionHistoryComponent;

    private final TargetTagFilterLayout targetTagFilterLayout;

    private final TargetTableLayout targetTableLayout;

    private final DistributionTagLayout distributionTagLayout;

    private final DistributionTableLayout distributionTableLayoutNew;

    private final DeleteActionsLayout deleteAndActionsLayout;

    private GridLayout mainLayout;

    @Autowired
    DeploymentView(final UIEventBus eventbus, final SpPermissionChecker permChecker, final I18N i18n,
            final UINotification uiNotification, final ManagementUIState managementUIState,
            final DeploymentManagement deploymentManagement, final UIEventBus eventBus,
            final DistributionTableFilters distFilterParameters,
            final DistributionSetManagement distributionSetManagement, final TargetManagement targetManagement,
            final EntityFactory entityFactory, final UiProperties uiproperties,
            final ManagementViewAcceptCriteria managementViewAcceptCriteria, final TagManagement tagManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement, final SystemManagement systemManagement) {
        this.eventbus = eventbus;
        this.permChecker = permChecker;
        this.i18n = i18n;
        this.uiNotification = uiNotification;
        this.managementUIState = managementUIState;
        this.actionHistoryComponent = new ActionHistoryComponent(i18n, deploymentManagement, eventBus, uiNotification,
                managementUIState);
        final CreateUpdateTargetTagLayoutWindow createUpdateTargetTagLayout = new CreateUpdateTargetTagLayoutWindow(
                i18n, tagManagement, entityFactory, eventBus, permChecker, uiNotification);
        this.targetTagFilterLayout = new TargetTagFilterLayout(i18n, createUpdateTargetTagLayout, managementUIState,
                managementViewAcceptCriteria, permChecker, eventBus, uiNotification, entityFactory, targetManagement,
                targetFilterQueryManagement);
        final TargetTable targetTable = new TargetTable(eventBus, i18n, uiNotification, targetManagement,
                managementUIState, permChecker, managementViewAcceptCriteria);

        this.targetTableLayout = new TargetTableLayout(eventbus, targetTable, targetManagement, entityFactory, i18n,
                eventBus, uiNotification, managementUIState, managementViewAcceptCriteria, deploymentManagement,
                uiproperties, permChecker, uiNotification, tagManagement);

        this.distributionTagLayout = new DistributionTagLayout(eventbus, managementUIState, i18n, permChecker, eventBus,
                tagManagement, entityFactory, uiNotification, distFilterParameters, distributionSetManagement,
                managementViewAcceptCriteria);
        this.distributionTableLayoutNew = new DistributionTableLayout(i18n, eventBus, permChecker, managementUIState,
                distributionSetManagement, managementViewAcceptCriteria, entityFactory, uiNotification, tagManagement,
                targetManagement);
        this.deleteAndActionsLayout = new DeleteActionsLayout(i18n, permChecker, eventBus, uiNotification,
                tagManagement, managementViewAcceptCriteria, managementUIState, targetManagement, targetTable,
                deploymentManagement, distributionSetManagement);
    }

    @PostConstruct
    void init() {
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

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionTableEvent event) {
        if (BaseEntityEventType.MINIMIZED == event.getEventType()) {
            minimizeDistTable();
        } else if (BaseEntityEventType.MAXIMIZED == event.getEventType()) {
            maximizeDistTable();
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetTableEvent event) {
        if (BaseEntityEventType.MINIMIZED == event.getEventType()) {
            minimizeTargetTable();
        } else if (BaseEntityEventType.MAXIMIZED == event.getEventType()) {
            maximizeTargetTable();
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent mgmtUIEvent) {
        if (mgmtUIEvent == ManagementUIEvent.MAX_ACTION_HISTORY) {
            UI.getCurrent().access(this::maximizeActionHistory);
        }
        if (mgmtUIEvent == ManagementUIEvent.MIN_ACTION_HISTORY) {
            UI.getCurrent().access(this::minimizeActionHistory);
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
        mainLayout.setRowExpandRatio(0, 1F);
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
        mainLayout.setColumnExpandRatio(1, 0.275F);
        mainLayout.setColumnExpandRatio(2, 0.275F);
        mainLayout.setColumnExpandRatio(4, 0.45F);
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
        mainLayout.setColumnExpandRatio(0, 1F);
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
        mainLayout.setColumnExpandRatio(1, 0.4F);
        mainLayout.setColumnExpandRatio(2, 0.6F);
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
        mainLayout.setColumnExpandRatio(1, 1F);
        mainLayout.setColumnExpandRatio(2, 0F);
        mainLayout.setColumnExpandRatio(3, 0F);
        mainLayout.setColumnExpandRatio(4, 0F);
    }

    private void maximizeDistTable() {
        if (permChecker.hasTargetReadPermission()) {
            mainLayout.removeComponent(targetTagFilterLayout);
            mainLayout.removeComponent(targetTableLayout);
            mainLayout.removeComponent(actionHistoryComponent);
        }
        mainLayout.removeComponent(deleteAndActionsLayout);
        mainLayout.setColumnExpandRatio(0, 0F);
        mainLayout.setColumnExpandRatio(1, 0F);
        mainLayout.setColumnExpandRatio(2, 1F);
        mainLayout.setColumnExpandRatio(4, 0F);
    }

    private void maximizeActionHistory() {
        mainLayout.setSpacing(false);
        mainLayout.removeComponent(targetTagFilterLayout);
        mainLayout.removeComponent(targetTableLayout);
        if (permChecker.hasReadDistributionPermission()) {
            mainLayout.removeComponent(distributionTableLayoutNew);
            mainLayout.removeComponent(distributionTagLayout);
        }
        mainLayout.setColumnExpandRatio(0, 0F);
        mainLayout.setColumnExpandRatio(1, 0F);
        mainLayout.setColumnExpandRatio(2, 0F);
        mainLayout.setColumnExpandRatio(3, 0F);
        mainLayout.setColumnExpandRatio(4, 1F);
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

    @Override
    public void enter(final ViewChangeEvent event) {
        // This view is constructed in the init() method()
    }

}
