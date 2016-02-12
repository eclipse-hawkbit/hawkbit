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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadViewAcceptCriteria;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
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
import com.vaadin.ui.UI;

/**
 * Header of Software module table.
 *
 *
 *
 */
@SpringComponent
@ViewScope
public class SoftwareModuleTable extends AbstractTable {

    private static final long serialVersionUID = 6469417305487144809L;

    @Autowired
    private I18N i18n;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private UploadViewAcceptCriteria uploadViewAcceptCriteria;
    
    private Boolean isFilterEvent = false;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
        setNoDataAvailable();
    }

    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SMFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {

            if (filterEvent == SMFilterEvent.FILTER_BY_TYPE || filterEvent == SMFilterEvent.FILTER_BY_TEXT
                    || filterEvent == SMFilterEvent.REMOVER_FILTER_BY_TYPE
                    || filterEvent == SMFilterEvent.REMOVER_FILTER_BY_TEXT) {
                
                if(prepareQueryConfigFilters().size()<1 && isFilterEvent==false){
                    UI.getCurrent().access(() -> ((LazyQueryContainer) getContainerDataSource()).refresh());
                                                           
                }else {
                     refreshFilter();
                    if(prepareQueryConfigFilters().size()<1){
                        isFilterEvent = false;
                    }else{
                        isFilterEvent = true;
                    }
                }
               //refreshFilter();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTable#getTableId()
     */
    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTable#createContainer()
     */
    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<BaseSwModuleBeanQuery> swQF = new BeanQueryFactory<BaseSwModuleBeanQuery>(
                BaseSwModuleBeanQuery.class);
        swQF.setQueryConfiguration(queryConfiguration);

        final LazyQueryContainer container = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, "swId"), swQF);
        return container;
    }
    
    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<String, Object>();
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
    protected void addCustomGeneratedColumns() {
        /* No generated columns */
    }

    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return artifactUploadState.getSelectedSoftwareModules().isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTable#getItemIdToSelect()
     */
    @Override
    protected Object getItemIdToSelect() {
        return artifactUploadState.getSelectedSoftwareModules();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTable#isMaximized()
     */
    @Override
    protected boolean isMaximized() {
        return artifactUploadState.isSwModuleTableMaximized();
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void onValueChange() {
        eventBus.publish(this, UploadArtifactUIEvent.HIDE_DROP_HINTS);
        @SuppressWarnings("unchecked")
        final Set<Long> values = (Set) getValue();
        if (values != null && !values.isEmpty()) {
            final Iterator<Long> iterator = values.iterator();
            Long value = null;
            while (iterator.hasNext()) {
                value = iterator.next();
            }
            if (null != value) {
                artifactUploadState.setSelectedBaseSwModuleId(value);
                final SoftwareModule baseSoftwareModule = softwareManagement.findSoftwareModuleById(value);
                artifactUploadState.setSelectedBaseSoftwareModule(baseSoftwareModule);
                artifactUploadState.setSelectedSoftwareModules(values);
                eventBus.publish(this,
                        new SoftwareModuleEvent(SoftwareModuleEventType.SELECTED_SOFTWARE_MODULE, baseSoftwareModule));
            }
        } else {
            artifactUploadState.setSelectedBaseSwModuleId(null);
            artifactUploadState.setSelectedBaseSoftwareModule(null);
            artifactUploadState.setSelectedSoftwareModules(Collections.emptySet());
            eventBus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.SELECTED_SOFTWARE_MODULE, null));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.MINIMIZED) {
            UI.getCurrent().access(() -> applyMinTableSettings());
        } else if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.MAXIMIZED) {
            UI.getCurrent().access(() -> applyMaxTableSettings());
        } else if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.NEW_SOFTWARE_MODULE) {
            UI.getCurrent().access(() -> addSoftwareModule(event.getSoftwareModule()));
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.DELETED_ALL_SOFWARE) {
            UI.getCurrent().access(() -> refreshFilter());
        }
    }

    /**
     * Add new software module to table.
     *
     * @param swModule
     *            new software module
     */
    @SuppressWarnings("unchecked")
    private void addSoftwareModule(final SoftwareModule swModule) {
        final Object addItem = addItem();
        final Item item = getItem(addItem);
        final String swNameVersion = HawkbitCommonUtil.concatStrings(":", swModule.getName(), swModule.getVersion());
        item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(swNameVersion);
        item.getItemProperty("swId").setValue(swModule.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_ID).setValue(swModule.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(swModule.getDescription());
        item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).setValue(swModule.getVersion());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(swModule.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_VENDOR).setValue(swModule.getVendor());
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_BY).setValue(swModule.getCreatedBy());
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setValue(swModule.getLastModifiedBy());
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(swModule.getCreatedAt()));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(swModule.getLastModifiedAt()));
        if (!artifactUploadState.getSelectedSoftwareModules().isEmpty()) {
            artifactUploadState.getSelectedSoftwareModules().stream().forEach(swmNameId -> unselect(swmNameId));
        }
        select(swModule.getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTable#getTableVisibleColumns
     * ()
     */
    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = new ArrayList<TableColumn>();
        if (isMaximized()) {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.get("header.name"), 0.2F));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VERSION, i18n.get("header.version"), 0.1F));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VENDOR, i18n.get("header.vendor"), 0.1F));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_BY, i18n.get("header.createdBy"), 0.1F));
            columnList
                    .add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.1F));
            columnList.add(
                    new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.1F));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, i18n.get("header.modifiedDate"),
                    0.1F));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.get("header.description"), 0.2F));
        } else {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.get("header.name"), 0.8F));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VERSION, i18n.get("header.version"), 0.2F));
        }
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

    private void setNoDataAvailable() {
        final int containerSize = getContainerDataSource().size();
        if (containerSize == 0) {
            artifactUploadState.setNoDataAvilableSoftwareModule(true);
        } else {
            artifactUploadState.setNoDataAvilableSoftwareModule(false);
        }
    }
}
