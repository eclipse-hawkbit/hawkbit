/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.distributionset;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractDetailsHeader;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Distribution set detail header
 */
public class DistributionSetDetailsHeader extends AbstractDetailsHeader<ProxyDistributionSet> {
    private static final long serialVersionUID = 1L;

    private final transient DsWindowBuilder dsWindowBuilder;
    private final transient DsMetaDataWindowBuilder dsMetaDataWindowBuilder;

    /**
     * Constructor for DistributionSetDetailsHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsWindowBuilder
     *            DsWindowBuilder
     * @param dsMetaDataWindowBuilder
     *            DsMetaDataWindowBuilder
     */
    public DistributionSetDetailsHeader(final CommonUiDependencies uiDependencies,
            final DsWindowBuilder dsWindowBuilder, final DsMetaDataWindowBuilder dsMetaDataWindowBuilder) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus(),
                uiDependencies.getUiNotification());

        this.dsWindowBuilder = dsWindowBuilder;
        this.dsMetaDataWindowBuilder = dsMetaDataWindowBuilder;

        buildHeader();
    }

    @Override
    protected String getMasterEntityType() {
        return i18n.getMessage("distribution.details.header");
    }

    @Override
    protected String getMasterEntityDetailsCaptionId() {
        return UIComponentIdProvider.DISTRIBUTION_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected String getMasterEntityName(final ProxyDistributionSet masterEntity) {
        return masterEntity.getNameVersion();
    }

    @Override
    protected String getEditIconId() {
        return UIComponentIdProvider.DS_EDIT_BUTTON;
    }

    @Override
    protected boolean editSelectedEntityAllowed() {
        return selectedEntity != null && selectedEntity.getIsValid();
    }

    @Override
    protected void onEdit() {
        if (selectedEntity == null) {
            return;
        }

        final Window updateWindow = dsWindowBuilder.getWindowForUpdate(selectedEntity);

        updateWindow.setCaption(i18n.getMessage("caption.update", i18n.getMessage("caption.distribution")));
        UI.getCurrent().addWindow(updateWindow);
        updateWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getMetaDataIconId() {
        return UIComponentIdProvider.DS_METADATA_BUTTON;
    }

    @Override
    protected void showMetaData() {
        if (selectedEntity == null) {
            return;
        }

        final Window metaDataWindow = dsMetaDataWindowBuilder.getWindowForShowDsMetaData(selectedEntity.getId(),
                selectedEntity.getNameVersion());

        UI.getCurrent().addWindow(metaDataWindow);
        metaDataWindow.setVisible(Boolean.TRUE);
    }
}
