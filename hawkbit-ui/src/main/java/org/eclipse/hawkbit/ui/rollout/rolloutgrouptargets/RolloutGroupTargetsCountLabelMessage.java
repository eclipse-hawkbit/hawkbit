/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Label;

/**
 * Count message label for the targets of the rollout group.
 */
@SpringComponent
@ViewScope
public class RolloutGroupTargetsCountLabelMessage extends Label {

    private static final long serialVersionUID = -3876685878918411453L;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Autowired
    private transient RolloutGroupTargetsListGrid rolloutGroupTargetsListGrid;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    /**
     * PostConstruct method called by spring after bean has been initialized.
     */
    @PostConstruct
    public void postConstruct() {
        applyStyle();
        displayRolloutGroupTargetMessage();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    /**
     * Event Listener to show the message count.
     * 
     * @param event
     */
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onEvent(final RolloutEvent event) {
        if (event == RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS_COUNT) {
            displayRolloutGroupTargetMessage();

        }
    }

    /**
     * 
     */
    private void applyStyle() {
        /* Create label for Targets count message displaying below the table */
        addStyleName(SPUILabelDefinitions.SP_LABEL_MESSAGE_STYLE);
        setContentMode(ContentMode.HTML);
        setId(SPUIComponentIdProvider.COUNT_LABEL);
    }

    private void displayRolloutGroupTargetMessage() {
        long totalTargetTableEnteries = rolloutGroupTargetsListGrid.getContainerDataSource().size();
        if (rolloutUIState.getRolloutGroupTargetsTruncated() != null) {
            // set the icon
            setIcon(FontAwesome.INFO_CIRCLE);
            setDescription(i18n.get("rollout.group.label.target.truncated",
                    rolloutUIState.getRolloutGroupTargetsTruncated(), SPUIDefinitions.MAX_TABLE_ENTRIES));
            totalTargetTableEnteries += rolloutUIState.getRolloutGroupTargetsTruncated();
        } else {
            setIcon(null);
            setDescription(null);
        }

        final StringBuilder message = new StringBuilder(i18n.get("label.target.filter.count"));
        message.append(rolloutUIState.getRolloutGroupTargetsTotalCount());
        message.append(HawkbitCommonUtil.SP_STRING_SPACE);
        if (totalTargetTableEnteries > SPUIDefinitions.MAX_TABLE_ENTRIES) {
            message.append(i18n.get("label.filter.shown"));
            message.append(SPUIDefinitions.MAX_TABLE_ENTRIES);
        } else {
            message.append(i18n.get("label.filter.shown"));
            message.append(rolloutGroupTargetsListGrid.getContainerDataSource().size());
        }
        message.append(HawkbitCommonUtil.SP_STRING_SPACE);
        setCaption(message.toString());
    }
}
