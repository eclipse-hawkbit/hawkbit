/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgroup;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
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
 * Header Layout of Rollout Group list view.
 */
public class RolloutGroupGridHeader extends AbstractBreadcrumbGridHeader
        implements MasterEntityAwareComponent<ProxyRollout> {
    private static final long serialVersionUID = 1L;

    private final RolloutManagementUIState rolloutManagementUIState;

    RolloutGroupGridHeader(final CommonUiDependencies uiDependencies, final RolloutManagementUIState rolloutManagementUIState) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.rolloutManagementUIState = rolloutManagementUIState;

        final BreadcrumbLink rolloutsLink = new BreadcrumbLink(i18n.getMessage("message.rollouts"),
                i18n.getMessage("message.rollouts"), this::closeRolloutGroups);
        addBreadcrumbLink(rolloutsLink);

        final CloseHeaderSupport closeHeaderSupport = new CloseHeaderSupport(i18n,
                UIComponentIdProvider.ROLLOUT_GROUP_CLOSE, this::closeRolloutGroups);
        addHeaderSupport(closeHeaderSupport);

        buildHeader();
    }

    /**
     * Close rollout group grid
     */
    private void closeRolloutGroups() {
        eventBus.publish(CommandTopics.CHANGE_LAYOUT_VISIBILITY, this, new LayoutVisibilityEventPayload(
                VisibilityType.HIDE, EventLayout.ROLLOUT_GROUP_LIST, EventView.ROLLOUT));
    }

    @Override
    protected String getHeaderCaptionDetailsId() {
        return UIComponentIdProvider.ROLLOUT_GROUP_HEADER_CAPTION;
    }

    @Override
    public void masterEntityChanged(final ProxyRollout masterEntity) {
        headerCaptionDetails.setValue(masterEntity != null ? masterEntity.getName() : "");
    }

    @Override
    protected void restoreCaption() {
        headerCaptionDetails.setValue(rolloutManagementUIState.getSelectedRolloutName());
    }
}
