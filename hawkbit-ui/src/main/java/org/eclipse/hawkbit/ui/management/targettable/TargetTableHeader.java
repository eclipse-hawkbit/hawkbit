/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Set;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.common.table.AbstractTableHeader;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.event.BulkUploadPopupEvent;
import org.eclipse.hawkbit.ui.management.event.BulkUploadValidationMessageEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUITargetDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableTransferable;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Target table header layout.
 */
public class TargetTableHeader extends AbstractTableHeader {

    private static final long serialVersionUID = -8647521126666320022L;

    private final UINotification notification;

    private final ManagementViewAcceptCriteria managementViewAcceptCriteria;

    private final TargetAddUpdateWindowLayout targetAddUpdateWindow;

    private final TargetBulkUpdateWindowLayout targetBulkUpdateWindow;

    private boolean isComplexFilterViewDisplayed;

    TargetTableHeader(final I18N i18n, final SpPermissionChecker permChecker, final UIEventBus eventbus,
            final UINotification notification, final ManagementUIState managementUIState,
            final ManagementViewAcceptCriteria managementViewAcceptCriteria, final TargetManagement targetManagement,
            final DeploymentManagement deploymentManagement, final UiProperties uiproperties, final UIEventBus eventBus,
            final EntityFactory entityFactory, final UINotification uinotification, final TagManagement tagManagement,
            final TargetTable targetTable) {
        super(i18n, permChecker, eventbus, managementUIState, null, null);
        this.notification = notification;
        this.managementViewAcceptCriteria = managementViewAcceptCriteria;
        this.targetAddUpdateWindow = new TargetAddUpdateWindowLayout(i18n, targetManagement, eventBus, uinotification,
                entityFactory, targetTable);
        this.targetBulkUpdateWindow = new TargetBulkUpdateWindowLayout(i18n, targetManagement, eventBus,
                managementUIState, deploymentManagement, uiproperties, permChecker, uinotification, tagManagement);

        onLoadRestoreState();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.HIDE_TARGET_TAG_LAYOUT) {
            setFilterButtonsIconVisible(true);
        } else if (event == ManagementUIEvent.SHOW_TARGET_TAG_LAYOUT) {
            setFilterButtonsIconVisible(false);
        } else if (event == ManagementUIEvent.RESET_SIMPLE_FILTERS) {
            UI.getCurrent().access(this::onSimpleFilterReset);
        } else if (event == ManagementUIEvent.RESET_TARGET_FILTER_QUERY) {
            UI.getCurrent().access(this::onCustomFilterReset);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final BulkUploadPopupEvent event) {
        if (BulkUploadPopupEvent.MAXIMIMIZED == event) {
            targetBulkUpdateWindow.restoreComponentsValue();
            openBulkUploadWindow();
        } else if (BulkUploadPopupEvent.CLOSED == event) {
            UI.getCurrent().access(() -> enableBulkUpload());
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final BulkUploadValidationMessageEvent event) {
        this.getUI().access(() -> notification.displayValidationError(event.getValidationErrorMessage()));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetTableEvent event) {
        if (TargetComponentEvent.BULK_TARGET_CREATED == event.getTargetComponentEvent()) {
            this.getUI().access(() -> targetBulkUpdateWindow.setProgressBarValue(
                    managementUIState.getTargetTableFilters().getBulkUpload().getProgressBarCurrentValue()));
        } else if (TargetComponentEvent.BULK_UPLOAD_COMPLETED == event.getTargetComponentEvent()) {
            this.getUI().access(() -> targetBulkUpdateWindow.onUploadCompletion());
        } else if (TargetComponentEvent.BULK_TARGET_UPLOAD_STARTED == event.getTargetComponentEvent()) {
            this.getUI().access(() -> onStartOfBulkUpload());
        } else if (TargetComponentEvent.BULK_UPLOAD_PROCESS_STARTED == event.getTargetComponentEvent()) {
            this.getUI().access(() -> targetBulkUpdateWindow.getBulkUploader().getUpload().setEnabled(false));
        }
    }

    private void onStartOfBulkUpload() {
        disableBulkUpload();
        targetBulkUpdateWindow.onStartOfUpload();
    }

    private void onCustomFilterReset() {
        isComplexFilterViewDisplayed = Boolean.FALSE;
        reEnableSearch();
    }

    private void onLoadRestoreState() {
        if (managementUIState.isCustomFilterSelected()) {
            onSimpleFilterReset();
        }
    }

    private void onSimpleFilterReset() {
        isComplexFilterViewDisplayed = Boolean.TRUE;
        disableSearch();
        if (isSearchFieldOpen()) {
            resetSearch();
        }
        if (managementUIState.getTargetTableFilters().getDistributionSet().isPresent()) {
            closeFilterByDistribution();
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DragEvent dragEvent) {
        if (dragEvent == DragEvent.DISTRIBUTION_DRAG) {
            if (!isComplexFilterViewDisplayed) {
                UI.getCurrent().access(() -> addStyleName("show-table-header-drop-hint"));
            }
        } else {
            UI.getCurrent().access(() -> removeStyleName("show-table-header-drop-hint"));
        }
    }

    @Override
    protected String getHeaderCaption() {
        return i18n.get("header.target.table");
    }

    @Override
    protected String getSearchBoxId() {
        return UIComponentIdProvider.TARGET_TEXT_FIELD;
    }

    @Override
    protected String getSearchRestIconId() {
        return UIComponentIdProvider.TARGET_TBL_SEARCH_RESET_ID;
    }

    @Override
    protected String getAddIconId() {
        return UIComponentIdProvider.TARGET_TBL_ADD_ICON_ID;
    }

    @Override
    protected String getBulkUploadIconId() {
        return UIComponentIdProvider.TARGET_TBL_BULK_UPLOAD_ICON_ID;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        return getSearchText();
    }

    @Override
    protected String getDropFilterId() {
        return UIComponentIdProvider.TARGET_DROP_FILTER_ICON;
    }

    @Override
    protected String getDropFilterWrapperId() {
        return UIComponentIdProvider.TARGET_FILTER_WRAPPER_ID;
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateTargetPermission();
    }

    @Override
    protected boolean isDropHintRequired() {
        return Boolean.TRUE;
    }

    @Override
    protected boolean isDropFilterRequired() {
        return Boolean.TRUE;
    }

    @Override
    protected String getShowFilterButtonLayoutId() {
        return UIComponentIdProvider.SHOW_TARGET_TAGS;
    }

    @Override
    protected void showFilterButtonsLayout() {
        managementUIState.setTargetTagFilterClosed(false);
        eventbus.publish(this, ManagementUIEvent.SHOW_TARGET_TAG_LAYOUT);
    }

    @Override
    protected void resetSearchText() {
        if (managementUIState.getTargetTableFilters().getSearchText().isPresent()) {
            managementUIState.getTargetTableFilters().setSearchText(null);
            eventbus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_TEXT);
        }
    }

    private String getSearchText() {
        return managementUIState.getTargetTableFilters().getSearchText().isPresent()
                ? managementUIState.getTargetTableFilters().getSearchText().get() : null;
    }

    @Override
    protected String getMaxMinIconId() {
        return UIComponentIdProvider.TARGET_MAX_MIN_TABLE_ICON;
    }

    @Override
    public void maximizeTable() {
        managementUIState.setTargetTableMaximized(Boolean.TRUE);
        eventbus.publish(this, new TargetTableEvent(BaseEntityEventType.MAXIMIZED, null));
    }

    @Override
    public void minimizeTable() {
        managementUIState.setTargetTableMaximized(Boolean.FALSE);
        eventbus.publish(this, new TargetTableEvent(BaseEntityEventType.MINIMIZED, null));
    }

    @Override
    public Boolean onLoadIsTableMaximized() {
        return managementUIState.isTargetTableMaximized();
    }

    @Override
    public Boolean onLoadIsShowFilterButtonDisplayed() {
        return managementUIState.isTargetTagFilterClosed();
    }

    @Override
    protected void searchBy(final String newSearchText) {
        managementUIState.getTargetTableFilters().setSearchText(newSearchText);
        eventbus.publish(this, TargetFilterEvent.FILTER_BY_TEXT);
    }

    @Override
    protected void addNewItem(final ClickEvent event) {
        eventbus.publish(this, DragEvent.HIDE_DROP_HINT);
        targetAddUpdateWindow.resetComponents();
        final Window addTargetWindow = targetAddUpdateWindow.getWindow();
        addTargetWindow.setCaption(i18n.get("caption.add.new.target"));
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected void bulkUpload(final ClickEvent event) {
        targetBulkUpdateWindow.resetComponents();
        openBulkUploadWindow();
    }

    private void openBulkUploadWindow() {
        final Window bulkUploadTargetWindow = targetBulkUpdateWindow.getWindow();
        UI.getCurrent().addWindow(bulkUploadTargetWindow);
        bulkUploadTargetWindow.setVisible(true);
    }

    @Override
    protected Boolean isAddNewItemAllowed() {
        return Boolean.TRUE;
    }

    @Override
    protected Boolean isBulkUploadAllowed() {
        return Boolean.TRUE;
    }

    @Override
    protected DropHandler getDropFilterHandler() {
        return new DropHandler() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void drop(final DragAndDropEvent event) {
                filterByDroppedDist(event);
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return managementViewAcceptCriteria;
            }

        };
    }

    private void filterByDroppedDist(final DragAndDropEvent event) {
        if (doValidations(event)) {
            final TableTransferable tableTransferable = (TableTransferable) event.getTransferable();
            final Table source = tableTransferable.getSourceComponent();
            if (source.getId().equals(UIComponentIdProvider.DIST_TABLE_ID)) {
                final Set<DistributionSetIdName> distributionIdSet = getDropppedDistributionDetails(tableTransferable);
                if (distributionIdSet != null && !distributionIdSet.isEmpty()) {
                    final DistributionSetIdName distributionSetIdName = distributionIdSet.iterator().next();
                    managementUIState.getTargetTableFilters().setDistributionSet(distributionSetIdName);
                    addFilterTextField(distributionSetIdName);
                }
            }
        }
    }

    /**
     * Validation for drag event.
     *
     * @param dragEvent
     * @return
     */
    private Boolean doValidations(final DragAndDropEvent dragEvent) {
        final Component compsource = dragEvent.getTransferable().getSourceComponent();
        Boolean isValid = Boolean.TRUE;
        if (compsource instanceof Table && !isComplexFilterViewDisplayed) {
            final TableTransferable transferable = (TableTransferable) dragEvent.getTransferable();
            final Table source = transferable.getSourceComponent();

            if (!source.getId().equals(UIComponentIdProvider.DIST_TABLE_ID)) {
                notification.displayValidationError(i18n.get("message.action.not.allowed"));
                isValid = Boolean.FALSE;
            } else {
                if (getDropppedDistributionDetails(transferable).size() > 1) {
                    notification.displayValidationError(i18n.get("message.onlyone.distribution.dropallowed"));
                    isValid = Boolean.FALSE;
                }
            }
        } else {
            notification.displayValidationError(i18n.get("message.action.not.allowed"));
            isValid = Boolean.FALSE;
        }
        return isValid;
    }

    private Set<DistributionSetIdName> getDropppedDistributionDetails(final TableTransferable transferable) {
        @SuppressWarnings("unchecked")
        final AbstractTable<?, DistributionSetIdName> distTable = (AbstractTable<?, DistributionSetIdName>) transferable
                .getSourceComponent();
        return distTable.getDeletedEntityByTransferable(transferable);
    }

    private void addFilterTextField(final DistributionSetIdName distributionSetIdName) {
        final Button filterLabelClose = SPUIComponentProvider.getButton("drop.filter.close", "", "", "", true,
                FontAwesome.TIMES_CIRCLE, SPUIButtonStyleSmallNoBorder.class);
        filterLabelClose.addClickListener(clickEvent -> closeFilterByDistribution());
        final Label filteredDistLabel = new Label();
        filteredDistLabel.setStyleName(ValoTheme.LABEL_COLORED + " " + ValoTheme.LABEL_SMALL);
        String name = HawkbitCommonUtil.getDistributionNameAndVersion(distributionSetIdName.getName(),
                distributionSetIdName.getVersion());
        if (name.length() > SPUITargetDefinitions.DISTRIBUTION_NAME_MAX_LENGTH_ALLOWED) {
            name = new StringBuilder(name.substring(0, SPUITargetDefinitions.DISTRIBUTION_NAME_LENGTH_ON_FILTER))
                    .append("...").toString();
        }
        filteredDistLabel.setValue(name);
        filteredDistLabel.setSizeUndefined();
        getFilterDroppedInfo().removeAllComponents();
        getFilterDroppedInfo().setSizeFull();
        getFilterDroppedInfo().addComponent(filteredDistLabel);
        getFilterDroppedInfo().addComponent(filterLabelClose);
        getFilterDroppedInfo().setExpandRatio(filteredDistLabel, 1.0F);
        eventbus.publish(this, TargetFilterEvent.FILTER_BY_DISTRIBUTION);
    }

    private void closeFilterByDistribution() {

        eventbus.publish(this, DragEvent.HIDE_DROP_HINT);
        /* Remove filter by distribution information. */
        getFilterDroppedInfo().removeAllComponents();
        getFilterDroppedInfo().setSizeUndefined();
        /* Remove distribution Id from target filter parameters */
        managementUIState.getTargetTableFilters().setDistributionSet(null);

        /* Reload the table */
        eventbus.publish(this, TargetFilterEvent.REMOVE_FILTER_BY_DISTRIBUTION);
    }

    @Override
    protected void displayFilterDropedInfoOnLoad() {
        if (managementUIState.getTargetTableFilters().getDistributionSet().isPresent()) {
            addFilterTextField(managementUIState.getTargetTableFilters().getDistributionSet().get());
        }
    }

    @Override
    protected String getFilterIconStyle() {
        return null;
    }

    @Override
    protected boolean isBulkUploadInProgress() {
        return managementUIState.getTargetTableFilters().getBulkUpload().getSucessfulUploadCount() != 0
                || managementUIState.getTargetTableFilters().getBulkUpload().getFailedUploadCount() != 0;
    }
}
