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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.eclipse.hawkbit.repository.TargetTagAssigmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.filter.FilterExpression;
import org.eclipse.hawkbit.ui.filter.Filters;
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
import org.eclipse.hawkbit.ui.management.event.TargetTagEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTagEvent.TargetTagComponentEvent;
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
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.base.Strings;
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
 *
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

    /**
     * EventListener method which is called by the event bus to notify about a
     * {@link TargetCreatedEvent}.
     *
     * @param targetCreatedEvent
     *            the target created event
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final TargetCreatedEvent targetCreatedEvent) {
        final Target createdTarget = targetCreatedEvent.getEntity();
        final TargetTableFilters targetTableFilters = managementUIState.getTargetTableFilters();
        final List<FilterExpression> filters = new ArrayList<>();
        if (targetTableFilters.getSearchText().isPresent()) {
            filters.add(new TargetSearchTextFilter(createdTarget, targetTableFilters.getSearchText().get()));
        }
        filters.add(new TargetStatusFilter(createdTarget, targetTableFilters.getClickedStatusTargetTags()));
        filters.add(new TargetTagFilter(createdTarget, targetTableFilters.getClickedTargetTags(), targetTableFilters
                .isNoTagSelected()));

        if (!Filters.or(filters).doFilter()) {
            addNewTarget(createdTarget);
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

    /**
     * EventListener method which is called by the event bus to notify about a
     * {@link TargetCreatedEvent}.
     *
     * @param event
     *            the target created event
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final TargetDeletedEvent event) {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final TargetIdName targetIdName = new TargetIdName(event.getTargetId(), null, null);
        if (getVisibleItemIds().contains(targetIdName)) {
            targetContainer.removeItem(targetIdName);
            targetContainer.commit();

        }
        if (managementUIState.getTargetsCountAll() > SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES) {
            ((LazyQueryContainer) getContainerDataSource()).refresh();
        }
        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.DELETE_TARGET, targetIdName));
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
        if (targetUIEvent.getTargetComponentEvent() == TargetComponentEvent.ADD_TARGET) {
            /** Below line to be removed when push is re-enable **/
            UI.getCurrent().access(() -> addNewTarget(targetUIEvent.getTarget()));
            setData(SPUIDefinitions.DATA_AVAILABLE);
        }
        if (targetUIEvent.getTargetComponentEvent() == TargetComponentEvent.EDIT_TARGET) {
            UI.getCurrent().access(() -> updateTarget(targetUIEvent.getTarget()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final TargetFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {
            if (checkFilterEvent(filterEvent)) {
                refreshFilter();
                /*
                 * TobeDone : remove explicit SHOW_COUNT_MESSAGE instead use
                 * TargetFilterEvent
                 */
                eventBus.publish(this, ManagementUIEvent.TARGET_TABLE_FILTER);
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent managementUIEvent) {
        UI.getCurrent().access(
                () -> {
                    if (managementUIEvent == ManagementUIEvent.UNASSIGN_TARGET_TAG
                            || managementUIEvent == ManagementUIEvent.ASSIGN_TARGET_TAG) {
                        refreshFilter();
                    }
                });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_ASSIGNMENTS) {
            refreshFilter();
        }
        if (event == SaveActionWindowEvent.DELETED_TARGETS) {
            eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.DELETE_TARGET, (Target) null));
        }
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
        final LazyQueryContainer targetTableContainer = new LazyQueryContainer(new LazyQueryDefinition(true,
                SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_CONT_ID_NAME), targetQF);
        targetTableContainer.getQueryView().getQueryDefinition().setMaxNestedPropertyDepth(PROPERTY_DEPT);

        return targetTableContainer;
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

        if (!managementUIState.getTargetTableFilters().getClickedTargetTags().isEmpty()) {
            final List<String> list = new ArrayList<String>();
            list.addAll(managementUIState.getTargetTableFilters().getClickedTargetTags());
            queryConfig.put(SPUIDefinitions.FILTER_BY_TAG, list.toArray(new String[list.size()]));
        }
        if (!managementUIState.getTargetTableFilters().getClickedStatusTargetTags().isEmpty()) {
            final List<TargetUpdateStatus> statusList = managementUIState.getTargetTableFilters()
                    .getClickedStatusTargetTags();
            queryConfig.put(SPUIDefinitions.FILTER_BY_STATUS, statusList);
        }
        return queryConfig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.AbstractTable#addContainerProperties
     * (com.vaadin.data.Container )
     */
    @Override
    protected void addContainerProperties(final Container container) {
        final LazyQueryContainer targetTableContainer = (LazyQueryContainer) container;
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CONT_ID, String.class, "", false, false);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_TARGET_STATUS, TargetUpdateStatus.class,
                TargetUpdateStatus.UNKNOWN, false, false);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_ID, Long.class, null,
                false, false);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_ID, Long.class, null,
                false, false);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER, String.class,
                "", false, true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_NAME_VER, String.class,
                "", false, true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.LAST_QUERY_DATE, Date.class, null, false, false);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null, false, true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class, null, false,
                true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
                true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class, null,
                false, true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_POLL_STATUS_TOOL_TIP, String.class, null,
                false, true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false, true);
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
            stylePinnedButton(pinBtn);
            isTargetPinned = Boolean.TRUE;
            targetPinnedBtn = pinBtn;
            eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
        }
        pinBtn.addStyleName(SPUIStyleDefinitions.TARGET_STATUS_PIN_TOGGLE);
        applyStatusLblStyle(pinBtn, itemId);
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
        stylePinnedButton(eventBtn);
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
        applyStatusLblStyle(pinBtn, pinBtn.getData());

    }

    /**
     * Set style of target table.
     *
     */
    @SuppressWarnings("serial")
    private void styleTargetTable() {
        setCellStyleGenerator((source, itemId, propertyId) -> null);
    }

    private void stylePinnedButton(final Button eventBtn) {
        eventBtn.addStyleName("targetPinned");
    }

    /**
     * Status Label Style.
     *
     * @param pinBtn
     * @param itemId
     */
    private void applyStatusLblStyle(final Button pinBtn, final Object itemId) {
        final Item item = getItem(itemId);
        if (item != null) {
            final TargetUpdateStatus updateStatus = (TargetUpdateStatus) item.getItemProperty(
                    SPUILabelDefinitions.VAR_TARGET_STATUS).getValue();
            pinBtn.removeStyleName("statusIconRed statusIconBlue statusIconGreen statusIconYellow statusIconLightBlue");
            if (updateStatus == TargetUpdateStatus.ERROR) {
                pinBtn.addStyleName("statusIconRed");
            } else if (updateStatus == TargetUpdateStatus.UNKNOWN) {
                pinBtn.addStyleName("statusIconBlue");
            } else if (updateStatus == TargetUpdateStatus.IN_SYNC) {
                pinBtn.addStyleName("statusIconGreen");
            } else if (updateStatus == TargetUpdateStatus.PENDING) {
                pinBtn.addStyleName("statusIconYellow");
            } else if (updateStatus == TargetUpdateStatus.REGISTERED) {
                pinBtn.addStyleName("statusIconLightBlue");
            }
        }
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
        final Set<TargetIdName> values = (Set) getValue();
        TargetIdName value = null;
        if (values != null && !values.isEmpty()) {
            final Iterator<TargetIdName> iterator = values.iterator();

            while (iterator.hasNext()) {
                value = iterator.next();
            }
            /**
             * Adding null check to make to avoid NPE.Its weird that at times
             * getValue returns null.
             */
            if (null != value) {
                managementUIState.setSelectedTargetIdName(values);
                managementUIState.setLastSelectedTargetIdName(value);
                final Target target = targetManagement.findTargetByControllerIDWithDetails(value.getControllerId());
                eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELECTED_TARGET, target));
            }
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
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.get("header.modifiedBy"),
                    0.1f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE,
                    i18n.get("header.modifiedDate"), 0.1f));
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

    private void doAssignments(final DragAndDropEvent event) {
        if (event.getTransferable().getSourceComponent() instanceof Table) {
            dsToTargetAssignment(event);
        } else if (event.getTransferable().getSourceComponent() instanceof DragAndDropWrapper && isNoTagAssigned(event)) {
            tagAssignment(event);
        }

    }

    private Boolean isNoTagAssigned(final DragAndDropEvent event) {
        final String tagName = ((DragAndDropWrapper) (event.getTransferable().getSourceComponent())).getData()
                .toString();
        if (tagName.equals(SPUIDefinitions.TARGET_TAG_BUTTON)) {
            notification.displayValidationError(i18n.get("message.tag.cannot.be.assigned",
                    new Object[] { i18n.get("label.no.tag.assigned") }));
            return false;
        }
        return true;
    }

    private void tagAssignment(final DragAndDropEvent event) {
        final com.vaadin.event.dd.TargetDetails taregtDet = event.getTargetDetails();
        final Table targetTable = (Table) taregtDet.getTarget();
        final Set<TargetIdName> targetSelected = (Set<TargetIdName>) targetTable.getValue();
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
        updateTagLayoutInDetails(result, targTagName);
    }

    private void updateTagLayoutInDetails(final TargetTagAssigmentResult result, final String targTagName) {
        if (result.getAssigned() > 0) {
            final List<String> assignedTargetNames = result.getAssignedTargets().stream().map(t -> t.getControllerId())
                    .collect(Collectors.toList());
            if (assignedTargetNames.contains(managementUIState.getLastSelectedTargetIdName().getControllerId())) {
                eventBus.publish(this, new TargetTagEvent(TargetTagComponentEvent.ASSIGNED, targTagName));
            }
        } else if (result.getUnassigned() > 0) {

            final List<String> unassignedTargetNames = result.getUnassignedTargets().stream()
                    .map(t -> t.getControllerId()).collect(Collectors.toList());
            if (unassignedTargetNames.contains(managementUIState.getLastSelectedTargetIdName().getControllerId())) {
                eventBus.publish(this, new TargetTagEvent(TargetTagComponentEvent.UNASSIGNED, targTagName));
            }

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
        if (!(source.getId().equals(SPUIComponetIdProvider.DIST_TABLE_ID) || source.getId().startsWith(
                SPUIDefinitions.TARGET_TAG_ID_PREFIXS))) {
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

    private Set<DistributionSetIdName> getDraggedDistributionSet(final TableTransferable transferable,
            final Table source) {
        @SuppressWarnings("unchecked")
        final Set<DistributionSetIdName> distSelected = (Set<DistributionSetIdName>) source.getValue();
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
        final String tagName = HawkbitCommonUtil
                .removePrefix(compsource.getId(), SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
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
                        message = getPendingActionMessage(message, HawkbitCommonUtil.getDistributionNameAndVersion(
                                distributionNameId.getName(), distributionNameId.getVersion()),
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
            item.getItemProperty(SPUILabelDefinitions.LAST_QUERY_DATE).setValue(
                    updatedTarget.getTargetInfo().getLastTargetQuery());

            item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setValue(
                    HawkbitCommonUtil.getIMUser(updatedTarget.getLastModifiedBy()));
            item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE).setValue(
                    SPDateTimeUtil.getFormattedDate(updatedTarget.getLastModifiedAt()));
            item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(updatedTarget.getDescription());

            /*
             * Update the status which will trigger the value change lister
             * registered for the target update status. That listener will
             * update the new status icon showing for this target in the table.
             */
            item.getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).setValue(
                    updatedTarget.getTargetInfo().getUpdateStatus());
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

    // Added by - Asha
    private String getTargetTableStyle(final Long assignedDistributionSetId, final Long installedDistributionSetId) {
        final Long distPinned = managementUIState.getTargetTableFilters().getPinnedDistId().isPresent() ? managementUIState
                .getTargetTableFilters().getPinnedDistId().get()
                : null;

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
            final Long assignedDistributionSetId = (Long) item.getItemProperty(
                    SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_ID).getValue();
            final Long installedDistributionSetId = (Long) item.getItemProperty(
                    SPUILabelDefinitions.INSTALLED_DISTRIBUTION_ID).getValue();
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
    public void addNewTarget(final Target newTarget) {
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        targetContainer.refresh();
        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.ADD_TARGET, newTarget));
    }

    private void updateVisibleItemOnEvent(final TargetInfo targetInfo, final Target target,
            final TargetIdName targetIdName, final LazyQueryContainer targetContainer) {
        final Item item = targetContainer.getItem(targetIdName);
        item.getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).setValue(targetInfo.getUpdateStatus());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(target.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_POLL_STATUS_TOOL_TIP).setValue(
                HawkbitCommonUtil.getPollStatusToolTip(targetInfo.getPollStatus(), i18n));

        // workaround until push is available for action history, re-select the
        // updated target so
        // the action history gets refreshed.
        if (isSelected(targetIdName)) {
            unselect(targetIdName);
            select(targetIdName);
        }
    }

    /**
     * EventListener method which is called by the event bus to notify about a
     * {@link TargetInfoUpdateEvent}.
     *
     * @param targetInfoUpdateEvent
     *            the target info update event
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final TargetInfoUpdateEvent targetInfoUpdateEvent) {
        final TargetInfo targetInfo = targetInfoUpdateEvent.getEntity();
        final Target target = targetInfo.getTarget();
        final TargetIdName targetIdName = target.getTargetIdName();
        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final TargetTableFilters targetTableFilters = managementUIState.getTargetTableFilters();

        @SuppressWarnings("unchecked")
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();
        if (visibleItemIds.contains(targetIdName)) {
            updateVisibleItemOnEvent(targetInfo, target, targetIdName, targetContainer);
        } else {
            final List<FilterExpression> filters = new ArrayList<>();
            if (targetTableFilters.getSearchText().isPresent()) {
                filters.add(new TargetSearchTextFilter(target, targetTableFilters.getSearchText().get()));
            }
            filters.add(new TargetStatusFilter(target, targetTableFilters.getClickedStatusTargetTags()));
            filters.add(new TargetTagFilter(target, targetTableFilters.getClickedTargetTags(), targetTableFilters
                    .isNoTagSelected()));

            if (!Filters.or(filters).doFilter()) {
                addNewTarget(target);
            }
            addNewTarget(target);
        }
    }

    /**
     * Select all rows in the table.
     */
    public void selectAll() {
        final Long filterByDistId = managementUIState.getTargetTableFilters().getDistributionSet().isPresent() ? managementUIState
                .getTargetTableFilters().getDistributionSet().get().getId()
                : null;
        final List<TargetUpdateStatus> statusList = new ArrayList<TargetUpdateStatus>();
        if (!managementUIState.getTargetTableFilters().getClickedStatusTargetTags().isEmpty()) {
            statusList.addAll(managementUIState.getTargetTableFilters().getClickedStatusTargetTags());
        }
        final List<String> tagList = new ArrayList<String>();
        if (!managementUIState.getTargetTableFilters().getClickedTargetTags().isEmpty()) {
            tagList.addAll(managementUIState.getTargetTableFilters().getClickedTargetTags());
        }
        String searchText = managementUIState.getTargetTableFilters().getSearchText().isPresent() ? managementUIState
                .getTargetTableFilters().getSearchText().get() : null;
        if (!Strings.isNullOrEmpty(searchText)) {
            searchText = String.format("%%%s%%", searchText);
        }
        final Boolean noTagSelected = managementUIState.getTargetTableFilters().isNoTagSelected();

        final String[] tagArray = tagList.toArray(new String[tagList.size()]);

        // limit the selection of all targets of the targets only currently
        // showed in the list, so
        // maxiumm the SP SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES
        final PageRequest pageRequest = new OffsetBasedPageRequest(0, SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES,
                new Sort(Direction.DESC, "createdAt"));
        setValue(targetManagement.findAllTargetIdsByFilters(pageRequest, filterByDistId, statusList, searchText,
                noTagSelected, tagList.toArray(tagArray)));
    }

    /**
     * Clear all selections in the table.
     */
    public void unSelectAll() {
        setValue(null);
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

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    private void setNoDataAvailable() {
        final int tableSize = getContainerDataSource().size();
        if (tableSize == 0) {
            managementUIState.setNoDataAvilableTarget(true);
        } else {
            managementUIState.setNoDataAvilableTarget(false);
        }
    }

}
