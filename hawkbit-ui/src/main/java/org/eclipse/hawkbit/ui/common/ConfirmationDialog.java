/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.util.StringUtils;

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
 *
 * module.
 *
 */
public class ConfirmationDialog implements Button.ClickListener {
    private static final long serialVersionUID = 1L;
    /** The confirmation callback. */
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
     */
    public ConfirmationDialog(final String caption, final String question, final String okLabel,
            final String cancelLabel, final ConfirmationDialogCallback callback) {
        this(caption, question, okLabel, cancelLabel, callback, null, null);
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
        this(caption, question, okLabel, cancelLabel, callback, null, id);
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
        this(caption, question, okLabel, cancelLabel, callback, icon, null);
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
     */
    public ConfirmationDialog(final String caption, final String question, final String okLabel,
            final String cancelLabel, final ConfirmationDialogCallback callback, final Resource icon, final String id) {
        window = new Window(caption);
        if (!StringUtils.isEmpty(id)) {
            window.setId(id);
        }
        window.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        if (icon != null) {
            window.setIcon(icon);
        }

        okButton = SPUIComponentProvider.getButton(UIComponentIdProvider.OK_BUTTON, okLabel, "",
                ValoTheme.BUTTON_PRIMARY, false, null, SPUIButtonStyleTiny.class);
        okButton.addClickListener(this);

        final Button cancelButton = SPUIComponentProvider.getButton(null, cancelLabel, "", null, false, null,
                SPUIButtonStyleTiny.class);
        cancelButton.addClickListener(this);
        cancelButton.setId(UIComponentIdProvider.CANCEL_BUTTON);
        window.setModal(true);
        window.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_WINDOW_SYLE);
        if (this.callback == null) {
            this.callback = callback;
        }
        final VerticalLayout vLayout = new VerticalLayout();

        if (question != null) {
            final Label questionLbl = new Label(String.format("<p>%s</p>", question.replaceAll("\n", "<br/>")),
                    ContentMode.HTML);
            questionLbl.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_QUESTION_LABEL);
            vLayout.addComponent(questionLbl);

        }

        final HorizontalLayout hButtonLayout = new HorizontalLayout();
        hButtonLayout.setSpacing(true);
        hButtonLayout.addComponent(okButton);
        hButtonLayout.addComponent(cancelButton);
        hButtonLayout.setSizeUndefined();
        hButtonLayout.setComponentAlignment(okButton, Alignment.TOP_CENTER);
        hButtonLayout.setComponentAlignment(cancelButton, Alignment.TOP_CENTER);

        vLayout.addComponent(hButtonLayout);
        vLayout.setComponentAlignment(hButtonLayout, Alignment.BOTTOM_CENTER);
        window.setContent(vLayout);
        window.setResizable(false);
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

    /**
     * Get the window which holds the confirmation dialog
     * 
     * @return the window which holds the confirmation dialog
     */
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

}
