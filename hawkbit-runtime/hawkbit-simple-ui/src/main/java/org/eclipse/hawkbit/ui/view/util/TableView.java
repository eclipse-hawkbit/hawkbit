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

import org.eclipse.hawkbit.ui.view.Constants;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class TableView<T, ID> extends Div implements Constants {

    protected SelectionGrid<T, ID> selectionGrid;
    private final Filter filter;

    public TableView(
            final Filter.Rsql rsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn) {
        this(rsql, null, entityRepresentation, queryFn, null, null);
    }
    public TableView(
            final Filter.Rsql rsql, final Filter.Rsql alternativeRsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn) {
        this(rsql, alternativeRsql, entityRepresentation, queryFn,null, null);
    }
    public TableView(
            final Filter.Rsql rsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> addHandler,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> removeHandler) {
        this(rsql, null, entityRepresentation, queryFn, addHandler, removeHandler);
    }
    public TableView(
            final Filter.Rsql rsql, final Filter.Rsql alternativeRsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> addHandler,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> removeHandler) {
        selectionGrid = new SelectionGrid<>(entityRepresentation, queryFn);
        filter = new Filter(selectionGrid::setRsqlFilter, rsql, alternativeRsql);

        setSizeFull();

        final VerticalLayout layout = new VerticalLayout(filter,  selectionGrid);
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        if (addHandler != null || removeHandler != null) {
            layout.add(Utils.addRemoveControls(addHandler, removeHandler, selectionGrid, false));
        }
        add(layout);
    }
}
