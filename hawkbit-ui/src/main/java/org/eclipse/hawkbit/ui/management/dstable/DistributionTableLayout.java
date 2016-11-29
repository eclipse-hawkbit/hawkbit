/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.common.table.AbstractTableLayout;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetadataPopupLayout;
import org.eclipse.hawkbit.ui.management.event.ManagementViewAcceptCriteria;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Software module table layout.
 */
public class DistributionTableLayout extends AbstractTableLayout {

    private static final long serialVersionUID = 6464291374980641235L;

    public DistributionTableLayout(final I18N i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManagementUIState managementUIState,
            final DistributionSetManagement distributionSetManagement,
            final ManagementViewAcceptCriteria managementViewAcceptCriteria, final EntityFactory entityFactory,
            final UINotification notification, final TagManagement tagManagement,
            final TargetManagement targetService) {

        final DsMetadataPopupLayout dsMetadataPopupLayout = new DsMetadataPopupLayout(i18n, notification, eventBus,
                distributionSetManagement, entityFactory, permissionChecker);

        final DistributionTable distributionTable = new DistributionTable(eventBus, i18n, permissionChecker,
                notification, managementUIState, managementViewAcceptCriteria, targetService, dsMetadataPopupLayout,
                distributionSetManagement);

        super.init(new DistributionTableHeader(i18n, permissionChecker, eventBus, managementUIState, null),
                distributionTable,
                new DistributionDetails(i18n, eventBus, permissionChecker, managementUIState, distributionSetManagement,
                        dsMetadataPopupLayout, entityFactory, notification, tagManagement, null));
    }

}
