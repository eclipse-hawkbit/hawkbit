/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.util.function.BiConsumer;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;

/**
 * Button click behaviour of target type filter buttons layout.
 */
public class TargetTypeFilterButtonClick extends AbstractFilterSingleButtonClick<ProxyTargetType> {
    private static final long serialVersionUID = 1L;

    private boolean noTargetTypeBtnClicked;

    private final transient BiConsumer<ProxyTargetType, ClickBehaviourType> filterChangedCallback;

    /**
     * Constructor
     *
     * @param filterChangedCallback
     *            filterChangedCallback
     */
    public TargetTypeFilterButtonClick(
            final BiConsumer<ProxyTargetType, ClickBehaviourType> filterChangedCallback) {
        this.filterChangedCallback = filterChangedCallback;
        this.noTargetTypeBtnClicked = false;
    }

    @Override
    public void processFilterClick(final ProxyTargetType clickedFilter) {
        if (isFilterPreviouslyClicked(clickedFilter) || isNoTargetTypePreviouslyClicked(clickedFilter)) {
            previouslyClickedFilterId = null;
            filterUnClicked(clickedFilter);
        } else {
            previouslyClickedFilterId = clickedFilter.getId();
            filterClicked(clickedFilter);
        }
    }

    @Override
    protected void filterUnClicked(ProxyTargetType clickedFilter) {
        if (clickedFilter.isNoTargetType()){
            noTargetTypeBtnClicked = false;
        }
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.UNCLICKED);
    }

    @Override
    protected void filterClicked(ProxyTargetType clickedFilter) {
        noTargetTypeBtnClicked = clickedFilter.isNoTargetType();
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.CLICKED);
    }

    @Override
    public boolean isFilterPreviouslyClicked(final ProxyTargetType clickedFilter) {
        return (previouslyClickedFilterId != null && previouslyClickedFilterId.equals(clickedFilter.getId()));
    }

    private boolean isNoTargetTypePreviouslyClicked(ProxyTargetType clickedFilter) {
        return clickedFilter.isNoTargetType() && isNoTargetTypeBtnClicked();
    }

    /**
     * @return true if no target type button clicked
     */
    public boolean isNoTargetTypeBtnClicked() {
        return noTargetTypeBtnClicked;
    }

}
