/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsLayout;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractSoftwareModuleDetails;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Implementation of software module details block using generic abstract
 * details style .
 */
public class SwModuleDetails extends AbstractSoftwareModuleDetails {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    private final ArtifactDetailsLayout artifactDetailsLayout;

    private Button artifactDetailsButton;

    SwModuleDetails(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker,
            final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow,
            final ManageDistUIState manageDistUIState, final SoftwareModuleManagement softwareManagement,
            final SwMetadataPopupLayout swMetadataPopupLayout, final ArtifactDetailsLayout artifactDetailsLayout) {
        super(i18n, eventBus, permissionChecker, null, softwareManagement, swMetadataPopupLayout,
                softwareModuleAddUpdateWindow);
        this.manageDistUIState = manageDistUIState;
        this.artifactDetailsLayout = artifactDetailsLayout;
        restoreState();
    }

    @Override
    protected boolean onLoadIsTableMaximized() {
        return manageDistUIState.isSwModuleTableMaximized();
    }

    @Override
    protected String getTabSheetId() {
        return UIComponentIdProvider.DIST_SW_MODULE_DETAILS_TABSHEET_ID;
    }

    @Override
    protected boolean isSoftwareModuleSelected(final SoftwareModule softwareModule) {
        return compareSoftwareModulesById(softwareModule,
                manageDistUIState.getLastSelectedSoftwareModule().orElse(null));
    }

    @Override
    protected void createComponents() {
        super.createComponents();
        createShowArtifactDetailsButton();
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getNameEditLayout().addComponent(artifactDetailsButton);
        getNameEditLayout().setComponentAlignment(artifactDetailsButton, Alignment.TOP_RIGHT);
    }

    private Button createShowArtifactDetailsButton() {
        artifactDetailsButton = SPUIComponentProvider.getButton("", "", "", null, false, FontAwesome.FILE_O,
                SPUIButtonStyleNoBorder.class);
        artifactDetailsButton.setDescription(getI18n().getMessage(UIMessageIdProvider.TOOLTIP_ARTIFACT_ICON));
        artifactDetailsButton.addClickListener(event -> showArtifactDetailsWindow(getSelectedBaseEntity()));
        return artifactDetailsButton;
    }

    private void showArtifactDetailsWindow(final SoftwareModule softwareModule) {
        final Window artifactDtlsWindow = new Window();
        artifactDtlsWindow.setCaption(HawkbitCommonUtil
                .getArtifactoryDetailsLabelId(softwareModule.getName() + "." + softwareModule.getVersion(), getI18n()));
        artifactDtlsWindow.setCaptionAsHtml(true);
        artifactDtlsWindow.setClosable(true);
        artifactDtlsWindow.setResizable(true);
        artifactDtlsWindow.setImmediate(true);
        artifactDtlsWindow.setWindowMode(WindowMode.NORMAL);
        artifactDtlsWindow.setModal(true);
        artifactDtlsWindow.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        artifactDetailsLayout.setFullWindowMode(false);
        artifactDetailsLayout.populateArtifactDetails(softwareModule);
        artifactDetailsLayout.getArtifactDetailsTable().setWidth(700, Unit.PIXELS);
        artifactDetailsLayout.getArtifactDetailsTable().setHeight(500, Unit.PIXELS);
        artifactDtlsWindow.setContent(artifactDetailsLayout.getArtifactDetailsTable());

        artifactDtlsWindow.addWindowModeChangeListener(event -> {
            if (event.getWindowMode() == WindowMode.MAXIMIZED) {
                artifactDtlsWindow.setSizeFull();
                artifactDetailsLayout.setFullWindowMode(true);
                artifactDetailsLayout.createMaxArtifactDetailsTable();
                artifactDetailsLayout.getMaxArtifactDetailsTable().setWidth(100, Unit.PERCENTAGE);
                artifactDetailsLayout.getMaxArtifactDetailsTable().setHeight(100, Unit.PERCENTAGE);
                artifactDtlsWindow.setContent(artifactDetailsLayout.getMaxArtifactDetailsTable());
            } else {
                artifactDtlsWindow.setSizeUndefined();
                artifactDtlsWindow.setContent(artifactDetailsLayout.getArtifactDetailsTable());
            }
        });
        UI.getCurrent().addWindow(artifactDtlsWindow);
    }

    @Override
    protected void populateData(final SoftwareModule selectedBaseEntity) {
        super.populateData(selectedBaseEntity);
        artifactDetailsButton.setEnabled(selectedBaseEntity != null);
    }

}
