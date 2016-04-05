/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsTable;
import org.eclipse.hawkbit.ui.common.tagdetails.DistributionTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent.DistributionComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
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
 * Distribution set details layout.
 */
@SpringComponent
@ViewScope
public class DistributionDetails extends AbstractTableDetailsLayout {

    private static final long serialVersionUID = 350360207334118826L;

   
    @Autowired
    private ManagementUIState managementUIState;

    @Autowired
    private DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout;

    @Autowired
    private DistributionTagToken distributionTagToken;

    private SoftwareModuleDetailsTable softwareModuleTable;

    private Long dsId;

    private DistributionSet selectedDsModule;

    @Override
    protected void init() {
        softwareModuleTable = new SoftwareModuleDetailsTable();
        softwareModuleTable.init(i18n, false, permissionChecker, null, null, null);
        super.init();
    }


    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.ON_VALUE_CHANGE
                || distributionTableEvent
                        .getDistributionComponentEvent() == DistributionComponentEvent.EDIT_DISTRIBUTION) {
            ui.access(() -> {
                /**
                 * distributionTableEvent.getDistributionSet() is null when
                 * table has no data.
                 */
                if (distributionTableEvent.getDistributionSet() != null) {
                    selectedDsModule = distributionTableEvent.getDistributionSet();
                    populateData(true);
                } else {
                    populateData(false);
                }
            });
        } else if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.MINIMIZED) {
            ui.access(() -> showLayout());
        } else if (distributionTableEvent.getDistributionComponentEvent() == DistributionComponentEvent.MAXIMIZED) {
            ui.access(() -> hideLayout());
        }
    }

    @Override
    protected String getDefaultCaption() {
        return i18n.get("distribution.details.header");
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), i18n.get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), i18n.get("caption.tab.description"), null);
        detailsTab.addTab(createSoftwareModuleTab(), i18n.get("caption.softwares.distdetail.tab"), null);
        detailsTab.addTab(createTagsLayout(), i18n.get("caption.tags.tab"), null);
        detailsTab.addTab(createLogLayout(), i18n.get("caption.logs.tab"), null);
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow();
        distributionAddUpdateWindowLayout.populateValuesOfDistribution(getDsId());
        newDistWindow.setCaption(i18n.get("caption.update.dist"));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getEditButtonId() {
        return SPUIComponetIdProvider.DS_EDIT_BUTTON;
    }

    @Override
    protected Boolean onLoadIsTableRowSelected() {
        return !(managementUIState.getSelectedDsIdName().isPresent()
                && managementUIState.getSelectedDsIdName().get().isEmpty());
    }

    @Override
    protected Boolean onLoadIsTableMaximized() {
        return managementUIState.isDsTableMaximized();
    }

    @Override
    protected void populateDetailsWidget() {
        populateDetailsWidget(selectedDsModule);
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
        return SPUIComponetIdProvider.DISTRIBUTION_DETAILS_TABSHEET;
    }

    private void populateDetailsWidget(final DistributionSet dist) {
        if (dist != null) {
            setDsId(dist.getId());
            setName(getDefaultCaption(), HawkbitCommonUtil.getFormattedNameVersion(dist.getName(), dist.getVersion()));
            populateDetails(dist);
            populateDescription(dist);
            populateLog(dist);
            softwareModuleTable.populateModule(dist);
        } else {
            setDsId(null);
            setName(getDefaultCaption(), HawkbitCommonUtil.SP_STRING_EMPTY);
            populateDetails(null);
            populateDescription(null);
            softwareModuleTable.populateModule(null);
            populateLog(null);
        }
    }

    private void populateLog(final DistributionSet ds) {
        if (null != ds) {
            updateLogLayout(getLogLayout(), ds.getLastModifiedAt(), ds.getLastModifiedBy(), ds.getCreatedAt(),
                    ds.getCreatedBy(), i18n);
        } else {
            updateLogLayout(getLogLayout(), null, HawkbitCommonUtil.SP_STRING_EMPTY, null, null, i18n);
        }
    }

    private void populateDetails(final DistributionSet ds) {
        if (ds != null) {
            updateDistributionDetailsLayout(ds.getType().getName(), ds.isRequiredMigrationStep());
        } else {
            updateDistributionDetailsLayout(null, null);
        }
    }

    private void populateDescription(final DistributionSet ds) {
        if (ds != null) {
            updateDescriptionLayout(i18n.get("label.description"), ds.getDescription());
        } else {
            updateDescriptionLayout(i18n.get("label.description"), null);
        }
    }

    private void updateDistributionDetailsLayout(final String type, final Boolean isMigrationRequired) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(i18n.get("label.dist.details.type"),
                    type);
            typeLabel.setId(SPUIComponetIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        if (isMigrationRequired != null) {
            detailsTabLayout.addComponent(
                    SPUIComponentProvider.createNameValueLabel(i18n.get("checkbox.dist.migration.required"),
                            isMigrationRequired.equals(Boolean.TRUE) ? i18n.get("label.yes") : i18n.get("label.no")));
        }
    }

    private VerticalLayout createSoftwareModuleTab() {
        final VerticalLayout swLayout = getTabLayout();
        swLayout.setSizeFull();
        swLayout.addComponent(softwareModuleTable);
        return swLayout;
    }

    protected VerticalLayout createTagsLayout() {
        final VerticalLayout tagsLayout = getTabLayout();
        tagsLayout.addComponent(distributionTagToken.getTokenField());
        return tagsLayout;
    }

    public Long getDsId() {
        return dsId;
    }

    public void setDsId(final Long dsId) {
        this.dsId = dsId;
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return SPUIComponetIdProvider.DISTRIBUTION_DETAILS_HEADER_LABEL_ID;
    }

}
