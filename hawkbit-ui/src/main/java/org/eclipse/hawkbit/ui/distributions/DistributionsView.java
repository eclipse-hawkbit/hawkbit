/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.distributions.disttype.DSTypeFilterLayout;
import org.eclipse.hawkbit.ui.distributions.dstable.DistributionSetTableLayout;
import org.eclipse.hawkbit.ui.distributions.event.DragEvent;
import org.eclipse.hawkbit.ui.distributions.footer.DSDeleteActionsLayout;
import org.eclipse.hawkbit.ui.distributions.smtable.SwModuleTableLayout;
import org.eclipse.hawkbit.ui.distributions.smtype.DistSMTypeFilterLayout;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
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
 * Manage distributions and distributions type view.
 * 
 *
 * 
 */
@SpringView(name = DistributionsView.VIEW_NAME, ui = HawkbitUI.class)
@ViewScope
public class DistributionsView extends VerticalLayout implements View, BrowserWindowResizeListener {

    public static final String VIEW_NAME = "distributions";
    private static final long serialVersionUID = 3887435076372276300L;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UINotification uiNotification;

    @Autowired
    private DSTypeFilterLayout filterByDSTypeLayout;

    @Autowired
    private DistributionSetTableLayout distributionTableLayout;

    @Autowired
    private SwModuleTableLayout softwareModuleTableLayout;

    @Autowired
    private DistSMTypeFilterLayout filterBySMTypeLayout;

    @Autowired
    private DSDeleteActionsLayout deleteActionsLayout;

    @Autowired
    private ManageDistUIState manageDistUIState;

    private GridLayout mainLayout;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.
     * ViewChangeEvent)
     */
    @Override
    public void enter(final ViewChangeEvent event) {
        // Build the Distributions view layout with all the required components.
        buildLayout();
        restoreState();
        checkNoDataAvaialble();
        eventBus.subscribe(this);
        Page.getCurrent().addBrowserWindowResizeListener(this);
        showOrHideFilterButtons(Page.getCurrent().getBrowserWindowWidth());
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
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
        // Check if user has permissions
        if (permChecker.hasUpdateDistributionPermission() || permChecker.hasCreateDistributionPermission()
                || permChecker.hasReadDistributionPermission()) {
            setSizeFull();
            setStyleName("rootLayout");
            createMainLayout();
            addComponents(mainLayout);
            setExpandRatio(mainLayout, 1);
            hideDropHints();
        }
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
        mainLayout.setRowExpandRatio(0, 1.0f);
        mainLayout.setColumnExpandRatio(1, 0.5f);
        mainLayout.setColumnExpandRatio(2, 0.5f);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
    }

    private void hideDropHints() {
        UI.getCurrent().addClickListener(new ClickListener() {
            @Override
            public void click(final com.vaadin.event.MouseEvents.ClickEvent event) {
                eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent event) {
        if (event.getDistributionComponentEvent() == DistributionComponentEvent.MINIMIZED) {
            minimizeDistTable();
        } else if (event.getDistributionComponentEvent() == DistributionComponentEvent.MAXIMIZED) {
            maximizeDistTable();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.MINIMIZED) {
            minimizeSwTable();
        } else if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.MAXIMIZED) {
            maximizeSwTable();
        }
    }

    private void maximizeSwTable() {
        mainLayout.removeComponent(filterByDSTypeLayout);
        mainLayout.removeComponent(distributionTableLayout);
        mainLayout.removeComponent(deleteActionsLayout);
        mainLayout.setColumnExpandRatio(2, 1f);
        mainLayout.setColumnExpandRatio(0, 0f);
        mainLayout.setColumnExpandRatio(1, 0f);
    }

    private void minimizeSwTable() {
        mainLayout.addComponent(filterByDSTypeLayout, 0, 0);
        mainLayout.addComponent(distributionTableLayout, 1, 0);
        mainLayout.addComponent(deleteActionsLayout, 1, 1, 2, 1);
        mainLayout.setColumnExpandRatio(1, 0.5f);
        mainLayout.setColumnExpandRatio(2, 0.5f);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
    }

    private void minimizeDistTable() {
        mainLayout.addComponent(softwareModuleTableLayout, 2, 0);
        mainLayout.addComponent(filterBySMTypeLayout, 3, 0);
        mainLayout.addComponent(deleteActionsLayout, 1, 1, 2, 1);
        mainLayout.setColumnExpandRatio(1, 0.5f);
        mainLayout.setColumnExpandRatio(2, 0.5f);
        mainLayout.setComponentAlignment(deleteActionsLayout, Alignment.BOTTOM_CENTER);
    }

    private void maximizeDistTable() {
        mainLayout.removeComponent(softwareModuleTableLayout);
        mainLayout.removeComponent(filterBySMTypeLayout);
        mainLayout.removeComponent(deleteActionsLayout);
        mainLayout.setColumnExpandRatio(1, 1f);
        mainLayout.setColumnExpandRatio(2, 0f);
        mainLayout.setColumnExpandRatio(3, 0f);
    }

    private void checkNoDataAvaialble() {
        if (manageDistUIState.isNoDataAvilableSwModule() && manageDistUIState.isNoDataAvailableDist()) {

            uiNotification.displayValidationError(i18n.get("message.no.data"));
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

}
