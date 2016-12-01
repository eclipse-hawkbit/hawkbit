/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.FILTER_BY_DISTRIBUTION;
import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.FILTER_BY_TAG;
import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.FILTER_BY_TARGET_FILTER_QUERY;
import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.FILTER_BY_TEXT;
import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.REMOVE_FILTER_BY_DISTRIBUTION;
import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.REMOVE_FILTER_BY_TAG;
import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.REMOVE_FILTER_BY_TARGET_FILTER_QUERY;
import static org.eclipse.hawkbit.ui.management.event.TargetFilterEvent.REMOVE_FILTER_BY_TEXT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.ManagmentEntityState;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetAddUpdateWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.state.TargetTableFilters;
import org.eclipse.hawkbit.ui.push.CancelTargetAssignmentEventContainer;
import org.eclipse.hawkbit.ui.push.TargetCreatedEventContainer;
import org.eclipse.hawkbit.ui.push.TargetDeletedEventContainer;
import org.eclipse.hawkbit.ui.push.TargetUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.AssignInstalledDSTooltipGenerator;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Concrete implementation of Target table.
 */
@SpringComponent
@ViewScope
public class TargetTable extends AbstractTable<Target, TargetIdName> {

    private static final Logger LOG = LoggerFactory.getLogger(TargetTable.class);
    private static final String TARGET_PINNED = "targetPinned";
    private static final long serialVersionUID = -2300392868806614568L;
    private static final int PROPERTY_DEPT = 3;

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private ManagementViewAcceptCriteria managementViewAcceptCriteria;

    private Button targetPinnedBtn;
    private Boolean isTargetPinned = Boolean.FALSE;

    @Override
    protected void init() {
        super.init();
        setItemDescriptionGenerator(new AssignInstalledDSTooltipGenerator());
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetDeletedEvents(final TargetDeletedEventContainer eventContainer) {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();
        boolean shouldRefreshTargets = false;
        for (final TargetDeletedEvent deletedEvent : eventContainer.getEvents()) {
            final TargetIdName targetIdName = new TargetIdName(deletedEvent.getEntityId(), null, null);
            if (visibleItemIds.contains(targetIdName)) {
                targetContainer.removeItem(targetIdName);
            } else {
                shouldRefreshTargets = true;
                break;
            }
        }
        if (shouldRefreshTargets) {
            refreshOnDelete();
        } else {
            targetContainer.commit();
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.REFRESH_TARGETS));
        }
        reSelectItemsAfterDeletionEvent();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onCancelTargetAssignmentEvents(final CancelTargetAssignmentEventContainer eventContainer) {
        // workaround until push is available for action
        // history, re-select
        // the updated target so the action history gets
        // refreshed.
        reselectTargetIfSelectedInStream(eventContainer.getEvents().stream().map(event -> event.getEntity()));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetUpdatedEvents(final TargetUpdatedEventContainer eventContainer) {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        @SuppressWarnings("unchecked")
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();

        if (isFilterEnabled()) {
            refreshTargets();
        } else {
            eventContainer.getEvents().stream().map(event -> event.getEntity())
                    .filter(target -> visibleItemIds.contains(target.getTargetIdName()))
                    .forEach(target -> updateVisibleItemOnEvent(target.getTargetInfo()));
            targetContainer.commit();
        }

        // workaround until push is available for action
        // history, re-select
        // the updated target so the action history gets
        // refreshed.
        reselectTargetIfSelectedInStream(eventContainer.getEvents().stream().map(event -> event.getEntity()));
    }

    private void reselectTargetIfSelectedInStream(final Stream<Target> targets) {
        targets.filter(target -> isLastSelectedTarget(target.getTargetIdName())).findAny().ifPresent(
                target -> eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.SELECTED_ENTITY, target)));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onTargetCreatedEvents(final TargetCreatedEventContainer holder) {
        refreshTargets();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent dragEvent) {
        if (dragEvent == DragEvent.TARGET_TAG_DRAG || dragEvent == DragEvent.DISTRIBUTION_DRAG) {
            UI.getCurrent().access(() -> addStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
        } else {
            UI.getCurrent().access(() -> removeStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final PinUnpinEvent pinUnpinEvent) {
        UI.getCurrent().access(() -> {
            if (pinUnpinEvent == PinUnpinEvent.PIN_DISTRIBUTION) {
                refreshFilter();
                styleTargetTableOnPinning();
            } else if (pinUnpinEvent == PinUnpinEvent.UNPIN_DISTRIBUTION) {
                refreshFilter();
                restoreTargetTableStyle();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void addOrEditEvent(final TargetAddUpdateWindowEvent targetUIEvent) {
        if (BaseEntityEventType.UPDATED_ENTITY != targetUIEvent.getEventType()) {
            return;
        }
        UI.getCurrent().access(() -> updateTarget(targetUIEvent.getEntity()));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {
            if (checkFilterEvent(filterEvent)) {
                refreshFilter();
                eventBus.publish(this, ManagementUIEvent.TARGET_TABLE_FILTER);
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent managementUIEvent) {
        UI.getCurrent().access(() -> {
            if (managementUIEvent == ManagementUIEvent.UNASSIGN_TARGET_TAG
                    || managementUIEvent == ManagementUIEvent.ASSIGN_TARGET_TAG) {
                refreshFilter();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_ASSIGNMENTS) {
            refreshTablecontainer();
        }
    }

    private void refreshTablecontainer() {
        final LazyQueryContainer tableContainer = (LazyQueryContainer) getContainerDataSource();
        tableContainer.refresh();
        selectRow();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetTableEvent event) {
        onBaseEntityEvent(event);
    }

    @Override
    protected String getTableId() {
        return UIComponentIdProvider.TARGET_TABLE_ID;
    }

    @Override
    protected Container createContainer() {
        // ADD all the filters to the query config
        final Map<String, Object> queryConfig = prepareQueryConfigFilters();
        // Create TargetBeanQuery factory with the query config.
        final BeanQueryFactory<TargetBeanQuery> targetQF = new BeanQueryFactory<>(TargetBeanQuery.class);
        targetQF.setQueryConfiguration(queryConfig);
        // create lazy query container with lazy defination and query
        final LazyQueryContainer targetTableContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_CONT_ID_NAME),
                targetQF);
        targetTableContainer.getQueryView().getQueryDefinition().setMaxNestedPropertyDepth(PROPERTY_DEPT);
        return targetTableContainer;
    }

    @Override
    protected void addContainerProperties(final Container container) {
        HawkbitCommonUtil.addTargetTableContainerProperties(container);
    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUIDefinitions.TARGET_STATUS_PIN_TOGGLE_ICON,
                (source, itemId, columnId) -> getTagetPinButton(itemId));
        addGeneratedColumn(SPUIDefinitions.TARGET_STATUS_POLL_TIME,
                (source, itemId, columnId) -> getTagetPollTime(itemId));
    }

    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return !managementUIState.getSelectedTargetIdName().isPresent()
                || managementUIState.getSelectedTargetIdName().get().isEmpty();
    }

    @Override
    protected Object getItemIdToSelect() {
        return managementUIState.getSelectedTargetIdName().orElse(null);
    }

    @Override
    protected void publishEntityAfterValueChange(final Target selectedLastEntity) {
        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.SELECTED_ENTITY, selectedLastEntity));
    }

    @Override
    protected Target findEntityByTableValue(final TargetIdName lastSelectedId) {
        return targetManagement.findTargetByControllerIDWithDetails(lastSelectedId.getControllerId());
    }

    @Override
    protected void setManagementEntitiyStateValues(final Set<TargetIdName> values, final TargetIdName lastId) {
        managementUIState.setSelectedTargetIdName(values);
        managementUIState.setLastSelectedTargetIdName(lastId);
    }

    @Override
    protected ManagmentEntityState<TargetIdName> getManagmentEntityState() {
        return null;
    }

    @Override
    protected boolean isMaximized() {
        return managementUIState.isTargetTableMaximized();
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        if (!isMaximized()) {
            columnList.add(new TableColumn(SPUIDefinitions.TARGET_STATUS_POLL_TIME, "", 0.0F));
            columnList.add(new TableColumn(SPUIDefinitions.TARGET_STATUS_PIN_TOGGLE_ICON, "", 0.0F));
        }
        return columnList;
    }

    @Override
    public AcceptCriterion getDropAcceptCriterion() {
        return managementViewAcceptCriteria;
    }

    private void reSelectItemsAfterDeletionEvent() {
        Set<Object> values;
        if (isMultiSelect()) {
            values = new HashSet<>((Set<?>) getValue());
        } else {
            values = Sets.newHashSetWithExpectedSize(1);
            values.add(getValue());
        }
        unSelectAll();

        for (final Object value : values) {
            if (getVisibleItemIds().contains(value)) {
                select(value);
            }
        }
    }

    private void refreshOnDelete() {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final int size = targetContainer.size();
        refreshTablecontainer();
        if (size != 0) {
            setData(SPUIDefinitions.DATA_AVAILABLE);
        }
        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.REFRESH_TARGETS));
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(7);
        managementUIState.getTargetTableFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));
        managementUIState.getTargetTableFilters().getDistributionSet()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_DISTRIBUTION, value.getId()));
        managementUIState.getTargetTableFilters().getPinnedDistId()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.ORDER_BY_DISTRIBUTION, value));
        managementUIState.getTargetTableFilters().getTargetFilterQuery()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TARGET_FILTER_QUERY, value));
        queryConfig.put(SPUIDefinitions.FILTER_BY_NO_TAG, managementUIState.getTargetTableFilters().isNoTagSelected());

        if (isFilteredByTags()) {
            final List<String> list = new ArrayList<>();
            list.addAll(managementUIState.getTargetTableFilters().getClickedTargetTags());
            queryConfig.put(SPUIDefinitions.FILTER_BY_TAG, list.toArray(new String[list.size()]));
        }
        if (isFilteredByStatus()) {
            final List<TargetUpdateStatus> statusList = managementUIState.getTargetTableFilters()
                    .getClickedStatusTargetTags();
            queryConfig.put(SPUIDefinitions.FILTER_BY_STATUS, statusList);
        }
        if (managementUIState.getTargetTableFilters().isOverdueFilterEnabled()) {
            queryConfig.put(SPUIDefinitions.FILTER_BY_OVERDUE_STATE, Boolean.TRUE);
        }
        return queryConfig;
    }

    private Label getTagetPollTime(final Object itemId) {
        final Label statusLabel = new Label();
        statusLabel.addStyleName(ValoTheme.LABEL_SMALL);
        statusLabel.setHeightUndefined();
        statusLabel.setContentMode(ContentMode.HTML);
        final String pollStatusToolTip = (String) getContainerDataSource().getItem(itemId)
                .getItemProperty(SPUILabelDefinitions.VAR_POLL_STATUS_TOOL_TIP).getValue();
        if (HawkbitCommonUtil.trimAndNullIfEmpty(pollStatusToolTip) != null) {
            statusLabel.setValue(FontAwesome.EXCLAMATION_CIRCLE.getHtml());
        } else {
            statusLabel.setValue(FontAwesome.CLOCK_O.getHtml());
        }
        statusLabel.setDescription(pollStatusToolTip);
        return statusLabel;
    }

    private Button getTagetPinButton(final Object itemId) {
        final Button pinBtn = new Button();
        final StringBuilder pinBtnStyle = new StringBuilder(ValoTheme.BUTTON_BORDERLESS_COLORED);
        pinBtnStyle.append(' ');
        pinBtnStyle.append(ValoTheme.BUTTON_SMALL);
        pinBtnStyle.append(' ');
        pinBtnStyle.append(ValoTheme.BUTTON_ICON_ONLY);
        pinBtn.setStyleName(pinBtnStyle.toString());
        pinBtn.setHeightUndefined();
        pinBtn.setData(itemId);
        pinBtn.setId(UIComponentIdProvider.TARGET_PIN_ICON + itemId);
        pinBtn.addClickListener(this::addPinClickListener);
        if (isPinned(((TargetIdName) itemId).getControllerId())) {
            pinBtn.addStyleName(TARGET_PINNED);
            isTargetPinned = Boolean.TRUE;
            targetPinnedBtn = pinBtn;
            eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
        }
        pinBtn.addStyleName(SPUIStyleDefinitions.TARGET_STATUS_PIN_TOGGLE);
        HawkbitCommonUtil.applyStatusLblStyle(this, pinBtn, itemId);
        return pinBtn;
    }

    private boolean isPinned(final String targetId) {
        return managementUIState.getDistributionTableFilters().getPinnedTargetId().isPresent()
                && targetId.equals(managementUIState.getDistributionTableFilters().getPinnedTargetId().get());
    }

    /**
     * Add listener to pin.
     *
     * @param event
     *            as event
     */
    private void addPinClickListener(final ClickEvent event) {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        checkifAlreadyPinned(event.getButton());
        if (isTargetPinned) {
            pinTarget(event.getButton());
        } else {
            unPinTarget(event.getButton());
        }
    }

    /**
     * Check already pinned.
     *
     * @param eventBtn
     *            as button
     */
    private void checkifAlreadyPinned(final Button eventBtn) {
        final String newPinnedTargetItemId = ((TargetIdName) eventBtn.getData()).getControllerId();
        String targetId = null;
        if (managementUIState.getDistributionTableFilters().getPinnedTargetId().isPresent()) {
            targetId = managementUIState.getDistributionTableFilters().getPinnedTargetId().get();
        }
        if (targetId == null) {
            isTargetPinned = !isTargetPinned;
            managementUIState.getDistributionTableFilters().setPinnedTargetId(newPinnedTargetItemId);
        } else if (targetId.equals(newPinnedTargetItemId)) {
            isTargetPinned = Boolean.FALSE;
        } else {
            isTargetPinned = true;
            managementUIState.getDistributionTableFilters().setPinnedTargetId(newPinnedTargetItemId);
            if (null != targetPinnedBtn) {
                resetPinStyle(targetPinnedBtn);
            }
        }
        targetPinnedBtn = eventBtn;
    }

    private void pinTarget(final Button eventBtn) {
        /* if distribution set is pinned ,unpin target if pinned */
        managementUIState.getTargetTableFilters().setPinnedDistId(null);
        /* on unpin of target dist table should refresh Dist table restyle */
        eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
        /* change target table styling */
        styleTargetTable();
        eventBtn.addStyleName(TARGET_PINNED);
        isTargetPinned = Boolean.FALSE;
    }

    private void unPinTarget(final Button eventBtn) {
        managementUIState.getDistributionTableFilters().setPinnedTargetId(null);
        eventBus.publish(this, PinUnpinEvent.UNPIN_TARGET);
        resetPinStyle(eventBtn);
    }

    private void resetPinStyle(final Button pinBtn) {
        pinBtn.removeStyleName(TARGET_PINNED);
        pinBtn.addStyleName(SPUIStyleDefinitions.TARGET_STATUS_PIN_TOGGLE);
        HawkbitCommonUtil.applyStatusLblStyle(this, pinBtn, pinBtn.getData());
    }

    /**
     * Set style of target table.
     */
    private void styleTargetTable() {
        setCellStyleGenerator((source, itemId, propertyId) -> null);
    }

    @Override
    protected void onDropEventFromTable(final DragAndDropEvent event) {
        dsToTargetAssignment(event);
    }

    @Override
    protected void onDropEventFromWrapper(final DragAndDropEvent event) {
        if (isNoTagAssigned(event)) {
            tagAssignment(event);
        }
    }

    private Boolean isNoTagAssigned(final DragAndDropEvent event) {
        final String tagName = ((DragAndDropWrapper) (event.getTransferable().getSourceComponent())).getData()
                .toString();
        if (tagName.equals(SPUIDefinitions.TARGET_TAG_BUTTON)) {
            notification.displayValidationError(
                    i18n.get("message.tag.cannot.be.assigned", new Object[] { i18n.get("label.no.tag.assigned") }));
            return false;
        }
        return true;
    }

    private void tagAssignment(final DragAndDropEvent event) {
        final List<String> targetList = getDraggedTargetList(event).stream()
                .map(targetIdName -> targetIdName.getControllerId()).collect(Collectors.toList());

        final String targTagName = HawkbitCommonUtil.removePrefix(event.getTransferable().getSourceComponent().getId(),
                SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        if (targetList.isEmpty()) {
            final String actionDidNotWork = i18n.get("message.action.did.not.work");
            notification.displayValidationError(actionDidNotWork);
            return;
        }

        final TargetTagAssignmentResult result = targetManagement.toggleTagAssignment(targetList, targTagName);

        final List<String> tagsClickedList = managementUIState.getTargetTableFilters().getClickedTargetTags();
        notification.displaySuccess(HawkbitCommonUtil.createAssignmentMessage(targTagName, result, i18n));
        if (result.getUnassigned() >= 1 && !tagsClickedList.isEmpty()) {
            refreshFilter();
        }
    }

    @Override
    protected boolean validateDragAndDropWrapper(final DragAndDropWrapper wrapperSource) {
        final String tagName = HawkbitCommonUtil.removePrefix(wrapperSource.getId(),
                SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        if (wrapperSource.getId().startsWith(SPUIDefinitions.TARGET_TAG_ID_PREFIXS)) {
            if ("NO TAG".equals(tagName)) {
                notification.displayValidationError(i18n.get(ACTION_NOT_ALLOWED_MSG));
                return false;
            }
        } else {
            notification.displayValidationError(i18n.get(ACTION_NOT_ALLOWED_MSG));
            return false;
        }

        return true;
    }

    @Override
    protected String getDropTableId() {
        return UIComponentIdProvider.DIST_TABLE_ID;
    }

    @Override
    protected boolean hasDropPermission() {
        return permChecker.hasUpdateTargetPermission();
    }

    private void dsToTargetAssignment(final DragAndDropEvent event) {
        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        final AbstractTable<NamedEntity, DistributionSetIdName> source = (AbstractTable<NamedEntity, DistributionSetIdName>) transferable
                .getSourceComponent();
        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object targetItemId = dropData.getItemIdOver();
        LOG.debug("Adding a log to check if targetItemId is null : {} ", targetItemId);
        if (targetItemId != null) {
            final TargetIdName targetId = (TargetIdName) targetItemId;
            String message = null;

            for (final DistributionSetIdName distributionNameId : source.getDeletedEntityByTransferable(transferable)) {
                if (null != distributionNameId) {
                    if (managementUIState.getAssignedList().keySet().contains(targetId)
                            && managementUIState.getAssignedList().get(targetId).equals(distributionNameId)) {
                        message = getPendingActionMessage(message,
                                HawkbitCommonUtil.getDistributionNameAndVersion(distributionNameId.getName(),
                                        distributionNameId.getVersion()),
                                targetId.getControllerId());
                    } else {
                        managementUIState.getAssignedList().put(targetId, distributionNameId);
                    }
                }
            }
            showOrHidePopupAndNotification(message);
        }
    }

    /**
     * Hide and show Notification Msg.
     *
     * @param message
     *            as msg
     */
    private void showOrHidePopupAndNotification(final String message) {
        if (null != managementUIState.getAssignedList() && !managementUIState.getAssignedList().isEmpty()) {
            eventBus.publish(this, ManagementUIEvent.UPDATE_COUNT);
        }
        if (null != message) {
            notification.displayValidationError(message);
        }
    }

    /**
     * Get message for pending Action.
     *
     * @param message
     *            as message
     * @param distName
     *            as Name
     * @param targetId
     *            as ID of Traget
     * @return String as msg
     */
    private String getPendingActionMessage(final String message, final String distName, final String targetId) {
        if (message == null) {
            return i18n.get("message.dist.pending.action", new Object[] { targetId, distName });
        }
        return i18n.get("message.target.assigned.pending");
    }

    /**
     * To update target details in the table.
     *
     * @param updatedTarget
     *            as reference
     */
    @SuppressWarnings("unchecked")
    public void updateTarget(final Target updatedTarget) {
        if (updatedTarget != null) {
            final Item item = getItem(updatedTarget.getTargetIdName());
            /* Update the new Name, Description and poll date */
            item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(updatedTarget.getName());

            // TO DO update SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER
            // &
            // SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER

            /*
             * Update the last query which will trigger the value change lister
             * registered for the target last query column. That listener will
             * update the latest query date for this target in the tooltip.
             */
            item.getItemProperty(SPUILabelDefinitions.LAST_QUERY_DATE)
                    .setValue(updatedTarget.getTargetInfo().getLastTargetQuery());

            item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY)
                    .setValue(UserDetailsFormatter.loadAndFormatLastModifiedBy(updatedTarget));
            item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE)
                    .setValue(SPDateTimeUtil.getFormattedDate(updatedTarget.getLastModifiedAt()));
            item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(updatedTarget.getDescription());

            /*
             * Update the status which will trigger the value change lister
             * registered for the target update status. That listener will
             * update the new status icon showing for this target in the table.
             */
            item.getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS)
                    .setValue(updatedTarget.getTargetInfo().getUpdateStatus());
        }
    }

    private static boolean checkFilterEvent(final TargetFilterEvent filterEvent) {
        return isNormalFilter(filterEvent) || isRemoveFilterEvent(filterEvent) || isStatusFilterEvent(filterEvent);
    }

    private static boolean isStatusFilterEvent(final TargetFilterEvent filterEvent) {
        return filterEvent == TargetFilterEvent.FILTER_BY_STATUS
                || filterEvent == TargetFilterEvent.REMOVE_FILTER_BY_STATUS;
    }

    private static boolean isRemoveFilterEvent(final TargetFilterEvent filterEvent) {
        return filterEvent == REMOVE_FILTER_BY_TEXT || filterEvent == REMOVE_FILTER_BY_TAG
                || filterEvent == REMOVE_FILTER_BY_DISTRIBUTION || filterEvent == REMOVE_FILTER_BY_TARGET_FILTER_QUERY;
    }

    private static boolean isNormalFilter(final TargetFilterEvent filterEvent) {
        return filterEvent == FILTER_BY_TEXT || filterEvent == FILTER_BY_TAG || filterEvent == FILTER_BY_DISTRIBUTION
                || filterEvent == FILTER_BY_TARGET_FILTER_QUERY;
    }

    private String getTargetTableStyle(final Long assignedDistributionSetId, final Long installedDistributionSetId) {
        final Long distPinned = managementUIState.getTargetTableFilters().getPinnedDistId().isPresent()
                ? managementUIState.getTargetTableFilters().getPinnedDistId().get() : null;

        if (null != distPinned && distPinned.equals(installedDistributionSetId)) {
            return SPUIDefinitions.HIGHTLIGHT_GREEN;
        } else if (null != distPinned && distPinned.equals(assignedDistributionSetId)) {
            return SPUIDefinitions.HIGHTLIGHT_ORANGE;
        }
        return null;
    }

    private String createTargetTableStyle(final Object itemId, final Object propertyId) {
        if (null == propertyId) {
            final Item item = getItem(itemId);
            final Long assignedDistributionSetId = (Long) item
                    .getItemProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_ID).getValue();
            final Long installedDistributionSetId = (Long) item
                    .getItemProperty(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_ID).getValue();
            return getTargetTableStyle(assignedDistributionSetId, installedDistributionSetId);
        }
        return null;
    }

    private void styleTargetTableOnPinning() {
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return createTargetTableStyle(itemId, propertyId);
            }
        });
    }

    private void restoreTargetTableStyle() {
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return null;
            }
        });
    }

    private void refreshTargets() {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final int size = targetContainer.size();
        if (size < SPUIDefinitions.MAX_TABLE_ENTRIES) {
            refreshTablecontainer();
        } else {
            // If table is not refreshed , explicitly target total count and
            // truncated count has to be updated
            resetTargetCountDetails();
        }

        if (size != 0) {
            setData(SPUIDefinitions.DATA_AVAILABLE);
        }

        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.REFRESH_TARGETS));
    }

    @SuppressWarnings("unchecked")
    private void updateVisibleItemOnEvent(final TargetInfo targetInfo) {
        final Target target = targetInfo.getTarget();
        final TargetIdName targetIdName = target.getTargetIdName();

        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final Item item = targetContainer.getItem(targetIdName);

        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(target.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_POLL_STATUS_TOOL_TIP)
                .setValue(HawkbitCommonUtil.getPollStatusToolTip(targetInfo.getPollStatus(), i18n));
        item.getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).setValue(targetInfo.getUpdateStatus());
    }

    private boolean isLastSelectedTarget(final TargetIdName targetIdName) {
        return null != managementUIState.getLastSelectedTargetIdName()
                && managementUIState.getLastSelectedTargetIdName().equals(targetIdName);
    }

    private boolean isFilterEnabled() {
        final TargetTableFilters targetTableFilters = managementUIState.getTargetTableFilters();
        return targetTableFilters.getSearchText().isPresent() || !targetTableFilters.getClickedTargetTags().isEmpty()
                || !targetTableFilters.getClickedStatusTargetTags().isEmpty()
                || targetTableFilters.getTargetFilterQuery().isPresent();
    }

    /**
     * Select all rows in the table.
     */
    @Override
    public void selectAll() {
        // As Vaadin Table only returns the current ItemIds which are visible
        // you don't need to search explicit for them.
        setValue(getItemIds());
    }

    /**
     * Clear all selections in the table.
     */
    private void unSelectAll() {
        setValue(null);
    }

    @Override
    protected void setDataAvailable(final boolean available) {
        managementUIState.setNoDataAvilableTarget(!available);
    }

    /**
     * Set total target count and count of targets truncated in target table.
     */
    private void resetTargetCountDetails() {
        final long totalTargetsCount = getTotalTargetsCount();
        managementUIState.setTargetsCountAll(totalTargetsCount);

        Collection<TargetUpdateStatus> status = null;
        Boolean overdueState = null;
        String[] targetTags = null;
        Long distributionId = null;
        String searchText = null;
        Long pinnedDistId = null;

        if (isFilteredByTags()) {
            targetTags = managementUIState.getTargetTableFilters().getClickedTargetTags().toArray(new String[0]);
        }
        if (isFilteredByStatus()) {
            status = managementUIState.getTargetTableFilters().getClickedStatusTargetTags();
        }
        if (managementUIState.getTargetTableFilters().isOverdueFilterEnabled()) {
            overdueState = managementUIState.getTargetTableFilters().isOverdueFilterEnabled();
        }
        if (managementUIState.getTargetTableFilters().getDistributionSet().isPresent()) {
            distributionId = managementUIState.getTargetTableFilters().getDistributionSet().get().getId();
        }
        if (isFilteredByText()) {
            searchText = String.format("%%%s%%", managementUIState.getTargetTableFilters().getSearchText().get());
        }
        final boolean noTagClicked = managementUIState.getTargetTableFilters().isNoTagSelected();
        if (managementUIState.getTargetTableFilters().getPinnedDistId().isPresent()) {
            pinnedDistId = managementUIState.getTargetTableFilters().getPinnedDistId().get();
        }

        final long size = getTargetsCountWithFilter(totalTargetsCount, pinnedDistId,
                new FilterParams(distributionId, status, overdueState, searchText, noTagClicked, targetTags));

        if (size > SPUIDefinitions.MAX_TABLE_ENTRIES) {
            managementUIState.setTargetsTruncated(size - SPUIDefinitions.MAX_TABLE_ENTRIES);
        }
    }

    private long getTargetsCountWithFilter(final long totalTargetsCount, final Long pinnedDistId,
            final FilterParams filterParams) {
        final long size;
        if (managementUIState.getTargetTableFilters().getTargetFilterQuery().isPresent()) {
            size = targetManagement.countTargetByTargetFilterQuery(
                    managementUIState.getTargetTableFilters().getTargetFilterQuery().get());
        } else if (noFilterSelected(filterParams.getFilterByStatus(), pinnedDistId,
                filterParams.getSelectTargetWithNoTag(), filterParams.getFilterByTagNames(),
                filterParams.getFilterBySearchText())) {
            size = totalTargetsCount;
        } else {
            size = targetManagement.countTargetByFilters(filterParams.getFilterByStatus(),
                    filterParams.getOverdueState(), filterParams.getFilterBySearchText(),
                    filterParams.getFilterByDistributionId(), filterParams.getSelectTargetWithNoTag(),
                    filterParams.getFilterByTagNames());
        }
        return size;
    }

    private boolean isFilteredByText() {
        return managementUIState.getTargetTableFilters().getSearchText().isPresent()
                && !Strings.isNullOrEmpty(managementUIState.getTargetTableFilters().getSearchText().get());
    }

    private static boolean noFilterSelected(final Collection<TargetUpdateStatus> status, final Long distributionId,
            final Boolean noTagClicked, final String[] targetTags, final String searchText) {
        return CollectionUtils.isEmpty(status) && distributionId == null && Strings.isNullOrEmpty(searchText)
                && !isTagSelected(targetTags, noTagClicked);
    }

    private static Boolean isTagSelected(final String[] targetTags, final Boolean noTagClicked) {
        return targetTags == null && !noTagClicked;
    }

    private long getTotalTargetsCount() {
        return targetManagement.countTargetsAll();
    }

    private boolean isFilteredByStatus() {
        return !managementUIState.getTargetTableFilters().getClickedStatusTargetTags().isEmpty();
    }

    private boolean isFilteredByTags() {
        return !managementUIState.getTargetTableFilters().getClickedTargetTags().isEmpty();
    }
}
