/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.function.Consumer;

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
 * Target add/update window layout.
 */
public class InvalidateDsAffectedEntitiesDialog {

    private final transient Consumer<Boolean> callback;

    private final CommonDialogWindow window;

    public InvalidateDsAffectedEntitiesDialog(final ProxyDistributionSet distributionSet,
            final VaadinMessageSource i18n, final Consumer<Boolean> callback, final int stoppedRollouts,
            final int stoppedAutoAssignments) {

        final VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setMargin(true);

        final Label consequencesLabel = new Label(i18n.getMessage(
                UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_INTRO,
                HawkbitCommonUtil.getFormattedNameVersion(distributionSet.getName(), distributionSet.getVersion())));
        content.addComponent(consequencesLabel);

        final Label stoppedRolloutsLabel = new Label(i18n.getMessage(
                UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_ROLLOUTS, stoppedRollouts));
        content.addComponent(stoppedRolloutsLabel);

        final Label stoppedAutoAssignmentsLabel = new Label(i18n.getMessage(
                UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES_AUTOASSIGNMENTS,
                stoppedAutoAssignments));
        content.addComponent(stoppedAutoAssignmentsLabel);

        final WindowBuilder windowBuilder = new WindowBuilder(SPUIDefinitions.CONFIRMATION_WINDOW)
                .id(UIComponentIdProvider.INVALIDATE_DS_AFFECTED_ENTITIES)
                .caption(i18n.getMessage(UIMessageIdProvider.CAPTION_INVALIDATE_DISTRIBUTIONSET_AFFECTED_ENTITIES))
                .content(content).cancelButtonClickListener(e -> callback.accept(false))
                .saveDialogCloseListener(getSaveDialogCloseListener()).hideMandatoryExplanation()
                .buttonDecorator(SPUIButtonStyleTiny.class).confirmStyle(ConfirmStyle.CONFIRM).i18n(i18n);

        this.window = windowBuilder.buildCommonDialogWindow();
        window.setSaveButtonEnabled(true);
        this.callback = callback;

        window.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_WINDOW_STYLE);
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
