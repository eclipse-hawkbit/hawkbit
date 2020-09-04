/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.management.miscs.DeploymentAssignmentWindowController;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Support for assigning the distribution sets to target.
 * 
 */
public class DistributionSetsToTargetAssignmentSupport
        extends DeploymentAssignmentSupport<ProxyDistributionSet, ProxyTarget> {
    private final SystemSecurityContext systemSecurityContext;
    private final TenantConfigurationManagement configManagement;
    private final SpPermissionChecker permChecker;

    private final DeploymentAssignmentWindowController assignmentController;

    /**
     * Constructor for DistributionSetsToTargetAssignmentSupport
     *
     * @param notification
     *            UINotification
     * @param i18n
     *            VaadinMessageSource
     * @param systemSecurityContext
     *            SystemSecurityContext
     * @param configManagement
     *            TenantConfigurationManagement
     * @param permChecker
     *            SpPermissionChecker
     * @param assignmentController
     *            DeploymentAssignmentWindowController
     */
    public DistributionSetsToTargetAssignmentSupport(final UINotification notification, final VaadinMessageSource i18n,
            final SystemSecurityContext systemSecurityContext, final TenantConfigurationManagement configManagement,
            final SpPermissionChecker permChecker, final DeploymentAssignmentWindowController assignmentController) {
        super(notification, i18n);

        this.systemSecurityContext = systemSecurityContext;
        this.configManagement = configManagement;
        this.permChecker = permChecker;
        this.assignmentController = assignmentController;
    }

    @Override
    protected List<ProxyDistributionSet> getFilteredSourceItems(final List<ProxyDistributionSet> sourceItemsToAssign,
            final ProxyTarget targetItem) {
        return isMultiAssignmentEnabled() ? sourceItemsToAssign : Collections.singletonList(sourceItemsToAssign.get(0));
    }

    private boolean isMultiAssignmentEnabled() {
        return systemSecurityContext.runAsSystem(() -> configManagement
                .getConfigurationValue(TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue());
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return permChecker.hasUpdateTargetPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_TARGET);
    }

    @Override
    protected void performAssignment(final List<ProxyDistributionSet> sourceItemsToAssign,
            final ProxyTarget targetItem) {
        assignmentController.populateWithData();

        final List<String> dsNames = sourceItemsToAssign.stream().map(ProxyDistributionSet::getNameVersion)
                .collect(Collectors.toList());
        final ConfirmationDialog confirmAssignDialog = openConfirmationWindowForAssignments(dsNames,
                targetItem.getName(), assignmentController.getLayout(),
                () -> assignmentController.isMaintenanceWindowValid() && assignmentController.isForceTimeValid(),
                () -> assignmentController.assignTargetsToDistributions(Collections.singletonList(targetItem),
                        sourceItemsToAssign));

        assignmentController.getLayout().addValidationListener(confirmAssignDialog::setOkButtonEnabled);
    }

    @Override
    protected String sourceEntityTypeSing() {
        return i18n.getMessage("distribution.details.header");
    }

    @Override
    protected String sourceEntityTypePlur() {
        return i18n.getMessage("caption.distributionsets");
    }

    @Override
    protected String targetEntityType() {
        return i18n.getMessage("caption.target");
    }

    @Override
    protected String confirmationWindowId() {
        return UIComponentIdProvider.DIST_SET_TO_TARGET_ASSIGNMENT_CONFIRM_ID;
    }
}
