/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.util.function.BiConsumer;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;

/**
 * Single button click behaviour of filter buttons layout.
 */
public class TypeFilterButtonClick extends AbstractFilterSingleButtonClick<ProxyType> {
    private static final long serialVersionUID = 1L;

    private final transient BiConsumer<ProxyType, ClickBehaviourType> filterChangedCallback;

    /**
     * Constructor for TypeFilterButtonClick
     *
     * @param filterChangedCallback
     *          Callback event of ClickBehaviourType
     */
    public TypeFilterButtonClick(final BiConsumer<ProxyType, ClickBehaviourType> filterChangedCallback) {
        this.filterChangedCallback = filterChangedCallback;
    }

    @Override
    protected void filterUnClicked(final ProxyType clickedFilter) {
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.UNCLICKED);
    }

    @Override
    protected void filterClicked(final ProxyType clickedFilter) {
        filterChangedCallback.accept(clickedFilter, ClickBehaviourType.CLICKED);
    }
}
