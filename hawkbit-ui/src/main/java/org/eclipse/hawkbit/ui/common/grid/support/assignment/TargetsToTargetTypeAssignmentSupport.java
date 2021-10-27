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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

/**
 * Support for assigning the {@link ProxyTarget} items to {@link ProxyTargetType}.
 *
 */
public class TargetsToTargetTypeAssignmentSupport extends AbstractTargetsToTargetTypeAssignmentSupport {
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
    public TargetsToTargetTypeAssignmentSupport(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement) {
        super(uiDependencies);
        this.targetManagement = targetManagement;
    }

    @Override
    protected List<ProxyTarget> getFilteredSourceItems(List<ProxyTarget> sourceItemsToAssign, ProxyTargetType targetItem) {
        if (!isAssignmentValid(sourceItemsToAssign, targetItem)) {
            return Collections.emptyList();
        }

        return sourceItemsToAssign;
    }

    /**
     *
     * @param sourceItemsToAssign
     * @param targetItem
     * @return false if some targets already have a type assigned
     */
    private boolean isAssignmentValid(List<ProxyTarget> sourceItemsToAssign, ProxyTargetType targetItem) {
        if(sourceItemsToAssign.size() > 1) {
            List<ProxyTarget> targetsWithDifferentType = sourceItemsToAssign.stream().filter(
                            target -> target.getTypeInfo() != null && !target.getTypeInfo().getId().equals(targetItem.getId()))
                    .collect(Collectors.toList());

            if (!targetsWithDifferentType.isEmpty()) {
                notification.displayValidationError(i18n.getMessage(UIMessageIdProvider.MESSAGE_TARGET_TARGETTYPE_ASSIGNED));
                return false;
            }
        }
        return true;
    }

    @Override
    protected void performAssignment(final List<ProxyTarget> sourceItemsToAssign, final ProxyTargetType targetItem) {
        final Long typeId = targetItem.getId();

        final AbstractAssignmentResult<Target> typesAssignmentResult = initiateTargetTypeAssignment(sourceItemsToAssign,
                typeId);

        final String assignmentMsg = createAssignmentMessage(typesAssignmentResult,
                i18n.getMessage(CAPTION_TARGET),
                i18n.getMessage(CAPTION_TYPE), targetItem.getName());
        notification.displaySuccess(assignmentMsg);

        publishTypeAssignmentEvent(sourceItemsToAssign);
    }

    private AbstractAssignmentResult<Target> initiateTargetTypeAssignment(final List<ProxyTarget> sourceItems,
                                                                          final Long typeId) {
        final Collection<String> controllerIdsToAssign = sourceItems.stream().map(ProxyTarget::getControllerId)
                .collect(Collectors.toList());

        return targetManagement.assignType(controllerIdsToAssign, typeId);
    }

}
