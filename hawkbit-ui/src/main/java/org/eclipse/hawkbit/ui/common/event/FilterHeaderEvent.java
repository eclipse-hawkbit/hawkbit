/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Event class for the table header of the tags and types table. Event is fired
 * when the edit or delete modus of a tag or type is closed by clicking on the
 * cancel icon. The menubar for selecting a tag/type action is shown again.
 */
public class FilterHeaderEvent {

    /**
     * FilterHeaderEnum which describes the action to execute
     */
    public enum FilterHeaderEnum {
        SHOW_MENUBAR, SHOW_CANCEL_BUTTON
    }

    private final FilterHeaderEnum filterHeaderEnum;

    /**
     * Constructor
     * 
     * @param filterHeaderEnum
     *            Enum which describes the action to execute
     */
    public FilterHeaderEvent(final FilterHeaderEnum filterHeaderEnum) {
        this.filterHeaderEnum = filterHeaderEnum;
    }

    public FilterHeaderEnum getFilterHeaderEnum() {
        return filterHeaderEnum;
    }

}
