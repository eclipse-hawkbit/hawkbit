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

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Target add/update window layout.
 */
public class InvalidateDsConsequencesDialog {

    private final transient Consumer<Boolean> callback;

    private final CommonDialogWindow window;

    private final CheckBox stopRolloutsCheckBox;

    public InvalidateDsConsequencesDialog(final ProxyDistributionSet distributionSet, final VaadinMessageSource i18n,
            final Consumer<Boolean> callback) {

        final VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setMargin(true);

        final Label consequencesLabel = new Label(
                i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES));
        consequencesLabel.setWidth(500, Unit.POINTS);
        content.addComponent(consequencesLabel);

        stopRolloutsCheckBox = new CheckBox();
        stopRolloutsCheckBox.setId(UIComponentIdProvider.INVALIDATE_DS_STOP_ROLLOUTS);
        stopRolloutsCheckBox.setCaption(i18n.getMessage(UIMessageIdProvider.LABEL_INVALIDATE_DS_STOP_ROLLOUTS));
        content.addComponent(stopRolloutsCheckBox);

        final WindowBuilder windowBuilder = new WindowBuilder(SPUIDefinitions.CONFIRMATION_WINDOW)
                .id(UIComponentIdProvider.INVALIDATE_DS_CONSEQUENCES)
                .caption(i18n.getMessage(UIMessageIdProvider.CAPTION_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES,
                        HawkbitCommonUtil.getFormattedNameVersion(distributionSet.getName(),
                                distributionSet.getVersion())))
                .content(content).cancelButtonClickListener(e -> callback.accept(false))
                .saveDialogCloseListener(getSaveDialogCloseListener()).hideMandatoryExplanation()
                .buttonDecorator(SPUIButtonStyleTiny.class).confirmStyle(ConfirmStyle.NEXT).i18n(i18n);

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

    boolean getStopRollouts() {
        return stopRolloutsCheckBox.getValue();
    }

    /**
     * @return confirmation window
     */
    public Window getWindow() {
        return window;
    }
}
