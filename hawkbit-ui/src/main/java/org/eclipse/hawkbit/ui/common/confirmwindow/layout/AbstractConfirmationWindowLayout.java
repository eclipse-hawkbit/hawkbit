/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.confirmwindow.layout;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract layout of confirm actions window.
 * 
 */
public abstract class AbstractConfirmationWindowLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private Label actionMessage;

    private Accordion accordion;

    private String consolidatedMessage;

    protected VaadinMessageSource i18n;

    protected transient EventBus.UIEventBus eventBus;

    protected AbstractConfirmationWindowLayout(final VaadinMessageSource i18n, final UIEventBus eventBus) {
        this.i18n = i18n;
        this.eventBus = eventBus;
    }

    /**
     * Initialize the confirmation window layout
     */
    public void initialize() {
        removeAllComponents();
        consolidatedMessage = "";
        createComponents();
        buildLayout();
    }

    private void createComponents() {
        createAccordian();
        createActionMessgaeLabel();
    }

    private void createActionMessgaeLabel() {
        actionMessage = new LabelBuilder().name("").id(UIComponentIdProvider.ACTION_LABEL).visible(false).buildLabel();
        actionMessage.addStyleName(SPUIStyleDefinitions.CONFIRM_WINDOW_INFO_BOX);
    }

    private void createAccordian() {
        accordion = new Accordion();
        accordion.setSizeFull();
    }

    private void buildLayout() {
        final Map<String, ConfirmationTab> confimrationTabs = getConfirmationTabs();
        for (final Entry<String, ConfirmationTab> captionConfirmationTab : confimrationTabs.entrySet()) {
            accordion.addTab(captionConfirmationTab.getValue(), captionConfirmationTab.getKey(), null);
        }
        final VerticalLayout confirmActionsLayout = new VerticalLayout();
        confirmActionsLayout.addStyleName(SPUIStyleDefinitions.CONFIRM_WINDOW_ACCORDIAN);
        confirmActionsLayout.setSpacing(false);
        confirmActionsLayout.setMargin(false);
        confirmActionsLayout.addComponent(actionMessage);
        confirmActionsLayout.addComponent(accordion);

        addComponent(confirmActionsLayout);
        setComponentAlignment(confirmActionsLayout, Alignment.MIDDLE_CENTER);
        setSpacing(false);
        setMargin(false);
    }

    /**
     * Add to the consolidated result message which will be displayed in the
     * notification on closing the window. Each message that will be added in
     * new line of previous messages using html <br>
     * 
     * @param message
     *            to be added to the consolidated messages.
     */
    public void addToConsolitatedMsg(final String message) {
        if (consolidatedMessage != null && consolidatedMessage.length() > 0) {
            consolidatedMessage = consolidatedMessage + "<br>";
        }
        consolidatedMessage = consolidatedMessage + message;
    }

    /**
     * @param tab
     */
    protected void removeCurrentTab(final ConfirmationTab tab) {
        accordion.removeComponent(tab);
    }

    /**
     * 
     * @param message
     */
    protected void setActionMessage(final String message) {
        actionMessage.setValue(message);
        actionMessage.setVisible(true);
    }

    /**
     * Get contents for each tab to be displayed in the accordian.
     * 
     * @return map of caption and content for each tab.
     */
    protected abstract Map<String, ConfirmationTab> getConfirmationTabs();

    /**
     * @return the consolidatedMessage
     */
    public String getConsolidatedMessage() {
        return consolidatedMessage;
    }

    protected Button createDiscardButton(final Object itemId, final ClickListener clickListener) {
        final Button deletesDsIcon = SPUIComponentProvider.getButton("", "", SPUILabelDefinitions.DISCARD,
                ValoTheme.BUTTON_TINY + " " + SPUIStyleDefinitions.REDICON, true, FontAwesome.REPLY,
                SPUIButtonStyleSmallNoBorder.class);
        deletesDsIcon.setData(itemId);
        deletesDsIcon.setImmediate(true);
        deletesDsIcon.addClickListener(clickListener);
        return deletesDsIcon;
    }
}
