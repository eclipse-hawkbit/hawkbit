/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIButtonDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 *
 *
 */
@SpringComponent
@VaadinSessionScope
public class FilterByStatusLayout extends VerticalLayout implements Button.ClickListener {
    private static final long serialVersionUID = -6930348859189929850L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementUIState managementUIState;

    private Button unknown;
    private Button inSync;
    private Button pending;
    private Button error;
    private Button registered;
    private Boolean unknownBtnClicked = false;
    private Boolean errorBtnClicked = false;
    private Boolean pendingBtnClicked = false;
    private Boolean inSyncBtnClicked = false;
    private Boolean registeredBtnClicked = false;
    private Button buttonClicked;
    private static final String BTN_CLICKED = "btnClicked";

    /**
     * Initialize the Status Layout Component.
     */
    @PostConstruct
    public void init() {
        getFilterTargetsStatusLayout();
        restorePreviousState();
        eventBus.subscribe(this);
    }

    public void getFilterTargetsStatusLayout() {

        getTargetFilterStatuses();

        addStyleName("target-status-filters");
        setMargin(false);

        final Label targetFilterStatusLabel = SPUIComponentProvider.getLabel(i18n.get("label.filter.by.status"),
                SPUILabelDefinitions.SP_LABEL_SIMPLE);
        targetFilterStatusLabel.addStyleName("target-status-filters-title");

        addComponent(targetFilterStatusLabel);
        setComponentAlignment(targetFilterStatusLabel, Alignment.MIDDLE_CENTER);
        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setStyleName("status-button-layout");
        buttonLayout.addComponent(unknown);
        buttonLayout.setComponentAlignment(unknown, Alignment.MIDDLE_CENTER);
        buttonLayout.addComponent(inSync);
        buttonLayout.setComponentAlignment(inSync, Alignment.MIDDLE_CENTER);
        buttonLayout.addComponent(pending);
        buttonLayout.setComponentAlignment(pending, Alignment.MIDDLE_CENTER);
        buttonLayout.addComponent(error);
        buttonLayout.setComponentAlignment(error, Alignment.MIDDLE_CENTER);
        buttonLayout.addComponent(registered);
        buttonLayout.setComponentAlignment(registered, Alignment.MIDDLE_CENTER);
        addComponent(buttonLayout);
        setComponentAlignment(buttonLayout, Alignment.MIDDLE_CENTER);

    }

    private void restorePreviousState() {
        if (!managementUIState.getTargetTableFilters().getClickedStatusTargetTags().isEmpty()) {
            for (final TargetUpdateStatus status : managementUIState.getTargetTableFilters()
                    .getClickedStatusTargetTags()) {
                if (status == TargetUpdateStatus.UNKNOWN) {
                    unknown.addStyleName(BTN_CLICKED);
                    unknownBtnClicked = Boolean.TRUE;
                } else if (status == TargetUpdateStatus.IN_SYNC) {
                    inSync.addStyleName(BTN_CLICKED);
                    inSyncBtnClicked = Boolean.TRUE;
                } else if (status == TargetUpdateStatus.PENDING) {
                    pending.addStyleName(BTN_CLICKED);
                    pendingBtnClicked = Boolean.TRUE;
                } else if (status == TargetUpdateStatus.ERROR) {
                    error.addStyleName(BTN_CLICKED);
                    errorBtnClicked = Boolean.TRUE;
                } else if (status == TargetUpdateStatus.REGISTERED) {
                    registered.addStyleName(BTN_CLICKED);
                    registeredBtnClicked = Boolean.TRUE;
                }
            }
        }
    }

    /**
     * Get - status of FILTER.
     */
    private void getTargetFilterStatuses() {
        unknown = SPUIComponentProvider.getButton(SPUIComponetIdProvider.UNKNOWN_STATUS_ICON,
                TargetUpdateStatus.UNKNOWN.toString(), i18n.get("tooltip.status.unknown"),
                SPUIButtonDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        inSync = SPUIComponentProvider.getButton(SPUIComponetIdProvider.INSYNCH_STATUS_ICON,
                TargetUpdateStatus.IN_SYNC.toString(), i18n.get("tooltip.status.insync"),
                SPUIButtonDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        pending = SPUIComponentProvider.getButton(SPUIComponetIdProvider.PENDING_STATUS_ICON,
                TargetUpdateStatus.PENDING.toString(), i18n.get("tooltip.status.pending"),
                SPUIButtonDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        error = SPUIComponentProvider.getButton(SPUIComponetIdProvider.ERROR_STATUS_ICON,
                TargetUpdateStatus.ERROR.toString(), i18n.get("tooltip.status.error"),
                SPUIButtonDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        registered = SPUIComponentProvider.getButton(SPUIComponetIdProvider.REGISTERED_STATUS_ICON,
                TargetUpdateStatus.REGISTERED.toString(), i18n.get("tooltip.status.registered"),
                SPUIButtonDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        applyStatusBtnStyle();
        unknown.setData("filterStatusOne");
        inSync.setData("filterStatusTwo");
        pending.setData("filterStatusThree");
        error.setData("filterStatusFour");
        registered.setData("filterStatusFive");

        unknown.addClickListener(this);
        inSync.addClickListener(this);
        pending.addClickListener(this);
        error.addClickListener(this);
        registered.addClickListener(this);
    }

    /**
     * Apply - status style.
     */
    private void applyStatusBtnStyle() {
        unknown.addStyleName("unknownBtn");
        inSync.addStyleName("inSynchBtn");
        pending.addStyleName("pendingBtn");
        error.addStyleName("errorBtn");
        registered.addStyleName("registeredBtn");
    }

    @Override
    public void buttonClick(final ClickEvent event) {
        buttonClicked = event.getButton();
        if (event.getButton().getCaption().equalsIgnoreCase(TargetUpdateStatus.UNKNOWN.toString())) {
            processUnknownFilterStatus();
        } else if (event.getButton().getCaption().equalsIgnoreCase(TargetUpdateStatus.IN_SYNC.toString())) {
            processInSyncFilterStatus();
        } else if (event.getButton().getCaption().equalsIgnoreCase(TargetUpdateStatus.PENDING.toString())) {
            processPendingFilterStatus();
        } else if (event.getButton().getCaption().equalsIgnoreCase(TargetUpdateStatus.ERROR.toString())) {
            processErrorFilterStatus();
        } else if (event.getButton().getCaption().equalsIgnoreCase(TargetUpdateStatus.REGISTERED.toString())) {
            processRegisteredFilterStatus();
        }
    }

    /**
     * Process - UNKNOWN.
     */
    private void processUnknownFilterStatus() {
        unknownBtnClicked = !unknownBtnClicked;
        processCommonFilterStatus(TargetUpdateStatus.UNKNOWN, unknownBtnClicked);
    }

    /**
     * Process - UINSYNC.
     */
    private void processInSyncFilterStatus() {
        inSyncBtnClicked = !inSyncBtnClicked;
        processCommonFilterStatus(TargetUpdateStatus.IN_SYNC, inSyncBtnClicked);
    }

    /**
     * Process - PENDING.
     */
    private void processPendingFilterStatus() {
        pendingBtnClicked = !pendingBtnClicked;
        processCommonFilterStatus(TargetUpdateStatus.PENDING, pendingBtnClicked);
    }

    /**
     * Process - ERROR.
     */
    private void processErrorFilterStatus() {
        errorBtnClicked = !errorBtnClicked;
        processCommonFilterStatus(TargetUpdateStatus.ERROR, errorBtnClicked);
    }

    /**
     * Process - REGISTERED.
     */
    private void processRegisteredFilterStatus() {
        registeredBtnClicked = !registeredBtnClicked;
        processCommonFilterStatus(TargetUpdateStatus.REGISTERED, registeredBtnClicked);
    }

    /**
     * Process - COMMON PROCESS.
     * 
     * @param status
     *            as enum
     * @param buttonReset
     *            as t|F
     */
    private void processCommonFilterStatus(final TargetUpdateStatus status, final boolean buttonPressed) {
        if (buttonPressed) {
            buttonClicked.addStyleName(BTN_CLICKED);
            managementUIState.getTargetTableFilters().getClickedStatusTargetTags().add(status);
            eventBus.publish(this, TargetFilterEvent.FILTER_BY_STATUS);

        } else {
            buttonClicked.removeStyleName(BTN_CLICKED);
            managementUIState.getTargetTableFilters().getClickedStatusTargetTags().remove(status);
            eventBus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_STATUS);
        }

    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.RESET_SIMPLE_FILTERS && isStatusFilterApplied()) {
            removeClickedStyle();
            managementUIState.getTargetTableFilters().getClickedStatusTargetTags().clear();
            eventBus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_STATUS);
        }
    }

    private void removeClickedStyle() {
        unknown.removeStyleName(BTN_CLICKED);
        registered.removeStyleName(BTN_CLICKED);
        inSync.removeStyleName(BTN_CLICKED);
        error.removeStyleName(BTN_CLICKED);
        pending.removeStyleName(BTN_CLICKED);
    }

    /**
     * Check if any status button in clicked.
     * 
     * @return
     */
    private boolean isStatusFilterApplied() {
        if (isPendingOrUnknownBtnClicked() || isErrorOrRegisteredBtnClicked() || inSyncBtnClicked) {
            return true;
        }
        return false;
    }

    private boolean isPendingOrUnknownBtnClicked() {
        if (unknownBtnClicked || pendingBtnClicked) {
            return true;
        }
        return false;
    }

    private boolean isErrorOrRegisteredBtnClicked() {
        if (errorBtnClicked || registeredBtnClicked) {
            return true;
        }
        return false;
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }
}
