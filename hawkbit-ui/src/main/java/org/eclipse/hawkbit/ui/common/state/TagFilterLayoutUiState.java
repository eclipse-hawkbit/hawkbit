/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Tag filter layout ui state
 */
public class TagFilterLayoutUiState extends HidableLayoutUiState {
    private static final long serialVersionUID = 1L;

    private boolean noTagClicked;
    private boolean noTargetTypeClicked;
    private final Map<Long, String> clickedTagIdsWithName = new HashMap<>();

    /**
     * @return True if no targetType is clicked or selected
     */
    public boolean isNoTargetTypeClicked() {
        return noTargetTypeClicked;
    }

    /**
     * Sets the status of no targetType clicked
     *
     * @param noTargetTypeClicked
     *          boolean
     */
    public void setNoTargetTypeClicked(boolean noTargetTypeClicked) {
        this.noTargetTypeClicked = noTargetTypeClicked;
    }

    /**
     * @return True if not tag is clicked or selected
     */
    public boolean isNoTagClicked() {
        return noTagClicked;
    }

    /**
     * Sets the status of no tag clicked
     *
     * @param noTagClicked
     *          boolean
     */
    public void setNoTagClicked(final boolean noTagClicked) {
        this.noTagClicked = noTagClicked;
    }

    /**
     * @return Key value pair of clicked tags with id and name
     */
    public Map<Long, String> getClickedTagIdsWithName() {
        return clickedTagIdsWithName;
    }

    /**
     * Sets the clicked tag id and name
     *
     * @param clickedTagIdsWithName
     *          Key value pairs
     */
    public void setClickedTagIdsWithName(final Map<Long, String> clickedTagIdsWithName) {
        this.clickedTagIdsWithName.clear();
        this.clickedTagIdsWithName.putAll(clickedTagIdsWithName);
    }
}
