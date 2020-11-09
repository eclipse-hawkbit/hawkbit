/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutGroup;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload.VisibilityType;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractBreadcrumbGridHeader;
import org.eclipse.hawkbit.ui.common.grid.header.support.CloseHeaderSupport;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.rollout.RolloutManagementUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

/**
 * Header Layout of Rollout Group Targets list view.
 */
public class RolloutGroupTargetGridHeader extends AbstractBreadcrumbGridHeader
        implements MasterEntityAwareComponent<ProxyRolloutGroup> {
    private static final long serialVersionUID = 1L;

    private final RolloutManagementUIState rolloutManagementUIState;

    private final transient Consumer<String> setRolloutNameCallback;

    RolloutGroupTargetGridHeader(final CommonUiDependencies uiDependencies,
            final RolloutManagementUIState rolloutManagementUIState) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.rolloutManagementUIState = rolloutManagementUIState;

        final BreadcrumbLink rolloutsLink = new BreadcrumbLink(i18n.getMessage("message.rollouts"),
                i18n.getMessage("message.rollouts"), this::showRolloutListView);
        final BreadcrumbLink rolloutNameLink = new BreadcrumbLink("", i18n.getMessage("dashboard.rollouts.caption"),
                this::closeRolloutGroupTargets);
        this.setRolloutNameCallback = rolloutNameLink.getSetCaptionCallback();
        addBreadcrumbLinks(Arrays.asList(rolloutsLink, rolloutNameLink));

        final CloseHeaderSupport closeHeaderSupport = new CloseHeaderSupport(i18n,
                UIComponentIdProvider.ROLLOUT_TARGET_VIEW_CLOSE_BUTTON_ID, this::closeRolloutGroupTargets);
        addHeaderSupport(closeHeaderSupport);

        buildHeader();
    }

    private void showRolloutListView() {
        eventBus.publish(CommandTopics.CHANGE_LAYOUT_VISIBILITY, this,
                new LayoutVisibilityEventPayload(VisibilityType.SHOW, EventLayout.ROLLOUT_LIST, EventView.ROLLOUT));
    }

    private void closeRolloutGroupTargets() {
        eventBus.publish(CommandTopics.CHANGE_LAYOUT_VISIBILITY, this, new LayoutVisibilityEventPayload(
                VisibilityType.HIDE, EventLayout.ROLLOUT_GROUP_TARGET_LIST, EventView.ROLLOUT));
    }

    /**
     * Updates the rollout name in the rollout group target header
     *
     * @param rollout
     *            ProxyRollout
     */
    public void rolloutChanged(final ProxyRollout rollout) {
        if (setRolloutNameCallback != null) {
            setRolloutNameCallback.accept(rollout != null ? rollout.getName() : "");
        }
    }

    @Override
    protected String getHeaderCaptionDetailsId() {
        return UIComponentIdProvider.ROLLOUT_GROUP_TARGETS_HEADER_CAPTION;
    }

    @Override
    public void masterEntityChanged(final ProxyRolloutGroup masterEntity) {
        headerCaptionDetails.setValue(masterEntity != null ? masterEntity.getName() : "");
    }

    @Override
    protected void restoreCaption() {
        if (setRolloutNameCallback != null) {
            setRolloutNameCallback.accept(rolloutManagementUIState.getSelectedRolloutName());
        }
        headerCaptionDetails.setValue(rolloutManagementUIState.getSelectedRolloutGroupName());
    }
}
