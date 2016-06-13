/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractNamedVersionedEntityTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.DistributionSetMetadatadetailslayout;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleMetadatadetailslayout;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Implementation of software module details block using generic abstract
 * details style .
 */
@SpringComponent
@ViewScope
public class SwModuleDetails extends AbstractNamedVersionedEntityTableDetailsLayout<SoftwareModule> {

    private static final long serialVersionUID = -1052279281066089812L;

    @Autowired
    private SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow;

    @Autowired
    private ManageDistUIState manageDistUIState;
    
    @Autowired
    private transient SoftwareManagement softwareManagement;
    
    @Autowired
    private SwMetadataPopupLayout swMetadataPopupLayout;
    
    @Autowired
    private EntityFactory entityFactory;
    
    private VerticalLayout metadataLayout;

   /**
     * softwareLayout Initialize the component.
     */
    @Override
    protected void init() {
        swmMetadataTable = new SoftwareModuleMetadatadetailslayout();
        swmMetadataTable.init(getI18n(), getPermissionChecker(),softwareManagement,swMetadataPopupLayout,entityFactory);
        super.init();
    }
    
      
    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final MetadataEvent event) {
        UI.getCurrent().access(() -> {

            if (event.getMetadataUIEvent() == MetadataEvent.MetadataUIEvent.CREATE_SOFTWAREMODULE_METADATA){
                swmMetadataTable.createMetadata(event.getMetadataKey());
            }else if(event.getMetadataUIEvent() == MetadataEvent.MetadataUIEvent.DELETE_SOFTWAREMODULE_METADATA){                                         
                swmMetadataTable.deleteMetadata(event.getMetadataKey());
            }
        });
    }

    
    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent softwareModuleEvent) {
        onBaseEntityEvent(softwareModuleEvent);
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        final Window addSoftwareModule = softwareModuleAddUpdateWindow
                .createUpdateSoftwareModuleWindow(getSelectedBaseEntityId());
        addSoftwareModule.setCaption(getI18n().get("upload.caption.update.swmodule"));
        UI.getCurrent().addWindow(addSoftwareModule);
        addSoftwareModule.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getEditButtonId() {
        return SPUIComponentIdProvider.UPLOAD_SW_MODULE_EDIT_BUTTON;
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), getI18n().get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), getI18n().get("caption.tab.description"), null);
        detailsTab.addTab(createLogLayout(), getI18n().get("caption.logs.tab"), null);
        detailsTab.addTab(createMetadataLayout(), getI18n().get("caption.metadata.tab"), null);
    }

    private VerticalLayout createMetadataLayout() {
        metadataLayout = getTabLayout();
        metadataLayout.setSizeFull();
        metadataLayout.setId(SPUIDefinitions.SOFTWAREMODULE_METADATA_TAB_ID);
        metadataLayout.addComponent(swmMetadataTable);
        return metadataLayout;
    }
    
    @Override
    protected String getDefaultCaption() {
        return getI18n().get("upload.swModuleTable.header");
    }

    @Override
    protected Boolean onLoadIsTableRowSelected() {
        return !manageDistUIState.getSelectedSoftwareModules().isEmpty();
    }

    @Override
    protected Boolean onLoadIsTableMaximized() {
        return manageDistUIState.isSwModuleTableMaximized();
    }

    @Override
    protected Boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateDistributionPermission();
    }

    @Override
    protected String getTabSheetId() {
        return SPUIComponentIdProvider.DIST_SW_MODULE_DETAILS_TABSHEET_ID;
    }

    private void populateDetails() {
        String maxAssign = HawkbitCommonUtil.SP_STRING_EMPTY;
        if (getSelectedBaseEntity() != null) {
            if (getSelectedBaseEntity().getType().getMaxAssignments() == Integer.MAX_VALUE) {
                maxAssign = getI18n().get("label.multiAssign.type");
            } else {
                maxAssign = getI18n().get("label.singleAssign.type");
            }
            updateSwModuleDetailsLayout(getSelectedBaseEntity().getType().getName(),
                    getSelectedBaseEntity().getVendor(), maxAssign);
        } else {
            updateSwModuleDetailsLayout(HawkbitCommonUtil.SP_STRING_EMPTY, HawkbitCommonUtil.SP_STRING_EMPTY,
                    maxAssign);
        }
    }

    private void updateSwModuleDetailsLayout(final String type, final String vendor, final String maxAssign) {

        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        final Label vendorLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.vendor"),
                HawkbitCommonUtil.trimAndNullIfEmpty(vendor) == null ? "" : vendor);
        vendorLabel.setId(SPUIComponentIdProvider.DETAILS_VENDOR_LABEL_ID);
        detailsTabLayout.addComponent(vendorLabel);

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.type"),
                    type);
            typeLabel.setId(SPUIComponentIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        final Label assignLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.assigned.type"),
                HawkbitCommonUtil.trimAndNullIfEmpty(maxAssign) == null ? "" : maxAssign);
        assignLabel.setId(SPUIComponentIdProvider.SWM_DTLS_MAX_ASSIGN);
        detailsTabLayout.addComponent(assignLabel);

    }

    @Override
    protected void populateDetailsWidget() {
        populateDetails();
        populateMetadataDetails();
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return SPUIComponentIdProvider.TARGET_DETAILS_HEADER_LABEL_ID;
    }
    
    private void populateMetadataDetails(){
        swmMetadataTable.populateSMMetadata(getSelectedBaseEntity());
   }

}
