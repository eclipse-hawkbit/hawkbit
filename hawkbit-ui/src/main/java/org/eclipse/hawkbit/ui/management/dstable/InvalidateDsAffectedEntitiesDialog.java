/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.ConfirmStyle;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Showing the affected entities for invalidating a distribution set.
 */
public class InvalidateDsAffectedEntitiesDialog {

    private final Consumer<Boolean> callback;

    private final CommonDialogWindow window;

    private final VaadinMessageSource i18n;

    /**
     * Constructor for {@link InvalidateDsAffectedEntitiesDialog}
     *
     * @param allDistributionSetsForInvalidation
     *            {@link List} of {@link ProxyDistributionSet} that are selected for
     *            invalidation
     * @param i18n
     *            {@link VaadinMessageSource}
     * @param callback
     *            callback for dialog result
     * @param affectedEntities
     *            number of affected {@link Rollout}s, {@link Action}s and
     *            auto-assignments
     */
    public InvalidateDsAffectedEntitiesDialog(final List<ProxyDistributionSet> allDistributionSetsForInvalidation,
            final VaadinMessageSource i18n, final Consumer<Boolean> callback,
            final DistributionSetInvalidationCount affectedEntities) {

        this.i18n = i18n;

        final VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setMargin(true);

        final Label consequencesLabel = new Label(createConsequencesText(allDistributionSetsForInvalidation));
        consequencesLabel.setWidthFull();
        content.addComponent(consequencesLabel);

        final Label stoppedSingleAssignmentsLabel = new Label(
                i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_ACTIONS,
                        affectedEntities.getActionCount()));
        stoppedSingleAssignmentsLabel.setId(UIComponentIdProvider.INVALIDATE_DS_AFFECTED_ENTITIES_ACTIONS);
        content.addComponent(stoppedSingleAssignmentsLabel);

        final Label stoppedRolloutsLabel = new Label(
                i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_ROLLOUTS,
                        affectedEntities.getRolloutsCount()));
        stoppedRolloutsLabel.setId(UIComponentIdProvider.INVALIDATE_DS_AFFECTED_ENTITIES_ROLLOUTS);
        content.addComponent(stoppedRolloutsLabel);

        final Label stoppedAutoAssignmentsLabel = new Label(i18n.getMessage(
                UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_AUTOASSIGNMENTS,
                affectedEntities.getAutoAssignmentCount()));
        stoppedAutoAssignmentsLabel.setId(UIComponentIdProvider.INVALIDATE_DS_AFFECTED_ENTITIES_AUTOASSIGNMENTS);
        content.addComponent(stoppedAutoAssignmentsLabel);

        final WindowBuilder windowBuilder = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .id(UIComponentIdProvider.INVALIDATE_DS_AFFECTED_ENTITIES)
                .caption(i18n.getMessage(UIMessageIdProvider.CAPTION_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES))
                .content(content).cancelButtonClickListener(e -> callback.accept(false))
                .saveDialogCloseListener(getSaveDialogCloseListener()).hideMandatoryExplanation()
                .buttonDecorator(SPUIButtonStyleTiny.class).confirmStyle(ConfirmStyle.CONFIRM).i18n(i18n);

        this.window = windowBuilder.buildCommonDialogWindow();
        window.setSaveButtonEnabled(true);
        window.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_WINDOW_STYLE);
        this.callback = callback;

    }

    private String createConsequencesText(final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        String consequencesText = "";
        if (allDistributionSetsForInvalidation.size() == 1) {
            final ProxyDistributionSet distributionSet = allDistributionSetsForInvalidation.get(0);
            consequencesText = i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_INTRO_SINGULAR,
                    HawkbitCommonUtil.getFormattedNameVersion(distributionSet.getName(), distributionSet.getVersion()));
        } else {
            consequencesText = i18n.getMessage(
                    UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_INTRO_PLURAL,
                    allDistributionSetsForInvalidation.size());
        }

        return consequencesText;
    }

    private SaveDialogCloseListener getSaveDialogCloseListener() {
        return new SaveDialogCloseListener() {
            @Override
            public void saveOrUpdate() {
                callback.accept(true);
            }

            @Override
            public boolean canWindowSaveOrUpdate() {
                return true;
            }
        };
    }

    /**
     * @return confirmation window
     */
    public Window getWindow() {
        return window;
    }
}
