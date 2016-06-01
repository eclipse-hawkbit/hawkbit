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
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleBorderWithIcon;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@Service
public class CommonDialogWindow extends Window{

    private static final long serialVersionUID = -1321949234316858703L;

    private static final Logger LOG = LoggerFactory.getLogger(CommonDialogWindow.class);

    private final VerticalLayout mainLayout = new VerticalLayout();

    private String caption;

    private Component content;

    private String helpLink;

    private Button saveButton;

    private Button cancelButton;

    private HorizontalLayout buttonsLayout;

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
        setCaptionAsHtml(true);
        setContent(mainLayout);
        setResizable(true);
        center();
        setModal(true);
    }

    private HorizontalLayout createActionButtonsLayout(final ClickListener saveButtonClickListener,
            final ClickListener cancelButtonClickListener) {

        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.setSpacing(true);

        saveButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SYSTEM_CONFIGURATION_SAVE, "save", "", "",
                true, FontAwesome.SAVE, SPUIButtonStyleBorderWithIcon.class);
        saveButton.setSizeUndefined();
        if (null != saveButtonClickListener) {
            saveButton.addClickListener(saveButtonClickListener);
        } else {
            LOG.warn("No ClickListener for saveButton specified");
        }
        buttonsLayout.addComponent(saveButton);
        buttonsLayout.setComponentAlignment(saveButton, Alignment.MIDDLE_RIGHT);
        buttonsLayout.setExpandRatio(saveButton, 1.0F);

        cancelButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.SYSTEM_CONFIGURATION_CANCEL, "cancel",
                "", "", true, FontAwesome.TIMES, SPUIButtonStyleBorderWithIcon.class);
        cancelButton.setSizeUndefined();
        if (null != cancelButtonClickListener) {
            cancelButton.addClickListener(cancelButtonClickListener);
        } else {
            LOG.warn("No ClickListener for cancelButton specified");
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

    public void setCancelButtonCaption(final String caption) {
        cancelButton.setCaption(caption);
    }

    public void setCancelButtonIcon(final Resource icon) {
        cancelButton.setIcon(icon);
    }


    public HorizontalLayout getButtonsLayout() {
        return buttonsLayout;
    }

}
