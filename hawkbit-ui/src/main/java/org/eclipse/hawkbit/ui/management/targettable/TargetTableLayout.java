/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.concurrent.Executor;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Target table layout.
 */
public class TargetTableLayout extends AbstractTableLayout<TargetTable> {

    private static final long serialVersionUID = 2248703121998709112L;

    private final transient EventBus.UIEventBus eventBus;

    private final TargetDetails targetDetails;

    private final TargetTableHeader targetTableHeader;

    public TargetTableLayout(final UIEventBus eventBus, final TargetTable targetTable,
            final TargetManagement targetManagement, final EntityFactory entityFactory, final VaadinMessageSource i18n,
            final UIEventBus eventbus, final UINotification notification, final ManagementUIState managementUIState,
            final ManagementViewClientCriterion managementViewClientCriterion,
            final DeploymentManagement deploymentManagement, final UiProperties uiproperties,
            final SpPermissionChecker permissionChecker, final UINotification uinotification,
            final TargetTagManagement tagManagement, final DistributionSetManagement distributionSetManagement,
            final Executor uiExecutor) {
        this.eventBus = eventBus;
        this.targetDetails = new TargetDetails(i18n, eventbus, permissionChecker, managementUIState, uinotification,
                tagManagement, targetManagement, deploymentManagement, entityFactory, targetTable);
        this.targetTableHeader = new TargetTableHeader(i18n, permissionChecker, eventBus, notification,
                managementUIState, managementViewClientCriterion, targetManagement, deploymentManagement, uiproperties,
                eventbus, entityFactory, uinotification, tagManagement, distributionSetManagement, uiExecutor,
                targetTable);

        super.init(targetTableHeader, targetTable, targetDetails);
    }

    @Override
    protected void publishEvent() {
        eventBus.publish(this, new TargetTableEvent(TargetComponentEvent.SELECT_ALL));
    }

}
