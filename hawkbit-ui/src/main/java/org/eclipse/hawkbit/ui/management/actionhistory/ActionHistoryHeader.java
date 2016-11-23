/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.SPUIButton;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 *
 *
 */
@SpringComponent
@UIScope
public class ActionHistoryHeader extends VerticalLayout {

    private static final long serialVersionUID = -6276188234115774351L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private ManagementUIState managementUIState;

    private Label titleOfActionHistory;
    private SPUIButton maxMinButton;

    /**
     * Initialize the Action History Header.
     */
    @PostConstruct
    public void init() {
        buildComponent();
        buildLayout();
        restorePreviousState();
    }

    private void buildComponent() {
        // create default title - it will be shown even when no data is
        // available
        titleOfActionHistory = new LabelBuilder().name(HawkbitCommonUtil.getArtifactoryDetailsLabelId(""))
                .buildCaptionLabel();

        titleOfActionHistory.setImmediate(true);
        titleOfActionHistory.setContentMode(ContentMode.HTML);

        maxMinButton = (SPUIButton) SPUIComponentProvider.getButton(SPUIDefinitions.EXPAND_ACTION_HISTORY, "", "", null,
                true, FontAwesome.EXPAND, SPUIButtonStyleSmallNoBorder.class);
        // listener for maximizing action history
        maxMinButton.addClickListener(event -> maxMinButtonClicked());

    }

    private void buildLayout() {
        final HorizontalLayout titleMaxIconsLayout = new HorizontalLayout();
        titleMaxIconsLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        titleMaxIconsLayout.setSpacing(false);
        titleMaxIconsLayout.setMargin(false);
        titleMaxIconsLayout.setSizeFull();
        titleMaxIconsLayout.addComponents(titleOfActionHistory, maxMinButton);
        titleMaxIconsLayout.setComponentAlignment(titleOfActionHistory, Alignment.TOP_LEFT);
        titleMaxIconsLayout.setComponentAlignment(maxMinButton, Alignment.TOP_RIGHT);
        titleMaxIconsLayout.setExpandRatio(titleOfActionHistory, 0.8f);
        titleMaxIconsLayout.setExpandRatio(maxMinButton, 0.2f);

        // Note: here the only purpose of adding drop hints to the layout is to
        // maintain consistent
        // height for all widgets headers.
        addComponent(titleMaxIconsLayout);
        setComponentAlignment(titleMaxIconsLayout, Alignment.TOP_LEFT);
        setWidth(100, Unit.PERCENTAGE);
        setImmediate(true);
        addStyleName("action-history-header");
        addStyleName("bordered-layout");
        addStyleName("no-border-bottom");
    }

    /**
     * Populate Header Data for Target.
     * 
     * @param targetName
     *            name of the target
     */
    public void populateHeader(final String targetName) {
        updateActionHistoryHeader(targetName);
    }

    private void maxMinButtonClicked() {
        final Boolean flag = (Boolean) maxMinButton.getData();
        if (flag == null || Boolean.FALSE.equals(flag)) {
            // Clicked on max Icon
            maximizedTableView();
            managementUIState.setActionHistoryMaximized(Boolean.TRUE);
        } else {
            // Clicked on min icon
            minimizeTableView();
            managementUIState.setActionHistoryMaximized(Boolean.FALSE);
        }
    }

    private void maximizedTableView() {
        showMinIcon();
        eventBus.publish(this, ManagementUIEvent.MAX_ACTION_HISTORY);
    }

    private void minimizeTableView() {
        showMaxIcon();
        eventBus.publish(this, ManagementUIEvent.MIN_ACTION_HISTORY);
    }

    /**
     * Updates header with target name.
     * 
     * @param targetName
     *            name of the target
     */
    public void updateActionHistoryHeader(final String targetName) {
        titleOfActionHistory.setValue(HawkbitCommonUtil.getActionHistoryLabelId(targetName));
    }

    private void showMinIcon() {
        maxMinButton.togleIcon(FontAwesome.COMPRESS);
        maxMinButton.setData(Boolean.TRUE);
    }

    private void showMaxIcon() {
        maxMinButton.togleIcon(FontAwesome.EXPAND);
        maxMinButton.setData(Boolean.FALSE);
    }

    private void restorePreviousState() {
        if (managementUIState.isActionHistoryMaximized()) {
            showMinIcon();
        }
    }
}
