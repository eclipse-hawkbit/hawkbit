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
 * Actions for layout and view
 */
public final class CommandTopics {
    public static final String CHANGE_GRID_ACTIONS_VISIBILITY = "changeGridActionsVisibility";
    public static final String CHANGE_LAYOUT_VISIBILITY = "changeLayoutVisibility";
    public static final String RESIZE_LAYOUT = "resizeLayout";
    public static final String SHOW_ENTITY_FORM_LAYOUT = "showEntityForm";
    public static final String SELECT_GRID_ENTITY = "selectGridEntity";

    private CommandTopics() {
    }
}
