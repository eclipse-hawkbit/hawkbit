/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleBorderWithIcon;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * Superclass for pop-up-windows including a minimize and close icon in the
 * upper right corner and a save and cancel button at the bottom.
 *
 */
public class CommonDialogWindow extends Window {

    private static final long serialVersionUID = -1321949234316858703L;

    private static final Logger LOG = LoggerFactory.getLogger(CommonDialogWindow.class);

    private final VerticalLayout mainLayout = new VerticalLayout();

    private String caption;

    private Component content;

    private String helpLink;

    private Button saveButton;

    private Button cancelButton;

    private HorizontalLayout buttonsLayout;

    protected ValueChangeListener buttonEnableListener;

    @Autowired
    private transient UiProperties uiProperties;

    public CommonDialogWindow() {

        init(null, null);
    }

    public CommonDialogWindow(final String caption, final Component content, final String helpLink,
            final ClickListener saveButtonClickListener, final ClickListener cancelButtonClickListener) {

        this.caption = caption;
        this.content = content;
        this.helpLink = helpLink;

        init(saveButtonClickListener, cancelButtonClickListener);
    }

    public void init(final ClickListener saveButtonClickListener, final ClickListener cancelButtonClickListener) {

        if (content instanceof AbstractOrderedLayout) {
            ((AbstractOrderedLayout) content).setSpacing(true);
            ((AbstractOrderedLayout) content).setMargin(true);
        }

        if (null != content) {
            mainLayout.addComponent(content);
        }
        final HorizontalLayout buttonLayout = createActionButtonsLayout(saveButtonClickListener,
                cancelButtonClickListener);
        mainLayout.addComponent(buttonLayout);
        mainLayout.setComponentAlignment(buttonLayout, Alignment.TOP_CENTER);

        setCaption(caption);
        setContent(mainLayout);
        setResizable(true);
        center();
        setModal(true);
        addStyleName("fontsize");
    }

    private HorizontalLayout createActionButtonsLayout(final ClickListener saveButtonClickListener,
            final ClickListener cancelButtonClickListener) {

        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.setSpacing(true);

        saveButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SYSTEM_CONFIGURATION_SAVE, "save", "", "",
                true, FontAwesome.SAVE, SPUIButtonStyleBorderWithIcon.class);
        saveButton.setSizeUndefined();
        saveButton.addStyleName("default-color");
        if (null != saveButtonClickListener) {
            saveButton.addClickListener(saveButtonClickListener);
        } else {
            throw new IllegalArgumentException("no ClickListener for save button specified");
        }
        buttonsLayout.addComponent(saveButton);
        buttonsLayout.setComponentAlignment(saveButton, Alignment.MIDDLE_RIGHT);
        buttonsLayout.setExpandRatio(saveButton, 1.0F);

        cancelButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SYSTEM_CONFIGURATION_CANCEL, "cancel", "",
                "", true, FontAwesome.TIMES, SPUIButtonStyleBorderWithIcon.class);
        cancelButton.setSizeUndefined();
        cancelButton.addStyleName("default-color");
        if (null != cancelButtonClickListener) {
            cancelButton.addClickListener(cancelButtonClickListener);
        } else {
            throw new IllegalArgumentException("no ClickListener for cancel button specified");
        }
        buttonsLayout.addComponent(cancelButton);
        buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
        buttonsLayout.setExpandRatio(cancelButton, 1.0F);
        buttonsLayout.addStyleName("actionButtonsMargin");

        addHelpLink();

        return buttonsLayout;
    }

    private void addHelpLink() {

        if (StringUtils.isNotEmpty(helpLink)) {
            final Link helpLinkComponent = SPUIComponentProvider.getHelpLink(helpLink);
            buttonsLayout.addComponent(helpLinkComponent);
            buttonsLayout.setComponentAlignment(helpLinkComponent, Alignment.MIDDLE_RIGHT);
        }
    }

    public void setSaveButtonEnabled(final boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    public void setCancelButtonEnabled(final boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    public HorizontalLayout getButtonsLayout() {
        return buttonsLayout;
    }

}
