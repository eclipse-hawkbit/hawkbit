/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.common.confirmwindow.layout.ConfirmationTab;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.util.StringUtils;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Class for the confirmation dialog which pops up when deleting, assigning...
 * entities.
 */
public class ConfirmationDialog implements Button.ClickListener {

    private static final long serialVersionUID = 1L;

    private transient ConfirmationDialogCallback callback;

    private final Button okButton;

    private final Window window;

    /**
     * Constructor for configuring confirmation dialog.
     * 
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
     * @param okLabel
     *            the Ok button label.
     * @param cancelLabel
     *            the cancel button label.
     * @param callback
     *            the callback.
     * @param tab
     *            ConfirmationTab which contains more information about the
     *            action which has to be confirmed, e.g. maintenance window
     * @param id
     *            the id of the confirmation window
     */
    public ConfirmationDialog(final String caption, final String question, final String okLabel,
            final String cancelLabel, final ConfirmationDialogCallback callback, final ConfirmationTab tab,
            final String id) {
        this(caption, question, okLabel, cancelLabel, callback, null, id, tab);
    }

    /**
     * Constructor for configuring confirmation dialog.
     * 
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
     * @param okLabel
     *            the Ok button label.
     * @param cancelLabel
     *            the cancel button label.
     * @param callback
     *            the callback.
     */
    public ConfirmationDialog(final String caption, final String question, final String okLabel,
            final String cancelLabel, final ConfirmationDialogCallback callback) {
        this(caption, question, okLabel, cancelLabel, callback, null, null, null);
    }

    /**
     * Constructor for configuring confirmation dialog.
     * 
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
     * @param okLabel
     *            the Ok button label.
     * @param cancelLabel
     *            the cancel button label.
     * @param callback
     *            the callback.
     * @param id
     *            the id of the confirmation dialog
     */
    public ConfirmationDialog(final String caption, final String question, final String okLabel,
            final String cancelLabel, final ConfirmationDialogCallback callback, final String id) {
        this(caption, question, okLabel, cancelLabel, callback, null, id, null);
    }

    /**
     * Constructor for configuring confirmation dialog.
     * 
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
     * @param okLabel
     *            the Ok button label.
     * @param cancelLabel
     *            the cancel button label.
     * @param callback
     *            the callback.
     * @param icon
     *            the icon of the dialog
     */
    public ConfirmationDialog(final String caption, final String question, final String okLabel,
            final String cancelLabel, final ConfirmationDialogCallback callback, final Resource icon) {
        this(caption, question, okLabel, cancelLabel, callback, icon, null, null);
    }

    /**
     * Constructor for configuring confirmation dialog.
     * 
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
     * @param okLabel
     *            the Ok button label.
     * @param cancelLabel
     *            the cancel button label.
     * @param callback
     *            the callback.
     * @param icon
     *            the icon of the dialog
     * @param id
     *            the id of the confirmation dialog
     * @param tab
     *            ConfirmationTab which contains more information about the
     *            action which has to be confirmed, e.g. maintenance window
     */
    public ConfirmationDialog(final String caption, final String question, final String okLabel,
            final String cancelLabel, final ConfirmationDialogCallback callback, final Resource icon, final String id,
            final ConfirmationTab tab) {
        window = new Window(caption);
        if (!StringUtils.isEmpty(id)) {
            window.setId(id);
        }
        window.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        if (icon != null) {
            window.setIcon(icon);
        }

        okButton = createOkButton(okLabel);

        final Button cancelButton = createCancelButton(cancelLabel);
        window.setModal(true);
        window.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_WINDOW_STYLE);
        if (this.callback == null) {
            this.callback = callback;
        }
        final VerticalLayout vLayout = new VerticalLayout();
        if (question != null) {
            vLayout.addComponent(createConfirmationQuestion(question));
        }
        if (tab != null) {
            vLayout.addComponent(tab);
        }

        final HorizontalLayout hButtonLayout = createButtonLayout(cancelButton);
        hButtonLayout.addStyleName("marginTop");
        vLayout.addComponent(hButtonLayout);
        vLayout.setComponentAlignment(hButtonLayout, Alignment.BOTTOM_CENTER);

        window.setContent(vLayout);
        window.setResizable(false);
    }

    private static Label createConfirmationQuestion(final String question) {
        final Label questionLbl = new Label(String.format("<p>%s</p>", question.replaceAll("\n", "<br/>")),
                ContentMode.HTML);
        questionLbl.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_QUESTION_LABEL);
        return questionLbl;
    }

    private HorizontalLayout createButtonLayout(final Button cancelButton) {
        final HorizontalLayout hButtonLayout = new HorizontalLayout();
        hButtonLayout.setSpacing(true);
        hButtonLayout.addComponent(okButton);
        hButtonLayout.addComponent(cancelButton);
        hButtonLayout.setSizeUndefined();
        hButtonLayout.setComponentAlignment(okButton, Alignment.TOP_CENTER);
        hButtonLayout.setComponentAlignment(cancelButton, Alignment.TOP_CENTER);
        return hButtonLayout;
    }

    private Button createCancelButton(final String cancelLabel) {
        final Button button = SPUIComponentProvider.getButton(UIComponentIdProvider.CANCEL_BUTTON, cancelLabel, "",
                null, false, null, SPUIButtonStyleTiny.class);
        button.addClickListener(this);
        button.setClickShortcut(KeyCode.ESCAPE);
        return button;
    }

    private Button createOkButton(final String okLabel) {
        final Button button = SPUIComponentProvider.getButton(UIComponentIdProvider.OK_BUTTON, okLabel, "",
                ValoTheme.BUTTON_PRIMARY, false, null, SPUIButtonStyleTiny.class);
        button.addClickListener(this);
        button.setClickShortcut(KeyCode.ENTER);
        return button;
    }

    /**
     * TenantAwareEvent handler for button clicks.
     * 
     * @param event
     *            the click event.
     */
    @Override
    public void buttonClick(final ClickEvent event) {
        if (window.getParent() != null) {
            UI.getCurrent().removeWindow(window);
        }
        callback.response(event.getSource().equals(okButton));
    }

    public Window getWindow() {
        return window;
    }

    /**
     * Interface for confirmation dialog callbacks.
     */
    @FunctionalInterface
    public interface ConfirmationDialogCallback {
        /**
         * The user response.
         * 
         * @param ok
         *            True if user clicked ok.
         */
        void response(boolean ok);
    }

    public Button getOkButton() {
        return okButton;
    }

}
