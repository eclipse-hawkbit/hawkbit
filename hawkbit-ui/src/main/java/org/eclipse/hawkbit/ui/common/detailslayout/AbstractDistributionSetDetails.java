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
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.tagdetails.DistributionTagToken;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetadataPopupLayout;
import org.eclipse.hawkbit.ui.management.dstable.DistributionAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
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

    private final DistributionSetMetadataDetailsLayout dsMetadataTable;

    private final UINotification uiNotification;

    private final transient DistributionSetManagement distributionSetManagement;

    private final DsMetadataPopupLayout dsMetadataPopupLayout;

    private final DistributionTagToken distributionTagToken;

    private final SoftwareModuleDetailsTable softwareModuleDetailsTable;

    private VerticalLayout softwareModuleTab;

    protected AbstractDistributionSetDetails(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManagementUIState managementUIState,
            final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout,
            final DistributionSetManagement distributionSetManagement,
            final DsMetadataPopupLayout dsMetadataPopupLayout, final UINotification uiNotification,
            final DistributionSetTagManagement distributionSetTagManagement,
            final SoftwareModuleDetailsTable softwareModuleDetailsTable) {
        super(i18n, eventBus, permissionChecker, managementUIState);
        this.distributionAddUpdateWindowLayout = distributionAddUpdateWindowLayout;
        this.uiNotification = uiNotification;
        this.distributionSetManagement = distributionSetManagement;
        this.dsMetadataPopupLayout = dsMetadataPopupLayout;
        this.distributionTagToken = new DistributionTagToken(permissionChecker, i18n, uiNotification, eventBus,
                managementUIState, distributionSetTagManagement, distributionSetManagement);
        this.softwareModuleDetailsTable = softwareModuleDetailsTable;

        dsMetadataTable = new DistributionSetMetadataDetailsLayout(i18n, distributionSetManagement,
                dsMetadataPopupLayout);
        createSoftwareModuleTab();
        addDetailsTab();
    }

    @Override
    protected void onEdit(final ClickEvent event) {
        final Window newDistWindow = distributionAddUpdateWindowLayout.getWindow(getSelectedBaseEntityId());
        newDistWindow.setCaption(getI18n().getMessage(UIComponentIdProvider.DIST_UPDATE_CAPTION));
        UI.getCurrent().addWindow(newDistWindow);
        newDistWindow.setVisible(Boolean.TRUE);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionTableEvent distributionTableEvent) {
        onBaseEntityEvent(distributionTableEvent);
    }

    @Override
    protected String getDefaultCaption() {
        return getI18n().getMessage("distribution.details.header");
    }

    @Override
    protected String getEditButtonId() {
        return UIComponentIdProvider.DS_EDIT_BUTTON;
    }

    @Override
    protected boolean hasEditPermission() {
        return getPermissionChecker().hasUpdateRepositoryPermission();
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
    protected boolean isMetadataIconToBeDisplayed() {
        return true;
    }

    @Override
    protected void showMetadata(final ClickEvent event) {
        final Optional<DistributionSet> ds = distributionSetManagement.get(getSelectedBaseEntityId());
        if (!ds.isPresent()) {
            uiNotification.displayWarning(getI18n().getMessage("distributionset.not.exists"));
            return;
        }
        UI.getCurrent().addWindow(dsMetadataPopupLayout.getWindow(ds.get(), null));
    }

    private final void addDetailsTab() {
        getDetailsTab().addTab(getDetailsLayout(), getI18n().getMessage("caption.tab.details"), null);
        getDetailsTab().addTab(getDescriptionLayout(), getI18n().getMessage("caption.tab.description"), null);
        getDetailsTab().addTab(softwareModuleTab, getI18n().getMessage("caption.softwares.distdetail.tab"), null);
        getDetailsTab().addTab(getTagsLayout(), getI18n().getMessage("caption.tags.tab"), null);
        getDetailsTab().addTab(getLogLayout(), getI18n().getMessage("caption.logs.tab"), null);
        getDetailsTab().addTab(dsMetadataTable, getI18n().getMessage("caption.metadata"), null);
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
        softwareModuleDetailsTable.populateModule(getSelectedBaseEntity());
    }

    private final void createSoftwareModuleTab() {
        this.softwareModuleTab = createTabLayout();
        softwareModuleTab.setSizeFull();
        softwareModuleTab.addComponent(softwareModuleDetailsTable);
    }

    private void updateDistributionSetDetailsLayout(final String type, final Boolean isMigrationRequired) {
        final VerticalLayout detailsTabLayout = getDetailsLayout();
        detailsTabLayout.removeAllComponents();

        final Label typeLabel = SPUIComponentProvider
                .createNameValueLabel(getI18n().getMessage("label.dist.details.type"), type == null ? "" : type);
        typeLabel.setId(UIComponentIdProvider.DETAILS_TYPE_LABEL_ID);
        detailsTabLayout.addComponent(typeLabel);

        detailsTabLayout.addComponent(
                SPUIComponentProvider.createNameValueLabel(getI18n().getMessage("checkbox.dist.migration.required"),
                        getMigrationRequiredValue(isMigrationRequired)));
    }

    private String getMigrationRequiredValue(final Boolean isMigrationRequired) {
        if (isMigrationRequired == null) {
            return "";
        }
        return isMigrationRequired.equals(Boolean.TRUE) ? getI18n().getMessage("label.yes")
                : getI18n().getMessage("label.no");
    }

    protected SoftwareModuleDetailsTable getSoftwareModuleTable() {
        return softwareModuleDetailsTable;
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
