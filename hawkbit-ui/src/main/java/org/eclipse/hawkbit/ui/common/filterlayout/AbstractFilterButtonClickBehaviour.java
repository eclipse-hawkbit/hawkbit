/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.io.Serializable;

/**
 * Abstract button click behaviour of filter buttons layout.
 * 
 * @param <T>
 *            The type of the Filter Button
 */
public abstract class AbstractFilterButtonClickBehaviour<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Process filter click
     *
     * @param clickedFilter
     *            Generic type of filter button
     */
    public abstract void processFilterClick(final T clickedFilter);

    /**
     * Verifies if filter is previously clicked
     *
     * @param clickedFilter
     *            Generic type of filter button
     * @return {@code true}: if the filter button was clicked before
     *         {@code false}: otherwise
     */
    public abstract boolean isFilterPreviouslyClicked(final T clickedFilter);

    /**
     * Filter unClicked
     *
     * @param clickedFilter
     *            Generic type of filter button
     */
    protected abstract void filterUnClicked(final T clickedFilter);

    /**
     * Filter clicked
     *
     * @param clickedFilter
     *            Generic type of filter button
     */
    protected abstract void filterClicked(final T clickedFilter);

    /**
     * Filter button click behaviour types
     */
    public enum ClickBehaviourType {
        CLICKED, UNCLICKED;
    }
}
