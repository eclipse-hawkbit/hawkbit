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

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

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

    private static final long serialVersionUID = -4042317361561298155L;

    private Label actionMessage;

    private Accordion accordion;

    private String consolidatedMessage;

    @Autowired
    protected I18N i18n;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    @PostConstruct
    public void inittialize() {
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
        actionMessage = SPUIComponentProvider.getLabel("", null);
        actionMessage.addStyleName(SPUIStyleDefinitions.CONFIRM_WINDOW_INFO_BOX);
        actionMessage.setId(SPUIComponetIdProvider.ACTION_LABEL);
        actionMessage.setVisible(false);
    }

    private void createAccordian() {
        accordion = new Accordion();
        accordion.setSizeFull();
    }

    private void buildLayout() {
        final Map<String, ConfirmationTab> confimrationTabs = getConfimrationTabs();
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
    protected abstract Map<String, ConfirmationTab> getConfimrationTabs();

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
