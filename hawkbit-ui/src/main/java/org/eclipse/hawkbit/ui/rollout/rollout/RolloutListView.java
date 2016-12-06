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
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.AbstractOrderedLayout;

/**
 * Rollout list view.
 */
public class RolloutListView extends AbstractGridComponentLayout {
    private static final long serialVersionUID = -2703552177439393208L;

    private final transient RolloutManagement rolloutManagement;
    private final transient TargetManagement targetManagement;
    private final transient EntityFactory entityFactory;

    private final SpPermissionChecker permissionChecker;
    private final RolloutUIState rolloutUIState;
    private final UINotification uiNotification;
    private final UiProperties uiProperties;


    public RolloutListView(final SpPermissionChecker permissionChecker, final RolloutUIState rolloutUIState,
            final UIEventBus eventBus, final RolloutManagement rolloutManagement,
            final TargetManagement targetManagement, final UINotification uiNotification,
            final UiProperties uiProperties, final EntityFactory entityFactory, final I18N i18n) {
        super(i18n, eventBus);
        this.permissionChecker = permissionChecker;
        this.rolloutUIState = rolloutUIState;
        this.rolloutManagement = rolloutManagement;
        this.targetManagement = targetManagement;
        this.uiNotification = uiNotification;
        this.uiProperties = uiProperties;
        this.entityFactory = entityFactory;
        init();
    }

    @Override
    public AbstractOrderedLayout createGridHeader() {
        return new RolloutListHeader(permissionChecker, rolloutUIState, eventBus, rolloutManagement, targetManagement,
                uiNotification, uiProperties, entityFactory, i18n);
    }

    @Override
    public AbstractGrid<LazyQueryContainer> createGrid() {
        return new RolloutListGrid(i18n, eventBus, rolloutManagement, uiNotification, rolloutUIState, permissionChecker,
                targetManagement, entityFactory, uiProperties);
    }

}
