/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Implementation of software module Layout on the Distribution View
 */
public class SwModuleTableLayout extends AbstractTableLayout<SwModuleTable> {

    private static final long serialVersionUID = 1L;

    private final SwModuleTable swModuleTable;

    public SwModuleTableLayout(final VaadinMessageSource i18n, final UINotification uiNotification,
            final UIEventBus eventBus, final SoftwareManagement softwareManagement, final EntityFactory entityFactory,
            final ManageDistUIState manageDistUIState, final SpPermissionChecker permChecker,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final ArtifactUploadState artifactUploadState, final ArtifactManagement artifactManagement) {

        final SwMetadataPopupLayout swMetadataPopupLayout = new SwMetadataPopupLayout(i18n, uiNotification, eventBus,
                softwareManagement, entityFactory, permChecker);

        this.swModuleTable = new SwModuleTable(eventBus, i18n, uiNotification, manageDistUIState, softwareManagement,
                distributionsViewClientCriterion, artifactManagement, swMetadataPopupLayout, artifactUploadState);

        final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow = new SoftwareModuleAddUpdateWindow(i18n,
                uiNotification, eventBus, softwareManagement, entityFactory, swModuleTable);
        super.init(
                new SwModuleTableHeader(i18n, permChecker, eventBus, manageDistUIState, softwareModuleAddUpdateWindow),
                swModuleTable, new SwModuleDetails(i18n, eventBus, permChecker, softwareModuleAddUpdateWindow,
                        manageDistUIState, softwareManagement, swMetadataPopupLayout, entityFactory));
    }

    public SwModuleTable getSwModuleTable() {
        return swModuleTable;
    }

}
