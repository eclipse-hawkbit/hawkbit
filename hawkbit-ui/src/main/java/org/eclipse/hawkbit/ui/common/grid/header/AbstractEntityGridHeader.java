/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.header;

import java.util.Arrays;

import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutResizeEventPayload.ResizeType;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload;
import org.eclipse.hawkbit.ui.common.event.LayoutVisibilityEventPayload.VisibilityType;
import org.eclipse.hawkbit.ui.common.grid.header.support.AddHeaderSupport;
import org.eclipse.hawkbit.ui.common.grid.header.support.FilterButtonsHeaderSupport;
import org.eclipse.hawkbit.ui.common.grid.header.support.ResizeHeaderSupport;
import org.eclipse.hawkbit.ui.common.grid.header.support.SearchHeaderSupport;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.HidableLayoutUiState;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Abstract header for entity grids.
 */
public abstract class AbstractEntityGridHeader extends AbstractGridHeader {
    private static final long serialVersionUID = 1L;

    private final HidableLayoutUiState filterLayoutUiState;
    private final GridLayoutUiState gridLayoutUiState;

    private transient AddHeaderSupport addHeaderSupport;
    private transient AbstractEntityWindowBuilder<?> entityWindowBuilder;

    private final transient SearchHeaderSupport searchHeaderSupport;
    private final transient FilterButtonsHeaderSupport filterButtonsHeaderSupport;
    private final transient ResizeHeaderSupport resizeHeaderSupport;

    private final EventLayout filterLayout;
    private final EventView view;

    /**
     * Constructor for AbstractEntityGridHeader
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param filterLayoutUiState
     *            HidableLayoutUiState
     * @param gridLayoutUiState
     *            GridLayoutUiState
     * @param filterLayout
     *            EventLayout
     * @param view
     *            EventView
     */
    protected AbstractEntityGridHeader(final CommonUiDependencies uiDependencies,
            final HidableLayoutUiState filterLayoutUiState,
            final GridLayoutUiState gridLayoutUiState, final EventLayout filterLayout, final EventView view) {
        super(uiDependencies.getI18n(), uiDependencies.getPermChecker(), uiDependencies.getEventBus());

        this.filterLayoutUiState = filterLayoutUiState;
        this.gridLayoutUiState = gridLayoutUiState;
        this.filterLayout = filterLayout;
        this.view = view;

        this.searchHeaderSupport = new SearchHeaderSupport(i18n, getSearchFieldId(), getSearchResetIconId(),
                this::getSearchTextFromUiState, this::searchBy);
        this.filterButtonsHeaderSupport = new FilterButtonsHeaderSupport(i18n, getFilterButtonsIconId(),
                this::showFilterButtonsLayout, this::onLoadIsShowFilterButtonDisplayed);
        this.resizeHeaderSupport = new ResizeHeaderSupport(i18n, getMaxMinIconId(), this::maximizeTable,
                this::minimizeTable, this::onLoadIsTableMaximized);

        addHeaderSupports(Arrays.asList(searchHeaderSupport, filterButtonsHeaderSupport, resizeHeaderSupport));
    }

    @Override
    protected Component getHeaderCaption() {
        return SPUIComponentProvider.generateCaptionLabel(i18n, getCaptionMsg());
    }

    protected abstract String getCaptionMsg();

    protected abstract String getSearchFieldId();

    protected abstract String getSearchResetIconId();

    private String getSearchTextFromUiState() {
        return gridLayoutUiState.getSearchFilter();
    }

    private void searchBy(final String newSearchText) {
        eventBus.publish(EventTopics.FILTER_CHANGED, this,
                new FilterChangedEventPayload<>(getEntityType(), FilterType.SEARCH, newSearchText, view));

        gridLayoutUiState.setSearchFilter(newSearchText);
    }

    protected abstract Class<? extends ProxyIdentifiableEntity> getEntityType();

    protected abstract String getFilterButtonsIconId();

    private void showFilterButtonsLayout() {
        eventBus.publish(CommandTopics.CHANGE_LAYOUT_VISIBILITY, this,
                new LayoutVisibilityEventPayload(VisibilityType.SHOW, filterLayout, view));

        filterLayoutUiState.setHidden(false);
    }

    private Boolean onLoadIsShowFilterButtonDisplayed() {
        return filterLayoutUiState.isHidden();
    }

    protected abstract String getMaxMinIconId();

    protected void maximizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this,
                new LayoutResizeEventPayload(ResizeType.MAXIMIZE, getLayout(), view));

        if (addHeaderSupport != null) {
            addHeaderSupport.hideAddIcon();
        }

        gridLayoutUiState.setMaximized(true);
    }

    protected abstract EventLayout getLayout();

    protected void minimizeTable() {
        eventBus.publish(CommandTopics.RESIZE_LAYOUT, this,
                new LayoutResizeEventPayload(ResizeType.MINIMIZE, getLayout(), view));

        if (addHeaderSupport != null) {
            addHeaderSupport.showAddIcon();
        }

        gridLayoutUiState.setMaximized(false);
    }

    protected Boolean onLoadIsTableMaximized() {
        return gridLayoutUiState.isMaximized();
    }

    /**
     * Add header support to grid
     *
     * @param entityWindowBuilder
     *            EntityWindowBuilder
     */
    public void addAddHeaderSupport(final AbstractEntityWindowBuilder<?> entityWindowBuilder) {
        if (addHeaderSupport == null && hasCreatePermission()) {
            this.entityWindowBuilder = entityWindowBuilder;

            addHeaderSupport = new AddHeaderSupport(i18n, getAddIconId(), this::addNewItem,
                    this::onLoadIsTableMaximized);
            addHeaderSupport(addHeaderSupport, getHeaderSupportsSize() - 1);
        }
    }

    protected abstract boolean hasCreatePermission();

    protected abstract String getAddIconId();

    private void addNewItem() {
        final Window addWindow = entityWindowBuilder.getWindowForAdd();

        addWindow.setCaption(i18n.getMessage("caption.create.new", i18n.getMessage(getAddWindowCaptionMsg())));
        UI.getCurrent().addWindow(addWindow);
        addWindow.setVisible(Boolean.TRUE);
    }

    protected abstract String getAddWindowCaptionMsg();

    /**
     * Show filter option
     */
    public void showFilterIcon() {
        filterButtonsHeaderSupport.showFilterButtonsIcon();
    }

    /**
     * Hide filter option
     */
    public void hideFilterIcon() {
        filterButtonsHeaderSupport.hideFilterButtonsIcon();
    }

    protected SearchHeaderSupport getSearchHeaderSupport() {
        return searchHeaderSupport;
    }
}
