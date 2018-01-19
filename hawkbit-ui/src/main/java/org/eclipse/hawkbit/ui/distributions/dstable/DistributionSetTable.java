/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.RefreshDistributionTableByFilterEvent;
import org.eclipse.hawkbit.ui.push.DistributionSetUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.eclipse.hawkbit.ui.view.filter.OnlyEventsFromDistributionsViewFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Distribution set table.
 */
public class DistributionSetTable extends AbstractNamedVersionTable<DistributionSet> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DistributionSetTable.class);

    private final SpPermissionChecker permissionChecker;

    private final ManageDistUIState manageDistUIState;

    private final transient DistributionSetManagement distributionSetManagement;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final DistributionsViewClientCriterion distributionsViewClientCriterion;

    private final transient TargetManagement targetManagement;

    private final DsMetadataPopupLayout dsMetadataPopupLayout;

    DistributionSetTable(final UIEventBus eventBus, final VaadinMessageSource i18n, final UINotification notification,
            final SpPermissionChecker permissionChecker, final ManageDistUIState manageDistUIState,
            final DistributionSetManagement distributionSetManagement,
            final SoftwareModuleManagement softwareManagement,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final TargetManagement targetManagement, final DsMetadataPopupLayout dsMetadataPopupLayout) {
        super(eventBus, i18n, notification);
        this.permissionChecker = permissionChecker;
        this.manageDistUIState = manageDistUIState;
        this.distributionSetManagement = distributionSetManagement;
        this.softwareModuleManagement = softwareManagement;
        this.distributionsViewClientCriterion = distributionsViewClientCriterion;
        this.targetManagement = targetManagement;
        this.dsMetadataPopupLayout = dsMetadataPopupLayout;
        addTableStyleGenerator();
        addNewContainerDS();
        setColumnProperties();
        setDataAvailable(getContainerDataSource().size() != 0);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetUpdateEvents(final DistributionSetUpdatedEventContainer eventContainer) {
        @SuppressWarnings("unchecked")
        final List<Long> visibleItemIds = (List<Long>) getVisibleItemIds();
        updateVisableTableEntries(eventContainer.getEvents(), visibleItemIds);
        handleSelectedAndUpdatedDs(eventContainer.getEvents());
    }

    private void handleSelectedAndUpdatedDs(final List<DistributionSetUpdatedEvent> events) {
        manageDistUIState.getLastSelectedDistribution()
                .ifPresent(lastSelectedDsIdName -> events.stream().filter(Objects::nonNull)
                        .filter(event -> event.getEntityId().equals(lastSelectedDsIdName)).findAny()
                        .ifPresent(event -> eventBus.publish(this,
                                new DistributionTableEvent(BaseEntityEventType.SELECTED_ENTITY, event.getEntity()))));
    }

    private void updateVisableTableEntries(final List<DistributionSetUpdatedEvent> events,
            final List<Long> visibleItemIds) {
        events.stream().filter(event -> visibleItemIds.contains(event.getEntityId()))
                .filter(DistributionSetUpdatedEvent::isComplete).filter(Objects::nonNull)
                .forEach(event -> updateDistributionInTable(event.getEntity()));
    }

    @Override
    protected String getTableId() {
        return UIComponentIdProvider.DIST_TABLE_ID;
    }

    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();
        final BeanQueryFactory<ManageDistBeanQuery> distributionQF = new BeanQueryFactory<>(ManageDistBeanQuery.class);

        distributionQF.setQueryConfiguration(queryConfiguration);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), distributionQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(2);
        manageDistUIState.getManageDistFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));

        if (manageDistUIState.getManageDistFilters().getClickedDistSetType() != null) {
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
        return manageDistUIState.getSelectedDistributions().isEmpty();
    }

    @Override
    protected Object getItemIdToSelect() {
        return isFirstRowSelectedOnLoad() ? null : manageDistUIState.getSelectedDistributions();
    }

    @Override
    protected Optional<DistributionSet> findEntityByTableValue(final Long entityTableId) {
        return distributionSetManagement.getWithDetails(entityTableId);
    }

    @Override
    protected ManageDistUIState getManagementEntityState() {
        return manageDistUIState;
    }

    @Override
    protected void afterEntityIsSelected() {
        eventBus.publish(this, DistributionsUIEvent.ORDER_BY_DISTRIBUTION);
    }

    @Override
    protected void publishSelectedEntityEvent(final DistributionSet distributionSet) {
        eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.SELECTED_ENTITY, distributionSet));
    }

    @Override
    protected boolean isMaximized() {
        return manageDistUIState.isDsTableMaximized();
    }

    @Override
    public AcceptCriterion getDropAcceptCriterion() {
        return distributionsViewClientCriterion;
    }

    @Override
    protected void onDropEventFromTable(final DragAndDropEvent event) {
        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        final AbstractTable<?> source = (AbstractTable<?>) transferable.getSourceComponent();
        final Set<Long> softwareModulesIdList = source.getDeletedEntityByTransferable(transferable);

        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();

        final Object distItemId = dropData.getItemIdOver();
        handleDropEvent(source, softwareModulesIdList, distItemId);
    }

    @Override
    protected void onDropEventFromWrapper(final DragAndDropEvent event) {
        // nothing to do
    }

    @Override
    protected boolean isDropValid(final DragAndDropEvent dragEvent) {
        final Component compsource = dragEvent.getTransferable().getSourceComponent();
        if (!(compsource instanceof Table)) {
            notification.displayValidationError(i18n.getMessage(ACTION_NOT_ALLOWED_MSG));
            return false;
        }
        return super.isDropValid(dragEvent);
    }

    private void handleDropEvent(final Table source, final Set<Long> softwareModulesIdList, final Object distId) {
        final Optional<DistributionSet> distributionSet = distributionSetManagement.get((Long) distId);

        if (!distributionSet.isPresent()) {
            notification.displayWarning(i18n.getMessage("distributionset.not.exists"));
            return;
        }

        final DistributionSetIdName distributionSetIdName = new DistributionSetIdName(distributionSet.get());

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

            final Optional<SoftwareModule> softwareModule = softwareModuleManagement.get(softwareModuleId);

            if (softwareModule.isPresent() && validSoftwareModule((Long) distId, softwareModule.get())) {
                final SoftwareModuleIdName softwareModuleIdName = new SoftwareModuleIdName(softwareModuleId,
                        name.concat(":" + swVersion));
                publishAssignEvent((Long) distId, softwareModule.get());
                handleSoftwareCase(map, softwareModule.get(), softwareModuleIdName);
                handleFirmwareCase(map, softwareModule.get(), softwareModuleIdName);
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
        if (manageDistUIState.getLastSelectedDistribution().map(distId::equals).orElse(false)) {
            eventBus.publish(this,
                    new SoftwareModuleEvent(SoftwareModuleEventType.ASSIGN_SOFTWARE_MODULE, softwareModule));
        }
    }

    private static void handleFirmwareCase(final Map<Long, HashSet<SoftwareModuleIdName>> map,
            final SoftwareModule softwareModule, final SoftwareModuleIdName softwareModuleIdName) {
        if (softwareModule.getType().getMaxAssignments() == 1) {
            if (!map.containsKey(softwareModule.getType().getId())) {
                map.put(softwareModule.getType().getId(), new HashSet<SoftwareModuleIdName>());
            }
            map.get(softwareModule.getType().getId()).clear();
            map.get(softwareModule.getType().getId()).add(softwareModuleIdName);
        }
    }

    private static void handleSoftwareCase(final Map<Long, HashSet<SoftwareModuleIdName>> map,
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
    }

    private boolean validSoftwareModule(final Long distId, final SoftwareModule sm) {
        if (!isSoftwareModuleDragged(distId, sm)) {
            return false;
        }
        final Optional<DistributionSet> ds = distributionSetManagement.getWithDetails(distId);
        if (!ds.isPresent() || !validateSoftwareModule(sm, ds.get())) {
            return false;
        }

        if (distributionSetManagement.isInUse(ds.get().getId())) {
            notification.displayValidationError(i18n.getMessage("message.error.notification.ds.target.assigned",
                    ds.get().getName(), ds.get().getVersion()));
            return false;
        }
        return true;
    }

    private boolean validateSoftwareModule(final SoftwareModule sm, final DistributionSet ds) {
        if (targetManagement.countByFilters(null, null, null, ds.getId(), Boolean.FALSE, new String[] {}) > 0) {
            /* Distribution is already assigned */
            notification.displayValidationError(i18n.getMessage("message.dist.inuse",
                    HawkbitCommonUtil.concatStrings(":", ds.getName(), ds.getVersion())));
            return false;
        }

        if (ds.getModules().contains(sm)) {
            /* Already has software module */
            notification.displayValidationError(i18n.getMessage("message.software.dist.already.assigned",
                    HawkbitCommonUtil.concatStrings(":", sm.getName(), sm.getVersion()),
                    HawkbitCommonUtil.concatStrings(":", ds.getName(), ds.getVersion())));
            return false;
        }

        if (!ds.getType().containsModuleType(sm.getType())) {
            /* Invalid type of the software module */
            notification.displayValidationError(i18n.getMessage("message.software.dist.type.notallowed",
                    HawkbitCommonUtil.concatStrings(":", sm.getName(), sm.getVersion()),
                    HawkbitCommonUtil.concatStrings(":", ds.getName(), ds.getVersion()), sm.getType().getName()));
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
                    notification.displayValidationError(i18n.getMessage("message.software.already.dragged",
                            HawkbitCommonUtil.concatStrings(":", sm.getName(), sm.getVersion())));
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected String getDropTableId() {
        return UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    @Override
    protected List<String> hasMissingPermissionsForDrop() {
        return permissionChecker.hasUpdateRepositoryPermission() ? Collections.emptyList()
                : Arrays.asList(SpPermission.UPDATE_REPOSITORY);
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

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionTableEvent event) {
        onBaseEntityEvent(event);
        if (BaseEntityEventType.UPDATED_ENTITY != event.getEventType()) {
            return;
        }
        UI.getCurrent().access(() -> updateDistributionInTable(event.getEntity()));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.DELETED_DISTRIBUTIONS || event == SaveActionWindowEvent.SAVED_ASSIGNMENTS) {
            UI.getCurrent().access(this::refreshFilter);
        }
    }

    /**
     * DistributionTableFilterEvent.
     *
     * @param filterEvent
     *            as instance of {@link RefreshDistributionTableByFilterEvent}
     */
    @EventBusListenerMethod(scope = EventScope.UI, filter = OnlyEventsFromDistributionsViewFilter.class)
    public void onEvent(final RefreshDistributionTableByFilterEvent filterEvent) {
        UI.getCurrent().access(this::refreshFilter);
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
            private static final long serialVersionUID = 1L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                final String nameVersionStr = getNameAndVerion(itemId);
                final Button manageMetaDataBtn = createManageMetadataButton(nameVersionStr);
                manageMetaDataBtn.addClickListener(event -> showMetadataDetails((Long) itemId));
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
                UIComponentIdProvider.DS_TABLE_MANAGE_METADATA_ID + "." + nameVersionStr, "", "", null, false,
                FontAwesome.LIST_ALT, SPUIButtonStyleSmallNoBorder.class);
        manageMetadataBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        manageMetadataBtn.setDescription(i18n.getMessage("tooltip.metadata.icon"));
        return manageMetadataBtn;
    }

    private void showMetadataDetails(final Long itemId) {
        distributionSetManagement.getWithDetails(itemId)
                .ifPresent(ds -> UI.getCurrent().addWindow(dsMetadataPopupLayout.getWindow(ds, null)));
    }

    private String getNameAndVerion(final Object itemId) {
        final Item item = getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        return name + "." + version;
    }

    private void updateDistributionInTable(final DistributionSet editedDs) {
        final Item item = getContainerDataSource().getItem(editedDs.getId());
        updateEntity(editedDs, item);
    }

}
