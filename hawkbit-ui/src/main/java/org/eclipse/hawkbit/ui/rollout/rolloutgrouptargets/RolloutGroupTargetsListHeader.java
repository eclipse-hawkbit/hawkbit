/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.grid.AbstractGridHeader;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Header Layout of Rollout Group Targets list view.
 */
public class RolloutGroupTargetsListHeader extends AbstractGridHeader {

    private static final long serialVersionUID = 5613986489156507581L;

    private final transient EventBus.UIEventBus eventBus;

    private Button rolloutNameLink;
    private Label headerCaption;

    public RolloutGroupTargetsListHeader(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final RolloutUIState rolloutUiState) {
        super(null, rolloutUiState, i18n);
        this.eventBus = eventBus;

        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final RolloutEvent event) {
        if (event == RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS) {
            setCaptionDetails();
        }
    }

    private void setCaptionDetails() {
        rolloutUIState.getRolloutGroup().map(RolloutGroup::getName).ifPresent(headerCaption::setCaption);
        rolloutNameLink.setCaption(rolloutUIState.getRolloutName().orElse(""));
    }

    @Override
    protected void resetSearchText() {
        /*
         * No implementation required.
         */
    }

    @Override
    protected String getSearchBoxId() {

        return null;
    }

    @Override
    protected String getSearchRestIconId() {

        return null;
    }

    @Override
    protected void searchBy(final String newSearchText) {
        // No implementation required.

    }

    @Override
    protected String getAddIconId() {

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
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
    }

    @Override
    protected boolean hasCreatePermission() {

        return false;
    }

    @Override
    protected String getCloseButtonId() {
        return UIComponentIdProvider.ROLLOUT_TARGET_VIEW_CLOSE_BUTTON_ID;
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

    @Override
    protected HorizontalLayout getHeaderCaptionLayout() {
        headerCaption = new LabelBuilder().name("").buildCaptionLabel();
        headerCaption.setStyleName(ValoTheme.LABEL_BOLD + " " + ValoTheme.LABEL_SMALL);
        final Button rolloutsListViewLink = SPUIComponentProvider.getButton(null, "", "", null, false, null,
                SPUIButtonStyleSmallNoBorder.class);
        rolloutsListViewLink.setStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link rollout-caption-links");
        rolloutsListViewLink.setDescription(i18n.getMessage("message.rollouts"));
        rolloutsListViewLink.setCaption(i18n.getMessage("message.rollouts"));
        rolloutsListViewLink.addClickListener(value -> showRolloutListView());

        rolloutNameLink = SPUIComponentProvider.getButton(null, "", "", null, false, null,
                SPUIButtonStyleSmallNoBorder.class);
        rolloutNameLink.setStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link rollout-caption-links");
        rolloutNameLink.setDescription("Rollout");
        rolloutNameLink.addClickListener(value -> showRolloutGroupListView());

        final HorizontalLayout headerCaptionLayout = new HorizontalLayout();
        headerCaptionLayout.addComponent(rolloutsListViewLink);
        headerCaptionLayout.addComponent(new Label(">"));
        headerCaptionLayout.addComponent(rolloutNameLink);
        headerCaptionLayout.addComponent(new Label("> "));
        headerCaptionLayout.addComponent(headerCaption);

        return headerCaptionLayout;
    }

    private void showRolloutGroupListView() {
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
    }

    private void showRolloutListView() {
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUTS);
    }

    @Override
    protected void restoreCaption() {
        setCaptionDetails();
    }

}
