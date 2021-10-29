/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.artifacts.smtable.SmMetaDataWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.providers.SmMetaDataDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyKeyValueDetails;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Abstract class which contains common code for Software Module Details
 *
 */
public class SoftwareModuleDetails extends AbstractGridDetailsLayout<ProxySoftwareModule> {
    private static final long serialVersionUID = 1L;

    private static final String SM_PREFIX = "sm.";

    private final MetadataDetailsGrid<Long> smMetadataGrid;

    private final transient SmMetaDataWindowBuilder smMetaDataWindowBuilder;
    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    /**
     * Constructor for SoftwareModuleDetails
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param softwareManagement
     *            SoftwareModuleManagement
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param smMetaDataWindowBuilder
     *            SmMetaDataWindowBuilder
     */
    public SoftwareModuleDetails(final CommonUiDependencies uiDependencies,
            final SoftwareModuleManagement softwareManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final SmMetaDataWindowBuilder smMetaDataWindowBuilder) {
        super(uiDependencies.getI18n());

        this.smMetaDataWindowBuilder = smMetaDataWindowBuilder;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;

        this.smMetadataGrid = new MetadataDetailsGrid<>(i18n, uiDependencies.getEventBus(),
                UIComponentIdProvider.SW_TYPE_PREFIX, this::showMetadataDetails,
                new SmMetaDataDataProvider(softwareManagement));

        addDetailsComponents(Arrays.asList(new SimpleEntry<>(i18n.getMessage("caption.tab.details"), entityDetails),
                new SimpleEntry<>(i18n.getMessage("caption.tab.description"), entityDescription),
                new SimpleEntry<>(i18n.getMessage("caption.logs.tab"), logDetails),
                new SimpleEntry<>(i18n.getMessage("caption.metadata"), smMetadataGrid)));
    }

    @Override
    protected String getTabSheetId() {
        return UIComponentIdProvider.DIST_SW_MODULE_DETAILS_TABSHEET_ID;
    }

    @Override
    protected List<ProxyKeyValueDetails> getEntityDetails(final ProxySoftwareModule entity) {
        final List<ProxyKeyValueDetails> details = new ArrayList<>();
        details.add(new ProxyKeyValueDetails(UIComponentIdProvider.DETAILS_VENDOR_LABEL_ID,
                i18n.getMessage("label.vendor"), entity.getVendor()));

        final Optional<SoftwareModuleType> smType = softwareModuleTypeManagement.get(entity.getTypeInfo().getId());
        smType.ifPresent(type -> {
            details.add(new ProxyKeyValueDetails(UIComponentIdProvider.DETAILS_TYPE_LABEL_ID,
                    i18n.getMessage("label.type"), type.getName()));
            details.add(new ProxyKeyValueDetails(UIComponentIdProvider.SWM_DTLS_MAX_ASSIGN,
                    i18n.getMessage("label.assigned.type"),
                    type.getMaxAssignments() == 1 ? i18n.getMessage("label.singleAssign.type")
                            : i18n.getMessage("label.multiAssign.type")));
        });

        details.add(new ProxyKeyValueDetails(UIComponentIdProvider.SWM_DTLS_ENCRYPTION,
                i18n.getMessage("label.artifact.encryption"),
                entity.isEncrypted() ? i18n.getMessage("label.enabled") : i18n.getMessage("label.disabled")));

        return details;
    }

    @Override
    protected String getDetailsDescriptionId() {
        return UIComponentIdProvider.SM_DETAILS_DESCRIPTION_LABEL_ID;
    }

    @Override
    protected String getLogLabelIdPrefix() {
        return SM_PREFIX;
    }

    private void showMetadataDetails(final ProxyMetaData metadata) {
        if (binder.getBean() == null) {
            return;
        }

        final Window metaDataWindow = smMetaDataWindowBuilder.getWindowForShowSmMetaData(binder.getBean().getId(),
                binder.getBean().getNameAndVersion(), metadata);

        UI.getCurrent().addWindow(metaDataWindow);
        metaDataWindow.setVisible(Boolean.TRUE);
    }

    @Override
    public void masterEntityChanged(final ProxySoftwareModule entity) {
        super.masterEntityChanged(entity);

        smMetadataGrid.masterEntityChanged(entity != null ? entity.getId() : null);
    }
}
