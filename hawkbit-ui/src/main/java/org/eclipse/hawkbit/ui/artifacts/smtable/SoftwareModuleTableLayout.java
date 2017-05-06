/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.eclipse.hawkbit.ui.dd.criteria.UploadViewClientCriterion;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Software module table layout. (Upload Management)
 */
public class SoftwareModuleTableLayout extends AbstractTableLayout<SoftwareModuleTable> {

    private static final long serialVersionUID = 1L;

    private final SoftwareModuleTable softwareModuleTable;

    public SoftwareModuleTableLayout(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final ArtifactUploadState artifactUploadState, final UINotification uiNotification,
            final UIEventBus eventBus, final SoftwareManagement softwareManagement, final EntityFactory entityFactory,
            final UploadViewClientCriterion uploadViewClientCriterion) {

        final SwMetadataPopupLayout swMetadataPopupLayout = new SwMetadataPopupLayout(i18n, uiNotification, eventBus,
                softwareManagement, entityFactory, permChecker);
        this.softwareModuleTable = new SoftwareModuleTable(eventBus, i18n, uiNotification, artifactUploadState,
                softwareManagement, uploadViewClientCriterion, swMetadataPopupLayout);

        final SoftwareModuleAddUpdateWindow softwareModuleAddUpdateWindow = new SoftwareModuleAddUpdateWindow(i18n,
                uiNotification, eventBus, softwareManagement, entityFactory, softwareModuleTable);

        super.init(
                new SoftwareModuleTableHeader(i18n, permChecker, eventBus, artifactUploadState,
                        softwareModuleAddUpdateWindow),
                softwareModuleTable,
                new SoftwareModuleDetails(i18n, eventBus, permChecker, softwareModuleAddUpdateWindow,
                        artifactUploadState, softwareManagement, swMetadataPopupLayout, entityFactory));
    }

    public SoftwareModuleTable getSoftwareModuleTable() {
        return softwareModuleTable;
    }

}
