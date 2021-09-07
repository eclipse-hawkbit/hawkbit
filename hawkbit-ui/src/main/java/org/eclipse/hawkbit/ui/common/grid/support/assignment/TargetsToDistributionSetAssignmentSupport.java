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
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.management.miscs.DeploymentAssignmentWindowController;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

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
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param assignmentController
     *            DeploymentAssignmentWindowController
     */
    public TargetsToDistributionSetAssignmentSupport(final CommonUiDependencies uiDependencies,
            final DeploymentAssignmentWindowController assignmentController) {
        super(uiDependencies.getUiNotification(), uiDependencies.getI18n());

        this.permChecker = uiDependencies.getPermChecker();
        this.assignmentController = assignmentController;
    }

    @Override
    public List<String> getMissingPermissionsForDrop() {
        return permChecker.hasUpdateTargetPermission() ? Collections.emptyList()
                : Collections.singletonList(SpPermission.UPDATE_TARGET);
    }

    @Override
    protected List<ProxyTarget> getFilteredSourceItems(final List<ProxyTarget> sourceItemsToAssign,
            final ProxyDistributionSet targetItem) {
        if (!isTargetDsValid(targetItem)) {
            return Collections.emptyList();
        }

        return sourceItemsToAssign;
    }

    private boolean isTargetDsValid(final ProxyDistributionSet distributionSet) {
        if (!distributionSet.getIsValid()) {
            addSpecificValidationErrorMessage(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_DISTRIBUTIONSET_INVALID,
                    distributionSet.getNameVersion()));
            return false;
        }
        return true;
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
    protected String sourceEntityTypeSing() {
        return i18n.getMessage("caption.target");
    }

    @Override
    protected String sourceEntityTypePlur() {
        return i18n.getMessage("caption.targets");
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
