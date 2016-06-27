/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadViewAcceptCriteria;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
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
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

/**
 * Header of Software module table.
 *
 */
@SpringComponent
@ViewScope
public class SoftwareModuleTable extends AbstractNamedVersionTable<SoftwareModule, Long> {

    private static final long serialVersionUID = 6469417305487144809L;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private UploadViewAcceptCriteria uploadViewAcceptCriteria;
    
    @Autowired
    private SwMetadataPopupLayout swMetadataPopupLayout;

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SMFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {

            if (filterEvent == SMFilterEvent.FILTER_BY_TYPE || filterEvent == SMFilterEvent.FILTER_BY_TEXT
                    || filterEvent == SMFilterEvent.REMOVER_FILTER_BY_TYPE
                    || filterEvent == SMFilterEvent.REMOVER_FILTER_BY_TEXT) {
                refreshFilter();
            }
        });
    }

    @Override
    protected String getTableId() {
        return SPUIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<BaseSwModuleBeanQuery> swQF = new BeanQueryFactory<>(BaseSwModuleBeanQuery.class);
        swQF.setQueryConfiguration(queryConfiguration);

        return new LazyQueryContainer(new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, "swId"), swQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<>();
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
    protected boolean isFirstRowSelectedOnLoad() {
        return artifactUploadState.getSelectedSoftwareModules().isEmpty();
    }

    @Override
    protected Object getItemIdToSelect() {
        return artifactUploadState.getSelectedSoftwareModules();
    }

    @Override
    protected boolean isMaximized() {
        return artifactUploadState.isSwModuleTableMaximized();
    }

    @Override
    protected SoftwareModule findEntityByTableValue(final Long entityTableId) {
        return softwareManagement.findSoftwareModuleById(entityTableId);
    }

    @Override
    protected ArtifactUploadState getManagmentEntityState() {
        return artifactUploadState;
    }

    @Override
    protected void publishEntityAfterValueChange(final SoftwareModule lastSoftwareModule) {
        artifactUploadState.setSelectedBaseSoftwareModule(lastSoftwareModule);
        eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.SELECTED_ENTITY, lastSoftwareModule));
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        onBaseEntityEvent(event);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.DELETED_ALL_SOFWARE) {
            UI.getCurrent().access(() -> refreshFilter());
        }
    }

    @Override
    protected Item addEntity(final SoftwareModule baseEntity) {
        final Item item = super.addEntity(baseEntity);
        if (!artifactUploadState.getSelectedSoftwareModules().isEmpty()) {
            artifactUploadState.getSelectedSoftwareModules().stream().forEach(this::unselect);
        }
        select(baseEntity.getId());
        return item;

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
            private static final long serialVersionUID = 117186282275044399L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                final String nameVersionStr = getNameAndVerion(itemId);
                final Button manageMetaDataBtn = createManageMetadataButton(nameVersionStr);
                manageMetaDataBtn.addClickListener(event -> showMetadataDetails((Long) itemId, nameVersionStr));
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
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VENDOR, i18n.get("header.vendor"), 0.1F));
        return columnList;
    }

    @Override
    protected DropHandler getTableDropHandler() {
        return new DropHandler() {

            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return uploadViewAcceptCriteria;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* Not required */
            }
        };
    }

    @Override
    protected void setDataAvailable(final boolean available) {
        artifactUploadState.setNoDataAvilableSoftwareModule(!available);
    }

    
    private Button createManageMetadataButton(String nameVersionStr) {
        final Button manageMetadataBtn = SPUIComponentProvider.getButton(
                SPUIComponentIdProvider.SW_TABLE_MANAGE_METADATA_ID + "." + nameVersionStr, "", "", null, false,
                FontAwesome.LIST_ALT, SPUIButtonStyleSmallNoBorder.class);
        manageMetadataBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        manageMetadataBtn.setDescription(i18n.get("tooltip.metadata.icon"));
        return manageMetadataBtn;
    }
    
    private String getNameAndVerion(final Object itemId) {
        final Item item = getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        return name + "." + version;
    }
    
    private void showMetadataDetails(Long itemId, String nameVersionStr) {
        SoftwareModule swmodule = softwareManagement.findSoftwareModuleWithDetails(itemId);
                /* display the window */
        UI.getCurrent().addWindow(swMetadataPopupLayout.getWindow(swmodule,null));
    }
}
