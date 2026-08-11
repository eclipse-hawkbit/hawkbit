/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.view;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;
import tools.jackson.databind.ObjectMapper;

@PageTitle("Target Filter Queries")
@Route(value = "target_filter_queries", layout = MainLayout.class)
@RolesAllowed({ "TARGET_READ" })
@Uses(Icon.class)
public class TargetFilterQueryView extends TableView<TargetFilterQueryView.TargetFilterQueryGridItem, Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    public TargetFilterQueryView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new TargetFilterQueryFilter(),
                null,
                new SelectionGrid.EntityRepresentation<>(TargetFilterQueryGridItem.class, TargetFilterQueryGridItem::getId) {

                    @Override
                    protected void addColumns(final Grid<TargetFilterQueryGridItem> grid) {
                        grid.addColumn(MgmtTargetFilterQuery::getId).setHeader(Constants.ID).setAutoWidth(true).setKey("id").setSortable(true);
                        grid.addColumn(MgmtTargetFilterQuery::getName)
                                .setHeader(Constants.NAME).setAutoWidth(true).setKey("name").setSortable(true).setResizable(true);
                        grid.addColumn(new ComponentRenderer<>(QueryCell::new))
                                .setHeader("Query").setAutoWidth(true).setKey("query").setResizable(true);
                        grid.addColumn(Utils.localDateTimeRenderer(MgmtTargetFilterQuery::getLastModifiedAt))
                                .setHeader(Constants.LAST_MODIFIED_AT).setKey("lastModifiedAt").setSortable(true).setAutoWidth(true)
                                .setResizable(true);

                        grid.addComponentColumn(rollout -> new Actions(rollout, grid, hawkbitClient)).setHeader(
                                Constants.ACTIONS).setAutoWidth(true);
                    }
                },
                (query, filter) -> Optional.ofNullable(
                                hawkbitClient.getTargetFilterQueryRestApi()
                                        .getFilters(
                                                filter, query.getOffset(), query.getPageSize(),
                                                Utils.getSortParam(query.getSortOrders(), Constants.NAME_ASC), "compact")
                                        .getBody())
                        .stream()
                        .map(PagedList::getContent)
                        .flatMap(List::stream)
                        .map(m -> TargetFilterQueryGridItem.from(m)),
                null,
                selectionGrid -> {
                    selectionGrid.getSelectedItems()
                            .forEach(toDelete -> hawkbitClient.getTargetFilterQueryRestApi().deleteFilter(toDelete.getId()));
                    return CompletableFuture.completedFuture(null);
                },
                filterQuery -> {
                    final TargetFilterQueryDetailedView detailedView = new TargetFilterQueryDetailedView();
                    detailedView.setItem(filterQuery);
                    return detailedView;
                }
        );
    }

    private static class TargetFilterQueryFilter implements Filter.Rsql {

        private final TextField name = Utils.textField(Constants.NAME);

        private TargetFilterQueryFilter() {
            name.setPlaceholder("<name filter>");
        }

        @Override
        public List<Component> components() {
            return List.of(name);
        }

        @Override
        public String filter() {
            return Filter.filter(
                    Map.of(
                            "name", name.getOptionalValue().map(s -> "*" + s + "*")
                    ));
        }
    }

    private static class QueryCell extends Div {

        private QueryCell(final TargetFilterQueryGridItem filterQuery) {
            String query = filterQuery.getQuery();
            if (query != null) {
                setText(query);
                setTitle(query);
            }
            getStyle().setOverflow(Style.Overflow.HIDDEN);
            getStyle().set("text-overflow", "ellipsis");
            setWhiteSpace(WhiteSpace.NOWRAP);
            setMaxWidth(400, Unit.PIXELS);
        }
    }

    private static class Actions extends HorizontalLayout {

        private final Grid<TargetFilterQueryGridItem> grid;
        private final transient HawkbitMgmtClient hawkbitClient;

        private Actions(
                final MgmtTargetFilterQuery filter, final Grid<TargetFilterQueryGridItem> grid, final HawkbitMgmtClient hawkbitClient) {
            this.grid = grid;
            this.hawkbitClient = hawkbitClient;
            init(filter);
        }

        private void init(final MgmtTargetFilterQuery filter) {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addClickListener(e -> {
                ConfirmDialog dialog = Utils.confirmDialog("Confirm Deletion",
                        "Are you sure you want to delete the target filter query '" + filter.getName() + "'?",
                        "Delete",
                        () -> {
                            hawkbitClient.getTargetFilterQueryRestApi().deleteFilter(filter.getId());
                            grid.getDataProvider().refreshAll();
                        });
                dialog.open();
            });
            add(Utils.tooltip(deleteButton, "Delete"));
        }
    }

    private static class TargetFilterQueryDetailedView extends VerticalLayout {

        private final Span filterName;
        private final TargetFilterQueryDetails details;

        private TargetFilterQueryDetailedView() {
            filterName = new Span();
            details = new TargetFilterQueryDetails();
            setWidthFull();

            add(filterName);
            final TabSheet tabSheet = new TabSheet();
            tabSheet.setWidthFull();
            tabSheet.add("Details", details);
            add(tabSheet);
        }

        private void setItem(final TargetFilterQueryGridItem filterQuery) {
            this.filterName.setText(filterQuery.getName());
            this.details.setItem(filterQuery);
        }
    }

    private static class TargetFilterQueryDetails extends FormLayout {

        private final TextField name = Utils.textField(Constants.NAME);
        private final TextArea query = new TextArea("Query");
        private final TextField createdBy = Utils.textField(Constants.CREATED_BY);
        private final TextField createdAt = Utils.textField(Constants.CREATED_AT);
        private final TextField lastModifiedBy = Utils.textField(Constants.LAST_MODIFIED_BY);
        private final TextField lastModifiedAt = Utils.textField(Constants.LAST_MODIFIED_AT);

        private TargetFilterQueryDetails() {
            query.setMinLength(2);
            Stream.of(
                            name, query,
                            createdBy, createdAt,
                            lastModifiedBy, lastModifiedAt)
                    .forEach(field -> {
                        field.setReadOnly(true);
                        add(field);
                    });

            setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
            setColspan(query, 2);
        }

        private void setItem(final TargetFilterQueryGridItem filterQuery) {
            name.setValue(filterQuery.getName() != null ? filterQuery.getName() : "");
            query.setValue(filterQuery.getQuery() != null ? filterQuery.getQuery() : "");
            createdBy.setValue(filterQuery.getCreatedBy() != null ? filterQuery.getCreatedBy() : "");
            createdAt.setValue(Utils.localDateTimeFromTs(filterQuery.getCreatedAt()));
            lastModifiedBy.setValue(filterQuery.getLastModifiedBy() != null ? filterQuery.getLastModifiedBy() : "");
            lastModifiedAt.setValue(Utils.localDateTimeFromTs(filterQuery.getLastModifiedAt()));
        }
    }

    public static class TargetFilterQueryGridItem extends MgmtTargetFilterQuery {

        TargetFilterQueryGridItem() {
            super();
        }

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        public static TargetFilterQueryGridItem from(   MgmtTargetFilterQuery filter) {
            return OBJECT_MAPPER.convertValue(filter, TargetFilterQueryGridItem.class);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TargetFilterQueryGridItem other)) return false;
            return Objects.equals(getId(), other.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getId());
        }
    }
}