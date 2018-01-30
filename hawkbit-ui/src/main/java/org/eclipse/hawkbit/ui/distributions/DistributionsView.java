/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.AbstractNotificationView;
import org.eclipse.hawkbit.ui.components.NotificationUnreadButton;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.disttype.DSTypeFilterLayout;
import org.eclipse.hawkbit.ui.distributions.dstable.DistributionSetTableLayout;
import org.eclipse.hawkbit.ui.distributions.footer.DSDeleteActionsLayout;
import org.eclipse.hawkbit.ui.distributions.smtable.SwModuleTableLayout;
import org.eclipse.hawkbit.ui.distributions.smtype.DistSMTypeFilterLayout;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.menu.DashboardMenuItem;
import org.eclipse.hawkbit.ui.push.DistributionSetCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.DistributionSetDeletedEventContainer;
import org.eclipse.hawkbit.ui.push.SoftwareModuleCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.SoftwareModuleDeletedEventContainer;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.Page.BrowserWindowResizeEvent;
import com.vaadin.server.Page.BrowserWindowResizeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.GridLayout;

/**
 * Manage distributions and distributions type view.
 */
@UIScope
@SpringView(name = DistributionsView.VIEW_NAME, ui = AbstractHawkbitUI.class)
public class DistributionsView extends AbstractNotificationView implements BrowserWindowResizeListener {

    private static final long serialVersionUID = 1L;

    public static final String VIEW_NAME = "distributions";

    private final SpPermissionChecker permChecker;

    private final VaadinMessageSource i18n;

    private final UINotification uiNotification;

    private final DSTypeFilterLayout filterByDSTypeLayout;

    private final DistributionSetTableLayout distributionTableLayout;

    private final SwModuleTableLayout softwareModuleTableLayout;

    private final DistSMTypeFilterLayout filterBySMTypeLayout;

    private final DSDeleteActionsLayout deleteActionsLayout;

    private final ManageDistUIState manageDistUIState;

    private final DistributionsViewMenuItem distributionsViewMenuItem;

    private GridLayout mainLayout;

    @Autowired
    DistributionsView(final SpPermissionChecker permChecker, final UIEventBus eventBus, final VaadinMessageSource i18n,
            final UINotification uiNotification, final ManageDistUIState manageDistUIState,
            final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetManagement distributionSetManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement, final TargetManagement targetManagement,
            final EntityFactory entityFactory, final DistributionSetTagManagement distributionSetTagManagement,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final ArtifactUploadState artifactUploadState, final SystemManagement systemManagement,
            final ArtifactManagement artifactManagement, final NotificationUnreadButton notificationUnreadButton,
            final DistributionsViewMenuItem distributionsViewMenuItem) {
        super(eventBus, notificationUnreadButton);
        this.permChecker = permChecker;
        this.i18n = i18n;
        this.uiNotification = uiNotification;
        this.filterByDSTypeLayout = new DSTypeFilterLayout(manageDistUIState, i18n, permChecker, eventBus,
                entityFactory, uiNotification, softwareModuleTypeManagement, distributionSetTypeManagement,
                distributionSetManagement, distributionsViewClientCriterion);
        this.distributionTableLayout = new DistributionSetTableLayout(i18n, eventBus, permChecker, manageDistUIState,
                softwareModuleManagement, distributionSetManagement, distributionSetTypeManagement, targetManagement,
                entityFactory, uiNotification, distributionSetTagManagement, distributionsViewClientCriterion,
                systemManagement);
        this.softwareModuleTableLayout = new SwModuleTableLayout(i18n, uiNotification, eventBus,
                softwareModuleManagement, softwareModuleTypeManagement, entityFactory, manageDistUIState, permChecker,
                distributionsViewClientCriterion, artifactUploadState, artifactManagement);
        this.filterBySMTypeLayout = new DistSMTypeFilterLayout(eventBus, i18n, permChecker, manageDistUIState,
                entityFactory, uiNotification, softwareModuleTypeManagement, distributionsViewClientCriterion);
        this.deleteActionsLayout = new DSDeleteActionsLayout(i18n, permChecker, eventBus, uiNotification,
                systemManagement, manageDistUIState, distributionsViewClientCriterion, distributionSetManagement,
                distributionSetTypeManagement, softwareModuleManagement, softwareModuleTypeManagement);
        this.manageDistUIState = manageDistUIState;
        this.distributionsViewMenuItem = distributionsViewMenuItem;
    }

    @PostConstruct
    void init() {
        buildLayout();
        restoreState();
        checkNoDataAvaialble();
        Page.getCurrent().addBrowserWindowResizeListener(this);
        showOrHideFilterButtons(Page.getCurrent().getBrowserWindowWidth());
    }

    @Override
    public void enter(final ViewChangeEvent event) {
        softwareModuleTableLayout.getSwModuleTable()
                .selectEntity(manageDistUIState.getLastSelectedSoftwareModule().orElse(null));

        distributionTableLayout.getDistributionSetTable()
                .selectEntity(manageDistUIState.getLastSelectedDistribution().orElse(null));
    }

    @Override
    protected DashboardMenuItem getDashboardMenuItem() {
        return distributionsViewMenuItem;
    }

    private void restoreState() {
        if (manageDistUIState.isDsTableMaximized()) {
            maximizeDistTable();
        }
        if (manageDistUIState.isSwModuleTableMaximized()) {
            maximizeSwTable();
        }
    }

    private void buildLayout() {
        if (!hasUserPermission()) {
            return;
        }
        setSizeFull();
        setStyleName("rootLayout");
        createMainLayout();
        addComponents(mainLayout);
        setExpandRatio(mainLayout, 1);
    }

    private boolean hasUserPermission() {
        return permChecker.hasUpdateRepositoryPermission() || permChecker.hasCreateRepositoryPermission()
                || permChecker.hasReadRepositoryPermission();
    }

    private void createMainLayout() {
        mainLayout = new GridLayout(4, 2);
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);
        mainLayout.addComponent(filterByDSTypeLayout, 0, 0);
        mainLayout.addComponent(distributionTableLayout, 1, 0);
        mainLayout.addComponent(softwareModuleTableLayout, 2, 0);
        mainLayout.addComponent(filterBySMTypeLayout, 3, 0);
        mainLayout.addComponent(deleteActionsLayout, 1, 1, 2, 1);
        mainLayout.setRowExpandRatio(0, 1.0F);
        mainLayout.setColumnExpandRatio(1, 0.5F);
        mainLayout.setColumnExpandRatio(2, 0.5F);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
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
    void onEvent(final SoftwareModuleEvent event) {
        if (BaseEntityEventType.MINIMIZED == event.getEventType()) {
            minimizeSwTable();
        } else if (BaseEntityEventType.MAXIMIZED == event.getEventType()) {
            maximizeSwTable();
        }
    }

    private void maximizeSwTable() {
        mainLayout.removeComponent(filterByDSTypeLayout);
        mainLayout.removeComponent(distributionTableLayout);
        mainLayout.removeComponent(deleteActionsLayout);
        mainLayout.setColumnExpandRatio(2, 1F);
        mainLayout.setColumnExpandRatio(0, 0F);
        mainLayout.setColumnExpandRatio(1, 0F);
    }

    private void minimizeSwTable() {
        mainLayout.addComponent(filterByDSTypeLayout, 0, 0);
        mainLayout.addComponent(distributionTableLayout, 1, 0);
        mainLayout.addComponent(deleteActionsLayout, 1, 1, 2, 1);
        mainLayout.setColumnExpandRatio(1, 0.5F);
        mainLayout.setColumnExpandRatio(2, 0.5F);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
    }

    private void minimizeDistTable() {
        mainLayout.addComponent(softwareModuleTableLayout, 2, 0);
        mainLayout.addComponent(filterBySMTypeLayout, 3, 0);
        mainLayout.addComponent(deleteActionsLayout, 1, 1, 2, 1);
        mainLayout.setColumnExpandRatio(1, 0.5F);
        mainLayout.setColumnExpandRatio(2, 0.5F);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
    }

    private void maximizeDistTable() {
        mainLayout.removeComponent(softwareModuleTableLayout);
        mainLayout.removeComponent(filterBySMTypeLayout);
        mainLayout.removeComponent(deleteActionsLayout);
        mainLayout.setColumnExpandRatio(1, 1F);
        mainLayout.setColumnExpandRatio(2, 0F);
        mainLayout.setColumnExpandRatio(3, 0F);
    }

    private void checkNoDataAvaialble() {
        if (manageDistUIState.isNoDataAvilableSwModule() && manageDistUIState.isNoDataAvailableDist()) {
            uiNotification.displayValidationError(i18n.getMessage("message.no.data"));
        }
    }

    @Override
    public void browserWindowResized(final BrowserWindowResizeEvent event) {
        showOrHideFilterButtons(event.getWidth());
    }

    private void showOrHideFilterButtons(final int browserWidth) {
        if (browserWidth < SPUIDefinitions.REQ_MIN_BROWSER_WIDTH) {
            filterByDSTypeLayout.setVisible(false);
            distributionTableLayout.setShowFilterButtonVisible(true);
            filterBySMTypeLayout.setVisible(false);
            softwareModuleTableLayout.setShowFilterButtonVisible(true);
        } else {
            if (!manageDistUIState.isDistTypeFilterClosed()) {
                filterByDSTypeLayout.setVisible(true);
                distributionTableLayout.setShowFilterButtonVisible(false);
            }
            if (!manageDistUIState.isSwTypeFilterClosed()) {
                filterBySMTypeLayout.setVisible(true);
                softwareModuleTableLayout.setShowFilterButtonVisible(false);
            }
        }
    }

    @Override
    protected Map<Class<?>, RefreshableContainer> getSupportedPushEvents() {
        final Map<Class<?>, RefreshableContainer> supportedEvents = Maps.newHashMapWithExpectedSize(4);

        supportedEvents.put(DistributionSetCreatedEventContainer.class, distributionTableLayout.getTable());
        supportedEvents.put(DistributionSetDeletedEventContainer.class, distributionTableLayout.getTable());

        supportedEvents.put(SoftwareModuleCreatedEventContainer.class, softwareModuleTableLayout.getTable());
        supportedEvents.put(SoftwareModuleDeletedEventContainer.class, softwareModuleTableLayout.getTable());

        return supportedEvents;
    }

}
