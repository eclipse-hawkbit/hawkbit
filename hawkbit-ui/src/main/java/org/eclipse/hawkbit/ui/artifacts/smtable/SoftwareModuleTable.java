/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.RefreshSoftwareModuleByFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.state.CustomFile;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.dd.criteria.UploadViewClientCriterion;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.push.SoftwareModuleUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.eclipse.hawkbit.ui.view.filter.OnlyEventsFromUploadArtifactViewFilter;
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
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * The Software module table.
 */
public class SoftwareModuleTable extends AbstractNamedVersionTable<SoftwareModule> {

    private static final long serialVersionUID = 1L;

    private final ArtifactUploadState artifactUploadState;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final UploadViewClientCriterion uploadViewClientCriterion;

    private final SwMetadataPopupLayout swMetadataPopupLayout;

    SoftwareModuleTable(final UIEventBus eventBus, final VaadinMessageSource i18n, final UINotification uiNotification,
            final ArtifactUploadState artifactUploadState, final SoftwareModuleManagement softwareManagement,
            final UploadViewClientCriterion uploadViewClientCriterion,
            final SwMetadataPopupLayout swMetadataPopupLayout, final SpPermissionChecker permChecker) {
        super(eventBus, i18n, uiNotification, permChecker);
        this.artifactUploadState = artifactUploadState;
        this.softwareModuleManagement = softwareManagement;
        this.uploadViewClientCriterion = uploadViewClientCriterion;
        this.swMetadataPopupLayout = swMetadataPopupLayout;

        addNewContainerDS();
        setColumnProperties();
        setDataAvailable(getContainerDataSource().size() != 0);
    }

    @EventBusListenerMethod(scope = EventScope.UI, filter = OnlyEventsFromUploadArtifactViewFilter.class)
    void onEvent(final RefreshSoftwareModuleByFilterEvent filterEvent) {
        UI.getCurrent().access(this::refreshFilter);
    }

    @Override
    protected String getTableId() {
        return UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<BaseSwModuleBeanQuery> swQF = new BeanQueryFactory<>(BaseSwModuleBeanQuery.class);
        swQF.setQueryConfiguration(queryConfiguration);

        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_SWM_ID), swQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(2);
        artifactUploadState.getSoftwareModuleFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));

        artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType()
                .ifPresent(type -> queryConfig.put(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE, type));

        return queryConfig;
    }

    @Override
    protected void addContainerProperties(final Container container) {
        final LazyQueryContainer lqc = (LazyQueryContainer) container;
        lqc.addContainerProperty("nameAndVersion", String.class, null, false, false);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_ID, Long.class, null, false, false);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_VERSION, String.class, null, false, false);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_VENDOR, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class, null, false, true);
    }

    @Override
    protected Object getItemIdToSelect() {
        return artifactUploadState.getSelectedSoftwareModules().isEmpty() ? null
                : artifactUploadState.getSelectedSoftwareModules();
    }

    @Override
    protected boolean isMaximized() {
        return artifactUploadState.isSwModuleTableMaximized();
    }

    @Override
    protected Optional<SoftwareModule> findEntityByTableValue(final Long entityTableId) {
        return softwareModuleManagement.get(entityTableId);
    }

    @Override
    protected ArtifactUploadState getManagementEntityState() {
        return artifactUploadState;
    }

    @Override
    protected void publishSelectedEntityEvent(final SoftwareModule lastSoftwareModule) {
        eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.SELECTED_ENTITY, lastSoftwareModule));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent event) {
        onBaseEntityEvent(event);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.DELETED_ALL_SOFTWARE) {
            UI.getCurrent().access(this::refreshFilter);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onSoftwareModuleUpdateEvents(final SoftwareModuleUpdatedEventContainer eventContainer) {
        @SuppressWarnings("unchecked")
        final List<Long> visibleItemIds = (List<Long>) getVisibleItemIds();
        eventContainer.getEvents().stream().filter(event -> visibleItemIds.contains(event.getEntityId()))
                .filter(Objects::nonNull).forEach(event -> updateSoftwareModuleInTable(event.getEntity()));
    }

    private void updateSoftwareModuleInTable(final SoftwareModule editedSm) {
        final Item item = getContainerDataSource().getItem(editedSm.getId());
        updateEntity(editedSm, item);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateEntity(final SoftwareModule baseEntity, final Item item) {
        final String swNameVersion = HawkbitCommonUtil.concatStrings(":", baseEntity.getName(),
                baseEntity.getVersion());
        item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(swNameVersion);
        item.getItemProperty("swId").setValue(baseEntity.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_VENDOR).setValue(baseEntity.getVendor());
        super.updateEntity(baseEntity, item);
    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUILabelDefinitions.METADATA_ICON, new ColumnGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                final String nameVersionStr = getNameAndVersion(itemId);
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
            return columnList;
        }
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VENDOR, i18n.getMessage("header.vendor"), 0.1F));
        return columnList;
    }

    @Override
    protected AcceptCriterion getDropAcceptCriterion() {
        return uploadViewClientCriterion;
    }

    @Override
    protected boolean isDropValid(final DragAndDropEvent dragEvent) {
        return false;
    }

    @Override
    protected void setDataAvailable(final boolean available) {
        artifactUploadState.setNoDataAvilableSoftwareModule(!available);
    }

    private Button createManageMetadataButton(final String nameVersionStr) {
        final Button manageMetadataBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.SW_TABLE_MANAGE_METADATA_ID + "." + nameVersionStr, "", "", null, false,
                FontAwesome.LIST_ALT, SPUIButtonStyleSmallNoBorder.class);
        manageMetadataBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        manageMetadataBtn.setDescription(i18n.getMessage("tooltip.metadata.icon"));
        return manageMetadataBtn;
    }

    private String getNameAndVersion(final Object itemId) {
        final Item item = getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        return name + "." + version;
    }

    private void showMetadataDetails(final Long itemId) {
        softwareModuleManagement.get(itemId)
                .ifPresent(swmodule -> UI.getCurrent().addWindow(swMetadataPopupLayout.getWindow(swmodule, null)));
    }

    @Override
    protected void handleOkDelete(final Set<Long> entitiesToDelete) {
        softwareModuleManagement.delete(entitiesToDelete);
        eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.REMOVE_ENTITY, entitiesToDelete));
        notification.displaySuccess(
                i18n.getMessage("message.delete.success", entitiesToDelete.size() + " Software Module(s) "));

        /*
         * Check if any information / files pending to upload for the deleted
         * software modules. If so, then delete the files from the upload list.
         */
        final List<CustomFile> toBeRemoved = new ArrayList<>();
        for (final Long id : entitiesToDelete) {
            final Optional<SoftwareModule> deleteSoftwareNameVersion = softwareModuleManagement.get(id);
            deleteSoftwareNameVersion.ifPresent(dsnv -> {
                // TODO Refactor this when PR for branch
                // 'feature_improve_upload_ux' is merged / merge branch into
                // this one
                for (final CustomFile customFile : artifactUploadState.getFileSelected()) {
                    if (dsnv.getName().equals(customFile.getBaseSoftwareModuleName())
                            && dsnv.getVersion().equals(customFile.getBaseSoftwareModuleVersion())) {
                        toBeRemoved.add(customFile);
                    }
                }
            });
        }
        if (!toBeRemoved.isEmpty()) {
            artifactUploadState.getFileSelected().removeAll(toBeRemoved);
        }
        artifactUploadState.getSelectedSoftwareModules().clear();
        eventBus.publish(this, UploadArtifactUIEvent.DELETED_ALL_SOFTWARE);
    }

    @Override
    protected String getEntityName() {
        return "Software Module";
    }

    @Override
    protected Set<Long> getSelectedEntities() {
        return artifactUploadState.getSelectedSoftwareModules();
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
            return softwareModule.get().getName();
        }
        return "";
    }

}
