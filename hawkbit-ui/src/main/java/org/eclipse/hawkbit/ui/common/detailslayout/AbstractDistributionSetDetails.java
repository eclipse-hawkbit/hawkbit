/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.tagdetails.DistributionTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetadataPopupLayout;
import org.eclipse.hawkbit.ui.management.dstable.DistributionAddUpdateWindowLayout;
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
 * Abstract class which contains common code for Distribution Set Details
 *
 */
public abstract class AbstractDistributionSetDetails
        extends AbstractNamedVersionedEntityTableDetailsLayout<DistributionSet> {

    private static final long serialVersionUID = 1L;

    private final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout;

    private final DistributionSetMetadatadetailslayout dsMetadataTable;

    private final UINotification uiNotification;

    private final transient DistributionSetManagement distributionSetManagement;

    private final DsMetadataPopupLayout dsMetadataPopupLayout;

    private final DistributionTagToken distributionTagToken;

    private SoftwareModuleDetailsTable softwareModuleTable;

    protected AbstractDistributionSetDetails(final I18N i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManagementUIState managementUIState,
            final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout,
            final DistributionSetManagement distributionSetManagement,
            final DsMetadataPopupLayout dsMetadataPopupLayout, final EntityFactory entityFactory,
            final UINotification uiNotification, final TagManagement tagManagement) {
        super(i18n, eventBus, permissionChecker, managementUIState);
        this.distributionAddUpdateWindowLayout = distributionAddUpdateWindowLayout;
        this.uiNotification = uiNotification;
        this.distributionSetManagement = distributionSetManagement;
        this.dsMetadataPopupLayout = dsMetadataPopupLayout;
        this.distributionTagToken = new DistributionTagToken(permissionChecker, i18n, uiNotification, eventBus,
                managementUIState, tagManagement, distributionSetManagement);

        dsMetadataTable = new DistributionSetMetadatadetailslayout(i18n, permissionChecker, distributionSetManagement,
                dsMetadataPopupLayout, entityFactory, uiNotification);
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow(getSelectedBaseEntityId());
        newDistWindow.setCaption(getI18n().get(UIComponentIdProvider.DIST_UPDATE_CAPTION));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        onBaseEntityEvent(distributionTableEvent);
    }

    @Override
    protected String getDefaultCaption() {
        return getI18n().get("distribution.details.header");
    }

    @Override
    protected String getEditButtonId() {
        return UIComponentIdProvider.DS_EDIT_BUTTON;
    }

    @Override
    protected Boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateDistributionPermission();
    }

    @Override
    protected String getTabSheetId() {
        return UIComponentIdProvider.DISTRIBUTIONSET_DETAILS_TABSHEET_ID;
    }

    @Override
    protected void populateMetadataDetails() {
        dsMetadataTable.populateDSMetadata(getSelectedBaseEntity());
    }

    @Override
    protected String getDetailsHeaderCaptionId() {
        return UIComponentIdProvider.DISTRIBUTION_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected Boolean isMetadataIconToBeDisplayed() {
        return true;
    }

    @Override
    protected void showMetadata(final ClickEvent event) {
        final Optional<DistributionSet> ds = distributionSetManagement
                .findDistributionSetById(getSelectedBaseEntityId());
        if (!ds.isPresent()) {
            uiNotification.displayWarning(getI18n().get("distributionset.not.exists"));
            return;
        }
        UI.getCurrent().addWindow(dsMetadataPopupLayout.getWindow(ds.get(), null));
    }

    @Override
    protected void addTabs(final TabSheet detailsTab) {
        detailsTab.addTab(getDetailsLayout(), getI18n().get("caption.tab.details"), null);
        detailsTab.addTab(getDescriptionLayout(), getI18n().get("caption.tab.description"), null);
        detailsTab.addTab(createSoftwareModuleTab(), getI18n().get("caption.softwares.distdetail.tab"), null);
        detailsTab.addTab(getTagsLayout(), getI18n().get("caption.tags.tab"), null);
        detailsTab.addTab(getLogLayout(), getI18n().get("caption.logs.tab"), null);
        detailsTab.addTab(dsMetadataTable, getI18n().get("caption.metadata"), null);
    }

    protected void populateDetails() {
        if (getSelectedBaseEntity() != null) {
            updateDistributionSetDetailsLayout(getSelectedBaseEntity().getType().getName(),
                    getSelectedBaseEntity().isRequiredMigrationStep());
        } else {
            updateDistributionSetDetailsLayout(null, null);
        }
    }

    protected void populateModule() {
        softwareModuleTable.populateModule(getSelectedBaseEntity());
    }

    private VerticalLayout createSoftwareModuleTab() {
        final VerticalLayout softwareLayout = createTabLayout();
        softwareLayout.setSizeFull();
        softwareLayout.addComponent(softwareModuleTable);
        return softwareLayout;
    }

    private void updateDistributionSetDetailsLayout(final String type, final Boolean isMigrationRequired) {
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

    protected SoftwareModuleDetailsTable getSoftwareModuleTable() {
        return softwareModuleTable;
    }

    protected void setSoftwareModuleTable(final SoftwareModuleDetailsTable softwareModuleTable) {
        this.softwareModuleTable = softwareModuleTable;
    }

    protected DistributionAddUpdateWindowLayout getDistributionAddUpdateWindowLayout() {
        return distributionAddUpdateWindowLayout;
    }

    protected UINotification getUiNotification() {
        return uiNotification;
    }

    protected DistributionSetManagement getDistributionSetManagement() {
        return distributionSetManagement;
    }

    protected DsMetadataPopupLayout getDsMetadataPopupLayout() {
        return dsMetadataPopupLayout;
    }

    protected DistributionTagToken getDistributionTagToken() {
        return distributionTagToken;
    }

}
