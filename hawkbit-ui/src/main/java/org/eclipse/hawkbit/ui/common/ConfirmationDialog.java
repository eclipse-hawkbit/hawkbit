/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.io.Serializable;
import java.util.function.Consumer;

import org.eclipse.hawkbit.ui.common.CommonDialogWindow.ConfirmStyle;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Class for the confirmation dialog which pops up when deleting, assigning...
 * entities.
 */
public class ConfirmationDialog implements Serializable {

    private static final long serialVersionUID = 1L;

    private final transient Consumer<Boolean> callback;

    private final CommonDialogWindow window;

    /**
     * Constructor for configuring confirmation dialog.
     * 
     * @param i18n
     *            internationalization
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
     * @param callback
     *            the callback.
     * @param tab
     *            ConfirmationTab which contains more information about the
     *            action which has to be confirmed, e.g. maintenance window
     * @param id
     *            the id of the confirmation window
     */
    public ConfirmationDialog(final VaadinMessageSource i18n, final String caption, final String question,
            final Consumer<Boolean> callback, final Component tab, final String id) {
        this(i18n, caption, question, callback, null, id, tab);
    }

    /**
     * Constructor for configuring confirmation dialog.
     *
     * @param i18n
     *            internationalization
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
     * @param callback
     *            the callback.
     * @param id
     *            the id of the confirmation dialog
     */
    public ConfirmationDialog(final VaadinMessageSource i18n, final String caption, final String question,
            final Consumer<Boolean> callback, final String id) {
        this(i18n, caption, question, callback, null, id, null);
    }

    /**
     * Constructor for configuring confirmation dialog.
     *
     * @param i18n
     *            internationalization
     * @param caption
     *            the dialog caption.
     * @param question
     *            the question.
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
    public ConfirmationDialog(final VaadinMessageSource i18n, final String caption, final String question,
            final Consumer<Boolean> callback, final Resource icon, final String id, final Component tab) {

        final VerticalLayout content = new VerticalLayout();
        content.setMargin(false);
        content.setSpacing(false);

        if (question != null) {
            content.addComponent(createConfirmationQuestion(question));
        }
        if (tab != null) {
            content.addComponent(tab);
        }
        final WindowBuilder windowBuilder = new WindowBuilder(SPUIDefinitions.CONFIRMATION_WINDOW).caption(caption)
                .content(content).cancelButtonClickListener(e -> callback.accept(false))
                .saveDialogCloseListener(getSaveDialogCloseListener()).hideMandatoryExplanation()
                .buttonDecorator(SPUIButtonStyleTiny.class).confirmStyle(ConfirmStyle.OK).i18n(i18n);

        if (!StringUtils.isEmpty(id)) {
            windowBuilder.id(id);
        }
        this.window = windowBuilder.buildCommonDialogWindow();
        window.setSaveButtonEnabled(true);
        this.callback = callback;

        if (icon != null) {
            window.setIcon(icon);
        }

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

    private static Label createConfirmationQuestion(final String question) {
        // ContentMode.HTML is used instead of ContentMode.PREFORMATTED here due
        // to no linebreaks if an entity name is very long
        final String questionHtmlSave = HawkbitCommonUtil.sanitizeHtml(question);
        final Label questionLbl = new Label(questionHtmlSave, ContentMode.HTML);
        questionLbl.setWidth(100, Unit.PERCENTAGE);
        questionLbl.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_QUESTION_LABEL);

        return questionLbl;
    }

    /**
     * Enables the ok save button
     *
     * @param enabled
     *            boolean
     */
    public void setOkButtonEnabled(final boolean enabled) {
        window.setSaveButtonEnabled(enabled);
    }

    /**
     * @return confirmation window
     */
    public Window getWindow() {
        return window;
    }

}
