/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.view.util;

import java.io.Serial;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.theme.lumo.LumoUtility;

// id type shall have proper equals and hashCode - i.e. eligible hash set element
@SuppressWarnings("java:S119") // better readability
public final class SelectionGrid<T, ID> extends Grid<T> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String HIGHLIGHTED_ROW_PART = "selected-row";

    private final transient Function<T, ID> idFn;

    private volatile String rsqlFilter;
    private transient T highlightedItem;

    public SelectionGrid(
            final EntityRepresentation<T, ID> entityRepresentation) {
        this(entityRepresentation, null);
    }

    public SelectionGrid(
            final EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn) {
        super(entityRepresentation.beanType, false);

        this.idFn = entityRepresentation.idFn;

        addThemeVariants(GridVariant.LUMO_NO_BORDER);
        addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);

        getDataCommunicator().getKeyMapper().setIdentifierGetter(idFn::apply);
        setPartNameGenerator(item -> highlightedItem != null
                && Objects.equals(idFn.apply(item), idFn.apply(highlightedItem)) ? HIGHLIGHTED_ROW_PART : null);

        setSelectionMode(Grid.SelectionMode.MULTI);
        entityRepresentation.addColumns(this);
        if (queryFn != null) {
            setItems(query -> {
                final Stream<T> fetch = queryFn.apply(query, rsqlFilter);
                final Set<T> selected = getSelectedItems();
                if (selected == null || selected.isEmpty()) {
                    final List<T> fetchList = fetch.toList();
                    if (fetchList.size() == 1) {
                        this.setDetailsVisible(fetchList.get(0), true);
                    }
                    return fetchList.stream();
                } else {
                    final Set<ID> selectedIds = new HashSet<>();
                    selected.forEach(next -> selectedIds.add(entityRepresentation.idFn.apply(next)));
                    return Stream.concat(selected.stream(),
                            fetch.filter(next -> !selectedIds.contains(entityRepresentation.idFn.apply(next))));
                }
            });
        }
    }

    public void setRsqlFilter(final String rsqlFilter, boolean refreshGrid) {
        if (!Objects.equals(this.rsqlFilter, rsqlFilter)) {
            this.rsqlFilter = rsqlFilter;
            if (refreshGrid)
                refreshGrid(true);
        }
    }

    public void refreshGrid(final boolean keepSelection) {
        if (keepSelection) {
            final Set<T> selected = getSelectedItems();
            getDataProvider().refreshAll();
            if (selected != null && !selected.isEmpty()) {
                selected.forEach(this::select);
            }
        } else {
            deselectAll();
            getDataProvider().refreshAll();
        }
    }

    /**
     * Highlights the given item's row (e.g. the row whose details panel is open) and clears the
     * previous highlight. Only the affected rows are re-rendered. Passing {@code null} clears it.
     */
    public void setHighlightedItem(final T item) {
        final T previous = highlightedItem;
        if (Objects.equals(idOf(previous), idOf(item))) {
            return;
        }
        highlightedItem = item;
        if (previous != null) {
            getDataProvider().refreshItem(previous);
        }
        if (item != null) {
            getDataProvider().refreshItem(item);
        }
    }

    /**
     * Whether the given item is the currently highlighted row (i.e. the row whose details panel is open).
     */
    public boolean isHighlighted(final T item) {
        return highlightedItem != null && Objects.equals(idOf(item), idOf(highlightedItem));
    }

    private ID idOf(final T item) {
        return item == null ? null : idFn.apply(item);
    }

    public abstract static class EntityRepresentation<T, ID> {

        private final Class<T> beanType;
        private final Function<T, ID> idFn;

        protected EntityRepresentation(final Class<T> beanType, final Function<T, ID> idFn) {
            this.beanType = beanType;
            this.idFn = idFn;
        }

        protected abstract void addColumns(final Grid<T> grid);
    }
}
