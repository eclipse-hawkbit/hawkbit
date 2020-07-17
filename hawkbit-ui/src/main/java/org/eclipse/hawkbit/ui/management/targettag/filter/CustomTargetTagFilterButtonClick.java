/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.filter;

import java.util.function.BiConsumer;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterSingleButtonClick;

/**
 * Single button click behaviour of custom target filter buttons layout.
 *
 */
public class CustomTargetTagFilterButtonClick extends AbstractFilterSingleButtonClick<ProxyTargetFilterQuery> {
    private static final long serialVersionUID = 1L;

    private final transient BiConsumer<ProxyTargetFilterQuery, ClickBehaviourType> filterChangedCallback;

    /**
     * Constructor
     * 
     * @param filterChangedCallback
     *            filterChangedCallback
     */
    public CustomTargetTagFilterButtonClick(
            final BiConsumer<ProxyTargetFilterQuery, ClickBehaviourType> filterChangedCallback) {
        this.filterChangedCallback = filterChangedCallback;
    }

    @Override
    protected void filterUnClicked(final ProxyTargetFilterQuery clickedFilter) {
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.UNCLICKED);
    }

    @Override
    protected void filterClicked(final ProxyTargetFilterQuery clickedFilter) {
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.CLICKED);
    }
}
