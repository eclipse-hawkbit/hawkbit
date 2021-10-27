/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.springframework.util.CollectionUtils;

/**
 * Support for un-assigning the {@link ProxyTarget} items from a {@link ProxyTargetType}.
 *
 */
public class TargetsToNoTargetTypeAssignmentSupport extends AbstractTargetsToTargetTypeAssignmentSupport {
    private final TargetManagement targetManagement;

    private static final String CAPTION_TYPE = "caption.type";
    private static final String CAPTION_TARGET = "caption.target";

    /**
     * Constructor for TargetsToTargetTypeAssignmentSupport
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     */
    public TargetsToNoTargetTypeAssignmentSupport(final CommonUiDependencies uiDependencies,
            final TargetManagement targetManagement) {
        super(uiDependencies);
        this.targetManagement = targetManagement;
    }

    @Override
    protected void performAssignment(List<ProxyTarget> sourceItemsToAssign, ProxyTargetType targetItem) {
        if (hasRequiredPermissions()) {
            final AbstractAssignmentResult<Target> typesAssignmentResult = initiateTargetTypeUnAssignment(
                    sourceItemsToAssign);

            final String assignmentMsg = createAssignmentMessage(typesAssignmentResult, i18n.getMessage(CAPTION_TARGET),
                    i18n.getMessage(CAPTION_TYPE), "");
            notification.displaySuccess(assignmentMsg);

            publishTypeAssignmentEvent(sourceItemsToAssign);
        }
    }

    /**
     *
     * @return false if required permissions are missing
     */
    private boolean hasRequiredPermissions() {
        final List<String> requiredPermissions = getMissingPermissionsForDrop();
        if (!CollectionUtils.isEmpty(requiredPermissions)) {
            notification
                    .displayValidationError(i18n.getMessage(UIMessageIdProvider.MESSAGE_ERROR_PERMISSION_INSUFFICIENT, requiredPermissions));
            return false;
        }
        return true;
    }

    protected AbstractAssignmentResult<Target> initiateTargetTypeUnAssignment(final List<ProxyTarget> sourceItems) {
        final Collection<String> controllerIdsToAssign = sourceItems.stream().map(ProxyTarget::getControllerId)
                .collect(Collectors.toList());

        return targetManagement.unAssignType(controllerIdsToAssign);
    }

}
