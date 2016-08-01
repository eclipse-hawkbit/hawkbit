/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetUpdateEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsViewAcceptCriteria;
import org.eclipse.hawkbit.ui.distributions.event.DragEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
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
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Distribution set table.
 */
@SpringComponent
@ViewScope
public class DistributionSetTable extends AbstractNamedVersionTable<DistributionSet, DistributionSetIdName> {

    private static final long serialVersionUID = -7731776093470487988L;

    private static final Logger LOG = LoggerFactory.getLogger(DistributionSetTable.class);

    private static final List<Object> DISPLAY_DROP_HINT_EVENTS = new ArrayList<>(
            Arrays.asList(DragEvent.SOFTWAREMODULE_DRAG));

    @Autowired
    private SpPermissionChecker permissionChecker;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private DistributionsViewAcceptCriteria distributionsViewAcceptCriteria;

    @Autowired
    private UINotification notification;

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private DsMetadataPopupLayout dsMetadataPopupLayout;

    /**
     * Initialize the component.
     */
    @Override
    protected void init() {
        super.init();
        addTableStyleGenerator();
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent event) {
        if (event == DragEvent.HIDE_DROP_HINT) {
            UI.getCurrent().access(() -> removeStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
        } else if (DISPLAY_DROP_HINT_EVENTS.contains(event)) {
            UI.getCurrent().access(() -> addStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvents(final DistributionSetUpdateEvent event) {
        final DistributionSet ds = event.getEntity();
        final DistributionSetIdName lastSelectedDsIdName = manageDistUIState.getLastSelectedDistribution().isPresent()
                ? manageDistUIState.getLastSelectedDistribution().get() : null;
        final List<DistributionSetIdName> visibleItemIds = (List<DistributionSetIdName>) getVisibleItemIds();

        // refresh the details tabs only if selected ds is updated
        if (lastSelectedDsIdName != null && lastSelectedDsIdName.getId().equals(ds.getId())) {
            // update table row+details layout
            eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.UPDATED_ENTITY, ds));
        } else if (visibleItemIds.stream().filter(e -> e.getId().equals(ds.getId())).findFirst().isPresent()) {
            // update the name/version details visible in table
            UI.getCurrent().access(() -> updateDistributionInTable(event.getEntity()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvents(final List<?> events) {
        final Object firstEvent = events.get(0);
        if (DistributionCreatedEvent.class.isInstance(firstEvent)) {
            refreshDistributions();
        } else if (DistributionDeletedEvent.class.isInstance(firstEvent)) {
            onDistributionDeleteEvent((List<DistributionDeletedEvent>) events);
        }
    }

    @Override
    protected String getTableId() {
        return SPUIComponentIdProvider.DIST_TABLE_ID;
    }

    @Override
    protected Container createContainer() {

        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();
        final BeanQueryFactory<ManageDistBeanQuery> distributionQF = new BeanQueryFactory<>(ManageDistBeanQuery.class);

        distributionQF.setQueryConfiguration(queryConfiguration);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_DIST_ID_NAME),
                distributionQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<>();
        manageDistUIState.getManageDistFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));

        if (null != manageDistUIState.getManageDistFilters().getClickedDistSetType()) {
            queryConfig.put(SPUIDefinitions.FILTER_BY_DISTRIBUTION_SET_TYPE,
                    manageDistUIState.getManageDistFilters().getClickedDistSetType());
        }

        return queryConfig;
    }

    @Override
    protected void addContainerProperties(final Container container) {
        HawkbitCommonUtil.getDsTableColumnProperties(container);
        ((LazyQueryContainer) container).addContainerProperty(SPUILabelDefinitions.VAR_IS_DISTRIBUTION_COMPLETE,
                Boolean.class, null, false, true);
    }

    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return !manageDistUIState.getSelectedDistributions().isPresent()
                || manageDistUIState.getSelectedDistributions().get().isEmpty();
    }

    @Override
    protected Object getItemIdToSelect() {
        if (manageDistUIState.getSelectedDistributions().isPresent()) {
            return manageDistUIState.getSelectedDistributions().get();
        }
        return null;
    }

    @Override
    protected DistributionSet findEntityByTableValue(final DistributionSetIdName entityTableId) {
        return distributionSetManagement.findDistributionSetByIdWithDetails(entityTableId.getId());
    }

    @Override
    protected ManageDistUIState getManagmentEntityState() {
        return manageDistUIState;
    }

    @Override
    protected void publishEntityAfterValueChange(final DistributionSet distributionSet) {
        eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.SELECTED_ENTITY, distributionSet));
        eventBus.publish(this, DistributionsUIEvent.ORDER_BY_DISTRIBUTION);
        if (distributionSet != null) {
            manageDistUIState.setLastSelectedEntity(new DistributionSetIdName(distributionSet.getId(),
                    distributionSet.getName(), distributionSet.getVersion()));
        }
    }

    @Override
    protected boolean isMaximized() {
        return manageDistUIState.isDsTableMaximized();
    }

    @Override
    protected DropHandler getTableDropHandler() {
        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return distributionsViewAcceptCriteria;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                if (doValidation(event)) {
                    onDrop(event);
                }
            }
        };
    }

    private void onDrop(final DragAndDropEvent event) {
        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        @SuppressWarnings("unchecked")
        final AbstractTable<?, Long> source = (AbstractTable<SoftwareModule, Long>) transferable.getSourceComponent();
        final Set<Long> softwareModulesIdList = source.getDeletedEntityByTransferable(transferable);

        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();

        final Object distItemId = dropData.getItemIdOver();
        final Item item = getItem(distItemId);
        if (item != null && item.getItemProperty("id") != null && item.getItemProperty("name") != null) {
            handleDropEvent(source, softwareModulesIdList, item);
        }
    }

    private void handleDropEvent(final Table source, final Set<Long> softwareModulesIdList, final Item item) {
        final Long distId = (Long) item.getItemProperty("id").getValue();
        final String distName = (String) item.getItemProperty("name").getValue();
        final String distVersion = (String) item.getItemProperty("version").getValue();
        final DistributionSetIdName distributionSetIdName = new DistributionSetIdName(distId, distName, distVersion);

        final HashMap<Long, HashSet<SoftwareModuleIdName>> map;
        if (manageDistUIState.getConsolidatedDistSoftwarewList().containsKey(distributionSetIdName)) {
            map = manageDistUIState.getConsolidatedDistSoftwarewList().get(distributionSetIdName);
        } else {
            map = new HashMap<>();
            manageDistUIState.getConsolidatedDistSoftwarewList().put(distributionSetIdName, map);
        }

        for (final Long softwareModuleId : softwareModulesIdList) {
            final Item softwareItem = source.getContainerDataSource().getItem(softwareModuleId);
            final String name = (String) softwareItem.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
            final String swVersion = (String) softwareItem.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();

            final SoftwareModule softwareModule = softwareManagement.findSoftwareModuleById(softwareModuleId);
            if (validSoftwareModule(distId, softwareModule)) {
                final SoftwareModuleIdName softwareModuleIdName = new SoftwareModuleIdName(softwareModuleId,
                        name.concat(":" + swVersion));
                publishAssignEvent(distId, softwareModule);
                /*
                 * If software module type is software, means multiple softwares
                 * can assigned to that type. Hence if multipe softwares belongs
                 * to same type is drroped, then add to the list.
                 */
                handleSoftwareCase(map, softwareModule, softwareModuleIdName);

                /*
                 * If software module type is firmware, means single software
                 * can be assigned to that type. Hence if multiple softwares
                 * belongs to same type is dropped, then override with previous
                 * one.
                 */
                handleFirmwareCase(map, softwareModule, softwareModuleIdName);
            } else {
                return;
            }
        }

        // hashset is seriablizable
        final HashSet<SoftwareModuleIdName> softwareModules = new HashSet<>();
        map.keySet().forEach(typeId -> softwareModules.addAll(map.get(typeId)));

        updateDropedDetails(distributionSetIdName, softwareModules);
    }

    private void publishAssignEvent(final Long distId, final SoftwareModule softwareModule) {
        if (manageDistUIState.getLastSelectedDistribution().isPresent()
                && manageDistUIState.getLastSelectedDistribution().get().getId().equals(distId)) {
            eventBus.publish(this,
                    new SoftwareModuleEvent(SoftwareModuleEventType.ASSIGN_SOFTWARE_MODULE, softwareModule));
        }
    }

    private void handleFirmwareCase(final Map<Long, HashSet<SoftwareModuleIdName>> map,
            final SoftwareModule softwareModule, final SoftwareModuleIdName softwareModuleIdName) {
        if (softwareModule.getType().getMaxAssignments() == 1) {
            if (!map.containsKey(softwareModule.getType().getId())) {
                map.put(softwareModule.getType().getId(), new HashSet<SoftwareModuleIdName>());

            }
            map.get(softwareModule.getType().getId()).clear();
            map.get(softwareModule.getType().getId()).add(softwareModuleIdName);

        }
    }

    private void handleSoftwareCase(final Map<Long, HashSet<SoftwareModuleIdName>> map,
            final SoftwareModule softwareModule, final SoftwareModuleIdName softwareModuleIdName) {
        if (softwareModule.getType().getMaxAssignments() > 1) {
            if (!map.containsKey(softwareModule.getType().getId())) {
                map.put(softwareModule.getType().getId(), new HashSet<SoftwareModuleIdName>());
            }
            map.get(softwareModule.getType().getId()).add(softwareModuleIdName);
        }
    }

    private void updateDropedDetails(final DistributionSetIdName distributionSetIdName,
            final HashSet<SoftwareModuleIdName> softwareModules) {
        LOG.debug("Adding a log to check if distributionSetIdName is null : {} ", distributionSetIdName);
        manageDistUIState.getAssignedList().put(distributionSetIdName, softwareModules);

        eventBus.publish(this, DistributionsUIEvent.UPDATE_COUNT);
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
    }

    private boolean validSoftwareModule(final Long distId, final SoftwareModule sm) {
        if (!isSoftwareModuleDragged(distId, sm)) {
            return false;
        }
        final DistributionSet ds = distributionSetManagement.findDistributionSetByIdWithDetails(distId);
        if (!validateSoftwareModule(sm, ds)) {
            return false;
        }

        if (distributionSetManagement.isDistributionSetInUse(ds)) {
            notification.displayValidationError(
                    String.format("Distribution set %s:%s is already assigned to targets and cannot be changed",
                            ds.getName(), ds.getVersion()));
            return false;
        }
        return true;
    }

    private boolean validateSoftwareModule(final SoftwareModule sm, final DistributionSet ds) {
        if (targetManagement.countTargetByFilters(null, null, ds.getId(), Boolean.FALSE, new String[] {}) > 0) {
            /* Distribution is already assigned */
            notification.displayValidationError(i18n.get("message.dist.inuse",
                    HawkbitCommonUtil.concatStrings(":", ds.getName(), ds.getVersion())));
            return false;
        }

        if (ds.getModules().contains(sm)) {
            /* Already has software module */
            notification.displayValidationError(i18n.get("message.software.dist.already.assigned",
                    HawkbitCommonUtil.concatStrings(":", ds.getName(), ds.getVersion()),
                    HawkbitCommonUtil.concatStrings(":", sm.getName(), sm.getVersion())));
            return false;
        }

        if (!ds.getType().containsModuleType(sm.getType())) {
            /* Invalid type of the software module */
            notification.displayValidationError(i18n.get("message.software.dist.type.notallowed",
                    HawkbitCommonUtil.concatStrings(":", sm.getName(), sm.getVersion()),
                    HawkbitCommonUtil.concatStrings(":", ds.getName(), ds.getVersion())));
            return false;
        }
        return true;
    }

    private boolean isSoftwareModuleDragged(final Long distId, final SoftwareModule sm) {
        for (final Entry<DistributionSetIdName, HashSet<SoftwareModuleIdName>> entry : manageDistUIState
                .getAssignedList().entrySet()) {
            if (!distId.equals(entry.getKey().getId())) {
                continue;
            }
            final Set<SoftwareModuleIdName> swModuleIdNames = entry.getValue();
            for (final SoftwareModuleIdName swModuleIdName : swModuleIdNames) {
                if ((sm.getName().concat(":" + sm.getVersion())).equals(swModuleIdName.getName())) {
                    notification.displayValidationError(i18n.get("message.software.already.dragged",
                            HawkbitCommonUtil.concatStrings(":", sm.getName(), sm.getVersion())));
                    return false;
                }
            }

        }
        return true;
    }

    /**
     * Validate event.
     *
     * @param dragEvent
     *            as event
     * @return boolean as flag
     */
    private Boolean doValidation(final DragAndDropEvent dragEvent) {
        if (!permissionChecker.hasUpdateDistributionPermission()) {
            notification.displayValidationError(i18n.get("message.permission.insufficient"));
            return false;
        } else {
            final Component compsource = dragEvent.getTransferable().getSourceComponent();
            final Table source = (Table) compsource;
            if (compsource instanceof Table) {
                if (!source.getId().equals(SPUIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE)) {
                    notification.displayValidationError(i18n.get("message.action.not.allowed"));
                    return false;
                }
            } else {
                notification.displayValidationError(i18n.get("message.action.not.allowed"));
                return false;
            }
        }
        return true;
    }

    private void addTableStyleGenerator() {
        setCellStyleGenerator((source, itemId, propertyId) -> {
            if (propertyId == null) {
                // Styling for row
                final Item item = getItem(itemId);
                final Boolean isComplete = (Boolean) item
                        .getItemProperty(SPUILabelDefinitions.VAR_IS_DISTRIBUTION_COMPLETE).getValue();
                if (!isComplete) {
                    return SPUIDefinitions.DISABLE_DISTRIBUTION;
                }
                return null;
            } else {
                return null;
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent event) {
        onBaseEntityEvent(event);
        if (BaseEntityEventType.UPDATED_ENTITY != event.getEventType()) {
            return;
        }
        UI.getCurrent().access(() -> updateDistributionInTable(event.getEntity()));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.SAVED_ASSIGNMENTS) {
            UI.getCurrent().access(() -> refreshFilter());
        }
    }

    /**
     * DistributionTableFilterEvent.
     *
     * @param event
     *            as instance of {@link DistributionTableFilterEvent}
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final DistributionTableFilterEvent event) {
        if (event == DistributionTableFilterEvent.FILTER_BY_TEXT
                || event == DistributionTableFilterEvent.REMOVE_FILTER_BY_TEXT
                || event == DistributionTableFilterEvent.FILTER_BY_TAG) {
            UI.getCurrent().access(() -> refreshFilter());
        }
    }

    @Override
    protected Item addEntity(final DistributionSet baseEntity) {
        final Item item = super.addEntity(baseEntity);
        if (manageDistUIState.getSelectedDistributions().isPresent()) {
            manageDistUIState.getSelectedDistributions().get().stream().forEach(this::unselect);
        }
        select(DistributionSetIdName.generate(baseEntity));
        return item;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void updateEntity(final DistributionSet baseEntity, final Item item) {
        item.getItemProperty(SPUILabelDefinitions.DIST_ID).setValue(baseEntity.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_IS_DISTRIBUTION_COMPLETE).setValue(baseEntity.isComplete());
        super.updateEntity(baseEntity, item);
    }

    @Override
    protected void setDataAvailable(final boolean available) {
        manageDistUIState.setNoDataAvailableDist(!available);

    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUILabelDefinitions.METADATA_ICON, new ColumnGenerator() {
            private static final long serialVersionUID = 117186282275065399L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                final String nameVersionStr = getNameAndVerion(itemId);
                final Button manageMetaDataBtn = createManageMetadataButton(nameVersionStr);
                manageMetaDataBtn
                        .addClickListener(event -> showMetadataDetails(((DistributionSetIdName) itemId).getId()));
                return manageMetaDataBtn;
            }
        });
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        if (!isMaximized()) {
            columnList.add(new TableColumn(SPUILabelDefinitions.METADATA_ICON, "", 0.1F));
        }
        return columnList;
    }

    private Button createManageMetadataButton(final String nameVersionStr) {
        final Button manageMetadataBtn = SPUIComponentProvider.getButton(
                SPUIComponentIdProvider.DS_TABLE_MANAGE_METADATA_ID + "." + nameVersionStr, "", "", null, false,
                FontAwesome.LIST_ALT, SPUIButtonStyleSmallNoBorder.class);
        manageMetadataBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        manageMetadataBtn.setDescription(i18n.get("tooltip.metadata.icon"));
        return manageMetadataBtn;
    }

    private void showMetadataDetails(final Long itemId) {
        final DistributionSet ds = distributionSetManagement.findDistributionSetByIdWithDetails(itemId);
        UI.getCurrent().addWindow(dsMetadataPopupLayout.getWindow(ds, null));
    }

    private String getNameAndVerion(final Object itemId) {
        final Item item = getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        return name + "." + version;
    }

    private void refreshDistributions() {
        final LazyQueryContainer dsContainer = (LazyQueryContainer) getContainerDataSource();
        final int size = dsContainer.size();
        if (size < SPUIDefinitions.MAX_TABLE_ENTRIES) {
            refreshTablecontainer();
        }
        if (size != 0) {
            setData(SPUIDefinitions.DATA_AVAILABLE);
        }
    }

    private void refreshTablecontainer() {
        final LazyQueryContainer dsContainer = (LazyQueryContainer) getContainerDataSource();
        dsContainer.refresh();
        selectRow();
    }

    private void updateDistributionInTable(final DistributionSet editedDs) {
        final Item item = getContainerDataSource()
                .getItem(new DistributionSetIdName(editedDs.getId(), editedDs.getName(), editedDs.getVersion()));
        updateEntity(editedDs, item);
    }

    private void onDistributionDeleteEvent(final List<DistributionDeletedEvent> events) {
        final LazyQueryContainer dsContainer = (LazyQueryContainer) getContainerDataSource();
        final List<Object> visibleItemIds = (List<Object>) getVisibleItemIds();
        boolean shouldRefreshDs = false;
        for (final DistributionDeletedEvent deletedEvent : events) {
            final Long distributionSetId = deletedEvent.getDistributionSetId();
            final DistributionSetIdName targetIdName = new DistributionSetIdName(distributionSetId, null, null);
            if (visibleItemIds.contains(targetIdName)) {
                dsContainer.removeItem(targetIdName);
            } else {
                shouldRefreshDs = true;
            }
        }

        if (shouldRefreshDs) {
            refreshOnDelete();
        } else {
            dsContainer.commit();
        }
        reSelectItemsAfterDeletionEvent();
    }

    private void refreshOnDelete() {
        final LazyQueryContainer dsContainer = (LazyQueryContainer) getContainerDataSource();
        final int size = dsContainer.size();
        refreshTablecontainer();
        if (size != 0) {
            setData(SPUIDefinitions.DATA_AVAILABLE);
        }
    }

    private void reSelectItemsAfterDeletionEvent() {
        Set<Object> values = new HashSet<>();
        if (isMultiSelect()) {
            values = new HashSet<>((Set<?>) getValue());
        } else {
            values.add(getValue());
        }
        setValue(null);

        for (final Object value : values) {
            if (getVisibleItemIds().contains(value)) {
                select(value);
            }
        }
    }
}
