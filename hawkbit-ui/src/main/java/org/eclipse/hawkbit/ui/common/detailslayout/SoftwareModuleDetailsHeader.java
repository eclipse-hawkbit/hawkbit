/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsGrid;
import org.eclipse.hawkbit.ui.artifacts.smtable.SmMetaDataWindowBuilder;
import org.eclipse.hawkbit.ui.artifacts.smtable.SmWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractDetailsHeader;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Software module details header
 */
public class SoftwareModuleDetailsHeader extends AbstractDetailsHeader<ProxySoftwareModule> {
    private static final long serialVersionUID = 1L;

    private final transient SmWindowBuilder smWindowBuilder;
    private final transient SmMetaDataWindowBuilder smMetaDataWindowBuilder;
    private final transient CommonUiDependencies uiDependencies;

    private transient ArtifactDetailsHeaderSupport artifactDetailsHeaderSupport;
    private transient ArtifactManagement artifactManagement;
    private ArtifactDetailsGrid artifactDetailsGrid;

    /**
     * Constructor for SoftwareModuleDetailsHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smWindowBuilder
     *            SmWindowBuilder
     * @param smMetaDataWindowBuilder
     *            SmMetaDataWindowBuilder
     */
    public SoftwareModuleDetailsHeader(final CommonUiDependencies uiDependencies, final SmWindowBuilder smWindowBuilder,
            final SmMetaDataWindowBuilder smMetaDataWindowBuilder) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus(), uiDependencies.getUiNotification());
        this.uiDependencies = uiDependencies;

        this.smWindowBuilder = smWindowBuilder;
        this.smMetaDataWindowBuilder = smMetaDataWindowBuilder;
    }

    @Override
    protected String getMasterEntityType() {
        return i18n.getMessage("upload.swModuleTable.header");
    }

    @Override
    protected String getMasterEntityDetailsCaptionId() {
        return UIComponentIdProvider.SOFTWARE_MODULE_DETAILS_HEADER_LABEL_ID;
    }

    @Override
    protected String getMasterEntityName(final ProxySoftwareModule masterEntity) {
        return masterEntity.getNameAndVersion();
    }

    @Override
    public void masterEntityChanged(final ProxySoftwareModule entity) {
        super.masterEntityChanged(entity);

        if (artifactDetailsHeaderSupport != null) {
            if (entity == null) {
                artifactDetailsHeaderSupport.disableArtifactDetailsIcon();
            } else {
                artifactDetailsHeaderSupport.enableArtifactDetailsIcon();
                if (artifactDetailsGrid != null) {
                    artifactDetailsGrid.refreshAll();
                }
            }
        }
    }

    @Override
    protected String getEditIconId() {
        return UIComponentIdProvider.UPLOAD_SW_MODULE_EDIT_BUTTON;
    }

    @Override
    protected void onEdit() {
        if (selectedEntity == null) {
            return;
        }

        final Window updateWindow = smWindowBuilder.getWindowForUpdate(selectedEntity);

        updateWindow.setCaption(i18n.getMessage("caption.update", i18n.getMessage("caption.software.module")));
        UI.getCurrent().addWindow(updateWindow);
        updateWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected String getMetaDataIconId() {
        return UIComponentIdProvider.UPLOAD_SW_MODULE_METADATA_BUTTON;
    }

    @Override
    protected void showMetaData() {
        if (selectedEntity == null) {
            return;
        }

        final Window metaDataWindow = smMetaDataWindowBuilder.getWindowForShowSmMetaData(selectedEntity.getId(),
                selectedEntity.getNameAndVersion());

        UI.getCurrent().addWindow(metaDataWindow);
        metaDataWindow.setVisible(Boolean.TRUE);
    }

    /**
     * Add artifact details header support
     *
     * @param artifactManagement
     *            ArtifactManagement
     */
    public void addArtifactDetailsHeaderSupport(final ArtifactManagement artifactManagement) {
        if (artifactDetailsHeaderSupport == null) {
            this.artifactManagement = artifactManagement;

            artifactDetailsHeaderSupport = new ArtifactDetailsHeaderSupport(i18n,
                    UIComponentIdProvider.SW_MODULE_ARTIFACT_DETAILS_BUTTON, this::showArtifactDetailsWindow);
            addHeaderSupport(artifactDetailsHeaderSupport);
        }
    }

    /**
     * Show artifact detail window layout
     */
    private void showArtifactDetailsWindow() {
        if (selectedEntity == null) {
            return;
        }

        if (artifactDetailsGrid == null) {
            artifactDetailsGrid = new ArtifactDetailsGrid(uiDependencies, artifactManagement);
        }
        setInitialArtifactDetailsGridSize(artifactDetailsGrid);
        artifactDetailsGrid.getMasterEntitySupport().masterEntityChanged(selectedEntity);

        final Window artifactDtlsWindow = new Window();
        artifactDtlsWindow.setId(UIComponentIdProvider.SHOW_ARTIFACT_DETAILS_POPUP_ID);

        artifactDtlsWindow.setClosable(true);
        artifactDtlsWindow.setResizable(true);
        artifactDtlsWindow.setWindowMode(WindowMode.NORMAL);
        artifactDtlsWindow.setModal(true);
        artifactDtlsWindow.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        artifactDtlsWindow
                .setAssistivePrefix(i18n.getMessage(UIMessageIdProvider.CAPTION_ARTIFACT_DETAILS_OF) + " " + "<b>");
        artifactDtlsWindow.setCaptionAsHtml(false);
        artifactDtlsWindow.setCaption(selectedEntity.getNameAndVersion());
        artifactDtlsWindow.setAssistivePostfix("</b>");

        artifactDtlsWindow.addWindowModeChangeListener(event -> {
            if (event.getWindowMode() == WindowMode.MAXIMIZED) {
                artifactDtlsWindow.setSizeFull();
                artifactDetailsGrid.setSizeFull();

                artifactDetailsGrid.createMaximizedContent();
            } else {
                artifactDtlsWindow.setSizeUndefined();
                setInitialArtifactDetailsGridSize(artifactDetailsGrid);

                artifactDetailsGrid.createMinimizedContent();
            }
        });

        artifactDtlsWindow.addCloseListener(event -> artifactDetailsGrid = null);

        artifactDtlsWindow.setContent(artifactDetailsGrid);

        UI.getCurrent().addWindow(artifactDtlsWindow);
    }

    private static void setInitialArtifactDetailsGridSize(final ArtifactDetailsGrid artifactDetailsGrid) {
        artifactDetailsGrid.setWidth(700, Unit.PIXELS);
        artifactDetailsGrid.setHeight(500, Unit.PIXELS);
    }
}
