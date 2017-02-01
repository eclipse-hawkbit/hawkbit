/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import org.eclipse.hawkbit.ui.common.grid.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.grid.DefaultGridHeader;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout responsible for action-states-grid and the corresponding header.
 */
public class ActionStatusLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    protected final ManagementUIState managementUIState;

    /**
     * Constructor.
     *
     * @param i18n
     * @param eventBus
     * @param managementUIState
     */
    public ActionStatusLayout(I18N i18n, UIEventBus eventBus, ManagementUIState managementUIState) {
        super(i18n, eventBus);
        this.managementUIState = managementUIState;
        init();
    }

    @Override
    public DefaultGridHeader createGridHeader() {
        return new DefaultGridHeader(managementUIState, "Action States").init();
    }

    @Override
    public ActionStatusGrid createGrid() {
        return new ActionStatusGrid(i18n, eventBus);
    }

}
