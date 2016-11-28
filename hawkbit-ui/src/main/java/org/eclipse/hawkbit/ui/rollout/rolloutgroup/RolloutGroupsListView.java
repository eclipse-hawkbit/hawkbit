/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgroup;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.grid.AbstractGridLayout;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Label;

/**
 * Groups List View.
 */
public class RolloutGroupsListView extends AbstractGridLayout {

    private static final long serialVersionUID = 7252345838154270259L;

    public RolloutGroupsListView(final I18N i18n, final UIEventBus eventBus,
            final RolloutGroupManagement rolloutGroupManagement, final RolloutUIState rolloutUIState,
            final SpPermissionChecker permissionChecker) {
        super(new RolloutGroupsListHeader(eventBus, rolloutUIState, i18n),
                new RolloutGroupListGrid(i18n, eventBus, rolloutGroupManagement, rolloutUIState, permissionChecker));

        buildLayout();
    }

    @Override
    protected boolean hasCountMessage() {
        return false;
    }

    @Override
    protected Label getCountMessageLabel() {
        return null;
    }

}
