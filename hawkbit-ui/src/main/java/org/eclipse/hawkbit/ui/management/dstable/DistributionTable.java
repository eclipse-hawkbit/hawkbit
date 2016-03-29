/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Distribution set table.
 */
@SpringComponent
@ViewScope
public class DistributionTable extends AbstractTable {

    private static final long serialVersionUID = -1928335256399519494L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permissionChecker;

    @Autowired
    private UINotification notification;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private ManagementViewAcceptCriteria managementViewAcceptCriteria;

    @Autowired
    private transient TargetManagement targetService;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    private String notAllowedMsg;

    private Boolean isDistPinned = false;

    private Button distributinPinnedBtn;

    @Override
    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
        notAllowedMsg = i18n.get("message.action.not.allowed");
        setNoDataAvailable();
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    /**
     * DistributionTableFilterEvent.
     * 
     * @param event
     *            as instance of {@link DistributionTableFilterEvent}
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableFilterEvent event) {
        if (event == DistributionTableFilterEvent.FILTER_BY_TEXT
                || event == DistributionTableFilterEvent.REMOVE_FILTER_BY_TEXT
                || event == DistributionTableFilterEvent.FILTER_BY_TAG) {
            UI.getCurrent().access(() -> refreshFilter());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent dragEvent) {
        if (dragEvent == DragEvent.TARGET_DRAG || dragEvent == DragEvent.TARGET_TAG_DRAG
                || dragEvent == DragEvent.DISTRIBUTION_TAG_DRAG) {
            UI.getCurrent().access(() -> addStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
        } else {
            UI.getCurrent().access(() -> removeStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent event) {
        if (event.getDistributionComponentEvent() == DistributionComponentEvent.MINIMIZED) {
            UI.getCurrent().access(() -> applyMinTableSettings());
        } else if (event.getDistributionComponentEvent() == DistributionComponentEvent.MAXIMIZED) {
            UI.getCurrent().access(() -> applyMaxTableSettings());
        } else if (event.getDistributionComponentEvent() == DistributionComponentEvent.EDIT_DISTRIBUTION) {
            UI.getCurrent().access(() -> updateDistributionInTable(event.getDistributionSet()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final PinUnpinEvent pinUnpinEvent) {
        UI.getCurrent().access(() -> {
            if (pinUnpinEvent == PinUnpinEvent.PIN_TARGET) {
                refreshFilter();
                styleDistributionTableOnPinning();
                // unstyleDistPin
                if (distributinPinnedBtn != null) {
                    distributinPinnedBtn.setStyleName(getPinStyle());
                }
            } else if (pinUnpinEvent == PinUnpinEvent.UNPIN_TARGET) {
                refreshFilter();
                restoreDistributionTableStyle();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.DELETED_DISTRIBUTIONS) {
            refreshFilter();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final ManagementUIEvent managementUIEvent) {
        UI.getCurrent().access(() -> {
            if (managementUIEvent == ManagementUIEvent.UNASSIGN_DISTRIBUTION_TAG
                    || managementUIEvent == ManagementUIEvent.ASSIGN_DISTRIBUTION_TAG) {
                refreshFilter();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.AbstractTable#getTableId()
     */
    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.DIST_TABLE_ID;
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
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<DistributionBeanQuery> distributionQF = new BeanQueryFactory<>(
                DistributionBeanQuery.class);
        distributionQF.setQueryConfiguration(queryConfiguration);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_DIST_ID_NAME),
                distributionQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<>();
        managementUIState.getDistributionTableFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));
        managementUIState.getDistributionTableFilters().getPinnedTargetId()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.ORDER_BY_PINNED_TARGET, value));
        final List<String> list = new ArrayList<>();
        queryConfig.put(SPUIDefinitions.FILTER_BY_NO_TAG,
                managementUIState.getDistributionTableFilters().isNoTagSelected());
        if (!managementUIState.getDistributionTableFilters().getDistSetTags().isEmpty()) {
            list.addAll(managementUIState.getDistributionTableFilters().getDistSetTags());
        }
        queryConfig.put(SPUIDefinitions.FILTER_BY_TAG, list);
        return queryConfig;
    }

    @Override
    protected void addContainerProperties(final Container container) {
        HawkbitCommonUtil.getDsTableColumnProperties(container);
    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUILabelDefinitions.PIN_COLUMN, new Table.ColumnGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                return getPinButton(itemId);
            }
        });
    }

    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return !managementUIState.getSelectedDsIdName().isPresent()
                || managementUIState.getSelectedDsIdName().get().isEmpty();
    }

    @Override
    protected Object getItemIdToSelect() {
        if (managementUIState.getSelectedDsIdName().isPresent()) {
            return managementUIState.getSelectedDsIdName().get();
        }
        return null;
    }

    @Override
    protected void onValueChange() {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        final Set<DistributionSetIdName> values = HawkbitCommonUtil.getSelectedDSDetails(this);
        DistributionSetIdName value = null;
        if (values != null && !values.isEmpty()) {
            final Iterator<DistributionSetIdName> iterator = values.iterator();

            while (iterator.hasNext()) {
                value = iterator.next();
            }

            if (null != value) {
                managementUIState.setSelectedDsIdName(values);
                managementUIState.setLastSelectedDsIdName(value);
                final DistributionSet lastSelectedDistSet = distributionSetManagement
                        .findDistributionSetByIdWithDetails(value.getId());
                eventBus.publish(this,
                        new DistributionTableEvent(DistributionComponentEvent.ON_VALUE_CHANGE, lastSelectedDistSet));
            }
        } else {
            managementUIState.setSelectedDsIdName(null);
            managementUIState.setLastSelectedDsIdName(null);
            eventBus.publish(this, new DistributionTableEvent(DistributionComponentEvent.ON_VALUE_CHANGE, null));
        }

    }

    @Override
    protected boolean isMaximized() {
        return managementUIState.isDsTableMaximized();
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        return HawkbitCommonUtil.getTableVisibleColumns(isMaximized(), true, i18n);
    }

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
                if (doValidation(event)) {
                    if (event.getTransferable().getSourceComponent() instanceof Table) {
                        assignTargetToDs(event);
                    } else if (event.getTransferable().getSourceComponent() instanceof DragAndDropWrapper) {
                        processWrapperDrop(event);
                    }
                }
            }
        };
    }

    private void processWrapperDrop(final DragAndDropEvent event) {
        if (event.getTransferable().getSourceComponent().getId()
                .startsWith(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS)) {
            assignDsTag(event);
        } else {
            assignTargetTag(event);
        }

    }

    private void assignDsTag(final DragAndDropEvent event) {
        final com.vaadin.event.dd.TargetDetails taregtDet = event.getTargetDetails();
        final Table distTable = (Table) taregtDet.getTarget();
        final Set<DistributionSetIdName> distsSelected = HawkbitCommonUtil.getSelectedDSDetails(distTable);
        final Set<Long> distList = new HashSet<>();

        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object distItemId = dropData.getItemIdOver();

        if (!distsSelected.contains(distItemId)) {
            distList.add(((DistributionSetIdName) distItemId).getId());
        } else {
            distList.addAll(distsSelected.stream().map(t -> t.getId()).collect(Collectors.toList()));
        }

        final String distTagName = HawkbitCommonUtil.removePrefix(event.getTransferable().getSourceComponent().getId(),
                SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS);

        final DistributionSetTagAssignmentResult result = distributionSetManagement.toggleTagAssignment(distList,
                distTagName);

        notification.displaySuccess(HawkbitCommonUtil.getDistributionTagAssignmentMsg(distTagName, result, i18n));
        if (result.getAssigned() >= 1 && managementUIState.getDistributionTableFilters().isNoTagSelected()) {
            refreshFilter();
        }
    }

    private void assignTargetTag(final DragAndDropEvent event) {
        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object distItemId = dropData.getItemIdOver();
        final String targetTagName = HawkbitCommonUtil.removePrefix(
                event.getTransferable().getSourceComponent().getId(), SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        // get all the targets assigned to the tag
        // assign dist to those targets
        final List<Target> assignedTargets = targetService.findTargetsByTag(targetTagName);
        if (!assignedTargets.isEmpty()) {
            final Set<TargetIdName> targetDetailsList = new HashSet<>();
            assignedTargets.forEach(target -> targetDetailsList
                    .add(new TargetIdName(target.getId(), target.getControllerId(), target.getName())));
            assignTargetToDs(getItem(distItemId), targetDetailsList);
        } else {
            notification.displaySuccess(i18n.get("message.no.targets.assiged.fortag", new Object[] { targetTagName }));
        }
    }

    private void assignTargetToDs(final DragAndDropEvent event) {
        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        final Table source = transferable.getSourceComponent();
        final Set<TargetIdName> targetsSelected = HawkbitCommonUtil.getSelectedTargetDetails(source);
        final Set<TargetIdName> targetDetailsList = new HashSet<>();

        if (!targetsSelected.contains(transferable.getData("itemId"))) {
            targetDetailsList.add((TargetIdName) transferable.getData("itemId"));
        } else {
            targetDetailsList.addAll(targetsSelected);
        }

        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();

        final Object distItemId = dropData.getItemIdOver();
        assignTargetToDs(getItem(distItemId), targetDetailsList);

    }

    private void assignTargetToDs(final Item item, final Set<TargetIdName> targetDetailsList) {
        if (item != null && item.getItemProperty("id") != null && item.getItemProperty("name") != null) {
            final Long distId = (Long) item.getItemProperty("id").getValue();
            final String distName = (String) item.getItemProperty("name").getValue();
            final String distVersion = (String) item.getItemProperty("version").getValue();
            final DistributionSetIdName distributionSetIdName = new DistributionSetIdName(distId, distName,
                    distVersion);
            showOrHidePopupAndNotification(validate(targetDetailsList, distributionSetIdName));
        }
    }

    private Boolean doValidation(final DragAndDropEvent dragEvent) {
        final Component compsource = dragEvent.getTransferable().getSourceComponent();
        if (compsource instanceof Table) {
            return validateTable(compsource);
        } else if (compsource instanceof DragAndDropWrapper) {
            return validateDragAndDropWrapper(compsource);
        } else {
            notification.displayValidationError(notAllowedMsg);
            return false;
        }
    }

    private Boolean validateTable(final Component compsource) {
        if (!permissionChecker.hasUpdateTargetPermission()) {
            notification.displayValidationError(i18n.get("message.permission.insufficient"));
            return false;
        } else {
            if (compsource instanceof Table && !compsource.getId().equals(SPUIComponetIdProvider.TARGET_TABLE_ID)) {
                notification.displayValidationError(notAllowedMsg);
                return false;
            }
        }
        return true;
    }

    private Boolean validateDragAndDropWrapper(final Component compsource) {
        final DragAndDropWrapper wrapperSource = (DragAndDropWrapper) compsource;
        final String tagData = wrapperSource.getData().toString();
        if (wrapperSource.getId().startsWith(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS)) {
            return !isNoTagButton(tagData, SPUIDefinitions.DISTRIBUTION_TAG_BUTTON);
        } else if (wrapperSource.getId().startsWith(SPUIDefinitions.TARGET_TAG_ID_PREFIXS)) {
            return !isNoTagButton(tagData, SPUIDefinitions.TARGET_TAG_BUTTON);
        } else {
            notification.displayValidationError(notAllowedMsg);
            return false;
        }
    }

    private Boolean isNoTagButton(final String tagData, final String targetNoTagData) {
        if (tagData.equals(targetNoTagData)) {
            notification.displayValidationError(
                    i18n.get("message.tag.cannot.be.assigned", new Object[] { i18n.get("label.no.tag.assigned") }));
            return true;
        }
        return false;
    }

    private String validate(final Set<TargetIdName> targetDetailsList,
            final DistributionSetIdName distributionSetIdName) {
        String pendingActionMessage = null;
        for (final TargetIdName trgtNameId : targetDetailsList) {
            if (null != trgtNameId) {
                if (managementUIState.getAssignedList().keySet().contains(trgtNameId)
                        && managementUIState.getAssignedList().get(trgtNameId).equals(distributionSetIdName)) {
                    pendingActionMessage = getPendingActionMessage(pendingActionMessage, trgtNameId.getControllerId(),
                            HawkbitCommonUtil.getDistributionNameAndVersion(distributionSetIdName.getName(),
                                    distributionSetIdName.getVersion()));
                } else {
                    managementUIState.getAssignedList().put(trgtNameId, distributionSetIdName);
                }
            }
        }
        return pendingActionMessage;
    }

    private String getPendingActionMessage(final String message, final String targId, final String distNameVersion) {
        String pendActionMsg = i18n.get("message.target.assigned.pending");
        if (null == message) {
            pendActionMsg = i18n.get("message.dist.pending.action", new Object[] { targId, distNameVersion });
        }
        return pendActionMsg;
    }

    private void showOrHidePopupAndNotification(final String message) {
        if (null != managementUIState.getAssignedList() && !managementUIState.getAssignedList().isEmpty()) {
            eventBus.publish(this, ManagementUIEvent.UPDATE_COUNT);
        }
        if (null != message) {
            notification.displayValidationError(message);
        }
    }

    private void updateDistributionInTable(final DistributionSet editedDs) {
        final Item item = getContainerDataSource()
                .getItem(new DistributionSetIdName(editedDs.getId(), editedDs.getName(), editedDs.getVersion()));
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(editedDs.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).setValue(editedDs.getVersion());
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_BY)
                .setValue(HawkbitCommonUtil.getIMUser(editedDs.getCreatedBy()));
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(editedDs.getCreatedAt()));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY)
                .setValue(HawkbitCommonUtil.getIMUser(editedDs.getLastModifiedBy()));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(editedDs.getLastModifiedAt()));
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(editedDs.getDescription());
    }

    private void restoreDistributionTableStyle() {
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return null;
            }
        });

    }

    private void styleDistributionTableOnPinning() {
        final Target targetObj = targetService.findTargetByControllerIDWithDetails(
                managementUIState.getDistributionTableFilters().getPinnedTargetId().get());

        if (targetObj != null) {
            final DistributionSet assignedDistribution = targetObj.getAssignedDistributionSet();
            final DistributionSet installedDistribution = targetObj.getTargetInfo().getInstalledDistributionSet();
            Long installedDistId = null;
            Long assignedDistId = null;
            if (null != installedDistribution) {
                installedDistId = installedDistribution.getId();
            }
            if (null != assignedDistribution) {
                assignedDistId = assignedDistribution.getId();
            }
            styleDistributionSetTable(installedDistId, assignedDistId);
        }
    }

    private String getPinnedDistributionStyle(final Long installedDistItemIds, final Long assignedDistTableItemIds,
            final Object itemId) {
        final Long distId = ((DistributionSetIdName) itemId).getId();
        if (distId != null && distId.equals(installedDistItemIds)) {
            return SPUIDefinitions.HIGHTLIGHT_GREEN;

        } else if (distId != null && distId.equals(assignedDistTableItemIds)) {
            return SPUIDefinitions.HIGHTLIGHT_ORANGE;
        } else {
            return null;
        }
    }

    private Object getPinButton(final Object itemId) {
        final DistributionSetIdName dist = (DistributionSetIdName) getContainerDataSource().getItem(itemId)
                .getItemProperty(SPUILabelDefinitions.VAR_DIST_ID_NAME).getValue();
        final Button pinBtn = getPinBtn(itemId, dist.getName(), dist.getVersion());
        saveDistributionPinnedBtn(pinBtn);
        pinBtn.addClickListener(this::addPinClickListener);
        rePinDistribution(pinBtn, dist.getId());
        return pinBtn;
    }

    private void saveDistributionPinnedBtn(final Button pinBtn) {
        if (managementUIState.getTargetTableFilters().getPinnedDistId().isPresent()
                && managementUIState.getTargetTableFilters().getPinnedDistId().get()
                        .equals(((DistributionSetIdName) pinBtn.getData()).getId())) {
            setDistributinPinnedBtn(pinBtn);
        }
    }

    private void addPinClickListener(final ClickEvent event) {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        checkifAlreadyPinned(event.getButton());
        if (isDistPinned) {
            pinDitribution(event.getButton());
        } else {
            unPinDistribution(event.getButton());
        }

    }

    private void checkifAlreadyPinned(final Button eventBtn) {
        final Long newPinnedDistItemId = ((DistributionSetIdName) eventBtn.getData()).getId();
        Long pinnedDistId = null;
        if (managementUIState.getTargetTableFilters().getPinnedDistId().isPresent()) {
            pinnedDistId = managementUIState.getTargetTableFilters().getPinnedDistId().get();
        }
        if (pinnedDistId == null) {
            isDistPinned = !isDistPinned;
            managementUIState.getTargetTableFilters().setPinnedDistId(newPinnedDistItemId);
        } else if (newPinnedDistItemId.equals(pinnedDistId)) {
            isDistPinned = Boolean.FALSE;
        } else {
            isDistPinned = true;
            managementUIState.getTargetTableFilters().setPinnedDistId(newPinnedDistItemId);
            distributinPinnedBtn.setStyleName(getPinStyle());
        }

        distributinPinnedBtn = eventBtn;
    }

    private void unPinDistribution(final Button eventBtn) {
        managementUIState.getTargetTableFilters().setPinnedDistId(null);
        eventBus.publish(this, PinUnpinEvent.UNPIN_DISTRIBUTION);
        resetPinStyle(eventBtn);
    }

    private void resetPinStyle(final Button pinBtn) {
        pinBtn.setStyleName(getPinStyle());
    }

    private void pinDitribution(final Button eventBtn) {

        /* if distribution set is pinned ,unpin target if pinned */
        managementUIState.getDistributionTableFilters().setPinnedTargetId(null);
        /* Dist table restyle */
        eventBus.publish(this, PinUnpinEvent.PIN_DISTRIBUTION);
        applyPinStyle(eventBtn);
        styleDistributionSetTable();
        isDistPinned = Boolean.FALSE;
    }

    private void rePinDistribution(final Button pinBtn, final Long distID) {
        if (managementUIState.getTargetTableFilters().getPinnedDistId().isPresent()
                && distID.equals(managementUIState.getTargetTableFilters().getPinnedDistId().get())) {
            applyPinStyle(pinBtn);
            isDistPinned = Boolean.TRUE;
            distributinPinnedBtn = pinBtn;
            eventBus.publish(this, PinUnpinEvent.PIN_DISTRIBUTION);
        }
    }

    private void styleDistributionSetTable() {
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return null;
            }
        });
    }

    private void applyPinStyle(final Button eventBtn) {
        final StringBuilder style = new StringBuilder(SPUIComponentProvider.getPinButtonStyle());
        style.append(' ').append(SPUIStyleDefinitions.DIST_PIN).append(' ').append("tablePin").append(' ')
                .append("pin-icon-red");
        eventBtn.setStyleName(style.toString());
    }

    private String getPinButtonId(final String distName, final String distVersion) {
        final StringBuilder pinBtnId = new StringBuilder(SPUIComponetIdProvider.DIST_PIN_BUTTON);
        pinBtnId.append('.');
        pinBtnId.append(distName);
        pinBtnId.append('.');
        pinBtnId.append(distVersion);
        return pinBtnId.toString();
    }

    private Button getPinBtn(final Object itemId, final String distName, final String distVersion) {
        final Button pinBtn = new Button();
        pinBtn.setIcon(FontAwesome.THUMB_TACK);
        pinBtn.setHeightUndefined();
        pinBtn.addStyleName(getPinStyle());
        pinBtn.setData(itemId);
        pinBtn.setId(getPinButtonId(distName, distVersion));
        pinBtn.setImmediate(true);
        return pinBtn;
    }

    private String getPinStyle() {
        final StringBuilder pinBtnStyle = new StringBuilder(SPUIComponentProvider.getPinButtonStyle());
        pinBtnStyle.append(' ');
        pinBtnStyle.append(SPUIStyleDefinitions.DIST_PIN);
        pinBtnStyle.append(' ');
        pinBtnStyle.append(SPUIStyleDefinitions.DIST_PIN_BLUE);
        return pinBtnStyle.toString();
    }

    /**
     * Added by Saumya Target pin listener.
     *
     * @param installedDistItemIds
     *            Item ids of installed distribution set
     * @param assignedDistTableItemIds
     *            Item ids of assigned distribution set
     */
    public void styleDistributionSetTable(final Long installedDistItemId, final Long assignedDistTableItemId) {
        setCellStyleGenerator((source, itemId, propertyId) -> getPinnedDistributionStyle(installedDistItemId,
                assignedDistTableItemId, itemId));
    }

    public void setDistributinPinnedBtn(final Button distributinPinnedBtn) {
        this.distributinPinnedBtn = distributinPinnedBtn;
    }

    private void setNoDataAvailable() {
        final int size = getContainerDataSource().size();
        if (size == 0) {
            managementUIState.setNoDataAvailableDistribution(true);
        } else {
            managementUIState.setNoDataAvailableDistribution(false);
        }
    }
}
