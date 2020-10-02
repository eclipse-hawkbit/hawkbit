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
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;

/**
 * Support for assigning the {@link ProxyTag} items to target item (target or
 * distribution set).
 *
 * @param <T>
 *            The item-type of target item
 * @param <R>
 *            The item-type of assignment result
 */
public abstract class TagsAssignmentSupport<T, R extends NamedEntity> extends AssignmentSupport<ProxyTag, T> {

    protected TagsAssignmentSupport(final CommonUiDependencies uiDependencies) {
        super(uiDependencies.getUiNotification(), uiDependencies.getI18n());
    }

    @Override
    protected void performAssignment(final List<ProxyTag> sourceItemsToAssign, final T targetItem) {

        // we are taking first tag because multi-tag assignment is
        // not supported
        final String tagName = sourceItemsToAssign.get(0).getName();
        final AbstractAssignmentResult<R> tagsAssignmentResult = toggleTagAssignment(tagName, targetItem);

        final String assignmentMsg = createAssignmentMessage(tagsAssignmentResult,
                i18n.getMessage(getAssignedEntityTypeMsgKey()), i18n.getMessage("caption.tag"), tagName);
        notification.displaySuccess(assignmentMsg);

        publishTagAssignmentEvent(targetItem);
    }

    protected abstract AbstractAssignmentResult<R> toggleTagAssignment(final String tagName, final T targetItem);

    protected abstract String getAssignedEntityTypeMsgKey();

    protected abstract void publishTagAssignmentEvent(final T targetItem);
}
