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

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.eclipse.hawkbit.ui.view.Constants;

@SuppressWarnings("java:S119") // better readability
public class TableView<T, ID> extends Div implements Constants, BeforeEnterObserver {

    private static final String COLOR = "color";
    private static final String VAR_LUMO_SECONDARY_TEXT_COLOR = "var(--lumo-secondary-text-color)";
    private static final String VAR_LUMO_PRIMARY_COLOR = "var(--lumo-primary-color)";
    private static final int DEFAULT_OPEN_POSITION_SIZE = 50;
    private static final String QUERY_PARAM_FILTER = "q";

    protected SelectionGrid<T, ID> selectionGrid;
    private final Filter filter;
    private final VerticalLayout gridLayout;
    protected final HorizontalLayout controlsLayout;
    private final SplitLayout splitLayout;
    private final Div detailsPanel;
    private Button currentSelectionButton;

    public TableView(
            final Filter.Rsql rsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> addHandler,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> removeHandler) {
        this(rsql, null, entityRepresentation, queryFn, addHandler, removeHandler, null);
    }

    public TableView(
            final Filter.Rsql rsql, final Filter.Rsql alternativeRsql,
            final SelectionGrid.EntityRepresentation<T, ID> entityRepresentation,
            final BiFunction<Query<T, Void>, String, Stream<T>> queryFn,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> addHandler,
            final Function<SelectionGrid<T, ID>, CompletionStage<Void>> removeHandler,
            final Function<T, Component> detailsButtonHandler) {
        selectionGrid = new SelectionGrid<>(entityRepresentation, queryFn);
        selectionGrid.setSizeFull();
        setSizeFull();

        splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        splitLayout.addThemeVariants(SplitLayoutVariant.LUMO_SMALL);
        splitLayout.setSplitterPosition(100);
        splitLayout.addToPrimary(selectionGrid);
        detailsPanel = new Div();

        if (detailsButtonHandler != null) {
            ComponentRenderer<Button, T> renderer = new ComponentRenderer<>(renderDetailsButton(detailsButtonHandler));
            selectionGrid.addColumn(renderer).setHeader("Details").setAutoWidth(true).setFlexGrow(0).setFrozenToEnd(true);
        }

        filter = new Filter(
                rsqlFilter -> {
                    closeDetailsPanel();
                    if (rsqlFilter != null) {
                        var queryParameters = UI.getCurrent().getActiveViewLocation()
                                .getQueryParameters()
                                .merging(QUERY_PARAM_FILTER, rsqlFilter);
                        UI.getCurrent().navigate(this.getClass(), queryParameters);
                    }
                    selectionGrid.setRsqlFilter(rsqlFilter, true);
                }, rsql, alternativeRsql
        );
        gridLayout = new VerticalLayout(filter, splitLayout);
        gridLayout.setSizeFull();
        gridLayout.setPadding(false);
        gridLayout.setSpacing(false);
        if (addHandler != null || removeHandler != null) {
            controlsLayout = Utils.addRemoveControls(addHandler, removeHandler, selectionGrid, false);
        } else {
            controlsLayout = new HorizontalLayout();
            controlsLayout.setWidthFull();
            controlsLayout.addClassNames(
                    LumoUtility.Padding.Horizontal.XLARGE, LumoUtility.Padding.Vertical.SMALL, LumoUtility.BoxSizing.BORDER);
            controlsLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        }
        gridLayout.add(controlsLayout);
        add(gridLayout);
    }

    private void closeDetailsPanel() {
        detailsPanel.removeAll();
        if (splitLayout.getSecondaryComponent() != null) {
            splitLayout.remove(detailsPanel);
        }
        splitLayout.setSplitterPosition(100);
        currentSelectionButton = null;
    }

    private SerializableFunction<T, Button> renderDetailsButton(final Function<T, Component> selectionHandler) {
        return selectedItem -> {
            final Button button = new Button(VaadinIcon.EYE.create());
            button.getStyle().set(COLOR, VAR_LUMO_SECONDARY_TEXT_COLOR);

            button.addClickListener(event -> {
                final Icon eyeIcon = VaadinIcon.EYE.create();
                final Icon closeIcon = VaadinIcon.CLOSE_SMALL.create();

                if (button == currentSelectionButton) {
                    button.setIcon(eyeIcon);
                    button.getStyle().set(COLOR, VAR_LUMO_SECONDARY_TEXT_COLOR);
                    closeDetailsPanel();
                } else {
                    button.setIcon(closeIcon);
                    button.getStyle().set(COLOR, VAR_LUMO_PRIMARY_COLOR);
                    if (currentSelectionButton == null) {
                        splitLayout.addToSecondary(detailsPanel);
                    } else {
                        currentSelectionButton.setIcon(eyeIcon);
                        currentSelectionButton.getStyle().set(COLOR, VAR_LUMO_SECONDARY_TEXT_COLOR);
                    }
                    detailsPanel.removeAll();
                    splitLayout.setSplitterPosition(DEFAULT_OPEN_POSITION_SIZE);
                    detailsPanel.add(selectionHandler.apply(selectedItem));
                    currentSelectionButton = button;
                }
            });

            button.setTooltipText("Show details");
            button.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return button;
        };
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var params = event.getLocation().getQueryParameters();
        params.getSingleParameter(QUERY_PARAM_FILTER)
                .ifPresent(f -> {
                    var newPage = event.getTrigger() == NavigationTrigger.UI_NAVIGATE;
                    selectionGrid.setRsqlFilter(f, newPage);
                    filter.setFilter(f, newPage);
                });

    }
}