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
import java.util.function.BooleanSupplier;

import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyNamedEntity;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 * Support for assigning the {@link ProxyNamedEntity} items between two grids
 * (targets to distribution set or distribution sets to target).
 * 
 * @param <S>
 *            The item-type of source items
 * @param <T>
 *            The item-type of target item
 */
public abstract class DeploymentAssignmentSupport<S extends ProxyNamedEntity, T extends ProxyNamedEntity>
        extends AssignmentSupport<S, T> {

    protected DeploymentAssignmentSupport(final UINotification notification, final VaadinMessageSource i18n) {
        super(notification, i18n);
    }

    protected ConfirmationDialog openConfirmationWindowForAssignments(final List<String> sourceItemNames,
            final String targetItemName, final Component content, final BooleanSupplier canWindowSave,
            final Runnable assignmentExecutor) {
        final String confirmationMessage = getConfirmationMessageForAssignments(sourceItemNames, targetItemName);
        final ConfirmationDialog confirmAssignDialog = createConfirmationWindow(confirmationMessage, content,
                canWindowSave, assignmentExecutor);

        UI.getCurrent().addWindow(confirmAssignDialog.getWindow());
        confirmAssignDialog.getWindow().bringToFront();

        return confirmAssignDialog;
    }

    private String getConfirmationMessageForAssignments(final List<String> sourceItemNames,
            final String targetItemName) {
        final int sourceItemsToAssignCount = sourceItemNames.size();

        if (sourceItemsToAssignCount > 1) {
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_CONFIRM_ASSIGN_MULTIPLE_ENTITIES_TO_ENTITY,
                    sourceItemsToAssignCount, sourceEntityTypePlur(), targetEntityType(), targetItemName);
        }

        return i18n.getMessage(UIMessageIdProvider.MESSAGE_CONFIRM_ASSIGN_MULTIPLE_ENTITIES_TO_ENTITY,
                sourceEntityTypeSing(), sourceItemNames.get(0), targetEntityType(), targetItemName);
    }

    private ConfirmationDialog createConfirmationWindow(final String confirmationMessage, final Component content,
            final BooleanSupplier canWindowSave, final Runnable assignmentExecutor) {
        final String caption = i18n.getMessage(UIMessageIdProvider.CAPTION_ENTITY_ASSIGN_ACTION_CONFIRMBOX);

        return ConfirmationDialog.newBuilder(i18n, confirmationWindowId()).caption(caption)
                .question(confirmationMessage).tab(content).onSaveOrUpdate(() -> {
                    if (canWindowSave.getAsBoolean()) {
                        assignmentExecutor.run();
                    }
                }).build();
    }

    protected abstract String sourceEntityTypeSing();

    protected abstract String sourceEntityTypePlur();

    protected abstract String targetEntityType();

    protected abstract String confirmationWindowId();
}
