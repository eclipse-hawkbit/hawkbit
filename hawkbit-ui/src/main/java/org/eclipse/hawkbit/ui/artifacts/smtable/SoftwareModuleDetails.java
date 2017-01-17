/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractNamedVersionedEntityTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleMetadatadetailslayout;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Software module details.
 */
public class SoftwareModuleDetails extends AbstractNamedVersionedEntityTableDetailsLayout<SoftwareModule> {

    private static final long serialVersionUID = -4900381301076646366L;

    private final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow;

    private final ArtifactUploadState artifactUploadState;

    private final transient SoftwareManagement softwareManagement;

    private final SwMetadataPopupLayout swMetadataPopupLayout;

    private final SoftwareModuleMetadatadetailslayout swmMetadataTable;

    SoftwareModuleDetails(final I18N i18n, final UIEventBus eventBus, final SpPermissionChecker permissionChecker,
            final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow,
            final ArtifactUploadState artifactUploadState, final SoftwareManagement softwareManagement,
            final SwMetadataPopupLayout swMetadataPopupLayout, final EntityFactory entityFactory) {
        super(i18n, eventBus, permissionChecker, null);
        this.softwareModuleAddUpdateWindow = softwareModuleAddUpdateWindow;
        this.artifactUploadState = artifactUploadState;
        this.softwareManagement = softwareManagement;
        this.swMetadataPopupLayout = swMetadataPopupLayout;

        swmMetadataTable = new SoftwareModuleMetadatadetailslayout();
        swmMetadataTable.init(getI18n(), getPermissionChecker(), softwareManagement, swMetadataPopupLayout,
                entityFactory);
        addTabs(detailsTab);
        restoreState();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final MetadataEvent event) {
        UI.getCurrent().access(() -> {
            final MetaData softwareModuleMetadata = event.getMetaData();
            if (softwareModuleMetadata != null && isSoftwareModuleSelected(event.getModule())) {
                if (event.getMetadataUIEvent() == MetadataEvent.MetadataUIEvent.CREATE_SOFTWARE_MODULE_METADATA) {
                    swmMetadataTable.createMetadata(event.getMetaData().getKey());
                } else if (event
                        .getMetadataUIEvent() == MetadataEvent.MetadataUIEvent.DELETE_SOFTWARE_MODULE_METADATA) {
                    swmMetadataTable.deleteMetadata(event.getMetaData().getKey());
                }
            }
        });
    }

    @Override
    protected String getEditButtonId() {
        return UIComponentIdProvider.UPLOAD_SW_MODULE_EDIT_BUTTON;
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), getI18n().get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), getI18n().get("caption.tab.description"), null);
        detailsTab.addTab(createLogLayout(), getI18n().get("caption.logs.tab"), null);
        detailsTab.addTab(swmMetadataTable, getI18n().get("caption.metadata"), null);
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
    protected void populateDetailsWidget() {
        if (getSelectedBaseEntity() != null) {
            String maxAssign;
            if (getSelectedBaseEntity().getType().getMaxAssignments() == 1) {
                maxAssign = getI18n().get("label.singleAssign.type");
            } else {
                maxAssign = getI18n().get("label.multiAssign.type");
            }
            updateSoftwareModuleDetailsLayout(getSelectedBaseEntity().getType().getName(),
                    getSelectedBaseEntity().getVendor(), maxAssign);
        } else {
            updateSoftwareModuleDetailsLayout(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        }
        populateMetadataDetails();
    }

    private void updateSoftwareModuleDetailsLayout(final String type, final String vendor, final String maxAssign) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();

        detailsTabLayout.removeAllComponents();

        final Label vendorLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.vendor"),
                HawkbitCommonUtil.trimAndNullIfEmpty(vendor) == null ? "" : vendor);
        vendorLabel.setId(UIComponentIdProvider.DETAILS_VENDOR_LABEL_ID);
        detailsTabLayout.addComponent(vendorLabel);

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.type"),
                    type);
            typeLabel.setId(UIComponentIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        final Label assignLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.assigned.type"),
                HawkbitCommonUtil.trimAndNullIfEmpty(maxAssign) == null ? "" : maxAssign);
        assignLabel.setId(UIComponentIdProvider.SWM_DTLS_MAX_ASSIGN);
        detailsTabLayout.addComponent(assignLabel);

    }

    @Override
    protected String getDefaultCaption() {
        return getI18n().get("upload.swModuleTable.header");
    }

    @Override
    protected Boolean onLoadIsTableRowSelected() {
        return artifactUploadState.getSelectedBaseSoftwareModule().isPresent();
    }

    @Override
    protected Boolean onLoadIsTableMaximized() {
        return artifactUploadState.isSwModuleTableMaximized();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent softwareModuleEvent) {
        onBaseEntityEvent(softwareModuleEvent);
    }

    @Override
    protected Boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateDistributionPermission();
    }

    @Override
    protected String getTabSheetId() {
        return null;
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return UIComponentIdProvider.TARGET_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected void populateMetadataDetails() {
        swmMetadataTable.populateSMMetadata(getSelectedBaseEntity());
    }

    private boolean isSoftwareModuleSelected(final SoftwareModule softwareModule) {
        final SoftwareModule selectedUploadSWModule = artifactUploadState.getSelectedBaseSoftwareModule().orElse(null);
        return softwareModule != null && selectedUploadSWModule != null
                && selectedUploadSWModule.getName().equals(softwareModule.getName())
                && selectedUploadSWModule.getVersion().equals(softwareModule.getVersion());
    }

    @Override
    protected Boolean isMetadataIconToBeDisplayed() {
        return true;
    }

    @Override
    protected void showMetadata(final ClickEvent event) {
        final SoftwareModule swmodule = softwareManagement.findSoftwareModuleById(getSelectedBaseEntityId());
        /* display the window */
        UI.getCurrent().addWindow(swMetadataPopupLayout.getWindow(swmodule, null));
    }

}
