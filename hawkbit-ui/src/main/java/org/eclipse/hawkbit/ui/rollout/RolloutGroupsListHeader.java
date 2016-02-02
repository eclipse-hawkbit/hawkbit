/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Header Layout of Rollout Group list view.
 *
 */
@SpringComponent
@ViewScope
public class RolloutGroupsListHeader extends AbstractSimpleTableHeader {

    private static final long serialVersionUID = 5077741997839715209L;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private RolloutUIState rolloutUiState;

    @Autowired
    private I18N i18n;

    private Label headerCaption;

    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final RolloutEvent event) {
        if (event == RolloutEvent.SHOW_ROLLOUT_GROUPS) {
            setCaptionDetails();
        }
    }

    private void setCaptionDetails() {
        headerCaption.setCaption(rolloutUiState.getRolloutName().isPresent() ? rolloutUiState.getRolloutName().get()
                : "");
    }

    @Override
    protected void resetSearchText() {
 /**
         * No implementation required.
         */
    }

    @Override
    protected String getSearchBoxId() {
    /**
         * No implementation required.
         */
        return null;
    }

    @Override
    protected String getSearchRestIconId() {
    /**
         * No implementation required.
         */
        return null;
    }

    @Override
    protected void searchBy(final String newSearchText) {
    /**
         * No implementation required.
         */

    }

    @Override
    protected String getAddIconId() {
    /**
         * No implementation required.
         */
        return null;
    }

    @Override
    protected void addNewItem(final ClickEvent event) {
/**
         * No implementation required.
         */
    }

    @Override
    protected void onClose(final ClickEvent event) {
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUTS);

    }

    @Override
    protected boolean hasCreatePermission() {

        return true;
    }

    @Override
    protected String getCloseButtonId() {
        return SPUIComponetIdProvider.ROLLOUT_GROUP_CLOSE;
    }

    @Override
    protected boolean showCloseButton() {

        return true;
    }

    @Override
    protected boolean isAllowSearch() {
        return false;
    }

    @Override
    protected String onLoadSearchBoxValue() {
        return null;
    }

    @Override
    protected boolean isRollout() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.rollout.AbstractSimpleTableHeader#
     * getHeaderCaptionLayout()
     */
    @Override
    protected HorizontalLayout getHeaderCaptionLayout() {
        headerCaption = SPUIComponentProvider.getLabel("", SPUILabelDefinitions.SP_WIDGET_CAPTION);
        headerCaption.setId(SPUIComponetIdProvider.ROLLOUT_GROUP_HEADER_CAPTION);
        final Button rolloutsListViewLink = SPUIComponentProvider.getButton(null, "", "", null, false, null,
                SPUIButtonStyleSmallNoBorder.class);
        rolloutsListViewLink.setStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link rollout-caption-links");
        rolloutsListViewLink.setDescription(i18n.get("message.rollouts"));
        rolloutsListViewLink.setCaption(i18n.get("message.rollouts"));
        rolloutsListViewLink.addClickListener(value -> showRolloutListView());

        final HorizontalLayout headerCaptionLayout = new HorizontalLayout();
        headerCaptionLayout.addComponent(rolloutsListViewLink);
        headerCaptionLayout.addComponent(new Label(">"));
        headerCaptionLayout.addComponent(headerCaption);

        return headerCaptionLayout;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.ui.rollout.AbstractSimpleTableHeader#restoreCaption()
     */
    @Override
    protected void restoreCaption() {
        setCaptionDetails();
    }

    private void showRolloutListView() {
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUTS);
    }

}
