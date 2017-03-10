/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractNamedVersionedEntityTableDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.DistributionSetMetadatadetailslayout;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsTable;
import org.eclipse.hawkbit.ui.common.tagdetails.DistributionTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetadataPopupLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
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
 * Distribution set details layout.
 */
public class DistributionDetails extends AbstractNamedVersionedEntityTableDetailsLayout<DistributionSet> {

    private static final long serialVersionUID = 350360207334118826L;

    private final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout;
    private final DistributionTagToken distributionTagToken;
    private final transient DistributionSetManagement distributionSetManagement;
    private final DsMetadataPopupLayout dsMetadataPopupLayout;

    private final SoftwareModuleDetailsTable softwareModuleTable;

    private final DistributionSetMetadatadetailslayout dsMetadataTable;

    private final UINotification notificationMessage;

    DistributionDetails(final I18N i18n, final UIEventBus eventBus, final SpPermissionChecker permissionChecker,
            final ManagementUIState managementUIState, final DistributionSetManagement distributionSetManagement,
            final DsMetadataPopupLayout dsMetadataPopupLayout, final EntityFactory entityFactory,
            final UINotification notificationMessage, final TagManagement tagManagement,
            final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout) {
        super(i18n, eventBus, permissionChecker, managementUIState);

        this.distributionTagToken = new DistributionTagToken(permissionChecker, i18n, notificationMessage, eventBus,
                managementUIState, tagManagement, distributionSetManagement);
        this.distributionSetManagement = distributionSetManagement;
        this.dsMetadataPopupLayout = new DsMetadataPopupLayout(i18n, notificationMessage, eventBus,
                distributionSetManagement, entityFactory, permissionChecker);
        this.distributionAddUpdateWindowLayout = distributionAddUpdateWindowLayout;
        this.notificationMessage = notificationMessage;

        softwareModuleTable = new SoftwareModuleDetailsTable(i18n, false, permissionChecker, null, null, null,
                notificationMessage);

        dsMetadataTable = new DistributionSetMetadatadetailslayout(i18n, permissionChecker, distributionSetManagement,
                dsMetadataPopupLayout, entityFactory, notificationMessage);
        addTabs(detailsTab);
        restoreState();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    private void onEvent(final DistributionTableEvent distributionTableEvent) {
        onBaseEntityEvent(distributionTableEvent);
    }

    @Override
    protected String getDefaultCaption() {
        return getI18n().get("distribution.details.header");
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(createDetailsLayout(), getI18n().get("caption.tab.details"), null);
        detailsTab.addTab(createDescriptionLayout(), getI18n().get("caption.tab.description"), null);
        detailsTab.addTab(createSoftwareModuleTab(), getI18n().get("caption.softwares.distdetail.tab"), null);
        detailsTab.addTab(createTagsLayout(), getI18n().get("caption.tags.tab"), null);
        detailsTab.addTab(createLogLayout(), getI18n().get("caption.logs.tab"), null);
        detailsTab.addTab(dsMetadataTable, getI18n().get("caption.metadata"), null);
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow(getSelectedBaseEntityId());
        newDistWindow.setCaption(getI18n().get(UIComponentIdProvider.DIST_UPDATE_CAPTION));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getEditButtonId() {
        return UIComponentIdProvider.DS_EDIT_BUTTON;
    }

    @Override
    protected boolean onLoadIsTableRowSelected() {
        return managementUIState.getSelectedDsIdName().map(selected -> !selected.isEmpty()).orElse(false);
    }

    @Override
    protected boolean onLoadIsTableMaximized() {
        return managementUIState.isDsTableMaximized();
    }

    @Override
    protected boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateDistributionPermission();
    }

    @Override
    protected String getTabSheetId() {
        return UIComponentIdProvider.DISTRIBUTIONSET_DETAILS_TABSHEET_ID;
    }

    @Override
    protected void populateDetailsWidget() {
        softwareModuleTable.populateModule(getSelectedBaseEntity());
        populateDetails(getSelectedBaseEntity());
        populateMetadataDetails();

    }

    @Override
    protected void populateMetadataDetails() {
        dsMetadataTable.populateDSMetadata(getSelectedBaseEntity());
    }

    private void populateDetails(final DistributionSet ds) {
        if (ds != null) {
            updateDistributionDetailsLayout(ds.getType().getName(), ds.isRequiredMigrationStep());
        } else {
            updateDistributionDetailsLayout(null, null);
        }
    }

    private void updateDistributionDetailsLayout(final String type, final Boolean isMigrationRequired) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        if (type != null) {
            final Label typeLabel = SPUIComponentProvider.createNameValueLabel(getI18n().get("label.dist.details.type"),
                    type);
            typeLabel.setId(UIComponentIdProvider.DETAILS_TYPE_LABEL_ID);
            detailsTabLayout.addComponent(typeLabel);
        }

        if (isMigrationRequired != null) {
            detailsTabLayout.addComponent(SPUIComponentProvider.createNameValueLabel(
                    getI18n().get("checkbox.dist.migration.required"),
                    isMigrationRequired.equals(Boolean.TRUE) ? getI18n().get("label.yes") : getI18n().get("label.no")));
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

    @Override
    protected String getDetailsHeaderCaptionId() {
        return UIComponentIdProvider.DISTRIBUTION_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected boolean isMetadataIconToBeDisplayed() {
        return true;
    }

    @Override
    protected void showMetadata(final ClickEvent event) {
        final Optional<DistributionSet> ds = distributionSetManagement
                .findDistributionSetById(getSelectedBaseEntityId());
        if (!ds.isPresent()) {
            notificationMessage.displayWarning(getI18n().get("distributionset.not.exists"));
            return;
        }
        UI.getCurrent().addWindow(dsMetadataPopupLayout.getWindow(ds.get(), null));
    }

}
