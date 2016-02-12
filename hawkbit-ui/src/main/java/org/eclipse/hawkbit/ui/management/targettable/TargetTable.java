/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.eventbus.event.TargetCreatedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetDeletedEvent;
import org.eclipse.hawkbit.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTagAssigmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.filter.FilterExpression;
import org.eclipse.hawkbit.ui.filter.Filters;
import org.eclipse.hawkbit.ui.filter.target.CustomTargetFilter;
import org.eclipse.hawkbit.ui.filter.target.TargetSearchTextFilter;
import org.eclipse.hawkbit.ui.filter.target.TargetStatusFilter;
import org.eclipse.hawkbit.ui.filter.target.TargetTagFilter;
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
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Concrete implementation of Target table.
 *
 */
@SpringComponent
@ViewScope
public class TargetTable extends AbstractTable implements Handler {

    private static final long serialVersionUID = -2300392868806614568L;

    private static final Logger LOG = LoggerFactory.getLogger(TargetTable.class);

    private static final int PROPERTY_DEPT = 3;
    private static final String ITEMID = "itemId";
    private static final String ACTION_NOT_ALLOWED_MSG = "message.action.not.allowed";

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private I18N i18n;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private SpPermissionChecker permChecker;

    @Autowired
    private UINotification notification;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementViewAcceptCriteria managementViewAcceptCriteria;

    private Button targetPinnedBtn;

    private Boolean isTargetPinned = Boolean.FALSE;
    private ShortcutAction actionSelectAll;
    private ShortcutAction actionUnSelectAll;
    
    private Boolean isFilterEvent = Boolean.FALSE;;
   

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        addActionHandler(this);
        actionSelectAll = new ShortcutAction(i18n.get("action.target.table.selectall"));
        actionUnSelectAll = new ShortcutAction(i18n.get("action.target.table.clear"));
        eventBus.subscribe(this);
        setNoDataAvailable();
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    /**
     * EventListener method which is called when a list of events is published.
     * Event types should not be mixed up.
     *
     * @param events
     *            list of events
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvents(final List<?> events) {
        final Object firstEvent = events.get(0);
        if (TargetCreatedEvent.class.isInstance(firstEvent)) {
            onTargetCreatedEvents();
        } else if (TargetInfoUpdateEvent.class.isInstance(firstEvent)) {
            onTargetInfoUpdateEvents((List<TargetInfoUpdateEvent>) events);
        } else if (TargetDeletedEvent.class.isInstance(firstEvent)) {
            onTargetDeletedEvent((List<TargetDeletedEvent>) events);
        }
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
        if (targetUIEvent.getTargetComponentEvent() == TargetComponentEvent.EDIT_TARGET) {
            UI.getCurrent().access(() -> updateTarget(targetUIEvent.getTarget()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {
            if (checkFilterEvent(filterEvent)) {
               if(((boolean)prepareQueryConfigFilters().get(SPUIDefinitions.FILTER_BY_NO_TAG)==false)
                        && prepareQueryConfigFilters().size()<2
                        && isFilterEvent==Boolean.FALSE){
                   ((LazyQueryContainer) getContainerDataSource()).refresh();
                                           
                }else {
                    refreshFilter();
                    if(prepareQueryConfigFilters().size()<2){
                        isFilterEvent = Boolean.FALSE;
                    }else{
                        isFilterEvent = Boolean.TRUE;
                    }
                } 
             
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
        if (event.getTargetComponentEvent() == TargetComponentEvent.MINIMIZED) {
            UI.getCurrent().access(() -> applyMinTableSettings());
        } else if (event.getTargetComponentEvent() == TargetComponentEvent.MAXIMIZED) {
            UI.getCurrent().access(() -> applyMaxTableSettings());
        } else if (event.getTargetComponentEvent() == TargetComponentEvent.EDIT_TARGET) {
            UI.getCurrent().access(() -> updateTarget(event.getTarget()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.AbstractTable#getTableId()
     */
    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.TARGET_TABLE_ID;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.AbstractTable#createContainer(
     * )
     */
    @Override
    protected Container createContainer() {
        // ADD all the filters to the query config
        final Map<String, Object> queryConfig = prepareQueryConfigFilters();

        // Create TargetBeanQuery factory with the query config.
        final BeanQueryFactory<TargetBeanQuery> targetQF = new BeanQueryFactory<TargetBeanQuery>(TargetBeanQuery.class);
        targetQF.setQueryConfiguration(queryConfig);

        // create lazy query container with lazy defination and query
        final LazyQueryContainer targetTableContainer = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_CONT_ID_NAME),
                targetQF);
        targetTableContainer.getQueryView().getQueryDefinition().setMaxNestedPropertyDepth(PROPERTY_DEPT);

        return targetTableContainer;
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.table.AbstractTable#addContainerProperties
     * (com.vaadin.data.Container )
     */
    @Override
    protected void addContainerProperties(final Container container) {
        HawkbitCommonUtil.addTargetTableContainerProperties(container);
    }

    @Override
    public Action[] getActions(final Object target, final Object sender) {
        return new Action[] { actionSelectAll, actionUnSelectAll };
    }

    @Override
    public void handleAction(final Action action, final Object sender, final Object target) {
        if (actionSelectAll.equals(action)) {
            selectAll();
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELLECT_ALL));
        }
        if (actionUnSelectAll.equals(action)) {
            unSelectAll();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTable#
     * addCustomGeneratedColumns ()
     */
    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUIDefinitions.TARGET_STATUS_PIN_TOGGLE_ICON,
                (source, itemId, columnId) -> getTagetPinButton(itemId));
        addGeneratedColumn(SPUIDefinitions.TARGET_STATUS_POLL_TIME,
                (source, itemId, columnId) -> getTagetPollTime(itemId));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTable#
     * isFirstRowSelectedOnLoad ()
     */
    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return !managementUIState.getSelectedTargetIdName().isPresent()
                || managementUIState.getSelectedTargetIdName().get().isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.table.AbstractTable#getItemIdToSelect()
     */
    @Override
    protected Object getItemIdToSelect() {
        if (managementUIState.getSelectedTargetIdName().isPresent()) {
            setCurrentPageFirstItemId(managementUIState.getLastSelectedTargetIdName());
            return managementUIState.getSelectedTargetIdName().get();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.AbstractTable#onValueChange()
     */
    @Override
    protected void onValueChange() {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        @SuppressWarnings("unchecked")
        final Set<TargetIdName> values = HawkbitCommonUtil.getSelectedTargetDetails(this);
        if (values != null && !values.isEmpty()) {
            final TargetIdName lastSelectedItem = getLastSelectedItem(values);
            managementUIState.setSelectedTargetIdName(values);
            managementUIState.setLastSelectedTargetIdName(lastSelectedItem);
            final Target target = targetManagement
                    .findTargetByControllerIDWithDetails(lastSelectedItem.getControllerId());
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELECTED_TARGET, target));
        } else {
            managementUIState.setSelectedTargetIdName(null);
            managementUIState.setLastSelectedTargetIdName(null);
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELECTED_TARGET, (Target) null));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.AbstractTable#isMaximized()
     */
    @Override
    protected boolean isMaximized() {
        return managementUIState.isTargetTableMaximized();
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.table.AbstractTable#getTableVisibleColumns
     * ()
     */
    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = new ArrayList<TableColumn>();
        if (isMaximized()) {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.get("header.name"), 0.2f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_BY, i18n.get("header.createdBy"), 0.1f));
            columnList
                    .add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.1f));
            columnList.add(
                    new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.1f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, i18n.get("header.modifiedDate"),
                    0.1f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.get("header.description"), 0.2f));
        } else {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.get("header.name"), 0.8f));
            columnList.add(new TableColumn(SPUIDefinitions.TARGET_STATUS_POLL_TIME, "", 0.0f));
            columnList.add(new TableColumn(SPUIDefinitions.TARGET_STATUS_PIN_TOGGLE_ICON, "", 0.0f));
        }
        return columnList;
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.table.AbstractTable#getTableDropHandler()
     */
    @Override
    protected DropHandler getTableDropHandler() {
        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return managementViewAcceptCriteria;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                if (doValidations(event)) {
                    doAssignments(event);
                }
            }
        };
    }

    private void onTargetDeletedEvent(final List<TargetDeletedEvent> events) {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();
        boolean shouldRefreshTargets = false;
        for (final TargetDeletedEvent deletedEvent : events) {
            final TargetIdName targetIdName = new TargetIdName(deletedEvent.getTargetId(), null, null);
            if (visibleItemIds.contains(targetIdName)) {
                targetContainer.removeItem(targetIdName);
            } else {
                shouldRefreshTargets = true;
            }
            unselect(targetIdName);
        }
        if (shouldRefreshTargets) {
            refreshOnDelete();
        } else {
            targetContainer.commit();
            selectRow();
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
        final Map<String, Object> queryConfig = new HashMap<String, Object>();
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
            final List<String> list = new ArrayList<String>();
            list.addAll(managementUIState.getTargetTableFilters().getClickedTargetTags());
            queryConfig.put(SPUIDefinitions.FILTER_BY_TAG, list.toArray(new String[list.size()]));
        }
        if (isFilteredByStatus()) {
            final List<TargetUpdateStatus> statusList = managementUIState.getTargetTableFilters()
                    .getClickedStatusTargetTags();
            queryConfig.put(SPUIDefinitions.FILTER_BY_STATUS, statusList);
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
        pinBtn.setId(SPUIComponetIdProvider.TARGET_PIN_ICON + "." + itemId);
        pinBtn.addClickListener(event -> addPinClickListener(event));
        if (isPinned(((TargetIdName) itemId).getControllerId())) {
            pinBtn.addStyleName("targetPinned");
            isTargetPinned = Boolean.TRUE;
            targetPinnedBtn = pinBtn;
            eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
        }
        pinBtn.addStyleName(SPUIStyleDefinitions.TARGET_STATUS_PIN_TOGGLE);
        HawkbitCommonUtil.applyStatusLblStyle(this, pinBtn, itemId);
        return pinBtn;
    }

    private boolean isPinned(final String targetId) {
        boolean result = false;
        if (managementUIState.getDistributionTableFilters().getPinnedTargetId().isPresent()
                && targetId.equals(managementUIState.getDistributionTableFilters().getPinnedTargetId().get())) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Add listener to pin.
     *
     * @param pinBtn
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
        eventBtn.addStyleName("targetPinned");
        isTargetPinned = Boolean.FALSE;
    }

    private void unPinTarget(final Button eventBtn) {
        managementUIState.getDistributionTableFilters().setPinnedTargetId(null);
        eventBus.publish(this, PinUnpinEvent.UNPIN_TARGET);
        resetPinStyle(eventBtn);
    }

    private void resetPinStyle(final Button pinBtn) {
        pinBtn.removeStyleName("targetPinned");
        pinBtn.addStyleName(SPUIStyleDefinitions.TARGET_STATUS_PIN_TOGGLE);
        HawkbitCommonUtil.applyStatusLblStyle(this, pinBtn, pinBtn.getData());
    }

    /**
     * Set style of target table.
     *
     */
    @SuppressWarnings("serial")
    private void styleTargetTable() {
        setCellStyleGenerator((source, itemId, propertyId) -> null);
    }

    private void doAssignments(final DragAndDropEvent event) {
        if (event.getTransferable().getSourceComponent() instanceof Table) {
            dsToTargetAssignment(event);
        } else if (event.getTransferable().getSourceComponent() instanceof DragAndDropWrapper
                && isNoTagAssigned(event)) {
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
        final com.vaadin.event.dd.TargetDetails taregtDet = event.getTargetDetails();
        final Table targetTable = (Table) taregtDet.getTarget();
        final Set<TargetIdName> targetSelected = HawkbitCommonUtil.getSelectedTargetDetails(targetTable);
        final Set<String> targetList = new HashSet<String>();
        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object targetItemId = dropData.getItemIdOver();
        if (!targetSelected.contains(targetItemId)) {
            targetList.add(((TargetIdName) targetItemId).getControllerId());
        } else {
            targetList.addAll(targetSelected.stream().map(t -> t.getControllerId()).collect(Collectors.toList()));
        }
        final String targTagName = HawkbitCommonUtil.removePrefix(event.getTransferable().getSourceComponent().getId(),
                SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        final TargetTagAssigmentResult result = targetManagement.toggleTagAssignment(targetList, targTagName);

        final List<String> tagsClickedList = managementUIState.getTargetTableFilters().getClickedTargetTags();
        notification.displaySuccess(HawkbitCommonUtil.getTargetTagAssigmentMsg(targTagName, result, i18n));
        if (result.getUnassigned() >= 1 && !tagsClickedList.isEmpty()) {
            refreshFilter();
        }
    }

    /**
     * Check Validation on Drop.
     *
     * @param dragEvent
     *            as drop event
     * @return Boolean as flag
     */
    private Boolean doValidations(final DragAndDropEvent dragEvent) {
        final Component compsource = dragEvent.getTransferable().getSourceComponent();
        if (compsource instanceof Table) {
            return validateTable(compsource, (TableTransferable) dragEvent.getTransferable());
        } else if (compsource instanceof DragAndDropWrapper) {
            validateDragAndDropWrapper(compsource);
        } else {
            notification.displayValidationError(i18n.get(ACTION_NOT_ALLOWED_MSG));
            return false;
        }
        return true;
    }

    private Boolean validateTable(final Component compsource, final TableTransferable transferable) {
        final Table source = (Table) compsource;
        if (!(source.getId().equals(SPUIComponetIdProvider.DIST_TABLE_ID)
                || source.getId().startsWith(SPUIDefinitions.TARGET_TAG_ID_PREFIXS))) {
            notification.displayValidationError(i18n.get(ACTION_NOT_ALLOWED_MSG));
            return false;
        } else if (!permChecker.hasUpdateTargetPermission()) {
            notification.displayValidationError(i18n.get("message.permission.insufficient"));
            return false;
        } else if (getDraggedDistributionSet(transferable, source).size() > 1) {
            notification.displayValidationError(i18n.get("message.onlyone.distribution.assigned"));
            return false;
        }
        return true;
    }

    private static Set<DistributionSetIdName> getDraggedDistributionSet(final TableTransferable transferable,
            final Table source) {
        @SuppressWarnings("unchecked")
        final Set<DistributionSetIdName> distSelected = HawkbitCommonUtil.getSelectedDSDetails(source);
        final Set<DistributionSetIdName> distributionIdSet = new HashSet<DistributionSetIdName>();
        if (!distSelected.contains(transferable.getData(ITEMID))) {
            distributionIdSet.add((DistributionSetIdName) transferable.getData(ITEMID));
        } else {
            distributionIdSet.addAll(distSelected);
        }
        return distributionIdSet;
    }

    private Boolean validateDragAndDropWrapper(final Component compsource) {
        final DragAndDropWrapper wrapperSource = (DragAndDropWrapper) compsource;
        final String tagName = HawkbitCommonUtil.removePrefix(compsource.getId(),
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

    private void dsToTargetAssignment(final DragAndDropEvent event) {
        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        final Table source = transferable.getSourceComponent();
        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object targetItemId = dropData.getItemIdOver();
        LOG.debug("Adding a log to check if targetItemId is null : {} ", targetItemId);
        if (targetItemId != null) {
            final TargetIdName targetId = (TargetIdName) targetItemId;
            String message = null;
            for (final DistributionSetIdName distributionNameId : getDraggedDistributionSet(transferable, source)) {
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
                    .setValue(HawkbitCommonUtil.getIMUser(updatedTarget.getLastModifiedBy()));
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

    /**
     * @param filterEvent
     * @return
     */
    private boolean checkFilterEvent(final TargetFilterEvent filterEvent) {
        boolean isFilterEvent = false;
        boolean isFilter = false;
        boolean isRemoveFilters = false;
        boolean isStatusFilter = false;
        isFilter = filterEvent == TargetFilterEvent.FILTER_BY_TEXT || filterEvent == TargetFilterEvent.FILTER_BY_TAG
                || filterEvent == TargetFilterEvent.FILTER_BY_DISTRIBUTION
                || filterEvent == TargetFilterEvent.FILTER_BY_TARGET_FILTER_QUERY;

        isRemoveFilters = filterEvent == TargetFilterEvent.REMOVE_FILTER_BY_TEXT
                || filterEvent == TargetFilterEvent.REMOVE_FILTER_BY_TAG
                || filterEvent == TargetFilterEvent.REMOVE_FILTER_BY_DISTRIBUTION
                || filterEvent == TargetFilterEvent.REMOVE_FILTER_BY_TARGET_FILTER_QUERY;
        isStatusFilter = filterEvent == TargetFilterEvent.FILTER_BY_STATUS
                || filterEvent == TargetFilterEvent.REMOVE_FILTER_BY_STATUS;

        isFilterEvent = isFilter || isRemoveFilters || isStatusFilter;

        return isFilterEvent;
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

    /**
     * @param itemId
     * @param propertyId
     * @return
     */
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

    /**
     * To add new target to the table on Top.
     *
     * @param newTarget
     *            as reference
     */
    private void refreshTargets() {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final int size = targetContainer.size();
        if (size < SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES) {
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

    private void updateVisibleItemOnEvent(final TargetInfo targetInfo, final Target target,
            final TargetIdName targetIdName) {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final Item item = targetContainer.getItem(targetIdName);
        item.getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).setValue(targetInfo.getUpdateStatus());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(target.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_POLL_STATUS_TOOL_TIP)
                .setValue(HawkbitCommonUtil.getPollStatusToolTip(targetInfo.getPollStatus(), i18n));
    }

    private boolean isLastSelectedTarget(final TargetIdName targetIdName) {
        return null != managementUIState.getLastSelectedTargetIdName()
                && managementUIState.getLastSelectedTargetIdName().equals(targetIdName);
    }

    /**
     * EventListener method which is called by the event bus to notify about a
     * list of {@link TargetInfoUpdateEvent}.
     *
     * @param targetInfoUpdateEvents
     *            list of target info update event
     */
    @SuppressWarnings("unchecked")
    private void onTargetInfoUpdateEvents(final List<TargetInfoUpdateEvent> targetInfoUpdateEvents) {
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();
        boolean shoulTargetsUpdated = false;
        Target lastSelectedTarget = null;
        for (final TargetInfoUpdateEvent targetInfoUpdateEvent : targetInfoUpdateEvents) {
            final TargetInfo targetInfo = targetInfoUpdateEvent.getEntity();
            final Target target = targetInfo.getTarget();
            final TargetIdName targetIdName = target.getTargetIdName();
            if (Filters.or(getTargetTableFilters(target)).doFilter()) {
                shoulTargetsUpdated = true;
            } else {
                if (visibleItemIds.contains(targetIdName)) {
                    updateVisibleItemOnEvent(targetInfo, target, targetIdName);
                }
            }
            // workaround until push is available for action history, re-select
            // the
            // updated target so
            // the action history gets refreshed.
            if (isLastSelectedTarget(targetIdName)) {
                lastSelectedTarget = target;
            }
        }
        if (shoulTargetsUpdated) {
            refreshTargets();
        }
        if (lastSelectedTarget != null) {
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELECTED_TARGET, lastSelectedTarget));
        }
    }

    private void onTargetCreatedEvents() {
        refreshTargets();
    }

    private List<FilterExpression> getTargetTableFilters(final Target target) {
        final TargetTableFilters targetTableFilters = managementUIState.getTargetTableFilters();
        final List<FilterExpression> filters = new ArrayList<>();
        if (targetTableFilters.getSearchText().isPresent()) {
            filters.add(new TargetSearchTextFilter(target, targetTableFilters.getSearchText().get()));
        }
        filters.add(new TargetStatusFilter(targetTableFilters.getClickedStatusTargetTags()));
        filters.add(new TargetTagFilter(target, targetTableFilters.getClickedTargetTags(),
                targetTableFilters.isNoTagSelected()));
        filters.add(new CustomTargetFilter(targetTableFilters.getTargetFilterQuery()));
        return filters;
    }

    /**
     * Select all rows in the table.
     */
    public void selectAll() {
        final PageRequest pageRequest = new OffsetBasedPageRequest(0, size(),
                new Sort(SPUIDefinitions.TARGET_TABLE_CREATE_AT_SORT_ORDER, "createdAt"));
        List<TargetIdName> targetIdList;
        // is custom filter selected
        if (managementUIState.getTargetTableFilters().getTargetFilterQuery().isPresent()) {
            targetIdList = getTargetIdsByCustomFilters(pageRequest);
        } else {
            targetIdList = getTargetIdsBySimpleFilters(pageRequest);
        }
        setValue(targetIdList);
    }

    private List<TargetIdName> getTargetIdsBySimpleFilters(final PageRequest pageRequest) {
        final Long filterByDistId = managementUIState.getTargetTableFilters().getDistributionSet().isPresent()
                ? managementUIState.getTargetTableFilters().getDistributionSet().get().getId() : null;
        final List<TargetUpdateStatus> statusList = new ArrayList<TargetUpdateStatus>();
        if (isFilteredByStatus()) {
            statusList.addAll(managementUIState.getTargetTableFilters().getClickedStatusTargetTags());
        }
        final List<String> tagList = new ArrayList<String>();
        if (isFilteredByTags()) {
            tagList.addAll(managementUIState.getTargetTableFilters().getClickedTargetTags());
        }
        String searchText = managementUIState.getTargetTableFilters().getSearchText().isPresent()
                ? managementUIState.getTargetTableFilters().getSearchText().get() : null;
        if (!Strings.isNullOrEmpty(searchText)) {
            searchText = String.format("%%%s%%", searchText);
        }
        final Boolean noTagSelected = managementUIState.getTargetTableFilters().isNoTagSelected();

        final String[] tagArray = tagList.toArray(new String[tagList.size()]);

        List<TargetIdName> targetIdList;
        targetIdList = targetManagement.findAllTargetIdsByFilters(pageRequest, filterByDistId, statusList, searchText,
                noTagSelected, tagList.toArray(tagArray));
        Collections.reverse(targetIdList);
        return targetIdList;
    }

    private List<TargetIdName> getTargetIdsByCustomFilters(final PageRequest pageRequest) {
        List<TargetIdName> targetIdList;
        final TargetFilterQuery targetFilterQuery = managementUIState.getTargetTableFilters().getTargetFilterQuery()
                .isPresent() ? managementUIState.getTargetTableFilters().getTargetFilterQuery().get() : null;
        targetIdList = targetManagement.findAllTargetIdsByTargetFilterQuery(pageRequest, targetFilterQuery);
        Collections.reverse(targetIdList);
        return targetIdList;
    }

    /**
     * Clear all selections in the table.
     */
    private void unSelectAll() {
        setValue(null);
    }

    private void setNoDataAvailable() {
        final int tableSize = getContainerDataSource().size();
        if (tableSize == 0) {
            managementUIState.setNoDataAvilableTarget(true);
        } else {
            managementUIState.setNoDataAvilableTarget(false);
        }
    }

    /**
     * Set total target count and count of targets truncated in target table.
     */
    private void resetTargetCountDetails() {
        final long size;
        final long totalTargetsCount = getTotalTargetsCount();
        managementUIState.setTargetsCountAll(totalTargetsCount);

        Collection<TargetUpdateStatus> status = null;
        String[] targetTags = null;
        Long distributionId = null;
        String searchText = null;
        Boolean noTagClicked = Boolean.FALSE;
        Long pinnedDistId = null;

        if (isFilteredByTags()) {
            targetTags = (String[]) managementUIState.getTargetTableFilters().getClickedTargetTags().toArray();
        }
        if (isFilteredByStatus()) {
            status = managementUIState.getTargetTableFilters().getClickedStatusTargetTags();
        }
        if (managementUIState.getTargetTableFilters().getDistributionSet().isPresent()) {
            distributionId = managementUIState.getTargetTableFilters().getDistributionSet().get().getId();
        }
        if (isFilteredByText()) {
            searchText = String.format("%%%s%%", managementUIState.getTargetTableFilters().getSearchText().get());
        }
        noTagClicked = managementUIState.getTargetTableFilters().isNoTagSelected();
        if (managementUIState.getTargetTableFilters().getPinnedDistId().isPresent()) {
            pinnedDistId = managementUIState.getTargetTableFilters().getPinnedDistId().get();
        }

        size = getTargetsCountWithFilter(totalTargetsCount, status, targetTags, distributionId, searchText,
                noTagClicked, pinnedDistId);

        if (size > SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES) {
            managementUIState.setTargetsTruncated(size - SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES);
        }
    }

    private long getTargetsCountWithFilter(final long totalTargetsCount, final Collection<TargetUpdateStatus> status,
            final String[] targetTags, final Long distributionId, final String searchText, final Boolean noTagClicked,
            final Long pinnedDistId) {
        final long size;
        if (managementUIState.getTargetTableFilters().getTargetFilterQuery().isPresent()) {
            size = targetManagement.countTargetByTargetFilterQuery(
                    managementUIState.getTargetTableFilters().getTargetFilterQuery().get());
        } else if (!anyFilterSelected(status, pinnedDistId, noTagClicked, targetTags, searchText)) {
            size = totalTargetsCount;
        } else {
            size = targetManagement.countTargetByFilters(status, searchText, distributionId, noTagClicked, targetTags);
        }
        return size;
    }

    private boolean isFilteredByText() {
        return managementUIState.getTargetTableFilters().getSearchText().isPresent()
                && !Strings.isNullOrEmpty(managementUIState.getTargetTableFilters().getSearchText().get());
    }

    private Boolean anyFilterSelected(final Collection<TargetUpdateStatus> status, final Long distributionId,
            final Boolean noTagClicked, final String[] targetTags, final String searchText) {
        return status == null && distributionId == null && Strings.isNullOrEmpty(searchText)
                && !isTagSelected(targetTags, noTagClicked);
    }

    private Boolean isTagSelected(final String[] targetTags, final Boolean noTagClicked) {
        return targetTags == null && !noTagClicked;
    }

    private long getTotalTargetsCount() {
        return targetManagement.countTargetsAll();
    }

    private static TargetIdName getLastSelectedItem(final Set<TargetIdName> values) {
        return Iterables.getLast(values);
    }

    private boolean isFilteredByStatus() {
        return !managementUIState.getTargetTableFilters().getClickedStatusTargetTags().isEmpty();
    }

    private boolean isFilteredByTags() {
        return !managementUIState.getTargetTableFilters().getClickedTargetTags().isEmpty();
    }
}