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
import org.springframework.util.Assert;
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

    private final transient Runnable callback;

    private final CommonDialogWindow window;

    public static Builder newBuilder(final VaadinMessageSource i18n, final String id) {
        return new Builder(i18n, id);
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
     * @param hint
     *            added in the dialog
     * @param onSaveOrUpdate
     *            the callback onSaveOrUpdate.
     * @param onCancel
     *            the callback on cancel.
     * @param icon
     *            the icon of the dialog
     * @param id
     *            the id of the confirmation dialog
     * @param tab
     *            ConfirmationTab which contains more information about the action
     *            which has to be confirmed, e.g. maintenance window
     */
    private ConfirmationDialog(final VaadinMessageSource i18n, final String caption, final String question,
            final String hint, final Runnable onSaveOrUpdate, final Runnable onCancel, final Resource icon,
            final String id, final Component tab) {

        final VerticalLayout content = new VerticalLayout();
        content.setMargin(false);
        content.setSpacing(false);

        if (question != null) {
            content.addComponent(createConfirmationLabel(question));
        }
        if (hint != null) {
            content.addComponent(createConfirmationLabel(hint));
        }
        if (tab != null) {
            content.addComponent(tab);
        }
        final WindowBuilder windowBuilder = new WindowBuilder(SPUIDefinitions.CONFIRMATION_WINDOW).caption(caption)
                .content(content).cancelButtonClickListener(e -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                }).saveDialogCloseListener(getSaveDialogCloseListener()).hideMandatoryExplanation()
                .buttonDecorator(SPUIButtonStyleTiny.class).confirmStyle(ConfirmStyle.OK).i18n(i18n);

        if (!StringUtils.isEmpty(id)) {
            windowBuilder.id(id);
        }
        this.window = windowBuilder.buildCommonDialogWindow();
        window.setSaveButtonEnabled(true);
        this.callback = onSaveOrUpdate;

        if (icon != null) {
            window.setIcon(icon);
        }

        window.addStyleName(SPUIStyleDefinitions.CONFIRMBOX_WINDOW_STYLE);
    }

    private SaveDialogCloseListener getSaveDialogCloseListener() {
        return new SaveDialogCloseListener() {
            @Override
            public void saveOrUpdate() {
                if (callback != null) {
                    callback.run();
                }
            }

            @Override
            public boolean canWindowSaveOrUpdate() {
                return true;
            }
        };
    }

    private static Label createConfirmationLabel(final String text) {
        // ContentMode.HTML is used instead of ContentMode.PREFORMATTED here due
        // to no linebreaks if an entity name is very long
        final String questionHtmlSave = HawkbitCommonUtil.sanitizeHtml(text);
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

    /**
     * Builder for a confirmation dialog
     */
    public static class Builder {

        private final VaadinMessageSource i18n;
        private final String id;
        private String caption;
        private String question;
        private String hint;
        private Resource icon;
        private Component tab;

        private Runnable callbackOnSaveOrUpdate;

        private Runnable callbackOnCancel;

        /**
         * private constructor
         * 
         * @param i18n
         *            required field
         * @param id
         *            required field
         */
        Builder(final VaadinMessageSource i18n, final String id) {
            this.i18n = i18n;
            this.id = id;
        }

        public Builder caption(final String caption) {
            this.caption = caption;
            return this;
        }

        public Builder question(final String question) {
            this.question = question;
            return this;
        }

        public Builder hint(final String hint) {
            this.hint = hint;
            return this;
        }

        public Builder icon(final Resource icon) {
            this.icon = icon;
            return this;
        }

        public Builder tab(final Component tab) {
            this.tab = tab;
            return this;
        }

        public Builder onSaveOrUpdate(final Runnable callback) {
            this.callbackOnSaveOrUpdate = callback;
            return this;
        }

        public Builder onCancel(final Runnable callback) {
            this.callbackOnCancel = callback;
            return this;
        }

        public ConfirmationDialog build() {
            Assert.isTrue(StringUtils.hasText(caption), "Caption cannot be null.");
            return new ConfirmationDialog(i18n, caption, question, hint, callbackOnSaveOrUpdate, callbackOnCancel, icon,
                    id, tab);
        }
    }
}
