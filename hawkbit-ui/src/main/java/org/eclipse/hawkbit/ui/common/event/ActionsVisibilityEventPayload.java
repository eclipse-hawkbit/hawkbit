/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Action visibility event payload for layouts and views
 */
public class ActionsVisibilityEventPayload extends EventLayoutViewAware {
    private final ActionsVisibilityType actionsVisibilityType;

    /**
     * Constructor for ActionsVisibilityEventPayload
     *
     * @param actionsVisibilityType
     *          ActionsVisibilityType
     * @param layout
     *          EventLayout
     * @param view
     *          EventView
     */
    public ActionsVisibilityEventPayload(final ActionsVisibilityType actionsVisibilityType, final EventLayout layout,
            final EventView view) {
        super(layout, view);

        this.actionsVisibilityType = actionsVisibilityType;
    }

    /**
     * @return actionsVisibilityType
     */
    public ActionsVisibilityType getActionsVisibilityType() {
        return actionsVisibilityType;
    }

    /**
     * Action types for view and layout
     */
    public enum ActionsVisibilityType {
        SHOW_EDIT, SHOW_DELETE, HIDE_ALL;
    }
}
