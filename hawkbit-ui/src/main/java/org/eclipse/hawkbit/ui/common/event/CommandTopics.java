/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
