package org.eclipse.hawkbit.ui.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtDistributionSetAutoAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@PageTitle("Target Filter Queries")
@Route(value = "target_filter_queries", layout = MainLayout.class)
@RolesAllowed({ "TARGET_READ" })
@Uses(Icon.class)
public class TargetFilterQueryView extends TableView<TargetFilterQueryView.TargetFilterQueryGridItem, Long>  {
    public TargetFilterQueryView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new TargetFilterQueryFilter(),
                new SelectionGrid.EntityRepresentation<>(TargetFilterQueryGridItem.class, TargetFilterQueryGridItem::getId) {

                    @Override
                    protected void addColumns(final Grid<TargetFilterQueryGridItem> grid) {
                        grid.addColumn(MgmtTargetFilterQuery::getId).setHeader(Constants.ID).setAutoWidth(true).setKey("id").setSortable(true);
                        grid.addColumn(MgmtTargetFilterQuery::getName).setHeader(Constants.NAME).setAutoWidth(true).setKey("name").setSortable(true).setResizable(true);
                        grid.addColumn(MgmtTargetFilterQuery::getCreatedBy).setHeader(Constants.CREATED_BY).setKey("createdBy").setSortable(true).setAutoWidth(true);
                        grid.addColumn(Utils.localDateTimeRenderer(MgmtTargetFilterQuery::getCreatedAt)).setHeader(Constants.CREATED_AT).setKey("createdAt").setSortable(true).setAutoWidth(true);
                        grid.addColumn(MgmtTargetFilterQuery::getLastModifiedBy).setHeader(Constants.LAST_MODIFIED_BY).setKey("lastModifiedBy").setSortable(true).setAutoWidth(true);
                        grid.addColumn(Utils.localDateTimeRenderer(MgmtTargetFilterQuery::getLastModifiedAt)).setHeader(Constants.LAST_MODIFIED_AT).setKey("lastModifiedAt").setSortable(true).setAutoWidth(true);
                        grid.addColumn(new ComponentRenderer<>(DistributionSetCell::new)).setHeader(Constants.DISTRIBUTION_SET).setAutoWidth(true).setFlexGrow(0);

                        grid.addComponentColumn(rollout -> new Actions(rollout, grid, hawkbitClient)).setHeader(
                                Constants.ACTIONS).setAutoWidth(true);
                    }
                },
                (query, filter) -> Optional.ofNullable(
                                hawkbitClient.getTargetFilterQueryRestApi()
                                        .getFilters(filter, query.getOffset(), query.getPageSize(), Utils.getSortParam(query.getSortOrders(), Constants.NAME_ASC), "compact")
                                        .getBody())
                        .stream()
                        .map(PagedList::getContent)
                        .flatMap(List::stream)
                        .map(m -> TargetFilterQueryGridItem.from(hawkbitClient, m)),
                null,
                selectionGrid -> {
                    selectionGrid.getSelectedItems()
                            .forEach(toDelete -> hawkbitClient.getTargetFilterQueryRestApi().deleteFilter(toDelete.getId()));
                    return CompletableFuture.completedFuture(null);
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

    private static class DistributionSetCell extends HorizontalLayout {

        private DistributionSetCell(final TargetFilterQueryGridItem filterQuery) {
            filterQuery.getDs().ifPresent(ds -> {
                Icon icon = getActionTypeIcon(filterQuery.getAutoAssignActionType());
                Span dsName = new Span(ds.getName() + ":" + ds.getVersion());

                add(icon, dsName);
            });
        }

        private Icon getActionTypeIcon(MgmtActionType actionType) {
            Icon icon = switch (actionType) {
                case FORCED -> VaadinIcon.BOLT.create();
                case SOFT -> VaadinIcon.USER_CHECK.create();
                case DOWNLOAD_ONLY -> VaadinIcon.DOWNLOAD.create();
                default -> VaadinIcon.QUESTION_CIRCLE.create();
            };
            icon.addClassNames(LumoUtility.IconSize.SMALL);
            return Utils.tooltip(icon, actionType.getName());
        }
    }

    private static class Actions extends HorizontalLayout {

        private final Grid<TargetFilterQueryGridItem> grid;
        private final transient HawkbitMgmtClient hawkbitClient;

        private Actions(final MgmtTargetFilterQuery filter, final Grid<TargetFilterQueryGridItem> grid,
                        final HawkbitMgmtClient hawkbitClient) {
            this.grid = grid;
            this.hawkbitClient = hawkbitClient;
            init(filter);
        }

        private void init(final MgmtTargetFilterQuery filter) {
            if (filter.getAutoAssignDistributionSet() == null) {
                add(Utils.tooltip(new Button(VaadinIcon.LINK.create()) {
                    {
                        addClickListener(v -> {
                            new AutoAssignDialog(filter.getId(), hawkbitClient, () -> refresh(filter.getId())).open();
                        });
                    }
                }, "Auto assign"));
            } else {
                add(Utils.tooltip(new Button(VaadinIcon.UNLINK.create()) {
                    {
                        addClickListener(v -> {
                            ConfirmDialog dialog = Utils.confirmDialog("Unassign Distribution Set",
                                    "Are you sure you want to unassign the distribution set of target filter query '" + filter.getName() + "'?",
                                    "Unassign",
                                    () -> {
                                        hawkbitClient.getTargetFilterQueryRestApi().deleteAssignedDistributionSet(filter.getId());
                                        refresh(filter.getId());
                                    });
                            dialog.open();
                        });
                    }
                }, "Unassign"));
            }
            add(Utils.tooltip(new Button(VaadinIcon.TRASH.create()) {
                {
                    addClickListener(v -> {
                        ConfirmDialog dialog = Utils.confirmDialog("Delete Target Filter Query",
                                "Are you sure you want to delete the target filter query '" + filter.getName() + "'?",
                                "Delete",
                                () -> {
                                    hawkbitClient.getTargetFilterQueryRestApi().deleteFilter(filter.getId());
                                    grid.getDataProvider().refreshAll();
                                });
                        dialog.open();
                    });
                }
            }, "Delete"));
        }

        private void refresh(Long filterId) {
            removeAll();
            final MgmtTargetFilterQuery body = hawkbitClient.getTargetFilterQueryRestApi().getFilter(filterId).getBody();
            if (body != null) {
                grid.getDataProvider().refreshItem(TargetFilterQueryGridItem.from(hawkbitClient, body));
                init(body);
            }
        }
    }

    private static class AutoAssignDialog extends Utils.BaseDialog<Void> {

        private final Long filterId;
        private final Select<MgmtActionType> actionType;
        private final ComboBox<MgmtDistributionSet> distributionSet;
        private final Button assign = new Button("Assign");

        private AutoAssignDialog(final Long filterId, final HawkbitMgmtClient hawkbitClient, Runnable onSuccess) {
            super("Select auto assignment distribution set");

            this.filterId = filterId;

            Paragraph description = new Paragraph("When an auto assign distribution set is selected, " +
                    "it will be automatically assigned to all targets that match the target filter.");

            actionType = Utils.actionTypeControls(new MgmtActionType[]{MgmtActionType.SOFT, MgmtActionType.FORCED, MgmtActionType.DOWNLOAD_ONLY}, MgmtActionType.FORCED, null);

            distributionSet = Utils.nameComboBox("Distribution Set", this::readyToAssign, query -> Optional.ofNullable(
                    hawkbitClient.getDistributionSetRestApi()
                            .getDistributionSets(
                                    query.getFilter().orElse(null),
                                    query.getOffset(),
                                    query.getLimit(),
                                    Constants.NAME_ASC)
                            .getBody()).stream().flatMap(body -> body.getContent().stream()));
            distributionSet.setItemLabelGenerator(ds -> ds.getName() + ":" + ds.getVersion());
            distributionSet.focus();
            distributionSet.setRequiredIndicatorVisible(true);
            distributionSet.setWidthFull();

            assign.setEnabled(false);
            assign.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addAssignClickListener(hawkbitClient, onSuccess);
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(assign);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setSpacing(false);
            layout.add(description, actionType, distributionSet);
            add(layout);
            open();
        }

        private void readyToAssign(final Object v) {
            final boolean createEnabled = !distributionSet.isEmpty();
            if (assign.isEnabled() != createEnabled) {
                assign.setEnabled(createEnabled);
            }
        }

        private void addAssignClickListener(final HawkbitMgmtClient hawkbitClient, Runnable onSuccess) {
            assign.addClickListener(e -> {
                MgmtDistributionSetAutoAssignment newAssignment = new MgmtDistributionSetAutoAssignment();
                newAssignment.setId(distributionSet.getValue().getId());
                newAssignment.setType(actionType.getValue());
                hawkbitClient.getTargetFilterQueryRestApi().postAssignedDistributionSet(filterId, newAssignment);
                onSuccess.run();
                close();
            });
        }
    }

    // todo change /targetfilters api to reduce api calls ?
    @Getter
    public static class TargetFilterQueryGridItem extends MgmtTargetFilterQuery {

        TargetFilterQueryGridItem() {
            super();
        }

        private Optional<MgmtDistributionSet> ds;
        static ObjectMapper objectMapper = new ObjectMapper();

        public static TargetFilterQueryGridItem from(final HawkbitMgmtClient hawkbitClient, MgmtTargetFilterQuery filter) {
            TargetFilterQueryGridItem filterGridItem = objectMapper.convertValue(filter, TargetFilterQueryGridItem.class);

            if (filterGridItem.getAutoAssignDistributionSet() != null) {
                filterGridItem.ds = Optional.ofNullable(
                        hawkbitClient.getTargetFilterQueryRestApi().getAssignedDistributionSet(filterGridItem.getId()).getBody()
                );
            } else {
                filterGridItem.ds = Optional.empty();
            }
            return filterGridItem;
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
