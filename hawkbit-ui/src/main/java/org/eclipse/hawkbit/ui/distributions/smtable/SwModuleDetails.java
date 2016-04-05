/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractTableDetailsLayout;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SwModuleDetails extends AbstractTableDetailsLayout {

    private static final long serialVersionUID = -1052279281066089812L;

    @Autowired
    private SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow;

    @Autowired
    private ManageDistUIState manageDistUIState;

    private Long swModuleId;

    private SoftwareModule selectedSwModule;

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent softwareModuleEvent) {
        if (softwareModuleEvent.getSoftwareModuleEventType() == SoftwareModuleEventType.SELECTED_SOFTWARE_MODULE
                || softwareModuleEvent
                        .getSoftwareModuleEventType() == SoftwareModuleEventType.UPDATED_SOFTWARE_MODULE) {
            ui.access(() -> {
                /**
                 * softwareModuleEvent.getSoftwareModule() is null when table
                 * has no data.
                 */
                if (softwareModuleEvent.getSoftwareModule() != null) {
                    selectedSwModule = softwareModuleEvent.getSoftwareModule();
                    populateData(true);
                } else {
                    populateData(false);
                }
            });
        } else if (softwareModuleEvent.getSoftwareModuleEventType() == SoftwareModuleEventType.MINIMIZED) {
            showLayout();
        } else if (softwareModuleEvent.getSoftwareModuleEventType() == SoftwareModuleEventType.MAXIMIZED) {
            hideLayout();
        }
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        final Window addSoftwareModule = softwareModuleAddUpdateWindow.createUpdateSoftwareModuleWindow(swModuleId);
        addSoftwareModule.setCaption(i18n.get("upload.caption.update.swmodule"));
        UI.getCurrent().addWindow(addSoftwareModule);
        addSoftwareModule.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getEditButtonId() {
        return SPUIComponetIdProvider.UPLOAD_SW_MODULE_EDIT_BUTTON;
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), i18n.get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), i18n.get("caption.tab.description"), null);
        detailsTab.addTab(createLogLayout(), i18n.get("caption.logs.tab"), null);
    }

    @Override
    protected String getDefaultCaption() {
        return i18n.get("upload.swModuleTable.header");
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
    protected void populateDetailsWidget() {
        populateDetailsWidget(selectedSwModule);
    }

    @Override
    protected void clearDetails() {
        populateDetailsWidget(null);
    }

    @Override
    protected Boolean hasEditPermission() {
        return permissionChecker.hasUpdateDistributionPermission();
    }

    @Override
    protected String getTabSheetId() {
        return null;
    }

    private void populateDetails(final SoftwareModule swModule) {
        String maxAssign = HawkbitCommonUtil.SP_STRING_EMPTY;
        if (swModule != null) {
            if (swModule.getType().getMaxAssignments() == Integer.MAX_VALUE) {
                maxAssign = i18n.get("label.multiAssign.type");
            } else {
                maxAssign = i18n.get("label.singleAssign.type");
            }
            updateSwModuleDetailsLayout(swModule.getType().getName(), swModule.getVendor(), maxAssign);
        } else {
            updateSwModuleDetailsLayout(HawkbitCommonUtil.SP_STRING_EMPTY, HawkbitCommonUtil.SP_STRING_EMPTY,
                    maxAssign);
        }
    }

    private void populateDescription(final SoftwareModule sw) {
        if (sw != null) {
            updateDescriptionLayout(i18n.get("label.description"), sw.getDescription());
        } else {
            updateDescriptionLayout(i18n.get("label.description"), null);
        }
    }

    private void updateSwModuleDetailsLayout(final String type, final String vendor, final String maxAssign) {

        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        final Label vendorLabel = SPUIComponentProvider.createNameValueLabel(i18n.get("label.dist.details.vendor"),
                HawkbitCommonUtil.trimAndNullIfEmpty(vendor) == null ? "" : vendor);
        vendorLabel.setId(SPUIComponetIdProvider.DETAILS_VENDOR_LABEL_ID);
        detailsTabLayout.addComponent(vendorLabel);

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(i18n.get("label.dist.details.type"),
                    type);
            typeLabel.setId(SPUIComponetIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        final Label assignLabel = SPUIComponentProvider.createNameValueLabel(i18n.get("label.assigned.type"),
                HawkbitCommonUtil.trimAndNullIfEmpty(maxAssign) == null ? "" : maxAssign);
        assignLabel.setId(SPUIComponetIdProvider.SWM_DTLS_MAX_ASSIGN);
        detailsTabLayout.addComponent(assignLabel);

    }

    private void populateLog(final SoftwareModule softwareModule) {
        if (null != softwareModule) {
            updateLogLayout(getLogLayout(), softwareModule.getLastModifiedAt(), softwareModule.getLastModifiedBy(),
                    softwareModule.getCreatedAt(), softwareModule.getCreatedBy(), i18n);
        } else {
            updateLogLayout(getLogLayout(), null, HawkbitCommonUtil.SP_STRING_EMPTY, null, null, i18n);
        }
    }

    public void setSwModuleId(final Long swModuleId) {
        this.swModuleId = swModuleId;
    }

    private void populateDetailsWidget(final SoftwareModule swModule) {
        if (swModule != null) {
            setSwModuleId(swModule.getId());
            setName(getDefaultCaption(),
                    HawkbitCommonUtil.getFormattedNameVersion(swModule.getName(), swModule.getVersion()));
            populateDetails(swModule);
            populateDescription(swModule);
            populateLog(swModule);
        } else {
            setSwModuleId(null);
            setName(getDefaultCaption(), HawkbitCommonUtil.SP_STRING_EMPTY);
            populateDetails(null);
            populateDescription(null);
            populateLog(null);
        }
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return SPUIComponetIdProvider.TARGET_DETAILS_HEADER_LABEL_ID;
    }

}
