/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import org.eclipse.hawkbit.ui.common.grid.AbstractGridLayout;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Label;

/**
 * Rollout Group Targets List View.
 */
public class RolloutGroupTargetsListView extends AbstractGridLayout {

    private static final long serialVersionUID = 26089134783467012L;

    private final RolloutGroupTargetsCountLabelMessage rolloutGroupTargetsCountLabelMessage;

    public RolloutGroupTargetsListView(final UIEventBus eventBus, final I18N i18n,
            final RolloutUIState rolloutUIState) {
        super(new RolloutGroupTargetsListHeader(eventBus, i18n, rolloutUIState),
                new RolloutGroupTargetsListGrid(i18n, eventBus, rolloutUIState));

        this.rolloutGroupTargetsCountLabelMessage = new RolloutGroupTargetsCountLabelMessage(rolloutUIState,
                (RolloutGroupTargetsListGrid) grid, i18n, eventBus);

        buildLayout();
    }

    @Override
    protected boolean hasCountMessage() {

        return true;
    }

    @Override
    protected Label getCountMessageLabel() {

        return rolloutGroupTargetsCountLabelMessage;
    }

}
