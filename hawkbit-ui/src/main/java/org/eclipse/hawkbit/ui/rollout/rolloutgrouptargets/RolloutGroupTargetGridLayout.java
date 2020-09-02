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
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRollout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyRolloutGroup;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;
import org.eclipse.hawkbit.ui.filtermanagement.TargetFilterCountMessageLabel;
import org.eclipse.hawkbit.ui.rollout.RolloutManagementUIState;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Rollout Group Targets List View.
 */
public class RolloutGroupTargetGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final RolloutGroupTargetGridHeader rolloutGroupTargetsListHeader;
    private final RolloutGroupTargetGrid rolloutGroupTargetsListGrid;
    private final transient TargetFilterCountMessageLabel rolloutGroupTargetCountMessageLabel;

    private final transient SelectionChangedListener<ProxyRollout> rolloutChangedListener;
    private final transient SelectionChangedListener<ProxyRolloutGroup> rolloutGroupChangedListener;

    /**
     * Constructor for RolloutGroupTargetGridLayout
     *
     * @param eventBus
     *          UIEventBus
     * @param i18n
     *          VaadinMessageSource
     * @param rolloutGroupManagement
     *          RolloutGroupManagement
     * @param rolloutManagementUIState
     *          RolloutManagementUIState
     */
    public RolloutGroupTargetGridLayout(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final RolloutGroupManagement rolloutGroupManagement,
            final RolloutManagementUIState rolloutManagementUIState) {
        this.rolloutGroupTargetsListHeader = new RolloutGroupTargetGridHeader(eventBus, i18n, rolloutManagementUIState);
        this.rolloutGroupTargetsListGrid = new RolloutGroupTargetGrid(i18n, eventBus, rolloutGroupManagement,
                rolloutManagementUIState);
        this.rolloutGroupTargetCountMessageLabel = new TargetFilterCountMessageLabel(i18n);

        initGridDataUpdatedListener();

        final EventLayoutViewAware rolloutLayoutView = new EventLayoutViewAware(EventLayout.ROLLOUT_LIST,
                EventView.ROLLOUT);
        final EventLayoutViewAware rolloutGroupLayoutView = new EventLayoutViewAware(EventLayout.ROLLOUT_GROUP_LIST,
                EventView.ROLLOUT);

        this.rolloutChangedListener = new SelectionChangedListener<>(eventBus, rolloutLayoutView,
                Collections.singletonList(rolloutGroupTargetsListHeader::rolloutChanged));
        this.rolloutGroupChangedListener = new SelectionChangedListener<>(eventBus, rolloutGroupLayoutView,
                getMasterEntityAwareComponents());

        buildLayout(rolloutGroupTargetsListHeader, rolloutGroupTargetsListGrid, rolloutGroupTargetCountMessageLabel);
    }

    private void initGridDataUpdatedListener() {
        rolloutGroupTargetsListGrid.addDataChangedListener(event -> rolloutGroupTargetCountMessageLabel
                .updateTotalFilteredTargetsCount(rolloutGroupTargetsListGrid.getDataSize()));
    }

    private List<MasterEntityAwareComponent<ProxyRolloutGroup>> getMasterEntityAwareComponents() {
        return Arrays.asList(rolloutGroupTargetsListHeader, rolloutGroupTargetsListGrid.getMasterEntitySupport());
    }

    /**
     * Restore the rollout group target list state
     */
    public void restoreState() {
        rolloutGroupTargetsListHeader.restoreState();
        rolloutGroupTargetsListGrid.restoreState();
    }

    /**
     * unsubscribe all listener
     */
    public void unsubscribeListener() {
        rolloutChangedListener.unsubscribe();
        rolloutGroupChangedListener.unsubscribe();
    }
}
