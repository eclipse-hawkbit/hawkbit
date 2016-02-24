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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityLockedException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetIdName;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleIdName;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsViewAcceptCriteria;
import org.eclipse.hawkbit.ui.distributions.event.DragEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableFilterEvent;
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
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Distribution set table.
 *
 *
 *
 */
@SpringComponent
@ViewScope
public class DistributionSetTable extends AbstractTable {

    private static final long serialVersionUID = -7731776093470487988L;

    private static final Logger LOG = LoggerFactory.getLogger(DistributionSetTable.class);

    private static final List<Object> DISPLAY_DROP_HINT_EVENTS = new ArrayList<>(
            Arrays.asList(DragEvent.SOFTWAREMODULE_DRAG));

    @Autowired
    private I18N i18n;

    @Autowired
    private SpPermissionChecker permissionChecker;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

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
    
    /**
     * Initialize the component.
     */
    @Override
    @PostConstruct
    protected void init() {
        super.init();
        addTableStyleGenerator();
        setNoDataAvailable();
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DragEvent event) {
        if (event == DragEvent.HIDE_DROP_HINT) {
            UI.getCurrent().access(() -> removeStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
        } else if (DISPLAY_DROP_HINT_EVENTS.contains(event)) {
            UI.getCurrent().access(() -> addStyleName(SPUIStyleDefinitions.SHOW_DROP_HINT_TABLE));
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
        final BeanQueryFactory<ManageDistBeanQuery> distributionQF = new BeanQueryFactory<>(ManageDistBeanQuery.class);

        distributionQF.setQueryConfiguration(queryConfiguration);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_DIST_ID_NAME),
                distributionQF);
    }
    
    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<String, Object>();
        manageDistUIState.getManageDistFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));

        if (null != manageDistUIState.getManageDistFilters().getClickedDistSetType()) {
            queryConfig.put(SPUIDefinitions.FILTER_BY_DISTRIBUTION_SET_TYPE,
                    manageDistUIState.getManageDistFilters().getClickedDistSetType());
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
        HawkbitCommonUtil.getDsTableColumnProperties(container);
        ((LazyQueryContainer) container).addContainerProperty(SPUILabelDefinitions.VAR_IS_DISTRIBUTION_COMPLETE,
                Boolean.class, null, false, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTable#
     * addCustomGeneratedColumns ()
     */
    @Override
    protected void addCustomGeneratedColumns() {
        /**
         * No generated columns.
         */
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.common.table.AbstractTable#
     * isFirstRowSelectedOnLoad ()
     */
    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return !manageDistUIState.getSelectedDistributions().isPresent()
                || manageDistUIState.getSelectedDistributions().get().isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.table.AbstractTable#getItemIdToSelect()
     */
    @Override
    protected Object getItemIdToSelect() {
        if (manageDistUIState.getSelectedDistributions().isPresent()) {
            return manageDistUIState.getSelectedDistributions().get();
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
        final Set<DistributionSetIdName> values = (Set<DistributionSetIdName>) getValue();
        DistributionSetIdName value = null;
        if (values != null && !values.isEmpty()) {
            final Iterator<DistributionSetIdName> iterator = values.iterator();

            while (iterator.hasNext()) {
                value = iterator.next();
            }
            /**
             * Adding null check to make to avoid NPE.Its weird that at times
             * getValue returns null.
             */
            if (null != value) {
                manageDistUIState.setSelectedDistributions(values);
                manageDistUIState.setLastSelectedDistribution(value);

                final DistributionSet lastSelectedDistSet = distributionSetManagement
                        .findDistributionSetByIdWithDetails(value.getId());
                eventBus.publish(this,
                        new DistributionTableEvent(DistributionComponentEvent.ON_VALUE_CHANGE, lastSelectedDistSet));
            }
        } else {
            manageDistUIState.setSelectedDistributions(null);
            manageDistUIState.setLastSelectedDistribution(null);
            eventBus.publish(this, new DistributionTableEvent(DistributionComponentEvent.ON_VALUE_CHANGE, null));
        }
        eventBus.publish(this, DistributionsUIEvent.ORDER_BY_DISTRIBUTION);

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.AbstractTable#isMaximized()
     */
    @Override
    protected boolean isMaximized() {
        return manageDistUIState.isDsTableMaximized();
    }

    /*
     * (non-Javadoc)
     *
     * @see hawkbit.server.ui.common.table.AbstractTable#getTableVisibleColumns
     * ()
     */
    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        return HawkbitCommonUtil.getTableVisibleColumns(isMaximized(), false, i18n);
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
        final Table source = transferable.getSourceComponent();
        final Set<Long> softwareModuleSelected = (Set<Long>) source.getValue();
        final Set<Long> softwareModulesIdList = new HashSet<>();

        if (!softwareModuleSelected.contains(transferable.getData("itemId"))) {
            softwareModulesIdList.add((Long) transferable.getData("itemId"));
        } else {
            softwareModulesIdList.addAll(softwareModuleSelected);
        }

        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();

        final Object distItemId = dropData.getItemIdOver();
        final Item item = getItem(distItemId);
        if (item != null && item.getItemProperty("id") != null && item.getItemProperty("name") != null) {
            handleDropEvent(source, softwareModulesIdList, item);
        }
    }

    /**
     * @param source
     * @param softwareModulesIdList
     * @param item
     */
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

    /**
     * @param distId
     * @param softwareModule
     */
    private void publishAssignEvent(final Long distId, final SoftwareModule softwareModule) {
        if (manageDistUIState.getLastSelectedDistribution().isPresent()
                && manageDistUIState.getLastSelectedDistribution().get().getId().equals(distId)) {
            eventBus.publish(this,
                    new SoftwareModuleEvent(SoftwareModuleEventType.ASSIGN_SOFTWARE_MODULE, softwareModule));
        }
    }

    /**
     * @param map
     * @param softwareModule
     * @param softwareModuleIdName
     */
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

    /**
     * @param map
     * @param softwareModule
     * @param softwareModuleIdName
     */
    private void handleSoftwareCase(final Map<Long, HashSet<SoftwareModuleIdName>> map,
            final SoftwareModule softwareModule, final SoftwareModuleIdName softwareModuleIdName) {
        if (softwareModule.getType().getMaxAssignments() == Integer.MAX_VALUE) {
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
        try {
            distributionSetManagement.checkDistributionSetAlreadyUse(ds);
        } catch (final EntityLockedException exception) {
            LOG.error("Unable to update distribution : ", exception);
            notification.displayValidationError(exception.getMessage());
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
                if (!source.getId().equals(SPUIComponetIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE)) {
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

    /**
     * Add new software module to table.
     *
     * @param swModule
     *            new software module
     */
    @SuppressWarnings("unchecked")
    private void addDistributionSet(final DistributionSet distributionSet) {
        final Object addItem = addItem();
        final Item item = getItem(addItem);

        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(distributionSet.getName());
        item.getItemProperty(SPUILabelDefinitions.DIST_ID).setValue(distributionSet.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_ID).setValue(distributionSet.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(distributionSet.getDescription());
        item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).setValue(distributionSet.getVersion());
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_BY)
                .setValue(HawkbitCommonUtil.getIMUser(distributionSet.getCreatedBy()));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY)
                .setValue(HawkbitCommonUtil.getIMUser(distributionSet.getLastModifiedBy()));
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(distributionSet.getCreatedAt()));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(distributionSet.getLastModifiedAt()));
        item.getItemProperty(SPUILabelDefinitions.VAR_IS_DISTRIBUTION_COMPLETE).setValue(distributionSet.isComplete());
        if (manageDistUIState.getSelectedDistributions().isPresent()) {
            manageDistUIState.getSelectedDistributions().get().stream().forEach(dsNameId -> unselect(dsNameId));
        }
        select(distributionSet.getDistributionSetIdName());
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
        if (event.getDistributionComponentEvent() == DistributionComponentEvent.MINIMIZED) {
            UI.getCurrent().access(() -> applyMinTableSettings());
        } else if (event.getDistributionComponentEvent() == DistributionComponentEvent.MAXIMIZED) {
            UI.getCurrent().access(() -> applyMaxTableSettings());
        } else if (event.getDistributionComponentEvent() == DistributionComponentEvent.ADD_DISTRIBUTION) {
            UI.getCurrent().access(() -> addDistributionSet(event.getDistributionSet()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.DELETED_DISTRIBUTIONS || event == SaveActionWindowEvent.SAVED_ASSIGNMENTS) {
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

    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventBus.unsubscribe(this);
    }

    private void setNoDataAvailable() {
        final int containerSize = getContainerDataSource().size();
        if (containerSize == 0) {
            manageDistUIState.setNoDataAvailableDist(true);
        } else {
            manageDistUIState.setNoDataAvailableDist(false);
        }
    }
}
