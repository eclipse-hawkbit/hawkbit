/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.Arrays;

import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.event.ShowFormEventPayload;
import org.eclipse.hawkbit.ui.common.event.ShowFormEventPayload.FormType;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractGridHeader;
import org.eclipse.hawkbit.ui.common.grid.header.support.AddHeaderSupport;
import org.eclipse.hawkbit.ui.common.grid.header.support.SearchHeaderSupport;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.filtermanagement.state.TargetFilterGridLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;

import com.vaadin.ui.Component;

/**
 * Layout for Custom Filter view
 */
public class TargetFilterGridHeader extends AbstractGridHeader {
    private static final long serialVersionUID = 1L;

    private final TargetFilterGridLayoutUiState uiState;

    private final transient SearchHeaderSupport searchHeaderSupport;
    private final transient AddHeaderSupport addHeaderSupport;

    /**
     * Constructor for TargetFilterHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param uiState
     *            TargetFilterGridLayoutUiState
     */
    public TargetFilterGridHeader(final CommonUiDependencies uiDependencies, final TargetFilterGridLayoutUiState uiState) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.uiState = uiState;

        this.searchHeaderSupport = new SearchHeaderSupport(i18n, UIComponentIdProvider.TARGET_FILTER_SEARCH_TEXT,
                UIComponentIdProvider.TARGET_FILTER_TBL_SEARCH_RESET_ID, this::getSearchTextFromUiState,
                this::searchBy);

        if (permChecker.hasCreateTargetPermission()) {
            this.addHeaderSupport = new AddHeaderSupport(i18n, UIComponentIdProvider.TARGET_FILTER_ADD_ICON_ID,
                    this::addNewItem, () -> false);
        } else {
            this.addHeaderSupport = null;
        }
        addHeaderSupports(Arrays.asList(searchHeaderSupport, addHeaderSupport));

        restoreState();
        buildHeader();
    }

    @Override
    protected Component getHeaderCaption() {
        return SPUIComponentProvider.generateCaptionLabel(i18n, UIMessageIdProvider.CAPTION_FILTER_CUSTOM);
    }

    private String getSearchTextFromUiState() {
        return uiState.getSearchFilterInput();
    }

    private void searchBy(final String newSearchText) {
        eventBus.publish(EventTopics.FILTER_CHANGED, this, new FilterChangedEventPayload<>(ProxyTargetFilterQuery.class,
                FilterType.SEARCH, newSearchText, EventView.TARGET_FILTER));

        uiState.setSearchFilterInput(newSearchText);
    }

    private void addNewItem() {
        eventBus.publish(CommandTopics.SHOW_ENTITY_FORM_LAYOUT, this, new ShowFormEventPayload<ProxyTargetFilterQuery>(
                FormType.ADD, ProxyTargetFilterQuery.class, EventView.TARGET_FILTER));
    }

    @Override
    public void restoreState() {
        this.searchHeaderSupport.restoreState();
    }
}
