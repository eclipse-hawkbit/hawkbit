/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid;

import java.util.Optional;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.grid.selection.RangeSelectionModel;
import org.eclipse.hawkbit.ui.common.grid.support.DragAndDropSupport;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.data.provider.DataProviderListener;
import com.vaadin.data.provider.Query;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column.NestedNullBehavior;
import com.vaadin.ui.components.grid.GridSelectionModel;
import com.vaadin.ui.renderers.AbstractRenderer;

/**
 * Abstract grid that offers various capabilities (aka support) to offer
 * convenient enhancements to the vaadin standard grid.
 *
 * @param <T>
 *            The container-type used by the grid
 * @param <F>
 *            The filter-type used by the grid
 */
public abstract class AbstractGrid<T extends ProxyIdentifiableEntity, F> extends Grid<T> {
    private static final long serialVersionUID = 1L;

    protected static final String CENTER_ALIGN = "v-align-center";
    public static final String MULTI_SELECT_STYLE = "multi-selection-grid";

    protected final VaadinMessageSource i18n;
    protected final transient UIEventBus eventBus;
    protected final SpPermissionChecker permissionChecker;

    private transient FilterSupport<T, F> filterSupport;
    private transient SelectionSupport<T> selectionSupport;
    private transient DragAndDropSupport<T> dragAndDropSupport;

    /**
     * Constructor.
     *
     * @param i18n
     *            i18n
     * @param eventBus
     *            eventBus
     */
    protected AbstractGrid(final VaadinMessageSource i18n, final UIEventBus eventBus) {
        this(i18n, eventBus, null);
    }

    /**
     * Constructor.
     *
     * @param i18n
     *            i18n
     * @param eventBus
     *            eventBus
     * @param permissionChecker
     *            permissionChecker
     */
    protected AbstractGrid(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker) {
        this(i18n, eventBus, permissionChecker, new DataCommunicator<>());
    }

    /**
     * Constructor.
     *
     * @param i18n
     *            i18n
     * @param eventBus
     *            eventBus
     * @param permissionChecker
     *            permissionChecker
     * @param dataCommunicator
     *            dataCommunicator
     */
    protected AbstractGrid(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final DataCommunicator<T> dataCommunicator) {
        super(dataCommunicator);

        this.i18n = i18n;
        this.eventBus = eventBus;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public GridSelectionModel<T> setSelectionMode(final SelectionMode mode) {
        if (mode == SelectionMode.MULTI) {
            final RangeSelectionModel<T> model = new RangeSelectionModel<>(i18n);
            setSelectionModel(model);

            // used to deactivate cell text selection by user
            addStyleName(MULTI_SELECT_STYLE);

            return model;
        }

        return super.setSelectionMode(mode);
    }

    /**
     * Initializes the grid.
     * <p>
     *
     * <b>NOTE:</b> Sub-classes should configure the grid before calling this
     * method (this means: set all support-classes needed, and then call init).
     */
    public void init() {
        setSizeFull();
        setId(getGridId());
        setColumnReorderingAllowed(false);
        addColumns();
        setFrozenColumnCount(-1);

        if (selectionSupport == null) {
            selectionSupport = new SelectionSupport<>(this);
            selectionSupport.disableSelection();
        }

        if (hasFilterSupport()) {
            setDataProvider(getFilterSupport().getFilterDataProvider());
        }
    }

    /**
     * Refresh all items.
     */
    public void refreshAll() {
        getDataProvider().refreshAll();
    }

    /**
     * Refresh single item.
     *
     * @param item
     *            grid item
     */
    public void refreshItem(final T item) {
        getDataProvider().refreshItem(item);
    }

    /**
     * Get total number of items.
     */
    public int getDataSize() {
        return getDataProvider().size(new Query<>());
    }

    /**
     * Add data change event listener
     *
     * @param listener
     *            Data provider listener
     */
    public void addDataChangedListener(final DataProviderListener<T> listener) {
        getDataProvider().addDataProviderListener(listener);
    }

    /**
     * Method for setting up the required columns together with their definition
     * and rendering options.
     */
    public abstract void addColumns();

    @Override
    public Column<T, ?> addColumn(final String propertyName) {
        return super.addColumn(propertyName).setSortable(false);
    }

    @Override
    public Column<T, ?> addColumn(final String propertyName, final AbstractRenderer<? super T, ?> renderer) {
        return super.addColumn(propertyName, renderer).setSortable(false);
    }

    @Override
    public Column<T, ?> addColumn(final String propertyName, final AbstractRenderer<? super T, ?> renderer,
            final NestedNullBehavior nestedNullBehavior) {
        return super.addColumn(propertyName, renderer, nestedNullBehavior).setSortable(false);
    }

    @Override
    public <V> Column<T, V> addColumn(final ValueProvider<T, V> valueProvider) {
        return super.addColumn(valueProvider).setSortable(false);
    }

    @Override
    public <V> Column<T, V> addColumn(final ValueProvider<T, V> valueProvider,
            final AbstractRenderer<? super T, ? super V> renderer) {
        return super.addColumn(valueProvider, renderer).setSortable(false);
    }

    @Override
    public <V, P> Column<T, V> addColumn(final ValueProvider<T, V> valueProvider,
            final ValueProvider<V, P> presentationProvider, final AbstractRenderer<? super T, ? super P> renderer) {
        return super.addColumn(valueProvider, presentationProvider, renderer).setSortable(false);
    }

    @Override
    public <V> Column<T, V> addColumn(final ValueProvider<T, V> valueProvider,
            final ValueProvider<V, String> presentationProvider) {
        return super.addColumn(valueProvider, presentationProvider).setSortable(false);
    }

    @Override
    public <V extends Component> Column<T, V> addComponentColumn(final ValueProvider<T, V> componentProvider) {
        return super.addComponentColumn(componentProvider).setSortable(false);
    }

    /**
     * Gets the FilterSupport implementation describing the data provider with
     * filter.
     *
     * @return filterSupport that encapsulates the data provider with filter.
     */
    public FilterSupport<T, F> getFilterSupport() {
        return filterSupport;
    }

    /**
     * Checks whether filter support is enabled.
     *
     * @return <code>true</code> iffilter support is enabled, otherwise
     *         <code>false</code>
     */
    protected boolean hasFilterSupport() {
        return filterSupport != null;
    }

    /**
     * Gets the filter for this grid from FilterSupport.
     *
     * @return filter for the grid.
     */
    public Optional<F> getFilter() {
        return filterSupport != null ? Optional.ofNullable(filterSupport.getFilter()) : Optional.empty();
    }

    /**
     * Enables filter support for the grid by setting a FilterSupport
     * implementation.
     *
     * @param filterSupport
     *            encapsulates the data provider with filter.
     */
    public void setFilterSupport(final FilterSupport<T, F> filterSupport) {
        this.filterSupport = filterSupport;
    }

    /**
     * Enables selection-support for the grid by setting SelectionSupport
     * configuration.
     *
     * @param selectionSupport
     *            encapsulates behavior for selection and offers some convenient
     *            functionality.
     */
    protected void setSelectionSupport(final SelectionSupport<T> selectionSupport) {
        this.selectionSupport = selectionSupport;
    }

    /**
     * Gets the SelectionSupport implementation configuring selection.
     *
     * @return selectionSupport that configures selection.
     */
    public SelectionSupport<T> getSelectionSupport() {
        return selectionSupport;
    }

    /**
     * Checks whether single or multi selection-support is enabled.
     *
     * @return <code>true</code> if single or multi selection-support is
     *         enabled, otherwise <code>false</code>
     */
    public boolean hasSelectionSupport() {
        return selectionSupport != null && !selectionSupport.isNoSelectionModel();
    }

    /**
     * Enables drag and drop for the grid by setting DragAndDropSupport
     * configuration.
     *
     * @param dragAndDropSupport
     *            encapsulates behavior for drag and drop and offers some
     *            convenient functionality.
     */
    protected void setDragAndDropSupportSupport(final DragAndDropSupport<T> dragAndDropSupport) {
        this.dragAndDropSupport = dragAndDropSupport;
    }

    /**
     * Gets the DragAndDropSupport implementation configuring drag and drop.
     *
     * @return dragAndDropSupport that configures drag and drop.
     */
    public DragAndDropSupport<T> getDragAndDropSupportSupport() {
        return dragAndDropSupport;
    }

    /**
     * Checks whether drag and drop is enabled.
     *
     * @return <code>true</code> if drag and drop is enabled, otherwise
     *         <code>false</code>
     */
    public boolean hasDragAndDropSupportSupport() {
        return dragAndDropSupport != null;
    }

    /**
     * Gets id of the grid.
     *
     * @return id of the grid
     */
    public abstract String getGridId();

    /**
     * Creates the grid content for maximized-state.
     */
    public void createMaximizedContent() {
        createMaximizedContent(SelectionMode.NONE);
    }

    protected void createMaximizedContent(final SelectionMode selectionMode) {
        removeAllColumns();
        addMaxColumns();

        if (selectionSupport != null) {
            changeSelection(selectionMode);
        }
        if (hasDragAndDropSupportSupport()) {
            getDragAndDropSupportSupport().removeDragSource();
        }
    }

    protected void addMaxColumns() {
        // can be overriden in order to define columns in maximized state
    }

    private void changeSelection(final SelectionMode selectionMode) {
        switch (selectionMode) {
        case NONE:
            getSelectionSupport().disableSelection();
            break;
        case SINGLE:
            getSelectionSupport().enableSingleSelection();
            break;
        case MULTI:
            getSelectionSupport().enableMultiSelection();
            break;
        }
    }

    /**
     * Creates the grid content for normal (minimized) state.
     */
    public void createMinimizedContent() {
        createMinimizedContent(SelectionMode.MULTI);
    }

    protected void createMinimizedContent(final SelectionMode selectionMode) {
        removeAllColumns();
        addColumns();

        if (selectionSupport != null) {
            changeSelection(selectionMode);
        }
        if (hasDragAndDropSupportSupport()) {
            getDragAndDropSupportSupport().addDragSource();
        }
    }

    /**
     * Restore filter and selection
     */
    public void restoreState() {
        if (hasFilterSupport()) {
            getFilterSupport().restoreFilter();
        }

        if (hasSelectionSupport()) {
            getSelectionSupport().restoreSelection();
        }
    }
}
