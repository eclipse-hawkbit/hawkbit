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
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractDistributionSetDetails;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsTable;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetadataPopupLayout;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Distribution set details layout.
 */
public class DistributionDetails extends AbstractDistributionSetDetails {

    private static final long serialVersionUID = 1L;

    DistributionDetails(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManagementUIState managementUIState,
            final DistributionSetManagement distributionSetManagement,
            final DsMetadataPopupLayout dsMetadataPopupLayout, final UINotification uiNotification,
            final DistributionSetTagManagement distributionSetTagManagement,
            final DistributionAddUpdateWindowLayout distributionAddUpdateWindowLayout) {
        super(i18n, eventBus, permissionChecker, managementUIState, distributionAddUpdateWindowLayout,
                distributionSetManagement, dsMetadataPopupLayout, uiNotification, distributionSetTagManagement,
                createSoftwareModuleDetailsTable(i18n, permissionChecker, uiNotification));
        restoreState();
    }

    private static final SoftwareModuleDetailsTable createSoftwareModuleDetailsTable(final VaadinMessageSource i18n,
            final SpPermissionChecker permissionChecker, final UINotification uiNotification) {
        return new SoftwareModuleDetailsTable(i18n, false, permissionChecker, null, null, null, uiNotification);
    }

    @Override
    protected boolean onLoadIsTableMaximized() {
        return getManagementUIState().isDsTableMaximized();
    }

    @Override
    protected void populateDetailsWidget() {
        populateModule();
        populateDetails();
        populateTags(getDistributionTagToken());
        populateMetadataDetails();
    }

}
