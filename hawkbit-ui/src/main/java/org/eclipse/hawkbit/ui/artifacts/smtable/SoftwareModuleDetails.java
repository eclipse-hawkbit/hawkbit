/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractSoftwareModuleDetails;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Software module details.
 */
public class SoftwareModuleDetails extends AbstractSoftwareModuleDetails {

    private static final long serialVersionUID = 1L;

    private final ArtifactUploadState artifactUploadState;

    SoftwareModuleDetails(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker,
            final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow,
            final ArtifactUploadState artifactUploadState, final SoftwareModuleManagement softwareManagement,
            final SwMetadataPopupLayout swMetadataPopupLayout) {
        super(i18n, eventBus, permissionChecker, null, softwareManagement, swMetadataPopupLayout,
                softwareModuleAddUpdateWindow);
        this.artifactUploadState = artifactUploadState;
        restoreState();
    }

    @Override
    protected boolean onLoadIsTableMaximized() {
        return artifactUploadState.isSwModuleTableMaximized();
    }

    @Override
    protected String getTabSheetId() {
        return null;
    }

    @Override
    protected boolean isSoftwareModuleSelected(final SoftwareModule softwareModule) {
        return compareSoftwareModulesById(softwareModule, artifactUploadState.getSelectedBaseSwModuleId().orElse(null));
    }

}
