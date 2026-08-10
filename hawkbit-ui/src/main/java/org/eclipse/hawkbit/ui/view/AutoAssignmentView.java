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

import static com.vaadin.flow.component.icon.VaadinIcon.PAUSE;
import static com.vaadin.flow.component.icon.VaadinIcon.START_COG;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;

import java.io.Serial;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;
import org.springframework.util.ObjectUtils;

@PageTitle("Auto Assignments")
@Route(value = "auto_assignments", layout = MainLayout.class)
@RolesAllowed({ "AUTO_ASSIGNMENT_READ" })
@Uses(Icon.class)
public final class AutoAssignmentView extends TableView<MgmtAutoAssignmentResponseBody, Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    public AutoAssignmentView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new AutoAssignmentFilter(),
                new SelectionGrid.EntityRepresentation<MgmtAutoAssignmentResponseBody, Long>(
                        MgmtAutoAssignmentResponseBody.class, MgmtAutoAssignmentResponseBody::getId) {

                    private final AutoAssignmentDetails details = new AutoAssignmentDetails(hawkbitClient);
                    @Override
                    protected void addColumns(final Grid<MgmtAutoAssignmentResponseBody> grid) {
                        grid.addColumn(MgmtAutoAssignmentResponseBody::getId).setHeader(Constants.ID).setAutoWidth(true);
                        grid.addColumn(MgmtAutoAssignmentResponseBody::getName).setHeader(Constants.NAME).setAutoWidth(true);
                        grid.addColumn(MgmtAutoAssignmentResponseBody::getStatus).setHeader(Constants.STATUS).setAutoWidth(true);

                        grid.addComponentColumn(autoAssignment ->
                                new Actions(autoAssignment, grid, hawkbitClient)).setHeader(Constants.ACTIONS).setAutoWidth(true);

                        grid.setItemDetailsRenderer(new ComponentRenderer<>(
                                () -> details, AutoAssignmentDetails::setItem));
                    }
                },
                (query, rsqlFilter) -> Optional.ofNullable(
                        hawkbitClient.getAutoAssignmentRestApi()
                                .getAutoAssignments(
                                        rsqlFilter,
                                        query.getOffset(),
                                        query.getPageSize(),
                                        Constants.NAME_ASC
                                ).getBody()).stream().flatMap(page -> page.getContent().stream()),
                selectionGrid -> new CreateDialog(hawkbitClient).result(),
                selectionGrid -> {
                    selectionGrid.getSelectedItems().forEach(
                            autoAssignment -> hawkbitClient.getAutoAssignmentRestApi().delete(autoAssignment.getId()));
                    selectionGrid.refreshGrid(false);
                    return CompletableFuture.completedFuture(null);
                });
        selectionGrid.getDataCommunicator().getKeyMapper().setIdentifierGetter(MgmtAutoAssignmentResponseBody::getId);
    }

    private static class Actions extends HorizontalLayout {

        @Serial
        private static final long serialVersionUID = 1L;

        private final long autoAssignmentId;
        private final Grid<MgmtAutoAssignmentResponseBody> grid;
        private final transient HawkbitMgmtClient hawkbitClient;

        private Actions(final MgmtAutoAssignmentResponseBody autoAssignment, final Grid<MgmtAutoAssignmentResponseBody> grid,
                final HawkbitMgmtClient hawkbitClient) {
            this.autoAssignmentId = autoAssignment.getId();
            this.grid = grid;
            this.hawkbitClient = hawkbitClient;
            init(autoAssignment);
        }

        private void init(final MgmtAutoAssignmentResponseBody autoAssignment) {
            if("READY".equalsIgnoreCase(autoAssignment.getStatus())) {
                add(Utils.tooltip(new Button(START_COG.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getAutoAssignmentRestApi().start(autoAssignment.getId());
                            refresh();
                        });
                    }
                }, "Start"));
            } else if("RUNNING".equalsIgnoreCase(autoAssignment.getStatus())) {
                add(Utils.tooltip(new Button(PAUSE.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getAutoAssignmentRestApi().pause(autoAssignment.getId());
                            refresh();
                        });
                    }
                }, "Pause"));
            } else if("PAUSED".equalsIgnoreCase(autoAssignment.getStatus())) {
                add(Utils.tooltip(new Button(START_COG.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getAutoAssignmentRestApi().resume(autoAssignment.getId());
                            refresh();
                        });
                    }
                }, "Resume"));
            }
            add(Utils.tooltip(new Button(TRASH.create()) {

                {
                    addClickListener(v -> Utils.confirmDialog("Confirm deletion",
                            "Are you sure you want to delete the selected auto assignment? This action cannot be undone.",
                            "Delete",
                            () -> {
                                hawkbitClient.getAutoAssignmentRestApi().delete(autoAssignment.getId());
                                grid.getDataProvider().refreshAll();
                            }).open()
                    );
                }
            }, "Cancel and remove"));
        }

        private void refresh() {
            final MgmtAutoAssignmentResponseBody body = hawkbitClient.getAutoAssignmentRestApi().getAutoAssignment(autoAssignmentId).getBody();
            if(body != null) {
                grid.getDataProvider().refreshItem(body);
            }
        }

    }

    private static class AutoAssignmentFilter implements Filter.Rsql {

        private final TextField name = Utils.textField(Constants.NAME);

        private AutoAssignmentFilter() {
            name.setPlaceholder("<name filter>");
        }

        @Override
        public List<Component> components() {
            return List.of(name);
        }

        @Override
        public String filter() {
            return Filter.filter(Map.of("name", name.getOptionalValue()));
        }
    }

    private static class AutoAssignmentDetails extends FormLayout {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient HawkbitMgmtClient hawkbitClient;

        private final TextArea description = new TextArea(Constants.DESCRIPTION);
        private final TextField createdBy = Utils.textField(Constants.CREATED_BY);
        private final TextField createdAt = Utils.textField(Constants.CREATED_AT);
        private final TextField lastModifiedBy = Utils.textField(Constants.LAST_MODIFIED_BY);
        private final TextField lastModifiedAt = Utils.textField(Constants.LAST_MODIFIED_AT);
        private final TextField targetFilter = Utils.textField(Constants.TARGET_FILTER);
        private final TextField distributionSet = Utils.textField(Constants.DISTRIBUTION_SET);
        private final TextField actonType = Utils.textField(Constants.ACTION_TYPE);
        private final TextField startAt = Utils.textField(Constants.START_AT);


        private AutoAssignmentDetails(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;

            description.setMinLength(2);
            Stream.of(
                            description,
                            createdBy, createdAt,
                            lastModifiedBy, lastModifiedAt,
                            targetFilter, distributionSet,
                            actonType, startAt)
                    .forEach(field -> {
                        field.setReadOnly(true);
                        add(field);
                    });
            setResponsiveSteps(new ResponsiveStep("0", 2));
            setColspan(description, 2);
        }

        private void setItem(final MgmtAutoAssignmentResponseBody autoAssignment) {
            description.setValue(Objects.requireNonNullElse(autoAssignment.getDescription(), ""));

            createdBy.setValue(autoAssignment.getCreatedBy());
            createdAt.setValue(Utils.localDateTimeFromTs(autoAssignment.getCreatedAt()));
            lastModifiedBy.setValue(autoAssignment.getLastModifiedBy());
            lastModifiedAt.setValue(Utils.localDateTimeFromTs(autoAssignment.getLastModifiedAt()));
            targetFilter.setValue(autoAssignment.getTargetFilterQuery());
            final MgmtDistributionSet distributionSetMgmt = hawkbitClient.getDistributionSetRestApi()
                    .getDistributionSet(autoAssignment.getDistributionSetId()).getBody();
            distributionSet.setValue(distributionSetMgmt == null
                    ? NOT_AVAILABLE_NULL //should not be the case
                    : distributionSetMgmt.getName() + ":" + distributionSetMgmt.getVersion());
            actonType.setValue(switch (autoAssignment.getActionType()) {
                case SOFT -> Constants.SOFT;
                case FORCED -> Constants.FORCED;
                case TIMEFORCED -> "";
                case DOWNLOAD_ONLY -> Constants.DOWNLOAD_ONLY;
            });
            startAt.setValue(ObjectUtils.isEmpty(autoAssignment.getStartAt()) ? "" : Utils.localDateTimeFromTs(autoAssignment.getStartAt()));
        }
    }

    private static class CreateDialog extends Utils.BaseDialog<Void> {

        @Serial
        private static final long serialVersionUID = 1L;

        private final TextField name;
        private final TextArea description;
        private final ComboBox<MgmtDistributionSet> distributionSet;
        private final ComboBox<MgmtTargetFilterQuery> targetFilter;
        private final Select<MgmtActionType> actionType;
        private final DateTimePicker startAt = new DateTimePicker(Constants.START_AT);
        private final Button create = new Button("Create");

        private CreateDialog(final HawkbitMgmtClient hawkbitClient) {
            super("Create Auto Assignment");

            name = Utils.textField("Name", this::readyToCreate);
            name.focus();
            distributionSet = Utils.nameComboBox(
                    "Distribution Set",
                    this::readyToCreate,
                    query -> hawkbitClient.getDistributionSetRestApi()
                            .getDistributionSets(
                                    query.getFilter().orElse(null),
                                    query.getOffset(), query.getPageSize(), Constants.NAME_ASC, null)
                            .getBody().getContent().stream());
            distributionSet.setRequiredIndicatorVisible(true);
            distributionSet.setItemLabelGenerator(distributionSet0 -> distributionSet0.getName() + ":" + distributionSet0.getVersion());
            distributionSet.setWidthFull();
            targetFilter = Utils.nameComboBox(
                    "Target Filter",
                    this::readyToCreate,
                    query -> hawkbitClient.getTargetFilterQueryRestApi()
                            .getFilters(query.getFilter().orElse(null), query.getOffset(), query.getPageSize(), Constants.NAME_ASC, null)
                            .getBody().getContent().stream());
            targetFilter.setRequiredIndicatorVisible(true);
            targetFilter.setItemLabelGenerator(MgmtTargetFilterQuery::getName);
            targetFilter.setWidthFull();
            description = new TextArea(Constants.DESCRIPTION);
            description.setMinLength(2);
            description.setWidthFull();

            actionType = Utils.actionTypeControls(
                    new MgmtActionType[] { MgmtActionType.FORCED, MgmtActionType.SOFT, MgmtActionType.DOWNLOAD_ONLY },
                    MgmtActionType.FORCED, null);

            create.setEnabled(false);
            create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addCreateClickListener(hawkbitClient);
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(create);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setSpacing(false);
            layout.add(name, distributionSet, targetFilter, description, actionType, startAt);
            add(layout);
            open();
        }

        private void readyToCreate(final Object v) {
            final boolean createEnabled = !name.isEmpty() && !distributionSet.isEmpty() && !targetFilter.isEmpty();
            if(create.isEnabled() != createEnabled) {
                create.setEnabled(createEnabled);
            }
        }

        private void addCreateClickListener(final HawkbitMgmtClient hawkbitClient) {
            create.addClickListener(e -> {
                close();
                final MgmtAutoAssignmentRestRequestBodyPost request = new MgmtAutoAssignmentRestRequestBodyPost();
                request.setName(name.getValue());
                request.setDescription(description.getValue());
                request.setDistributionSetId(distributionSet.getValue().getId());
                request.setTargetFilterQuery(targetFilter.getValue().getQuery());

                request.setActionType(actionType.getValue());
                request.setStartAt(!startAt.isEmpty() ? startAt.getValue().toEpochSecond(ZoneOffset.UTC) * 1000 : null);
                hawkbitClient.getAutoAssignmentRestApi().create(request);
            });
        }
    }
}
