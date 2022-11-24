/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import com.vaadin.data.Binder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.UI;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.builder.FormComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetConfirmationOptions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import java.util.function.Consumer;

import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_ACTIVATION_DIALOG_INITIATOR;
import static org.eclipse.hawkbit.ui.utils.UIComponentIdProvider.AUTO_CONFIRMATION_ACTIVATION_DIALOG_REMARK;

/**
 * Target auto confirmation activation layout
 */
public class TargetAutoConfActivationLayout extends AbstractEntityWindowLayout<ProxyTargetConfirmationOptions> {

    public static final String CAPTION_TEXTFIELD_INITIATOR = "caption.target.auto.confirmation.activate.initiator";
    public static final String PROMPT_TEXTFIELD_INITIATOR = "prompt.target.auto.confirmation.activate.initiator";

    private final ConfirmationManagement confirmationManagement;
    private final VaadinMessageSource i18n;
    private final TextField initiator;
    private final TextArea remarkArea;

    /**
     * Constructor for TargetAutoConfActivationLayout
     *
     * @param i18n
     *            to get UI messages
     * @param confirmationManagement
     *            to calculate affected actions
     */
    public TargetAutoConfActivationLayout(final VaadinMessageSource i18n,
            final ConfirmationManagement confirmationManagement) {
        super();
        this.i18n = i18n;
        this.confirmationManagement = confirmationManagement;

        this.initiator = createInitiatorField(binder);
        this.remarkArea = createRemarkInputArea(binder);
    }

    @Override
    public ComponentContainer getRootComponent() {
        final FormLayout autoConfirmationLayout = new FormLayout();

        autoConfirmationLayout.setSpacing(true);
        autoConfirmationLayout.setMargin(true);
        autoConfirmationLayout.setSizeUndefined();

        autoConfirmationLayout.addComponent(initiator);
        autoConfirmationLayout.addComponent(remarkArea);
        remarkArea.focus();
        return autoConfirmationLayout;
    }

    public TextField createInitiatorField(final Binder<ProxyTargetConfirmationOptions> binder) {
        final TextField initiatorField = new TextFieldBuilder(64).id(AUTO_CONFIRMATION_ACTIVATION_DIALOG_INITIATOR)
                .caption(i18n.getMessage(CAPTION_TEXTFIELD_INITIATOR))
                .prompt(i18n.getMessage(PROMPT_TEXTFIELD_INITIATOR)).buildTextComponent();

        binder.forField(initiatorField).bind(ProxyTargetConfirmationOptions::getInitiator,
                ProxyTargetConfirmationOptions::setInitiator);

        initiatorField.setSizeUndefined();
        return initiatorField;
    }

    /**
     * create remark field
     *
     * @param binder
     *            binder the input will be bound to
     * @return input component
     */
    public TextArea createRemarkInputArea(final Binder<ProxyTargetConfirmationOptions> binder) {
        return FormComponentBuilder
                .createBigTextInput(binder, i18n, AUTO_CONFIRMATION_ACTIVATION_DIALOG_REMARK,
                        i18n.getMessage("caption.target.auto.confirmation.activate.remark"),
                        i18n.getMessage("prompt.target.auto.confirmation.activate.remark"),
                        ProxyTargetConfirmationOptions::getRemark, ProxyTargetConfirmationOptions::setRemark)
                .getComponent();
    }

    public void setSaveCallback(final Consumer<CommonDialogWindow.SaveDialogCloseListener> saveCallback) {
        saveCallback.accept(new CommonDialogWindow.SaveDialogCloseListener() {
            @Override
            public boolean canWindowSaveOrUpdate() {
                return true;
            }

            @Override
            public void saveOrUpdate() {
                final ProxyTargetConfirmationOptions newOptions = getEntity();
                if (newOptions != null) {
                    final int count = calculateAffectedActionsOnActivation(newOptions.getControllerId());
                    if (count > 0) {
                        final ConfirmationDialog confirmationDialog = ConfirmationDialog.newBuilder(i18n, "test")
                                .caption(i18n
                                        .getMessage("caption.target.auto.confirmation.activate.consequences.caption"))
                                .question(i18n.getMessage(
                                        "caption.target.auto.confirmation.activate.consequences.question", count))
                                .icon(VaadinIcons.WARNING).onSaveOrUpdate(() -> activateAutoConfirmation(newOptions))
                                .build();
                        UI.getCurrent().addWindow(confirmationDialog.getWindow());
                        confirmationDialog.getWindow().bringToFront();
                    } else {
                        activateAutoConfirmation(newOptions);
                    }
                }
            }
        });
    }

    private void activateAutoConfirmation(final ProxyTargetConfirmationOptions options) {
        confirmationManagement.activateAutoConfirmation(options.getControllerId(), options.getInitiator(),
                options.getRemark());
    }

    private int calculateAffectedActionsOnActivation(final String controllerId) {
        return confirmationManagement.findActiveActionsWaitingConfirmation(controllerId).size();
    }

}
