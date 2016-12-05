/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.grid.AbstractGridLayout;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Label;

/**
 * Rollout list view.
 */
public class RolloutListView extends AbstractGridLayout {

    private static final long serialVersionUID = -2703552177439393208L;

    public RolloutListView(final SpPermissionChecker permissionChecker, final RolloutUIState rolloutUIState,
            final UIEventBus eventBus, final RolloutManagement rolloutManagement,
            final TargetManagement targetManagement, final UINotification uiNotification,
            final UiProperties uiProperties, final EntityFactory entityFactory, final I18N i18n) {
        super(new RolloutListHeader(permissionChecker, rolloutUIState, eventBus, rolloutManagement, targetManagement,
                uiNotification, uiProperties, entityFactory, i18n),
                new RolloutListGrid(i18n, eventBus, rolloutManagement, uiNotification, rolloutUIState,
                        permissionChecker, targetManagement, entityFactory, uiProperties));

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
