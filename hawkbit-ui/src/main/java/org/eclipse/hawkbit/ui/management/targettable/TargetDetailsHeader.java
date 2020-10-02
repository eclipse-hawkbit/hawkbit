/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractDetailsHeader;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Header for target details
 */
public class TargetDetailsHeader extends AbstractDetailsHeader<ProxyTarget> {
    private static final long serialVersionUID = 1L;

    private final transient TargetWindowBuilder targetWindowBuilder;
    private final transient TargetMetaDataWindowBuilder targetMetaDataWindowBuilder;

    /**
     * Constructor for TargetDetailsHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetWindowBuilder
     *            TargetWindowBuilder
     * @param targetMetaDataWindowBuilder
     *            TargetMetaDataWindowBuilder
     */
    public TargetDetailsHeader(final CommonUiDependencies uiDependencies, final TargetWindowBuilder targetWindowBuilder,
            final TargetMetaDataWindowBuilder targetMetaDataWindowBuilder) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus(), uiDependencies.getUiNotification());

        this.targetWindowBuilder = targetWindowBuilder;
        this.targetMetaDataWindowBuilder = targetMetaDataWindowBuilder;

        buildHeader();
    }

    @Override
    protected String getMasterEntityType() {
        return i18n.getMessage("target.details.header");
    }

    @Override
    protected String getMasterEntityDetailsCaptionId() {
        return UIComponentIdProvider.TARGET_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected String getMasterEntityName(final ProxyTarget masterEntity) {
        return masterEntity.getName();
    }

    @Override
    protected boolean hasEditPermission() {
        return permChecker.hasUpdateTargetPermission();
    }

    @Override
    protected String getEditIconId() {
        return UIComponentIdProvider.TARGET_EDIT_ICON;
    }

    @Override
    protected void onEdit() {
        if (selectedEntity == null) {
            return;
        }

        final Window updateWindow = targetWindowBuilder.getWindowForUpdate(selectedEntity);

        updateWindow.setCaption(i18n.getMessage("caption.update", i18n.getMessage("caption.target")));
        UI.getCurrent().addWindow(updateWindow);
        updateWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getMetaDataIconId() {
        return UIComponentIdProvider.TARGET_METADATA_BUTTON;
    }

    @Override
    protected void showMetaData() {
        if (selectedEntity == null) {
            return;
        }

        final Window metaDataWindow = targetMetaDataWindowBuilder
                .getWindowForShowTargetMetaData(selectedEntity.getControllerId(), selectedEntity.getName());

        UI.getCurrent().addWindow(metaDataWindow);
        metaDataWindow.setVisible(Boolean.TRUE);
    }
}
