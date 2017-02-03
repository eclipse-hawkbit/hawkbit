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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.RemoteEntityEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.ManagmentEntityState;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetAddUpdateWindowEvent;
import org.eclipse.hawkbit.ui.management.event.TargetFilterEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.state.TargetTableFilters;
import org.eclipse.hawkbit.ui.push.CancelTargetAssignmentEventContainer;
import org.eclipse.hawkbit.ui.push.TargetUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.AssignInstalledDSTooltipGenerator;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
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
public class TargetTable extends AbstractTable<Target, Long> {

    private static final Logger LOG = LoggerFactory.getLogger(TargetTable.class);
    private static final String TARGET_PINNED = "targetPinned";
    private static final long serialVersionUID = -2300392868806614568L;
    private static final int PROPERTY_DEPT = 3;

    private final transient TargetManagement targetManagement;
    private final transient DistributionSetManagement distributionSetManagement;
    private final transient TagManagement tagManagement;

    private final SpPermissionChecker permChecker;

    private final ManagementViewClientCriterion managementViewClientCriterion;

    private final ManagementUIState managementUIState;

    private Button targetPinnedBtn;
    private boolean isTargetPinned;

    public TargetTable(final UIEventBus eventBus, final I18N i18n, final UINotification notification,
            final TargetManagement targetManagement, final ManagementUIState managementUIState,
            final SpPermissionChecker permChecker, final ManagementViewClientCriterion managementViewClientCriterion,
            final DistributionSetManagement distributionSetManagement, final TagManagement tagManagement) {
        super(eventBus, i18n, notification);
        this.targetManagement = targetManagement;
        this.permChecker = permChecker;
        this.managementViewClientCriterion = managementViewClientCriterion;
        this.managementUIState = managementUIState;
        this.distributionSetManagement = distributionSetManagement;
        this.tagManagement = tagManagement;

        setItemDescriptionGenerator(new AssignInstalledDSTooltipGenerator());

        addNewContainerDS();
        setColumnProperties();
        setDataAvailable(getContainerDataSource().size() != 0);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onCancelTargetAssignmentEvents(final CancelTargetAssignmentEventContainer eventContainer) {
        // workaround until push is available for action
        // history, re-select
        // the updated target so the action history gets
        // refreshed.
        publishTargetSelectedEntityForRefresh(eventContainer.getEvents().stream());
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onTargetUpdatedEvents(final TargetUpdatedEventContainer eventContainer) {
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();

        if (isFilterEnabled()) {
            refreshTargets();
        } else {
            eventContainer.getEvents().stream().filter(event -> visibleItemIds.contains(event.getEntityId()))
                    .forEach(event -> updateVisibleItemOnEvent(event.getEntity().getTargetInfo()));
        }
        publishTargetSelectedEntityForRefresh(eventContainer.getEvents().stream());
    }

    private void publishTargetSelectedEntityForRefresh(
            final Stream<? extends RemoteEntityEvent<Target>> targetEntityEventStream) {
        targetEntityEventStream.filter(event -> isLastSelectedTarget(event.getEntityId())).findAny()
                .ifPresent(event -> eventBus.publish(this,
                        new TargetTableEvent(BaseEntityEventType.SELECTED_ENTITY, event.getEntity())));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
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

    @EventBusListenerMethod(scope = EventScope.UI)
    void addOrEditEvent(final TargetAddUpdateWindowEvent targetUIEvent) {
        if (BaseEntityEventType.UPDATED_ENTITY != targetUIEvent.getEventType()) {
            return;
        }
        UI.getCurrent().access(() -> updateTarget(targetUIEvent.getEntity()));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {
            if (checkFilterEvent(filterEvent)) {
                refreshFilter();
                eventBus.publish(this, ManagementUIEvent.TARGET_TABLE_FILTER);
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent managementUIEvent) {
        UI.getCurrent().access(() -> {
            if (managementUIEvent == ManagementUIEvent.UNASSIGN_TARGET_TAG
                    || managementUIEvent == ManagementUIEvent.ASSIGN_TARGET_TAG) {
                refreshFilter();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_ASSIGNMENTS) {
            refreshContainer();
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
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
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), targetQF);
        targetTableContainer.getQueryView().getQueryDefinition().setMaxNestedPropertyDepth(PROPERTY_DEPT);
        return targetTableContainer;
    }

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
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER, String.class, "",
                false, true);
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

        targetTableContainer.addContainerProperty(SPUILabelDefinitions.ASSIGN_DIST_SET, DistributionSet.class, null,
                false, true);
        targetTableContainer.addContainerProperty(SPUILabelDefinitions.INSTALL_DIST_SET, DistributionSet.class, null,
                false, true);
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
        return !managementUIState.getSelectedTargetId().isPresent()
                || managementUIState.getSelectedTargetId().get().isEmpty();
    }

    @Override
    protected Object getItemIdToSelect() {
        return managementUIState.getSelectedTargetId().orElse(null);
    }

    @Override
    protected void publishEntityAfterValueChange(final Target selectedLastEntity) {
        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.SELECTED_ENTITY, selectedLastEntity));
    }

    @Override
    protected Target findEntityByTableValue(final Long lastSelectedId) {
        return targetManagement.findTargetById(lastSelectedId);
    }

    @Override
    protected void setManagementEntitiyStateValues(final Set<Long> values, final Long lastId) {
        managementUIState.setSelectedTargetId(values);
        managementUIState.setLastSelectedTargetId(lastId);
    }

    @Override
    protected ManagmentEntityState<Long> getManagmentEntityState() {
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
        return managementViewClientCriterion;
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
        final String controllerId = (String) getContainerDataSource().getItem(itemId)
                .getItemProperty(SPUILabelDefinitions.VAR_CONT_ID).getValue();
        final TargetIdName pinnedTarget = new TargetIdName((Long) itemId, controllerId);
        final StringBuilder pinBtnStyle = new StringBuilder(ValoTheme.BUTTON_BORDERLESS_COLORED);
        pinBtnStyle.append(' ');
        pinBtnStyle.append(ValoTheme.BUTTON_SMALL);
        pinBtnStyle.append(' ');
        pinBtnStyle.append(ValoTheme.BUTTON_ICON_ONLY);
        pinBtn.setStyleName(pinBtnStyle.toString());
        pinBtn.setHeightUndefined();
        pinBtn.setData(pinnedTarget);
        pinBtn.setId(UIComponentIdProvider.TARGET_PIN_ICON + controllerId);
        pinBtn.addClickListener(this::addPinClickListener);
        if (isPinned(pinnedTarget)) {
            pinBtn.addStyleName(TARGET_PINNED);
            isTargetPinned = Boolean.TRUE;
            targetPinnedBtn = pinBtn;
            eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
        }
        pinBtn.addStyleName(SPUIStyleDefinitions.TARGET_STATUS_PIN_TOGGLE);
        HawkbitCommonUtil.applyStatusLblStyle(this, pinBtn, itemId);
        return pinBtn;
    }

    private boolean isPinned(final TargetIdName pinnedTarget) {
        return managementUIState.getDistributionTableFilters().getPinnedTarget().isPresent()
                && managementUIState.getDistributionTableFilters().getPinnedTarget().get().equals(pinnedTarget);
    }

    /**
     * Add listener to pin.
     *
     * @param event
     *            as event
     */
    private void addPinClickListener(final ClickEvent event) {
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
        final TargetIdName newPinnedTargetItemId = (TargetIdName) eventBtn.getData();
        TargetIdName targetId = null;
        if (managementUIState.getDistributionTableFilters().getPinnedTarget().isPresent()) {
            targetId = managementUIState.getDistributionTableFilters().getPinnedTarget().get();
        }
        if (targetId == null) {
            isTargetPinned = !isTargetPinned;
            managementUIState.getDistributionTableFilters().setPinnedTarget(newPinnedTargetItemId);
        } else if (targetId.equals(newPinnedTargetItemId)) {
            isTargetPinned = Boolean.FALSE;
        } else {
            isTargetPinned = true;
            managementUIState.getDistributionTableFilters().setPinnedTarget(newPinnedTargetItemId);
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
        managementUIState.getDistributionTableFilters().setPinnedTarget(null);
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
        final List<Long> targetList = getDraggedTargetList(event).stream().collect(Collectors.toList());

        final String targTagName = HawkbitCommonUtil.removePrefix(event.getTransferable().getSourceComponent().getId(),
                SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        if (targetList.isEmpty()) {
            final String actionDidNotWork = i18n.get("message.action.did.not.work");
            notification.displayValidationError(actionDidNotWork);
            return;
        }

        final TargetTagAssignmentResult result = toggleTagAssignment(targetList, targTagName);

        final List<String> tagsClickedList = managementUIState.getTargetTableFilters().getClickedTargetTags();
        if (result.getUnassigned() >= 1 && !tagsClickedList.isEmpty()) {
            refreshFilter();
        }
    }

    /**
     * Toggles {@link TargetTag} assignment to given target ids by means that if
     * some (or all) of the targets in the list have the {@link Tag} not yet
     * assigned, they will be. If all of theme have the tag already assigned
     * they will be removed instead. Additionally a success popup is shown.
     *
     * @param targetIds
     *            to toggle for
     * @param targTagName
     *            to toggle
     * @return TagAssigmentResult with all meta data of the assignment outcome.
     */
    public TargetTagAssignmentResult toggleTagAssignment(final Collection<Long> targetIds, final String targTagName) {
        final List<String> controllerIds = targetManagement.findTargetAllById(targetIds).stream()
                .map(Target::getControllerId).collect(Collectors.toList());
        if (controllerIds.isEmpty()) {
            notification.displayWarning(i18n.get("targets.not.exists"));
            return new TargetTagAssignmentResult(0, 0, 0, Lists.newArrayListWithCapacity(0),
                    Lists.newArrayListWithCapacity(0), null);
        }

        final TargetTag tag = tagManagement.findTargetTag(targTagName);
        if (tag == null) {
            notification.displayWarning(i18n.get("targettag.not.exists", new Object[] { targTagName }));
            return new TargetTagAssignmentResult(0, 0, 0, Lists.newArrayListWithCapacity(0),
                    Lists.newArrayListWithCapacity(0), null);
        }

        final TargetTagAssignmentResult result = targetManagement.toggleTagAssignment(controllerIds, targTagName);

        notification.displaySuccess(HawkbitCommonUtil.createAssignmentMessage(targTagName, result, i18n));

        return result;
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
        @SuppressWarnings("unchecked")
        final AbstractTable<?, Long> source = (AbstractTable<?, Long>) transferable.getSourceComponent();
        final Set<Long> ids = source.getDeletedEntityByTransferable(transferable);
        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object targetItemId = dropData.getItemIdOver();
        LOG.debug("Adding a log to check if targetItemId is null : {} ", targetItemId);
        if (targetItemId == null) {
            getNotification().displayWarning(i18n.get("target.not.exists", new Object[] { "" }));
            return;
        }
        final Long targetId = (Long) targetItemId;
        final Target target = targetManagement.findTargetById(targetId);
        if (target == null) {
            getNotification().displayWarning(i18n.get("target.not.exists", new Object[] { "" }));
            return;
        }
        final TargetIdName createTargetIdName = new TargetIdName(target);

        final List<DistributionSet> findDistributionSetAllById = distributionSetManagement
                .findDistributionSetAllById(ids);

        if (findDistributionSetAllById.isEmpty()) {
            notification.displayWarning(i18n.get("distributionsets.not.exists"));
            return;
        }

        addNewTargetToAssignmentList(createTargetIdName, findDistributionSetAllById);
    }

    private void addNewTargetToAssignmentList(final TargetIdName createTargetIdName,
            final List<DistributionSet> findDistributionSetAllById) {
        String message = null;
        final Set<DistributionSetIdName> distributionIdNameSet = findDistributionSetAllById.stream()
                .map(distributionSet -> new DistributionSetIdName(distributionSet)).collect(Collectors.toSet());

        for (final DistributionSetIdName distributionNameId : distributionIdNameSet) {
            if (distributionNameId != null) {
                if (managementUIState.getAssignedList().keySet().contains(createTargetIdName)
                        && managementUIState.getAssignedList().get(createTargetIdName).equals(distributionNameId)) {
                    message = getPendingActionMessage(message,
                            HawkbitCommonUtil.getDistributionNameAndVersion(distributionNameId.getName(),
                                    distributionNameId.getVersion()),
                            createTargetIdName.getControllerId());
                } else {
                    managementUIState.getAssignedList().put(createTargetIdName, distributionNameId);
                }
            }
        }
        showOrHidePopupAndNotification(message);
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
     *            as ID of target
     * @return String as msg
     */
    private String getPendingActionMessage(final String message, final String distName, final String controllerId) {
        if (message == null) {
            return i18n.get("message.dist.pending.action", new Object[] { controllerId, distName });
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
            final Item item = getItem(updatedTarget.getId());
            // TO DO update SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER
            // &
            // SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER

            /*
             * Update the status which will trigger the value change lister
             * registered for the target update status. That listener will
             * update the new status icon showing for this target in the table.
             */
            item.getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS)
                    .setValue(updatedTarget.getTargetInfo().getUpdateStatus());
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

            /* Update the new Name, Description and poll date */
            item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(updatedTarget.getName());

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
            super.refreshContainer();
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

    @Override
    public void refreshContainer() {
        super.refreshContainer();
        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.REFRESH_TARGETS));
    }

    @SuppressWarnings("unchecked")
    private void updateVisibleItemOnEvent(final TargetInfo targetInfo) {
        final Target target = targetInfo.getTarget();
        final Long targetId = target.getId();

        final LazyQueryContainer targetContainer = (LazyQueryContainer) getContainerDataSource();
        final Item item = targetContainer.getItem(targetId);

        item.getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).setValue(targetInfo.getUpdateStatus());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(target.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_POLL_STATUS_TOOL_TIP)
                .setValue(HawkbitCommonUtil.getPollStatusToolTip(targetInfo.getPollStatus(), i18n));
    }

    private boolean isLastSelectedTarget(final Long targetId) {
        return managementUIState.getLastSelectedTargetId() != null
                && managementUIState.getLastSelectedTargetId().equals(targetId);
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
