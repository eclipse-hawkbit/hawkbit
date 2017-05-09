/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.footer;

import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/**
 * @author Venugopal Boodidadinne(RBEI/BSJ)
 * 
 *         Count message label which display current filter details and details
 *         on pinning.
 */
public class TargetFilterCountMessageLabel extends Label {

    private static final long serialVersionUID = -7188528790042766877L;

    private final FilterManagementUIState filterManagementUIState;

    private final VaadinMessageSource i18n;

    public TargetFilterCountMessageLabel(final FilterManagementUIState filterManagementUIState, final VaadinMessageSource i18n,
            final UIEventBus eventBus) {
        this.filterManagementUIState = filterManagementUIState;
        this.i18n = i18n;

        applyStyle();
        displayTargetFilterMessage();
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final CustomFilterUIEvent custFUIEvent) {
        if (custFUIEvent == CustomFilterUIEvent.TARGET_DETAILS_VIEW
                || custFUIEvent == CustomFilterUIEvent.CREATE_NEW_FILTER_CLICK
                || custFUIEvent == CustomFilterUIEvent.EXIT_CREATE_OR_UPDATE_FILTRER_VIEW
                || custFUIEvent == CustomFilterUIEvent.UPDATE_TARGET_FILTER_SEARCH_ICON) {
            UI.getCurrent().access(() -> displayTargetFilterMessage());
        }
    }

    private void applyStyle() {
        addStyleName(SPUILabelDefinitions.SP_LABEL_MESSAGE_STYLE);
        setContentMode(ContentMode.HTML);
        setId(UIComponentIdProvider.COUNT_LABEL);
    }

    private void displayTargetFilterMessage() {
        long totalTargets = 0;
        if (filterManagementUIState.isCreateFilterViewDisplayed() || filterManagementUIState.isEditViewDisplayed()) {
            if (null != filterManagementUIState.getFilterQueryValue()) {
                totalTargets = filterManagementUIState.getTargetsCountAll().get();
            }
            final StringBuilder targetMessage = new StringBuilder(i18n.getMessage("label.target.filtered.total"));
            if (filterManagementUIState.getTargetsTruncated() != null) {
                // set the icon
                setIcon(FontAwesome.INFO_CIRCLE);
                setDescription(i18n.getMessage("label.target.filter.truncated", filterManagementUIState.getTargetsTruncated(),
                        SPUIDefinitions.MAX_TABLE_ENTRIES));

            } else {
                setIcon(null);
                setDescription(null);
            }
            targetMessage.append(totalTargets);

            if (totalTargets > SPUIDefinitions.MAX_TABLE_ENTRIES) {
                targetMessage.append(HawkbitCommonUtil.SP_STRING_PIPE);
                targetMessage.append(i18n.getMessage("label.filter.shown"));
                targetMessage.append(SPUIDefinitions.MAX_TABLE_ENTRIES);
            }

            setCaption(targetMessage.toString());
        }

    }
}
