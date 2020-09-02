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
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.management.miscs.DeploymentAssignmentWindowController;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Support for assigning targets to distribution set.
 * 
 */
public class TargetsToDistributionSetAssignmentSupport
        extends DeploymentAssignmentSupport<ProxyTarget, ProxyDistributionSet> {
    private final SpPermissionChecker permChecker;

    private final DeploymentAssignmentWindowController assignmentController;

    /**
     * Constructor for TargetsToDistributionSetAssignmentSupport
     *
     * @param notification
     *          UINotification
     * @param i18n
     *          VaadinMessageSource
     * @param permChecker
     *          SpPermissionChecker
     * @param assignmentController
     *          DeploymentAssignmentWindowController
     */
    public TargetsToDistributionSetAssignmentSupport(final UINotification notification, final VaadinMessageSource i18n,
            final SpPermissionChecker permChecker, final DeploymentAssignmentWindowController assignmentController) {
        super(notification, i18n);

        this.permChecker = permChecker;
        this.assignmentController = assignmentController;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return permChecker.hasUpdateTargetPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_TARGET);
    }

    @Override
    protected void performAssignment(final List<ProxyTarget> sourceItemsToAssign,
            final ProxyDistributionSet targetItem) {
        assignmentController.populateWithData();

        final List<String> targetNames = sourceItemsToAssign.stream().map(ProxyTarget::getName)
                .collect(Collectors.toList());
        final ConfirmationDialog confirmAssignDialog = openConfirmationWindowForAssignments(targetNames,
                targetItem.getNameVersion(), assignmentController.getLayout(),
                () -> assignmentController.isMaintenanceWindowValid() && assignmentController.isForceTimeValid(),
                () -> assignmentController.assignTargetsToDistributions(sourceItemsToAssign,
                        Collections.singletonList(targetItem)));

        assignmentController.getLayout().addValidationListener(confirmAssignDialog::setOkButtonEnabled);
    }

    @Override
    protected String sourceEntityType() {
        return i18n.getMessage("caption.target");
    }

    @Override
    protected String targetEntityType() {
        return i18n.getMessage("distribution.details.header");
    }

    @Override
    protected String confirmationWindowId() {
        return UIComponentIdProvider.DIST_SET_TO_TARGET_ASSIGNMENT_CONFIRM_ID;
    }
}
