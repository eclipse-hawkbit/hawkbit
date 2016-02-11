/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.footer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
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
 * @author Venugopal Boodidadinne(RBEI/BSJ)
 * 
 *         Count message label which display current filter details and details
 *         on pinning.
 */
@SpringComponent
@ViewScope
public class TargetFilterCountMessageLabel extends Label {

    private static final long serialVersionUID = -7188528790042766877L;

    @Autowired
    private FilterManagementUIState filterManagementUIState;

    @Autowired
    private transient TargetManagement targetManagement;

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
        displayTargetFilterMessage();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final CustomFilterUIEvent custFUIEvent) {
        if (custFUIEvent == CustomFilterUIEvent.TARGET_DETAILS_VIEW
                || custFUIEvent == CustomFilterUIEvent.CREATE_NEW_FILTER_CLICK
                || custFUIEvent == CustomFilterUIEvent.EXIT_CREATE_OR_UPDATE_FILTRER_VIEW) {
            displayTargetFilterMessage();
        }
        else if (custFUIEvent == CustomFilterUIEvent.FILTER_TARGET_BY_QUERY){
        	this.getUI().access(() -> displayTargetFilterMessage());
        }
    }

    private void applyStyle() {
        addStyleName(SPUILabelDefinitions.SP_LABEL_MESSAGE_STYLE);
        setContentMode(ContentMode.HTML);
        setId(SPUIComponetIdProvider.COUNT_LABEL);
    }

    private void displayTargetFilterMessage() {
        long totalTargets = 0;
        if (filterManagementUIState.isCreateFilterViewDisplayed() || filterManagementUIState.isEditViewDisplayed()) {
            if (null != filterManagementUIState.getFilterQueryValue()) {
                totalTargets = targetManagement.countTargetByTargetFilterQuery(filterManagementUIState
                        .getFilterQueryValue());
            }
            final StringBuilder targetMessage = new StringBuilder(i18n.get("label.target.filtered.total"));
            if (filterManagementUIState.getTargetsTruncated() != null) {
                // set the icon
                setIcon(FontAwesome.INFO_CIRCLE);
                setDescription(i18n.get("label.target.filter.truncated", filterManagementUIState.getTargetsTruncated(),
                        SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES));

            } else {
                setIcon(null);
                setDescription(null);
            }
            targetMessage.append(totalTargets);
            targetMessage.append(HawkbitCommonUtil.SP_STRING_SPACE);
            targetMessage.append(i18n.get("label.filter.shown"));
            if (totalTargets > SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES) {
                targetMessage.append(SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES);
            } else {
                targetMessage.append(HawkbitCommonUtil.SP_STRING_SPACE);
                targetMessage.append(totalTargets);
            }

            setCaption(targetMessage.toString());
        }

    }
}
