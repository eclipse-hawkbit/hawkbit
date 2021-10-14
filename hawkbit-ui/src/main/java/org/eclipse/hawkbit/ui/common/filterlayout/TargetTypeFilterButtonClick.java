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

    private final transient BiConsumer<ProxyTargetType, ClickBehaviourType> filterChangedCallback;

    TargetTypeFilterButtonClick(final BiConsumer<ProxyTargetType, ClickBehaviourType> filterChangedCallback) {
        this.filterChangedCallback = filterChangedCallback;
    }

    @Override
    protected void filterUnClicked(ProxyTargetType clickedFilter) {
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.UNCLICKED);
    }

    @Override
    protected void filterClicked(ProxyTargetType clickedFilter) {
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.CLICKED);
    }

    @Override
    public boolean isFilterPreviouslyClicked(final ProxyTargetType clickedFilter) {
        return false;
    }
}
