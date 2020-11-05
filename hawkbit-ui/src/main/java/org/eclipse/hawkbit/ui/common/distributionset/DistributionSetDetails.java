/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.distributionset;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.providers.DsMetaDataDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyKeyValueDetails;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractGridDetailsLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.MetadataDetailsGrid;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsGrid;
import org.eclipse.hawkbit.ui.common.detailslayout.TargetFilterQueryDetailsGrid;
import org.eclipse.hawkbit.ui.common.tagdetails.DistributionTagToken;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Code for Distribution Set Details
 *
 */
public class DistributionSetDetails extends AbstractGridDetailsLayout<ProxyDistributionSet> {
    private static final long serialVersionUID = 1L;

    private static final String DS_PREFIX = "ds.";
    private final MetadataDetailsGrid<Long> dsMetadataGrid;
    private final SoftwareModuleDetailsGrid smDetailsGrid;
    private TargetFilterQueryDetailsGrid tfqDetailsGrid;

    private final transient DsMetaDataWindowBuilder dsMetaDataWindowBuilder;

    private final transient TenantConfigurationManagement tenantConfigurationManagement;
    private final transient SystemSecurityContext systemSecurityContext;
    private final transient SpPermissionChecker permissionChecker;

    private final transient DistributionTagToken distributionTagToken;

    /**
     * Constructor for DistributionSetDetails
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsManagement
     *            DistributionSetManagement
     * @param smManagement
     *            SoftwareModuleManagement
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param dsTagManagement
     *            DistributionSetTagManagement
     * @param tenantConfigurationManagement
     *            TenantConfigurationManagement
     * @param systemSecurityContext
     *            SystemSecurityContext
     * @param dsMetaDataWindowBuilder
     *            DsMetaDataWindowBuilder
     */
    public DistributionSetDetails(final CommonUiDependencies uiDependencies, final DistributionSetManagement dsManagement,
            final SoftwareModuleManagement smManagement, final DistributionSetTypeManagement dsTypeManagement,
            final DistributionSetTagManagement dsTagManagement,
            final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext, final DsMetaDataWindowBuilder dsMetaDataWindowBuilder) {
        super(uiDependencies.getI18n());

        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.permissionChecker = uiDependencies.getPermChecker();
        this.dsMetaDataWindowBuilder = dsMetaDataWindowBuilder;

        this.smDetailsGrid = new SoftwareModuleDetailsGrid(uiDependencies, dsManagement, smManagement, dsTypeManagement);

        this.distributionTagToken = new DistributionTagToken(uiDependencies, dsTagManagement, dsManagement);

        this.dsMetadataGrid = new MetadataDetailsGrid<>(uiDependencies.getI18n(), uiDependencies.getEventBus(),
                UIComponentIdProvider.DS_TYPE_PREFIX, this::showMetadataDetails,
                new DsMetaDataDataProvider(dsManagement));

        addDetailsComponents(Arrays.asList(new SimpleEntry<>(i18n.getMessage("caption.tab.details"), entityDetails),
                new SimpleEntry<>(i18n.getMessage("caption.tab.description"), entityDescription),
                new SimpleEntry<>(i18n.getMessage("caption.softwares.distdetail.tab"), smDetailsGrid),
                new SimpleEntry<>(i18n.getMessage("caption.tags.tab"), distributionTagToken.getTagPanel()),
                new SimpleEntry<>(i18n.getMessage("caption.logs.tab"), logDetails),
                new SimpleEntry<>(i18n.getMessage("caption.metadata"), dsMetadataGrid)));
    }

    @Override
    protected String getTabSheetId() {
        return UIComponentIdProvider.DISTRIBUTIONSET_DETAILS_TABSHEET_ID;
    }

    @Override
    protected List<ProxyKeyValueDetails> getEntityDetails(final ProxyDistributionSet entity) {
        final ProxyKeyValueDetails typeLabel = new ProxyKeyValueDetails(UIComponentIdProvider.DETAILS_TYPE_LABEL_ID,
                i18n.getMessage("label.type"), entity.getTypeInfo().getName());

        if (isMultiAssignmentEnabled()) {
            return Collections.singletonList(typeLabel);
        } else {
            return Arrays.asList(typeLabel,
                    new ProxyKeyValueDetails(UIComponentIdProvider.DETAILS_REQUIRED_MIGRATION_STEP_LABEL_ID,
                            i18n.getMessage("label.dist.required.migration.step"),
                            getMigrationRequiredValue(entity.isRequiredMigrationStep())));
        }
    }

    private boolean isMultiAssignmentEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue());
    }

    private String getMigrationRequiredValue(final Boolean isMigrationRequired) {
        if (isMigrationRequired == null) {
            return "";
        }
        return isMigrationRequired.equals(Boolean.TRUE) ? i18n.getMessage("label.yes") : i18n.getMessage("label.no");
    }

    @Override
    protected String getDetailsDescriptionId() {
        return UIComponentIdProvider.DS_DETAILS_DESCRIPTION_ID;
    }

    @Override
    protected String getLogLabelIdPrefix() {
        return DS_PREFIX;
    }

    private void showMetadataDetails(final ProxyMetaData metadata) {
        if (binder.getBean() == null) {
            return;
        }

        final Window metaDataWindow = dsMetaDataWindowBuilder.getWindowForShowDsMetaData(binder.getBean().getId(),
                binder.getBean().getNameVersion(), metadata);

        UI.getCurrent().addWindow(metaDataWindow);
        metaDataWindow.setVisible(Boolean.TRUE);
    }

    @Override
    public void masterEntityChanged(final ProxyDistributionSet entity) {
        super.masterEntityChanged(entity);

        dsMetadataGrid.masterEntityChanged(entity != null ? entity.getId() : null);
        smDetailsGrid.masterEntityChanged(entity);
        distributionTagToken.masterEntityChanged(entity);
        if (tfqDetailsGrid != null) {
            tfqDetailsGrid.masterEntityChanged(entity != null ? entity.getId() : null);
        }
    }

    /**
     * @param isUnassignSmAllowed
     *            <code>true</code> if unassigned software module is allowed,
     *            otherwise <code>false</code>
     */
    public void setUnassignSmAllowed(final boolean isUnassignSmAllowed) {
        smDetailsGrid.setUnassignSmAllowed(isUnassignSmAllowed);
    }

    /**
     * Add target filter query detail grid
     *
     * @param targetFilterQueryManagement
     *            TargetFilterQueryManagement
     */
    public void addTfqDetailsGrid(final TargetFilterQueryManagement targetFilterQueryManagement) {
        if (tfqDetailsGrid == null && permissionChecker.hasTargetReadPermission()) {
            tfqDetailsGrid = new TargetFilterQueryDetailsGrid(i18n, targetFilterQueryManagement);

            addDetailsComponents(Collections
                    .singletonList(new SimpleEntry<>(i18n.getMessage("caption.auto.assignment.ds"), tfqDetailsGrid)));
        }
    }

    /**
     * Gets the distributionTagToken
     *
     * @return distributionTagToken
     */
    public DistributionTagToken getDistributionTagToken() {
        return distributionTagToken;
    }
}
