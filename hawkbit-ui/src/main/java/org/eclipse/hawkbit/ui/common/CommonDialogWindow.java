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
import org.eclipse.hawkbit.ui.decorators.SPUIButtonDecorator;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorderWithIcon;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 * Table pop-up-windows including a minimize and close icon in the upper right
 * corner and a save and cancel button at the bottom. Is not intended to reuse.
 *
 */
public class CommonDialogWindow extends Window {
    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    private final VerticalLayout mainLayout;
    private final String caption;
    private final Component content;
    private final String helpLink;
    private final ConfirmStyle confirmStyle;
    private final Class<? extends SPUIButtonDecorator> buttonDecorator;
    private Button confirmButton;
    private Button cancelButton;
    private HorizontalLayout buttonsLayout;
    private Label mandatoryLabel;

    private final ClickListener cancelButtonClickListener;
    private final ClickListener closeClickListener;

    private transient SaveDialogCloseListener closeListener;

    /**
     * Different kinds of confirm buttons
     */
    public enum ConfirmStyle {
        SAVE, OK, CONFIRM, NEXT
    }

    /**
     * Constructor
     *
     * @param caption
     *            the caption
     * @param content
     *            the content
     * @param helpLink
     *            the helpLinks
     * @param closeListener
     *            the saveDialogCloseListener
     * @param cancelButtonClickListener
     *            the cancelButtonClickListener
     * @param confirmStyle
     *            what kind of button is used
     * @param buttonDecorator
     *            to style the confirm and cancel buttons
     * @param i18n
     *            internationalization
     */
    public CommonDialogWindow(final String caption, final Component content, final String helpLink,
            final SaveDialogCloseListener closeListener, final ClickListener cancelButtonClickListener,
            final ConfirmStyle confirmStyle, final Class<? extends SPUIButtonDecorator> buttonDecorator,
            final VaadinMessageSource i18n) {
        this.caption = caption;
        this.content = content;
        this.helpLink = helpLink;
        this.closeListener = closeListener;
        this.cancelButtonClickListener = cancelButtonClickListener;
        this.confirmStyle = confirmStyle;
        this.buttonDecorator = buttonDecorator;
        this.i18n = i18n;

        this.mainLayout = new VerticalLayout();
        this.closeClickListener = this::onCloseEvent;

        init();
    }

    private void onCloseEvent(final ClickEvent clickEvent) {
        if (!clickEvent.getButton().equals(confirmButton)) {
            close();
            return;
        }

        if (!closeListener.canWindowSaveOrUpdate()) {
            return;
        }
        closeListener.saveOrUpdate();

        if (closeListener.canWindowClose()) {
            close();
        }

    }

    @Override
    public void close() {
        super.close();
        this.confirmButton.setEnabled(false);
    }

    private final void init() {
        mainLayout.setMargin(false);
        mainLayout.setSpacing(false);

        if (content instanceof GridLayout) {
            addStyleName("marginTop");
        }

        if (content != null) {
            mainLayout.addComponent(content);
            mainLayout.setExpandRatio(content, 1.0F);
        }

        createMandatoryLabel();

        final HorizontalLayout buttonLayout = createActionButtonsLayout();
        mainLayout.addComponent(buttonLayout);
        mainLayout.setComponentAlignment(buttonLayout, Alignment.TOP_CENTER);

        setCaptionAsHtml(false);
        setCaption(caption);
        setContent(mainLayout);
        setResizable(false);
        center();
        setModal(true);
        addStyleName("fontsize");
    }

    protected void addCloseListenerForSaveButton() {
        confirmButton.addClickListener(closeClickListener);
    }

    protected void addCloseListenerForCancelButton() {
        cancelButton.addClickListener(closeClickListener);
    }

    private HorizontalLayout createActionButtonsLayout() {

        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.addStyleName("actionButtonsMargin");

        createSaveButton();
        createCancelButton();

        addHelpLink();

        return buttonsLayout;
    }

    private void createMandatoryLabel() {

        mandatoryLabel = new Label(i18n.getMessage("label.mandatory.field"));
        mandatoryLabel.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_TINY);

        mainLayout.addComponent(mandatoryLabel);
    }

    /**
     * Hide the line that explains the mandatory decorator
     *
     */
    public void hideMandatoryExplanation() {
        if (mandatoryLabel != null) {
            mainLayout.removeComponent(mandatoryLabel);
        }
    }

    private void createCancelButton() {
        cancelButton = SPUIComponentProvider.getButton(UIComponentIdProvider.CANCEL_BUTTON,
                i18n.getMessage(UIMessageIdProvider.BUTTON_CANCEL), "", "", true, VaadinIcons.CLOSE,
                SPUIButtonStyleNoBorderWithIcon.class);
        cancelButton.setSizeUndefined();
        cancelButton.addStyleName("default-color");
        addCloseListenerForCancelButton();
        if (cancelButtonClickListener != null) {
            cancelButton.addClickListener(cancelButtonClickListener);
        }

        buttonsLayout.addComponent(cancelButton);
        buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
        buttonsLayout.setExpandRatio(cancelButton, 1.0F);
    }

    private void createSaveButton() {
        if (confirmStyle == ConfirmStyle.SAVE) {
            confirmButton = SPUIComponentProvider.getButton(UIComponentIdProvider.SAVE_BUTTON,
                    i18n.getMessage(UIMessageIdProvider.BUTTON_SAVE), "", "", true, VaadinIcons.HARDDRIVE,
                    buttonDecorator);
        } else if (confirmStyle == ConfirmStyle.NEXT) {
            confirmButton = SPUIComponentProvider.getButton(UIComponentIdProvider.OK_BUTTON,
                    i18n.getMessage(UIMessageIdProvider.BUTTON_NEXT), "", ValoTheme.BUTTON_PRIMARY, false, null,
                    buttonDecorator);
        } else if (confirmStyle == ConfirmStyle.CONFIRM) {
            confirmButton = SPUIComponentProvider.getButton(UIComponentIdProvider.OK_BUTTON,
                    i18n.getMessage(UIMessageIdProvider.BUTTON_CONFIRM), "", ValoTheme.BUTTON_PRIMARY, false, null,
                    buttonDecorator);
        } else {
            confirmButton = SPUIComponentProvider.getButton(UIComponentIdProvider.OK_BUTTON,
                    i18n.getMessage(UIMessageIdProvider.BUTTON_OK), "", ValoTheme.BUTTON_PRIMARY, false, null,
                    buttonDecorator);
        }
        confirmButton.setSizeUndefined();
        confirmButton.addStyleName("default-color");
        addCloseListenerForSaveButton();
        confirmButton.setEnabled(false);
        confirmButton.setClickShortcut(KeyCode.ENTER);
        buttonsLayout.addComponent(confirmButton);
        buttonsLayout.setComponentAlignment(confirmButton, Alignment.MIDDLE_RIGHT);
        buttonsLayout.setExpandRatio(confirmButton, 1.0F);
    }

    private void addHelpLink() {

        if (StringUtils.isEmpty(helpLink)) {
            return;
        }
        final Link helpLinkComponent = SPUIComponentProvider.getHelpLink(i18n, helpLink);
        buttonsLayout.addComponent(helpLinkComponent);
        buttonsLayout.setComponentAlignment(helpLinkComponent, Alignment.MIDDLE_RIGHT);
    }

    public AbstractComponent getButtonsLayout() {
        return this.buttonsLayout;
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    /**
     * Enables the save confirmation button
     *
     * @param enabled
     *            boolean
     *
     */
    public void setSaveButtonEnabled(final boolean enabled) {
        confirmButton.setEnabled(enabled);
    }

    public void setCancelButtonEnabled(final boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    /**
     * Sets the close listener in save dialog
     *
     * @param closeListener
     *            SaveDialogCloseListener
     */
    public void setCloseListener(final SaveDialogCloseListener closeListener) {
        this.closeListener = closeListener;
    }

    /**
     * Check if the safe action can executed. After a the save action the
     * listener checks if the dialog can closed.
     *
     */
    public interface SaveDialogCloseListener {

        /**
         * Checks if the safe action can executed.
         *
         * @return <true> = save action can executed <false> = cannot execute
         *         safe action .
         */
        boolean canWindowSaveOrUpdate();

        /**
         * Checks if the window can be closed after the save action is executed
         *
         * @return <true> = window will close <false> = will not closed.
         */
        default boolean canWindowClose() {
            return true;
        }

        /**
         * Saves/Updates action. Is called if canWindowSaveOrUpdate is <true>.
         *
         */
        void saveOrUpdate();
    }
}
