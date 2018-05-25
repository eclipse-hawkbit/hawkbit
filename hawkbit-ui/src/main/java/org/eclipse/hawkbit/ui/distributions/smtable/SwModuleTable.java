/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.RefreshSoftwareModuleByFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.ManagementEntityState;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.push.SoftwareModuleUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.eclipse.hawkbit.ui.view.filter.OnlyEventsFromDistributionsViewFilter;
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
import com.vaadin.server.Page;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Implementation of software module table using generic abstract table styles .
 */
public class SwModuleTable extends AbstractNamedVersionTable<SoftwareModule> {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final DistributionsViewClientCriterion distributionsViewClientCriterion;

    SwModuleTable(final UIEventBus eventBus, final VaadinMessageSource i18n, final UINotification uiNotification,
            final ManageDistUIState manageDistUIState, final SoftwareModuleManagement softwareManagement,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final SpPermissionChecker permChecker) {
        super(eventBus, i18n, uiNotification, permChecker);
        this.manageDistUIState = manageDistUIState;
        this.softwareModuleManagement = softwareManagement;
        this.distributionsViewClientCriterion = distributionsViewClientCriterion;

        addNewContainerDS();
        setColumnProperties();
        setDataAvailable(getContainerDataSource().size() != 0);
        styleTableOnDistSelection();
    }

    @EventBusListenerMethod(scope = EventScope.UI, filter = OnlyEventsFromDistributionsViewFilter.class)
    void onEvent(final RefreshSoftwareModuleByFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {
            refreshFilter();
            styleTableOnDistSelection();
        });
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionsUIEvent event) {
        UI.getCurrent().access(() -> {
            if (event == DistributionsUIEvent.ORDER_BY_DISTRIBUTION) {
                refreshFilter();
                styleTableOnDistSelection();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.DELETE_ALL_SOFWARE) {
            UI.getCurrent().access(this::refreshFilter);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent event) {
        onBaseEntityEvent(event);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onSoftwareModuleUpdateEvents(final SoftwareModuleUpdatedEventContainer eventContainer) {
        @SuppressWarnings("unchecked")
        final List<Long> visibleItemIds = (List<Long>) getVisibleItemIds();
        handleSelectedAndUpdatedSoftwareModules(eventContainer.getEvents());
        eventContainer.getEvents().stream().filter(event -> visibleItemIds.contains(event.getEntityId()))
                .filter(Objects::nonNull).forEach(event -> updateSoftwareModuleInTable(event.getEntity()));
    }

    private void handleSelectedAndUpdatedSoftwareModules(final List<SoftwareModuleUpdatedEvent> events) {
        manageDistUIState.getLastSelectedSoftwareModule()
                .ifPresent(lastSelectedModuleId -> events.stream()
                        .filter(event -> lastSelectedModuleId.equals(event.getEntityId())).filter(Objects::nonNull)
                        .findAny().ifPresent(lastEvent -> getEventBus().publish(this,
                                new SoftwareModuleEvent(BaseEntityEventType.SELECTED_ENTITY, lastEvent.getEntity()))));
    }

    private void updateSoftwareModuleInTable(final SoftwareModule editedSm) {
        final Item item = getContainerDataSource().getItem(editedSm.getId());
        updateEntity(editedSm, item);
    }

    @Override
    protected String getTableId() {
        return UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<SwModuleBeanQuery> swQF = new BeanQueryFactory<>(SwModuleBeanQuery.class);
        swQF.setQueryConfiguration(queryConfiguration);

        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_SWM_ID), swQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(3);
        manageDistUIState.getSoftwareModuleFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));
        manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType()
                .ifPresent(type -> queryConfig.put(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE, type));
        manageDistUIState.getLastSelectedDistribution()
                .ifPresent(id -> queryConfig.put(SPUIDefinitions.ORDER_BY_DISTRIBUTION, id));
        return queryConfig;
    }

    @Override
    protected void addContainerProperties(final Container container) {
        final LazyQueryContainer lazyContainer = (LazyQueryContainer) container;
        lazyContainer.addContainerProperty(SPUILabelDefinitions.NAME_VERSION, String.class, null, false, false);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_ID, Long.class, null, false, false);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_VERSION, String.class, null, false, false);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_VENDOR, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class, null, false,
                true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_COLOR, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_SOFT_TYPE_ID, Long.class, null, false, true);
    }

    @Override
    protected Object getItemIdToSelect() {
        return manageDistUIState.getSelectedSoftwareModules().isEmpty() ? null
                : manageDistUIState.getSelectedSoftwareModules();
    }

    @Override
    protected boolean isMaximized() {
        return manageDistUIState.isSwModuleTableMaximized();
    }

    @Override
    protected void publishSelectedEntityEvent(final SoftwareModule selectedLastEntity) {
        getEventBus().publish(this, new SoftwareModuleEvent(BaseEntityEventType.SELECTED_ENTITY, selectedLastEntity));
    }

    @Override
    protected void setLastSelectedEntityId(final Long selectedLastEntityId) {
        if (selectedLastEntityId == null) {
            return;
        }
        manageDistUIState.setLastSelectedSoftwareModule(selectedLastEntityId);
    }

    @Override
    protected void setManagementEntityStateValues(final Set<Long> values, final Long lastId) {
        final ManagementEntityState managementEntityState = getManagementEntityState();
        if (managementEntityState == null) {
            return;
        }
        manageDistUIState.setLastSelectedSoftwareModule(lastId);
        manageDistUIState.setSelectedSoftwareModules(values);
    }

    @Override
    protected Optional<SoftwareModule> findEntityByTableValue(final Long lastSelectedId) {
        return softwareModuleManagement.get(lastSelectedId);
    }

    @Override
    protected ManagementEntityState getManagementEntityState() {
        return manageDistUIState;
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        if (isMaximized()) {
            columnList
                    .add(new TableColumn(SPUILabelDefinitions.VAR_VENDOR, getI18n().getMessage("header.vendor"), 0.1F));
        }
        return columnList;
    }

    @Override
    protected float getColumnNameMinimizedSize() {
        return 0.7F;
    }

    @Override
    protected AcceptCriterion getDropAcceptCriterion() {
        return distributionsViewClientCriterion;
    }

    @Override
    protected boolean isDropValid(final DragAndDropEvent dragEvent) {
        return false;
    }

    private void styleTableOnDistSelection() {
        Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.getScriptSMHighlightReset());
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return createTableStyle(itemId, propertyId);
            }
        });
    }

    private static String getTableStyle(final Long typeId, final boolean isAssigned, final String color) {
        if (isAssigned) {
            addTypeStyle(typeId, color);
            return "distribution-upload-type-" + typeId;
        }
        return null;
    }

    private static void addTypeStyle(final Long tagId, final String color) {
        final JavaScript javaScript = UI.getCurrent().getPage().getJavaScript();
        UI.getCurrent()
                .access(() -> javaScript.execute(
                        HawkbitCommonUtil.getScriptSMHighlightWithColor(".v-table-row-distribution-upload-type-" + tagId
                                + "{background-color:" + color + " !important;background-image:none !important }")));
    }

    private String createTableStyle(final Object itemId, final Object propertyId) {
        if (null == propertyId) {
            final Item item = getItem(itemId);
            final boolean isAssigned = (boolean) item.getItemProperty(SPUILabelDefinitions.VAR_ASSIGNED).getValue();
            final Long typeId = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_SOFT_TYPE_ID).getValue();
            String color = (String) item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).getValue();
            if (color == null) {
                // In case the color is null apply default green color. eg:- for
                // types ECL OS, ECL
                // Application etc.,
                color = SPUIDefinitions.DEFAULT_COLOR;
            }
            return getTableStyle(typeId, isAssigned, color);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void updateEntity(final SoftwareModule baseEntity, final Item item) {
        final String swNameVersion = HawkbitCommonUtil.concatStrings(":", baseEntity.getName(),
                baseEntity.getVersion());
        item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(swNameVersion);
        item.getItemProperty(SPUILabelDefinitions.VAR_SWM_ID).setValue(baseEntity.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_VENDOR).setValue(baseEntity.getVendor());
        item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).setValue(baseEntity.getType().getColour());
        super.updateEntity(baseEntity, item);
    }

    @Override
    protected void setDataAvailable(final boolean available) {
        manageDistUIState.setNoDataAvilableSwModule(!available);
    }

    @Override
    protected void handleOkDelete(final List<Long> entitiesToDelete) {
        softwareModuleManagement.delete(entitiesToDelete);
        getEventBus().publish(this, new SoftwareModuleEvent(BaseEntityEventType.REMOVE_ENTITY, entitiesToDelete));
        getNotification().displaySuccess(getI18n().getMessage("message.delete.success",
                entitiesToDelete.size() + " " + getI18n().getMessage("caption.software.module") + "(s)"));
        manageDistUIState.getSelectedSoftwareModules().clear();
    }

    @Override
    protected String getEntityType() {
        return getI18n().getMessage("upload.swModuleTable.header");
    }

    @Override
    protected Set<Long> getSelectedEntities() {
        return manageDistUIState.getSelectedSoftwareModules();
    }

    @Override
    protected String getEntityId(final Object itemId) {
        final String entityId = String.valueOf(
                getContainerDataSource().getItem(itemId).getItemProperty(SPUILabelDefinitions.VAR_SWM_ID).getValue());
        return "softwareModule." + entityId;
    }

    @Override
    protected String getDeletedEntityName(final Long entityId) {
        final Optional<SoftwareModule> softwareModule = softwareModuleManagement.get(entityId);
        if (softwareModule.isPresent()) {
            return softwareModule.get().getName() + ":" + softwareModule.get().getVersion();
        }
        return "";
    }

}
