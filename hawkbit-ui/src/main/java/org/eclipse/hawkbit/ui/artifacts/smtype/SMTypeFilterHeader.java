/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Software module type filter buttons header.
 */
public class SMTypeFilterHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = -4855810338059032342L;

    private final ArtifactUploadState artifactUploadState;
    private final CreateUpdateSoftwareTypeLayout createUpdateSWTypeLayout;

    SMTypeFilterHeader(final VaadinMessageSource i18n, final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final ArtifactUploadState artifactUploadState, final EntityFactory entityFactory,
            final UINotification uiNotification, final SoftwareModuleTypeManagement softwareModuletypeManagement) {
        super(permChecker, eventBus, i18n);
        this.artifactUploadState = artifactUploadState;
        this.createUpdateSWTypeLayout = new CreateUpdateSoftwareTypeLayout(i18n, entityFactory, eventBus, permChecker,
                uiNotification, softwareModuletypeManagement);

        if (permChecker.hasCreateRepositoryPermission() || permChecker.hasUpdateRepositoryPermission()) {
            createUpdateSWTypeLayout.init();
        }
    }

    @Override
    protected boolean hasCreateUpdatePermission() {
        return permChecker.hasCreateRepositoryPermission() || permChecker.hasUpdateRepositoryPermission();
    }

    @Override
    protected String getTitle() {
        return SPUILabelDefinitions.TYPE;
    }

    @Override
    protected void settingsIconClicked(final ClickEvent event) {
        final Window addUpdateWindow = createUpdateSWTypeLayout.getWindow();
        UI.getCurrent().addWindow(addUpdateWindow);
        addUpdateWindow.setVisible(Boolean.TRUE);
    }

    @Override
    protected boolean dropHitsRequired() {
        return false;
    }

    @Override
    protected void hideFilterButtonLayout() {
        artifactUploadState.setSwTypeFilterClosed(true);
        eventBus.publish(this, UploadArtifactUIEvent.HIDE_FILTER_BY_TYPE);
    }

    @Override
    protected String getConfigureFilterButtonId() {
        return SPUIDefinitions.ADD_SOFTWARE_MODULE_TYPE;
    }

    @Override
    protected String getHideButtonId() {
        return UIComponentIdProvider.SM_SHOW_FILTER_BUTTON_ID;
    }

    @Override
    protected boolean isAddTagRequired() {
        return true;
    }
}
