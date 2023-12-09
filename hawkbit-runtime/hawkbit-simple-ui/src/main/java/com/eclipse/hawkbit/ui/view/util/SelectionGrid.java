/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.eclipse.hawkbit.ui.view.util;

import com.google.common.collect.Streams;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

// id type shall have proper equals and hashCode - i.e. eligible hash set element
public class SelectionGrid<T,ID> extends Grid<T> {

    private volatile String rsqlFilter;


    public SelectionGrid(
            final EntityRepresentation<T, ID> entityRepresentation) {
        this(entityRepresentation, null);
    }

    public SelectionGrid(
            final EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn) {
        super(entityRepresentation.beanType, false);

        addThemeVariants(GridVariant.LUMO_NO_BORDER);
        addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);

        setSelectionMode(Grid.SelectionMode.MULTI);
        entityRepresentation.addColumns(this);
        if (queryFn != null) {
            setItems(query -> {
                final Stream<T> fetch = queryFn.apply(query, rsqlFilter);
                final Set<T> selected = getSelectedItems();
                if (selected == null || selected.isEmpty()) {
                    return fetch;
                } else {
                    final Set<ID> selectedIds = new HashSet<>();
                    selected.forEach(next -> selectedIds.add(entityRepresentation.idFn.apply(next)));
                    // if matching keeps old entries instead of new the new ones in order to
                    // select them in case refresh is made with keepSelection
                    // this however means that if they are changed the old state will be shown!!!
                    return Streams.concat(selected.stream(),
                            fetch.filter(next -> !selectedIds.contains(entityRepresentation.idFn.apply(next))));
                }
            });
        } // else externally managed
    }

    public void setRsqlFilter(final String rsqlFilter) {
        if (!Objects.equals(this.rsqlFilter, rsqlFilter)) {
            this.rsqlFilter = rsqlFilter;
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

    public static abstract class EntityRepresentation<T, ID> {

        private final Class<T> beanType;
        private final Function<T, ID> idFn;

        protected EntityRepresentation(final Class<T> beanType, final Function<T, ID> idFn) {
            this.beanType = beanType;
            this.idFn = idFn;
        }

        protected abstract void addColumns(final Grid<T> grid);
    }
}
