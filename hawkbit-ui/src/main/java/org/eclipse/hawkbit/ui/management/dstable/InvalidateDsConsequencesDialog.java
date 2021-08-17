/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.ConfirmStyle;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

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

    private final VaadinMessageSource i18n;

    /**
     * Constructor for {@link InvalidateDsConsequencesDialog}
     *
     * @param allDistributionSetsForInvalidation
     *            {@link List} of {@link ProxyDistributionSet} that are selected
     *            for invalidation
     * @param i18n
     *            {@link VaadinMessageSource}
     * @param callback
     *            callback for dialog result
     */
    public InvalidateDsConsequencesDialog(final List<ProxyDistributionSet> allDistributionSetsForInvalidation,
            final VaadinMessageSource i18n, final Consumer<Boolean> callback) {

        this.i18n = i18n;
        final VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setMargin(true);

        final Label consequencesLabel = new Label(createConsequencesText(allDistributionSetsForInvalidation));
        consequencesLabel.setWidthFull();
        content.addComponent(consequencesLabel);

        stopRolloutsCheckBox = new CheckBox();
        stopRolloutsCheckBox.setId(UIComponentIdProvider.INVALIDATE_DS_STOP_ROLLOUTS);
        stopRolloutsCheckBox.setCaption(i18n.getMessage(UIMessageIdProvider.LABEL_INVALIDATE_DS_STOP_ROLLOUTS));
        content.addComponent(stopRolloutsCheckBox);

        final WindowBuilder windowBuilder = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .id(UIComponentIdProvider.INVALIDATE_DS_CONSEQUENCES)
                .caption(createCaption(allDistributionSetsForInvalidation)).content(content)
                .cancelButtonClickListener(e -> callback.accept(false))
                .saveDialogCloseListener(getSaveDialogCloseListener()).hideMandatoryExplanation()
                .buttonDecorator(SPUIButtonStyleTiny.class).confirmStyle(ConfirmStyle.NEXT).i18n(i18n);

        this.window = windowBuilder.buildCommonDialogWindow();
        window.setSaveButtonEnabled(true);
        window.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_WINDOW_STYLE);
        this.callback = callback;

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

    private String createCaption(final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        String caption = "";
        if (allDistributionSetsForInvalidation.size() == 1) {
            caption = i18n.getMessage(UIMessageIdProvider.CAPTION_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_SINGULAR,
                    allDistributionSetsForInvalidation.get(0).getNameVersion());
        } else {
            caption = i18n.getMessage(UIMessageIdProvider.CAPTION_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_PLURAL,
                    allDistributionSetsForInvalidation.size());
        }
        return caption;
    }

    private String createConsequencesText(final List<ProxyDistributionSet> allDistributionSetsForInvalidation) {
        if (allDistributionSetsForInvalidation.size() == 1) {
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_SINGULAR);
        } else {
            return i18n.getMessage(UIMessageIdProvider.MESSAGE_INVALIDATE_DISTRIBUTIONSET_CONSEQUENCES_PLURAL,
                    allDistributionSetsForInvalidation.size());
        }
    }

    /**
     * Returns the user selection stop rollouts
     *
     * @return boolean value of checkbox stop rollouts
     */
    boolean isStopRolloutsSelected() {
        return stopRolloutsCheckBox.getValue();
    }

    /**
     * @return confirmation window
     */
    public Window getWindow() {
        return window;
    }
}
