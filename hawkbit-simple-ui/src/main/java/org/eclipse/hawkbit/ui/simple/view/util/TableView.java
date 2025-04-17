/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.view.util;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.theme.lumo.LumoUtility;

import org.eclipse.hawkbit.ui.simple.view.Constants;

@SuppressWarnings("java:S119") // better readability
public class TableView<T, ID> extends Div implements Constants {

    protected SelectionGrid<T, ID> selectionGrid;
    private final Filter filter;
    final VerticalLayout gridLayout;
    protected final HorizontalLayout controlsLayout;

    public TableView(
            final Filter.Rsql rsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn) {
        this(rsql, null, entityRepresentation, queryFn);
    }

    public TableView(
            final Filter.Rsql rsql,
            final Filter.Rsql alternativeRsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn) {
        this(rsql, alternativeRsql, entityRepresentation, queryFn, null, null);
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

        gridLayout = new VerticalLayout(filter, selectionGrid);
        gridLayout.setSizeFull();
        gridLayout.setPadding(false);
        gridLayout.setSpacing(false);
        if (addHandler != null || removeHandler != null) {
            controlsLayout = Utils.addRemoveControls(addHandler, removeHandler, selectionGrid, false);
        } else {
            controlsLayout = new HorizontalLayout();
            controlsLayout.setWidthFull();
            controlsLayout.addClassNames(LumoUtility.Padding.Horizontal.XLARGE, LumoUtility.Padding.Vertical.SMALL, LumoUtility.BoxSizing.BORDER);
            controlsLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        }
        gridLayout.add(controlsLayout);
        add(gridLayout);
    }
}
