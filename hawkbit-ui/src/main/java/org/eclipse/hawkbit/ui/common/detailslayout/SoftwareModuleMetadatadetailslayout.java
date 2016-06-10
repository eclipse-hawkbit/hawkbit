/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.List;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

@SpringComponent
@ViewScope
public class SoftwareModuleMetadatadetailslayout extends Table {   
  

    private static final long serialVersionUID = 2913758299611838818L;
        
  
    
    private static final Logger LOG = LoggerFactory.getLogger(SoftwareModuleMetadatadetailslayout.class);

    private static final String METADATA_KEY = "Key";
    
    private SpPermissionChecker permissionChecker;
    
    private SoftwareManagement  softwareManagement;
    
    private SwMetadataPopupLayout swMetadataPopupLayout;

    private I18N i18n;
    
    private  Long selectedSWModuleId;
    
    private EntityFactory entityFactory;
    
    
   /**
    * 
    * @param i18n
    * @param permissionChecker
    * @param softwareManagement
    * @param swMetadataPopupLayout
    */
    public void init(final I18N i18n, final SpPermissionChecker permissionChecker,
                     final SoftwareManagement softwareManagement,
                     final SwMetadataPopupLayout swMetadataPopupLayout,
                     final EntityFactory entityFactory) {
        this.i18n = i18n;
        this.permissionChecker = permissionChecker;
        this.softwareManagement = softwareManagement;
        this.swMetadataPopupLayout = swMetadataPopupLayout;
        this.entityFactory = entityFactory;
        createSWMMetadataTable();
        addCustomGeneratedColumns();
    }

    private void createSWMMetadataTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getSwModuleMetadataContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addSMMetadataTableHeader();
        setSizeFull();
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
    }

    private IndexedContainer getSwModuleMetadataContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(METADATA_KEY, String.class, "");
        setColumnExpandRatio(METADATA_KEY, 0.7f);
        setColumnAlignment(METADATA_KEY, Align.LEFT);
        return container;
    }

    private void addSMMetadataTableHeader() {
        setColumnHeader(METADATA_KEY, i18n.get("label.dist.details.key"));
    }

    /**
     * Populate software module metadata table.
     * 
     * @param swModule
     */
    public void populateSMMetadata(final SoftwareModule swModule) {
        removeAllItems();
        
        if (null != swModule) {   
            selectedSWModuleId = swModule.getId();
            final List<SoftwareModuleMetadata> swMetadataList = swModule.getMetadata();
            if (null != swMetadataList && !swMetadataList.isEmpty()) {
                swMetadataList.forEach(swMetadata -> setSWMetadataProperties(swMetadata));
            }
         }

    }
    
    private void setSWMetadataProperties(final SoftwareModuleMetadata swMetadata){
        final Item item = getContainerDataSource().addItem(swMetadata.getKey());
        item.getItemProperty(METADATA_KEY).setValue(swMetadata.getKey());
    }
    
    protected void addCustomGeneratedColumns() {       
        addGeneratedColumn(METADATA_KEY,
                (source, itemId, columnId) -> customMetadataDetailButton((String) itemId));
    }

    private Button customMetadataDetailButton(final String itemId) {
        final Item row1 = getItem(itemId);
        final String metadataKey = (String) row1.getItemProperty(METADATA_KEY).getValue();

        final Button viewLink = SPUIComponentProvider.getButton(getDetailLinkId(metadataKey), metadataKey,
                "View" + metadataKey +" Metadata details", null, false, null, SPUIButtonStyleSmallNoBorder.class);
        viewLink.setData(metadataKey);
        if(permissionChecker.hasUpdateDistributionPermission()) {
            viewLink.addStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link");
            viewLink.addClickListener(event -> showMetadataDetails(selectedSWModuleId,metadataKey));
        }
        return viewLink;
    }
    
    private static String getDetailLinkId(final String name) {
        return new StringBuilder(SPUIComponetIdProvider.SW_METADATA_DETAIL_LINK).append('.').append(name)
                .toString();
    }
    
    private void showMetadataDetails(final Long selectedSWModuleId , final String metadataKey) {
       SoftwareModule swmodule = softwareManagement.findSoftwareModuleById(selectedSWModuleId);
       
        /* display the window */
        UI.getCurrent().addWindow(swMetadataPopupLayout.getWindow(swmodule,
                entityFactory.generateSoftwareModuleMetadata(swmodule, metadataKey, "")));
    }
    
    public void createMetadata(final String metadataKeyName){
        final IndexedContainer metadataContainer = (IndexedContainer) getContainerDataSource();
        final Item item = metadataContainer.addItem(metadataKeyName);
        item.getItemProperty(METADATA_KEY).setValue(metadataKeyName);
        
    }
    
    public void deleteMetadata(final String metadataKeyName){
        final IndexedContainer metadataContainer = (IndexedContainer) getContainerDataSource();
         metadataContainer.removeItem(metadataKeyName);
    }
   
}
