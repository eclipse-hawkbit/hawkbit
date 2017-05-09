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
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Groups List View.
 */
public class RolloutGroupsListView extends AbstractGridComponentLayout {

    private static final long serialVersionUID = 7252345838154270259L;

    private final SpPermissionChecker permissionChecker;
    private final RolloutUIState rolloutUIState;
    private final transient RolloutGroupManagement rolloutGroupManagement;

    /**
     * Constructor for RolloutGroupsListView
     * 
     * @param i18n
     *            I18N
     * @param eventBus
     *            UIEventBus
     * @param rolloutGroupManagement
     *            RolloutGroupManagement
     * @param rolloutUIState
     *            RolloutUIState
     * @param permissionChecker
     *            SpPermissionChecker
     */
    public RolloutGroupsListView(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final RolloutGroupManagement rolloutGroupManagement, final RolloutUIState rolloutUIState,
            final SpPermissionChecker permissionChecker) {
        super(i18n, eventBus);
        this.permissionChecker = permissionChecker;
        this.rolloutUIState = rolloutUIState;
        this.rolloutGroupManagement = rolloutGroupManagement;
        init();
    }

    @Override
    public RolloutGroupsListHeader createGridHeader() {
        return new RolloutGroupsListHeader(eventBus, rolloutUIState, i18n);
    }

    @Override
    public AbstractGrid<LazyQueryContainer> createGrid() {
        return new RolloutGroupListGrid(i18n, eventBus, rolloutGroupManagement, rolloutUIState, permissionChecker);
    }

}
