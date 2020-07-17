/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;

/**
 * Abstract Single button click behaviour of filter buttons layout.
 * 
 * @param <T>
 *            The type of the Filter Button
 */
public abstract class AbstractFilterSingleButtonClick<T extends ProxyIdentifiableEntity>
        extends AbstractFilterButtonClickBehaviour<T> {
    private static final long serialVersionUID = 1L;

    protected Long previouslyClickedFilterId;

    @Override
    public void processFilterClick(final T clickedFilter) {
        if (isFilterPreviouslyClicked(clickedFilter)) {
            previouslyClickedFilterId = null;
            filterUnClicked(clickedFilter);
        } else {
            previouslyClickedFilterId = clickedFilter.getId();
            filterClicked(clickedFilter);
        }
    }

    @Override
    public boolean isFilterPreviouslyClicked(final T clickedFilter) {
        return previouslyClickedFilterId != null && previouslyClickedFilterId.equals(clickedFilter.getId());
    }

    /**
     * Sets the id of previously clicked filter
     *
     * @param id
     *          Filter id
     */
    public void setPreviouslyClickedFilterId(final Long id) {
        this.previouslyClickedFilterId = id;
    }

    /**
     * @return Id of previously clicked filter
     */
    public Long getPreviouslyClickedFilterId() {
        return previouslyClickedFilterId;
    }
}
