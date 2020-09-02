/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.support.assignment;

import java.util.List;

import org.eclipse.hawkbit.repository.model.AbstractAssignmentResult;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * Support for assigning items (target or distribution set) to tag item.
 * 
 * @param <T>
 *            The item-type of source item
 * @param <R>
 *            The item-type of assignment result
 */
public abstract class ToTagAssignmentSupport<T, R extends NamedEntity> extends AssignmentSupport<T, ProxyTag> {

    protected ToTagAssignmentSupport(final UINotification notification, final VaadinMessageSource i18n) {
        super(notification, i18n);
    }

    @Override
    protected void performAssignment(final List<T> sourceItemsToAssign, final ProxyTag targetItem) {
        final String tagName = targetItem.getName();

        final AbstractAssignmentResult<R> tagsAssignmentResult = toggleTagAssignment(sourceItemsToAssign, tagName);

        final String assignmentMsg = createAssignmentMessage(tagsAssignmentResult,
                i18n.getMessage(getAssignedEntityTypeMsgKey()), i18n.getMessage("caption.tag"), tagName);
        notification.displaySuccess(assignmentMsg);

        publishTagAssignmentEvent(sourceItemsToAssign);
    }

    protected abstract AbstractAssignmentResult<R> toggleTagAssignment(final List<T> sourceItems, final String tagName);

    protected abstract String getAssignedEntityTypeMsgKey();

    protected abstract void publishTagAssignmentEvent(final List<T> sourceItems);
}
