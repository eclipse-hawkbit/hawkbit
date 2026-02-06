/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.view;

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
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutErrorAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;
import org.springframework.util.ObjectUtils;

@PageTitle("Rollouts")
@Route(value = "rollouts", layout = MainLayout.class)
@RolesAllowed({ "ROLLOUT_READ" })
@Uses(Icon.class)
@SuppressWarnings({ "java:S1171", "java:S3599" })
public class RolloutView extends TableView<MgmtRolloutResponseBody, Long> {

    public RolloutView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new RolloutFilter(),
                new SelectionGrid.EntityRepresentation<>(
                        MgmtRolloutResponseBody.class, MgmtRolloutResponseBody::getId) {

                    private final RolloutDetails details = new RolloutDetails(hawkbitClient);

                    @Override
                    protected void addColumns(final Grid<MgmtRolloutResponseBody> grid) {
                        grid.addColumn(MgmtRolloutResponseBody::getId).setHeader(Constants.ID).setAutoWidth(true);
                        grid.addColumn(MgmtRolloutResponseBody::getName).setHeader(Constants.NAME).setAutoWidth(true);
                        grid.addColumn(MgmtRolloutResponseBody::getTotalGroups).setHeader(Constants.GROUP_COUNT).setAutoWidth(true);
                        grid.addColumn(MgmtRolloutResponseBody::getTotalTargets).setHeader(Constants.TARGET_COUNT).setAutoWidth(true);
                        grid.addColumn(MgmtRolloutResponseBody::getTotalTargetsPerStatus).setHeader(Constants.STATS).setAutoWidth(true);
                        grid.addColumn(MgmtRolloutResponseBody::getStatus).setHeader(Constants.STATUS).setAutoWidth(true);

                        grid.addComponentColumn(rollout -> new Actions(rollout, grid, hawkbitClient)).setHeader(
                                Constants.ACTIONS).setAutoWidth(true);

                        grid.setItemDetailsRenderer(new ComponentRenderer<>(
                                () -> details, RolloutDetails::setItem));
                    }
                },
                (query, rsqlFilter) -> Optional.ofNullable(
                        hawkbitClient.getRolloutRestApi()
                                .getRollouts(
                                        rsqlFilter, query.getOffset(), query.getPageSize(), Constants.NAME_ASC, "full")
                                .getBody()).stream().flatMap(page -> page.getContent().stream()),
                selectionGrid -> new CreateDialog(hawkbitClient).result(),
                selectionGrid -> {
                    selectionGrid.getSelectedItems().forEach(
                            rollout -> hawkbitClient.getRolloutRestApi().delete(rollout.getId()));
                    selectionGrid.refreshGrid(false);
                    return CompletableFuture.completedFuture(null);
                });
    }

    private static class Actions extends HorizontalLayout {

        private final long rolloutId;
        private final Grid<MgmtRolloutResponseBody> grid;
        private final transient HawkbitMgmtClient hawkbitClient;

        private Actions(final MgmtRolloutResponseBody rollout, final Grid<MgmtRolloutResponseBody> grid,
                final HawkbitMgmtClient hawkbitClient) {
            this.rolloutId = rollout.getId();
            this.grid = grid;
            this.hawkbitClient = hawkbitClient;
            init(rollout);
        }

        private void init(final MgmtRolloutResponseBody rollout) {
            if ("READY".equalsIgnoreCase(rollout.getStatus())) {
                add(Utils.tooltip(new Button(VaadinIcon.START_COG.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getRolloutRestApi().start(rollout.getId());
                            refresh();
                        });
                    }
                }, "Start"));
            } else if ("RUNNING".equalsIgnoreCase(rollout.getStatus())) {
                add(Utils.tooltip(new Button(VaadinIcon.PAUSE.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getRolloutRestApi().pause(rollout.getId());
                            refresh();
                        });
                    }
                }, "Pause"));
            } else if ("PAUSED".equalsIgnoreCase(rollout.getStatus())) {
                add(Utils.tooltip(new Button(VaadinIcon.START_COG.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getRolloutRestApi().resume(rollout.getId());
                            refresh();
                        });
                    }
                }, "Resume"));
            }
            add(Utils.tooltip(new Button(VaadinIcon.TRASH.create()) {

                {
                    addClickListener(v -> {
                        hawkbitClient.getRolloutRestApi().delete(rollout.getId());
                        grid.getDataProvider().refreshAll();
                    });
                }
            }, "Cancel and Remove"));
        }

        private void refresh() {
            removeAll();
            final MgmtRolloutResponseBody body = hawkbitClient.getRolloutRestApi().getRollout(rolloutId).getBody();
            if (body != null) {
                init(body);
            }
        }
    }

    private static class RolloutFilter implements Filter.Rsql {

        private final TextField name = Utils.textField(Constants.NAME);

        private RolloutFilter() {
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

    private static class RolloutDetails extends FormLayout {

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
        private final Checkbox dynamic = new Checkbox(Constants.DYNAMIC);
        private final SelectionGrid<MgmtRolloutGroupResponseBody, Long> groupGrid;

        private RolloutDetails(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;

            description.setMinLength(2);
            groupGrid = createGroupGrid();
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
            dynamic.setReadOnly(true);
            dynamic.setEnabled(false);
            add(dynamic);
            add(groupGrid);

            setResponsiveSteps(new ResponsiveStep("0", 2));
            setColspan(description, 2);
            setColspan(groupGrid, 2);
        }

        private void setItem(final MgmtRolloutResponseBody rollout) {
            description.setValue(Objects.requireNonNullElse(rollout.getDescription(), ""));

            createdBy.setValue(rollout.getCreatedBy());
            createdAt.setValue(Utils.localDateTimeFromTs(rollout.getCreatedAt()));
            lastModifiedBy.setValue(rollout.getLastModifiedBy());
            lastModifiedAt.setValue(Utils.localDateTimeFromTs(rollout.getLastModifiedAt()));
            targetFilter.setValue(rollout.getTargetFilterQuery());
            final MgmtDistributionSet distributionSetMgmt = hawkbitClient.getDistributionSetRestApi()
                    .getDistributionSet(rollout.getDistributionSetId()).getBody();
            distributionSet.setValue(distributionSetMgmt == null
                    ? NOT_AVAILABLE_NULL // should not be the case
                    : distributionSetMgmt.getName() + ":" + distributionSetMgmt.getVersion());
            actonType.setValue(switch (rollout.getType()) {
                case SOFT -> Constants.SOFT;
                case FORCED -> Constants.FORCED;
                case DOWNLOAD_ONLY -> Constants.DOWNLOAD_ONLY;
                case TIMEFORCED -> "Scheduled at " + Utils.localDateTimeFromTs(rollout.getForcetime());
            });
            startAt.setValue(ObjectUtils.isEmpty(rollout.getStartAt()) ? "" : Utils.localDateTimeFromTs(rollout.getStartAt()));
            dynamic.setValue(rollout.isDynamic());

            groupGrid.setItems(query -> Optional.ofNullable(
                    hawkbitClient.getRolloutRestApi()
                            .getRolloutGroups(
                                    rollout.getId(),
                                    null, query.getOffset(), query.getPageSize(),
                                    null, "full")
                            .getBody())
                    .stream().flatMap(body -> body.getContent().stream())
                    .skip(query.getOffset())
                    .limit(query.getPageSize()));
            groupGrid.setSelectionMode(Grid.SelectionMode.NONE);
        }

        private static SelectionGrid<MgmtRolloutGroupResponseBody, Long> createGroupGrid() {
            return new SelectionGrid<>(
                    new SelectionGrid.EntityRepresentation<>(MgmtRolloutGroupResponseBody.class, MgmtRolloutGroupResponseBody::getId) {

                        @Override
                        protected void addColumns(final Grid<MgmtRolloutGroupResponseBody> grid) {
                            grid.addColumn(MgmtRolloutGroupResponseBody::getId).setHeader(Constants.ID).setAutoWidth(true);
                            grid.addColumn(MgmtRolloutGroupResponseBody::getName).setHeader(Constants.NAME).setAutoWidth(true);
                            grid.addColumn(MgmtRolloutGroupResponseBody::getTotalTargets).setHeader(Constants.TARGET_COUNT).setAutoWidth(true);
                            grid.addColumn(MgmtRolloutGroupResponseBody::getTotalTargetsPerStatus).setHeader(Constants.STATS).setAutoWidth(
                                    true);
                            grid.addColumn(MgmtRolloutGroupResponseBody::getStatus).setHeader(Constants.STATUS).setAutoWidth(true);
                        }
                    });
        }
    }

    private static class CreateDialog extends Utils.BaseDialog<Void> {

        private final TextField name;
        private final ComboBox<MgmtDistributionSet> distributionSet;
        private final ComboBox<MgmtTargetFilterQuery> targetFilter;
        private final TextArea description;
        private final Select<MgmtActionType> actionType;
        private final DateTimePicker forceTime = new DateTimePicker("Force Time");
        private final Select<StartType> startType;
        private final DateTimePicker startAt = new DateTimePicker(Constants.START_AT);
        private final NumberField groupNumber;
        private final NumberField triggerThreshold;
        private final NumberField errorThreshold;
        private final Checkbox dynamic = new Checkbox(Constants.DYNAMIC);
        private final Button create = new Button("Create");

        private CreateDialog(final HawkbitMgmtClient hawkbitClient) {
            super("Create Rollout");

            name = Utils.textField("Name", this::readyToCreate);
            name.focus();
            distributionSet = Utils.nameComboBox(
                    "Distribution Set",
                    this::readyToCreate,
                    query -> hawkbitClient.getDistributionSetRestApi()
                            .getDistributionSets(query.getFilter().orElse(null), query.getOffset(), query.getPageSize(), Constants.NAME_ASC)
                            .getBody().getContent().stream());
            distributionSet.setRequiredIndicatorVisible(true);
            distributionSet.setItemLabelGenerator(distributionSetO -> distributionSetO.getName() + ":" + distributionSetO.getVersion());
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

            actionType = Utils.actionTypeControls(MgmtActionType.FORCED, forceTime);

            startType = new Select<>();
            startType.setValue(StartType.MANUAL);
            startType.setLabel(Constants.START_TYPE);
            startType.setItems(StartType.values());
            startType.setValue(StartType.MANUAL);
            final ComponentRenderer<Component, StartType> startTypeRenderer = new ComponentRenderer<>(startTypeO -> switch (startTypeO) {
                case MANUAL -> new Text(Constants.MANUAL);
                case AUTO -> new Text(Constants.AUTO);
                case SCHEDULED -> startAt;
            });
            startType.setRenderer(startTypeRenderer);
            startType.addValueChangeListener(e -> startType.setRenderer(startTypeRenderer));
            startType.setItemLabelGenerator(startTypeO -> switch (startTypeO) {
                case MANUAL -> Constants.MANUAL;
                case AUTO -> Constants.AUTO;
                case SCHEDULED -> "Scheduled" + (startAt.isEmpty() ? "" : "  at " + startAt.getValue());
            });
            startType.setWidthFull();

            final Div percentSuffix = new Div();
            percentSuffix.setText("%");
            groupNumber = Utils.numberField("Group number", this::readyToCreate);
            groupNumber.setMin(1);
            groupNumber.setValue(1.0);
            triggerThreshold = Utils.numberField("Trigger Threshold", this::readyToCreate);
            triggerThreshold.setMin(0);
            triggerThreshold.setMax(100);
            triggerThreshold.setValue(100.0);
            triggerThreshold.setSuffixComponent(percentSuffix);
            errorThreshold = Utils.numberField("Error Threshold", this::readyToCreate);
            errorThreshold.setMin(1);
            errorThreshold.setMax(100);
            errorThreshold.setValue(10.0);
            errorThreshold.setSuffixComponent(percentSuffix);

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
            layout.add(
                    name, distributionSet, targetFilter, description,
                    actionType, startType,
                    groupNumber, triggerThreshold, errorThreshold,
                    dynamic);
            add(layout);
            open();
        }

        private void readyToCreate(final Object v) {
            final boolean createEnabled = !name.isEmpty() && !distributionSet.isEmpty() && !targetFilter.isEmpty() && !groupNumber
                    .isEmpty() && !triggerThreshold.isEmpty() && !errorThreshold.isEmpty();
            if (create.isEnabled() != createEnabled) {
                create.setEnabled(createEnabled);
            }
        }

        private void addCreateClickListener(final HawkbitMgmtClient hawkbitClient) {
            create.addClickListener(e -> {
                close();
                final MgmtRolloutRestRequestBodyPost request = new MgmtRolloutRestRequestBodyPost();
                request.setName(name.getValue());
                request.setDistributionSetId(distributionSet.getValue().getId());
                request.setTargetFilterQuery(targetFilter.getValue().getQuery());
                request.setDescription(description.getValue());

                request.setType(actionType.getValue());
                if (actionType.getValue() == MgmtActionType.TIMEFORCED) {
                    request.setForcetime(
                            forceTime.isEmpty() ? System.currentTimeMillis() : forceTime.getValue().toEpochSecond(ZoneOffset.UTC) * 1000);
                }
                switch (startType.getValue()) {
                    case AUTO -> request.setStartAt(System.currentTimeMillis());
                    case SCHEDULED -> request.setStartAt(
                            startAt.isEmpty() ? System.currentTimeMillis() : startAt.getValue().toEpochSecond(ZoneOffset.UTC) * 1000);
                    case MANUAL -> {
                        // do nothing, will be started manually
                    }
                }

                request.setAmountGroups(groupNumber.getValue().intValue());
                request.setSuccessCondition(
                        new MgmtRolloutCondition(
                                MgmtRolloutCondition.Condition.THRESHOLD,
                                triggerThreshold.getValue().intValue() + ""));
                request.setErrorCondition(
                        new MgmtRolloutCondition(
                                MgmtRolloutCondition.Condition.THRESHOLD,
                                errorThreshold.getValue().intValue() + ""));
                request.setErrorAction(
                        new MgmtRolloutErrorAction(
                                MgmtRolloutErrorAction.ErrorAction.PAUSE, ""));
                request.setDynamic(dynamic.getValue());
                hawkbitClient.getRolloutRestApi().create(request).getBody();
            });
        }

        private enum StartType {
            MANUAL, AUTO, SCHEDULED
        }
    }
}
