/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.layout.listener.LayoutVisibilityListener;
import org.eclipse.hawkbit.ui.common.layout.listener.LayoutVisibilityListener.VisibilityHandler;
import org.eclipse.hawkbit.ui.rollout.rollout.RolloutGridLayout;
import org.eclipse.hawkbit.ui.rollout.rolloutgroup.RolloutGroupGridLayout;
import org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets.RolloutGroupTargetGridLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * Rollout management view.
 */
@UIScope
@SpringView(name = RolloutView.VIEW_NAME, ui = AbstractHawkbitUI.class)
public class RolloutView extends VerticalLayout implements View {
    private static final long serialVersionUID = 1L;

    public static final String VIEW_NAME = "rollout";

    private final RolloutGridLayout rolloutsLayout;
    private final RolloutGroupGridLayout rolloutGroupsLayout;
    private final RolloutGroupTargetGridLayout rolloutGroupTargetsLayout;
    private final RolloutManagementUIState rolloutManagementUIState;

    private final transient LayoutVisibilityListener layoutVisibilityListener;

    @Autowired
    RolloutView(final SpPermissionChecker permissionChecker, final RolloutManagementUIState rolloutManagementUIState,
            final UIEventBus eventBus, final RolloutManagement rolloutManagement,
            final RolloutGroupManagement rolloutGroupManagement, final TargetManagement targetManagement,
            final UINotification uiNotification, final UiProperties uiProperties, final EntityFactory entityFactory,
            final VaadinMessageSource i18n, final TargetFilterQueryManagement targetFilterQueryManagement,
            final QuotaManagement quotaManagement, final TenantConfigurationManagement tenantConfigManagement,
            final DistributionSetManagement distributionSetManagement,
            final SystemSecurityContext systemSecurityContext) {
        this.rolloutManagementUIState = rolloutManagementUIState;

        this.rolloutsLayout = new RolloutGridLayout(permissionChecker, rolloutManagementUIState, eventBus,
                rolloutManagement, targetManagement, uiNotification, uiProperties, entityFactory, i18n,
                targetFilterQueryManagement, rolloutGroupManagement, quotaManagement, tenantConfigManagement,
                distributionSetManagement, systemSecurityContext);
        this.rolloutGroupsLayout = new RolloutGroupGridLayout(i18n, eventBus, rolloutGroupManagement,
                rolloutManagementUIState, permissionChecker);
        this.rolloutGroupTargetsLayout = new RolloutGroupTargetGridLayout(eventBus, i18n, rolloutGroupManagement,
                rolloutManagementUIState);

        final Map<EventLayout, VisibilityHandler> layoutVisibilityHandlers = new EnumMap<>(EventLayout.class);
        layoutVisibilityHandlers.put(EventLayout.ROLLOUT_LIST,
                new VisibilityHandler(this::showRolloutListLayout, this::showRolloutGroupListLayout));
        layoutVisibilityHandlers.put(EventLayout.ROLLOUT_GROUP_LIST,
                new VisibilityHandler(this::showRolloutGroupListLayout, this::showRolloutListLayout));
        layoutVisibilityHandlers.put(EventLayout.ROLLOUT_GROUP_TARGET_LIST,
                new VisibilityHandler(this::showRolloutGroupTargetsListLayout, this::showRolloutGroupListLayout));
        this.layoutVisibilityListener = new LayoutVisibilityListener(eventBus, new EventViewAware(EventView.ROLLOUT),
                layoutVisibilityHandlers);
    }

    @PostConstruct
    void init() {
        buildLayout();
        restoreState();
    }

    private void buildLayout() {
        setSpacing(false);
        setMargin(false);
        setSizeFull();

        addComponent(rolloutsLayout);
        setComponentAlignment(rolloutsLayout, Alignment.TOP_CENTER);
        setExpandRatio(rolloutsLayout, 1.0F);

        rolloutGroupsLayout.setVisible(false);
        addComponent(rolloutGroupsLayout);
        setComponentAlignment(rolloutGroupsLayout, Alignment.TOP_CENTER);
        setExpandRatio(rolloutGroupsLayout, 1.0F);

        rolloutGroupTargetsLayout.setVisible(false);
        addComponent(rolloutGroupTargetsLayout);
        setComponentAlignment(rolloutGroupTargetsLayout, Alignment.TOP_CENTER);
        setExpandRatio(rolloutGroupTargetsLayout, 1.0F);
    }

    private void showRolloutListLayout() {
        rolloutManagementUIState.setCurrentLayout(EventLayout.ROLLOUT_LIST);
        rolloutManagementUIState.setSelectedRolloutId(null);
        rolloutManagementUIState.setSelectedRolloutName("");
        rolloutManagementUIState.setSelectedRolloutGroupId(null);
        rolloutManagementUIState.setSelectedRolloutGroupName("");

        rolloutsLayout.setVisible(true);
        rolloutGroupsLayout.setVisible(false);
        rolloutGroupTargetsLayout.setVisible(false);
    }

    private void showRolloutGroupListLayout() {
        rolloutManagementUIState.setCurrentLayout(EventLayout.ROLLOUT_GROUP_LIST);
        rolloutManagementUIState.setSelectedRolloutGroupId(null);
        rolloutManagementUIState.setSelectedRolloutGroupName("");

        rolloutsLayout.setVisible(false);
        rolloutGroupsLayout.setVisible(true);
        rolloutGroupTargetsLayout.setVisible(false);
    }

    private void showRolloutGroupTargetsListLayout() {
        rolloutManagementUIState.setCurrentLayout(EventLayout.ROLLOUT_GROUP_TARGET_LIST);

        rolloutsLayout.setVisible(false);
        rolloutGroupsLayout.setVisible(false);
        rolloutGroupTargetsLayout.setVisible(true);
    }

    private void restoreState() {
        final EventLayout layout = rolloutManagementUIState.getCurrentLayout().orElse(EventLayout.ROLLOUT_LIST);
        switch (layout) {
        case ROLLOUT_LIST:
            showRolloutListLayout();
            break;
        case ROLLOUT_GROUP_LIST:
            showRolloutGroupListLayout();
            break;
        case ROLLOUT_GROUP_TARGET_LIST:
            showRolloutGroupTargetsListLayout();
            break;
        default:
            break;
        }

        rolloutsLayout.restoreState();
        rolloutGroupsLayout.restoreState();
        rolloutGroupTargetsLayout.restoreState();
    }

    @PreDestroy
    void destroy() {
        layoutVisibilityListener.unsubscribe();

        rolloutsLayout.unsubscribeListener();
        rolloutGroupsLayout.unsubscribeListener();
        rolloutGroupTargetsLayout.unsubscribeListener();
    }
}
