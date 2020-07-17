/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;

/**
 * Multi button click behaviour of tag filter buttons layout.
 */
public class TagFilterButtonClick extends AbstractFilterMultiButtonClick<ProxyTag> {
    private static final long serialVersionUID = 1L;

    private final transient Consumer<Map<Long, String>> filterChangedCallback;
    private final transient Consumer<ClickBehaviourType> noTagChangedCallback;

    TagFilterButtonClick(final Consumer<Map<Long, String>> filterChangedCallback,
            final Consumer<ClickBehaviourType> noTagChangedCallback) {
        this.filterChangedCallback = filterChangedCallback;
        this.noTagChangedCallback = noTagChangedCallback;
    }

    @Override
    protected void filterUnClicked(final ProxyTag clickedFilter) {
        if (clickedFilter.isNoTag()) {
            noTagChangedCallback.accept(ClickBehaviourType.UNCLICKED);
        } else {
            filterChangedCallback.accept(previouslyClickedFilterIdsWithName);
        }
    }

    @Override
    protected void filterClicked(final ProxyTag clickedFilter) {
        if (clickedFilter.isNoTag()) {
            noTagChangedCallback.accept(ClickBehaviourType.CLICKED);
        } else {
            filterChangedCallback.accept(previouslyClickedFilterIdsWithName);
        }
    }
}
