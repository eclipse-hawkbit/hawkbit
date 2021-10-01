/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;

/**
 * Support for assigning the items between two grids.
 *
 * @param <S>
 *            The item-type of source items
 * @param <T>
 *            The item-type of target item
 */
public abstract class AssignmentSupport<S, T> {
    protected final UINotification notification;
    protected final VaadinMessageSource i18n;
    protected final List<String> lastSpecificErrorMessages = new ArrayList<>();

    protected AssignmentSupport(final UINotification notification, final VaadinMessageSource i18n) {
        this.notification = notification;
        this.i18n = i18n;
    }

    /**
     * only needed for drag and drop support
     *
     * @param sourceItemsToAssign
     *            Source item object
     * @param targetItem
     *            Generic type target item
     */
    public void assignSourceItemsToTargetItem(final Object sourceItemsToAssign, final T targetItem) {
        // sourceItemsToAssign are of type UnmodifiableRandomAccessList
        if (sourceItemsToAssign instanceof List) {
            assignSourceItemsToTargetItem((List<S>) sourceItemsToAssign, targetItem);
        } else {
            showErrorNotification();
        }
    }

    /**
     * Assign source items to target item
     *
     * @param sourceItemsToAssign
     *            List of selectd source items
     * @param targetItem
     *            Target item
     */
    public void assignSourceItemsToTargetItem(final List<S> sourceItemsToAssign, final T targetItem) {
        if (sourceItemsToAssign.isEmpty()) {
            showErrorNotification();
            return;
        }

        final List<S> filteredSourceItems = getFilteredSourceItems(sourceItemsToAssign, targetItem);
        if (filteredSourceItems.isEmpty()) {
            showErrorNotification();
            return;
        }

        performAssignment(filteredSourceItems, targetItem);
    }

    private void showErrorNotification() {
        if (lastSpecificErrorMessages.isEmpty()) {
            notification.displayValidationError(i18n.getMessage("message.action.did.not.work"));
        } else {
            lastSpecificErrorMessages.forEach(notification::displayValidationError);
            lastSpecificErrorMessages.clear();
        }
    }

    protected void addSpecificValidationErrorMessage(final String errorMessage) {
        lastSpecificErrorMessages.add(errorMessage);
    }

    /**
     * Can be overriden in child classes in order to filter source items list.
     *
     * @param targetItem
     *            may be used for further filtering of source items
     */
    protected List<S> getFilteredSourceItems(final List<S> sourceItemsToAssign, final T targetItem) {
        return sourceItemsToAssign;
    }

    protected final List<S> getFilteredSourceItems(final List<S> sourceItemsToAssign) {
        return getFilteredSourceItems(sourceItemsToAssign, null);
    }

    /**
     * @return List of missing required permission to drop the item
     */
    public abstract List<String> getMissingPermissionsForDrop();

    protected abstract void performAssignment(final List<S> sourceItemsToAssign, final T targetItem);

    protected String createAssignmentMessage(final AbstractAssignmentResult<? extends NamedEntity> assignmentResult,
            final String assignedEntityType, final String targetEntityType, final String targetEntityName) {
        final StringBuilder assignmentMsg = new StringBuilder();
        final int assignedCount = assignmentResult.getAssigned();
        final int alreadyAssignedCount = assignmentResult.getAlreadyAssigned();
        final int unassignedCount = assignmentResult.getUnassigned();

        if (assignedCount > 0 && !CollectionUtils.isEmpty(assignmentResult.getAssignedEntity())) {
            final String assignedMsg = getAssignmentMsgFor(assignedCount, "message.assigned.one",
                    "message.assigned.many", assignedEntityType, assignmentResult.getAssignedEntity().get(0).getName(),
                    targetEntityType, targetEntityName);
            assignmentMsg.append(assignedMsg).append("\n");
        }

        if (alreadyAssignedCount > 0) {
            assignmentMsg.append(i18n.getMessage("message.alreadyAssigned", alreadyAssignedCount, assignedEntityType,
                    targetEntityType, targetEntityName)).append("\n");
        }

        if (unassignedCount > 0 && !CollectionUtils.isEmpty(assignmentResult.getUnassignedEntity())) {
            final String unassignedMsg = getAssignmentMsgFor(unassignedCount, "message.unassigned.one",
                    "message.unassigned.many", assignedEntityType,
                    assignmentResult.getUnassignedEntity().get(0).getName(), targetEntityType, targetEntityName);
            assignmentMsg.append(unassignedMsg).append("\n");
        }

        return assignmentMsg.toString();
    }

    private String getAssignmentMsgFor(final int count, final String singularMsgKey, final String pluralMsgKey,
            final String assignedEntityType, final String assignedEntityName, final String targetEntityType,
            final String targetEntityName) {
        if (count < 1) {
            return "";
        }
        if (count == 1) {
            return i18n.getMessage(singularMsgKey, assignedEntityType, assignedEntityName, targetEntityType,
                    targetEntityName);
        } else {
            return i18n.getMessage(pluralMsgKey, count, assignedEntityType, targetEntityType, targetEntityName);
        }
    }
}
