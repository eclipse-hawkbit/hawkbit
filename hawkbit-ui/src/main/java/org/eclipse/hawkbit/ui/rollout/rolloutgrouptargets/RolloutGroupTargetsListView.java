/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import org.eclipse.hawkbit.ui.common.grid.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Label;

/**
 * Rollout Group Targets List View.
 */
public class RolloutGroupTargetsListView extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 26089134783467012L;

    private final RolloutUIState rolloutUIState;

    public RolloutGroupTargetsListView(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final RolloutUIState rolloutUIState) {
        super(i18n, eventBus);
        this.rolloutUIState = rolloutUIState;
        this.setFooterSupport(new RolloutTargetsCountFooterSupport());
        init();
    }

    @Override
    public AbstractOrderedLayout createGridHeader() {
        return new RolloutGroupTargetsListHeader(eventBus, i18n, rolloutUIState);
    }

    @Override
    public RolloutGroupTargetsListGrid createGrid() {
        return new RolloutGroupTargetsListGrid(i18n, eventBus, rolloutUIState);
    }

    class RolloutTargetsCountFooterSupport extends AbstractFooterSupport {
        private static final long serialVersionUID = 3300400541786329735L;

        @Override
        protected Label getFooterMessageLabel() {
            final RolloutGroupTargetsCountLabelMessage countMessageLabel = new RolloutGroupTargetsCountLabelMessage(
                    rolloutUIState, (RolloutGroupTargetsListGrid) getGrid(), i18n, eventBus);
            countMessageLabel.setId(UIComponentIdProvider.ROLLOUT_GROUP_TARGET_LABEL);
            return countMessageLabel;
        }
    }
}
