/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Target table layout.
 */
public class TargetTableLayout extends AbstractTableLayout {

    private static final long serialVersionUID = 2248703121998709112L;

    private final transient EventBus.UIEventBus eventBus;

    private final TargetDetails targetDetails;

    private final TargetTableHeader targetTableHeader;

    public TargetTableLayout(final UIEventBus eventBus, final TargetTable targetTable,
            final TargetManagement targetManagement, final EntityFactory entityFactory, final I18N i18n,
            final UIEventBus eventbus, final UINotification notification, final ManagementUIState managementUIState,
            final ManagementViewAcceptCriteria managementViewAcceptCriteria,
            final DeploymentManagement deploymentManagement, final UiProperties uiproperties,
            final SpPermissionChecker permissionChecker, final UINotification uinotification,
            final TagManagement tagManagement) {
        this.eventBus = eventBus;
        this.targetDetails = new TargetDetails(i18n, eventbus, permissionChecker, managementUIState, uinotification,
                tagManagement, targetManagement, entityFactory, targetTable);
        this.targetTableHeader = new TargetTableHeader(i18n, permissionChecker, eventBus, notification,
                managementUIState, managementViewAcceptCriteria, targetManagement, deploymentManagement, uiproperties,
                eventbus, entityFactory, uinotification, tagManagement, targetTable);

        super.init(targetTableHeader, targetTable, targetDetails);
    }

    @Override
    protected void publishEvent() {

        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELECT_ALL));
    }

}
