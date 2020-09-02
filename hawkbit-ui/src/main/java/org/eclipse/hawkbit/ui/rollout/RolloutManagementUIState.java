/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.io.Serializable;
import java.util.Optional;

import org.eclipse.hawkbit.ui.common.event.EventLayout;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Stores rollout management view UI state according to user interactions.
 *
 */
@VaadinSessionScope
@SpringComponent
public class RolloutManagementUIState implements Serializable {
    private static final long serialVersionUID = 1L;

    private EventLayout currentLayout;
    private String searchText;

    private Long selectedRolloutId;
    private String selectedRolloutName;

    private Long selectedRolloutGroupId;
    private String selectedRolloutGroupName;

    /**
     * @return Current event layout
     */
    public Optional<EventLayout> getCurrentLayout() {
        return Optional.ofNullable(currentLayout);
    }

    /**
     * Sets the current event layout
     *
     * @param currentLayout
     *          EventLayout
     */
    public void setCurrentLayout(final EventLayout currentLayout) {
        this.currentLayout = currentLayout;
    }

    /**
     * @return Search text
     */
    public Optional<String> getSearchText() {
        return Optional.ofNullable(searchText);
    }

    /**
     * Sets the search text
     *
     * @param searchText
     *          Text
     */
    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

    /**
     * @return Selected rollout id
     */
    public Long getSelectedRolloutId() {
        return selectedRolloutId;
    }

    /**
     * Sets the selected rollout id
     *
     * @param selectedRolloutId
     *          Id
     */
    public void setSelectedRolloutId(final Long selectedRolloutId) {
        this.selectedRolloutId = selectedRolloutId;
    }

    /**
     * @return Selected rollout name
     */
    public String getSelectedRolloutName() {
        return selectedRolloutName;
    }

    /**
     * Sets the selected rollout name
     *
     * @param selectedRolloutName
     *          name
     */
    public void setSelectedRolloutName(final String selectedRolloutName) {
        this.selectedRolloutName = selectedRolloutName;
    }

    /**
     * @return Selected rollout group id
     */
    public Long getSelectedRolloutGroupId() {
        return selectedRolloutGroupId;
    }

    /**
     * Sets the selected rollout group id
     *
     * @param selectedRolloutGroupId
     *          Group id
     */
    public void setSelectedRolloutGroupId(final Long selectedRolloutGroupId) {
        this.selectedRolloutGroupId = selectedRolloutGroupId;
    }

    /**
     * @return Selected rollout group id
     */
    public String getSelectedRolloutGroupName() {
        return selectedRolloutGroupName;
    }

    /**
     * Sets the selected rollout group name
     *
     * @param selectedRolloutGroupName
     *          Group name
     */
    public void setSelectedRolloutGroupName(final String selectedRolloutGroupName) {
        this.selectedRolloutGroupName = selectedRolloutGroupName;
    }
}
