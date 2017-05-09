/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.server.FontAwesome;
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
public class FilterByStatusLayout extends VerticalLayout implements Button.ClickListener {
    private static final long serialVersionUID = -6930348859189929850L;

    private final VaadinMessageSource i18n;

    private final transient EventBus.UIEventBus eventBus;

    private final ManagementUIState managementUIState;

    private static final String OVERDUE_CAPTION = "overdue";

    private Button unknown;
    private Button inSync;
    private Button pending;
    private Button error;
    private Button registered;
    private Button overdue;
    private boolean unknownBtnClicked;
    private boolean errorBtnClicked;
    private boolean pendingBtnClicked;
    private boolean inSyncBtnClicked;
    private boolean registeredBtnClicked;
    private boolean overdueBtnClicked;
    private Button buttonClicked;
    private static final String BTN_CLICKED = "btnClicked";

    FilterByStatusLayout(final VaadinMessageSource i18n, final UIEventBus eventBus, final ManagementUIState managementUIState) {
        this.i18n = i18n;
        this.eventBus = eventBus;
        this.managementUIState = managementUIState;
    }

    /**
     * Initialize the Status Layout Component.
     */
    void init() {
        getFilterTargetsStatusLayout();
        restorePreviousState();
        eventBus.subscribe(this);
    }

    private void getFilterTargetsStatusLayout() {

        getTargetFilterStatuses();

        addStyleName("target-status-filters");
        setMargin(false);

        final Label targetFilterStatusLabel = new LabelBuilder().name(i18n.getMessage("label.filter.by.status")).buildLabel();

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
        setComponentAlignment(buttonLayout, Alignment.MIDDLE_LEFT);

        final HorizontalLayout overdueLayout = new HorizontalLayout();
        final Label overdueLabel = new LabelBuilder().name(i18n.getMessage("label.filter.by.overdue")).buildLabel();
        overdueLayout.setStyleName("overdue-button-layout");
        overdueLayout.addComponent(overdue);
        overdueLayout.setComponentAlignment(overdue, Alignment.MIDDLE_LEFT);
        overdueLayout.addComponent(overdueLabel);
        overdueLayout.setComponentAlignment(overdueLabel, Alignment.MIDDLE_LEFT);
        addComponent(overdueLayout);
        setComponentAlignment(overdueLayout, Alignment.MIDDLE_LEFT);

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
        if (managementUIState.getTargetTableFilters().isOverdueFilterEnabled()) {
            overdue.addStyleName(BTN_CLICKED);
            overdueBtnClicked = Boolean.TRUE;
        }
    }

    /**
     * Get - status of FILTER.
     */
    private void getTargetFilterStatuses() {
        unknown = SPUIComponentProvider.getButton(UIComponentIdProvider.UNKNOWN_STATUS_ICON,
                TargetUpdateStatus.UNKNOWN.toString(), i18n.getMessage("tooltip.status.unknown"),
                SPUIDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        inSync = SPUIComponentProvider.getButton(UIComponentIdProvider.INSYNCH_STATUS_ICON,
                TargetUpdateStatus.IN_SYNC.toString(), i18n.getMessage("tooltip.status.insync"),
                SPUIDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        pending = SPUIComponentProvider.getButton(UIComponentIdProvider.PENDING_STATUS_ICON,
                TargetUpdateStatus.PENDING.toString(), i18n.getMessage("tooltip.status.pending"),
                SPUIDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        error = SPUIComponentProvider.getButton(UIComponentIdProvider.ERROR_STATUS_ICON,
                TargetUpdateStatus.ERROR.toString(), i18n.getMessage("tooltip.status.error"),
                SPUIDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        registered = SPUIComponentProvider.getButton(UIComponentIdProvider.REGISTERED_STATUS_ICON,
                TargetUpdateStatus.REGISTERED.toString(), i18n.getMessage("tooltip.status.registered"),
                SPUIDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE, SPUIButtonStyleSmall.class);
        overdue = SPUIComponentProvider.getButton(UIComponentIdProvider.OVERDUE_STATUS_ICON, OVERDUE_CAPTION,
                i18n.getMessage("tooltip.status.overdue"), SPUIDefinitions.SP_BUTTON_STATUS_STYLE, false, FontAwesome.SQUARE,
                SPUIButtonStyleSmall.class);
        applyStatusBtnStyle();
        unknown.setData("filterStatusOne");
        inSync.setData("filterStatusTwo");
        pending.setData("filterStatusThree");
        error.setData("filterStatusFour");
        registered.setData("filterStatusFive");
        overdue.setData("filterStatusSix");

        unknown.addClickListener(this);
        inSync.addClickListener(this);
        pending.addClickListener(this);
        error.addClickListener(this);
        registered.addClickListener(this);
        overdue.addClickListener(this);
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
        overdue.addStyleName("overdueBtn");
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
        } else if (event.getButton().getCaption().equalsIgnoreCase(OVERDUE_CAPTION)) {
            processOverdueFilterStatus();
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
     * Process - OVERDUE.
     */
    private void processOverdueFilterStatus() {
        overdueBtnClicked = !overdueBtnClicked;
        managementUIState.getTargetTableFilters().setOverdueFilterEnabled(overdueBtnClicked);

        if (overdueBtnClicked) {
            buttonClicked.addStyleName(BTN_CLICKED);
            eventBus.publish(this, TargetFilterEvent.FILTER_BY_STATUS);
        } else {
            buttonClicked.removeStyleName(BTN_CLICKED);
            eventBus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_STATUS);
        }

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

    @EventBusListenerMethod(scope = EventScope.UI)
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
        overdue.removeStyleName(BTN_CLICKED);
    }

    /**
     * Check if any status button in clicked.
     *
     * @return
     */
    private boolean isStatusFilterApplied() {
        return isPendingOrUnknownBtnClicked() || isErrorOrRegisteredBtnClicked() || inSyncBtnClicked;
    }

    private boolean isPendingOrUnknownBtnClicked() {
        return unknownBtnClicked || pendingBtnClicked;
    }

    private boolean isErrorOrRegisteredBtnClicked() {
        return errorBtnClicked || registeredBtnClicked;
    }
}
