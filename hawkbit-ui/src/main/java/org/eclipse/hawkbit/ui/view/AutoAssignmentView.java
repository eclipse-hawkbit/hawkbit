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

import static com.vaadin.flow.component.icon.VaadinIcon.CHECK;
import static com.vaadin.flow.component.icon.VaadinIcon.CLOSE;
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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;

@PageTitle("Auto Assignments")
@Route(value = "auto_assignments", layout = MainLayout.class)
@RolesAllowed({ "AUTO_ASSIGNMENT_READ" })
@Uses(Icon.class)
public final class AutoAssignmentView extends TableView<MgmtAutoAssignmentResponseBody, Long> {

    @Serial
    private static final long serialVersionUID = 1L;
    public static final String READY_STATUS = "READY";
    public static final String RUNNING_STATUS = "RUNNING";
    public static final String PAUSED_STATUS = "PAUSED";
    public static final String WAITING_FOR_APPROVAL_STATUS = "WAITING_FOR_APPROVAL";
    public static final String APPROVE_CONFIRMATION = "Approve";
    public static final String DENY_CONFIRMATION = "Deny";

    public AutoAssignmentView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new AutoAssignmentFilter(),
                null,
                new SelectionGrid.EntityRepresentation<MgmtAutoAssignmentResponseBody, Long>(
                        MgmtAutoAssignmentResponseBody.class, MgmtAutoAssignmentResponseBody::getId) {

                    @Override
                    protected void addColumns(final Grid<MgmtAutoAssignmentResponseBody> grid) {
                        grid.addColumn(MgmtAutoAssignmentResponseBody::getId).setHeader(Constants.ID).setAutoWidth(true);
                        grid.addColumn(MgmtAutoAssignmentResponseBody::getName).setHeader(Constants.NAME).setAutoWidth(true);
                        grid.addColumn(MgmtAutoAssignmentResponseBody::getStatus).setHeader(Constants.STATUS).setAutoWidth(true);

                        grid.addComponentColumn(autoAssignment ->
                                new Actions(autoAssignment, grid, hawkbitClient)).setHeader(Constants.ACTIONS).setAutoWidth(true);
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
                },
                autoAssignment -> {
                    final AutoAssignmentDetailedView detailedView = new AutoAssignmentDetailedView(hawkbitClient);
                    detailedView.setItem(autoAssignment);
                    return detailedView;
                },
                SplitLayout.Orientation.VERTICAL,
                autoAssignment -> new EditDialog(autoAssignment, hawkbitClient).result());
    }

    private static class AutoAssignmentDetailedView extends VerticalLayout {

        @Serial
        private static final long serialVersionUID = 1L;

        private final Span autoAssignmentName;
        private final AutoAssignmentDetails details;

        private AutoAssignmentDetailedView(final HawkbitMgmtClient hawkbitClient) {
            autoAssignmentName = new Span();
            details = new AutoAssignmentDetails(hawkbitClient);
            setWidthFull();
            setHeightFull();
            getStyle().set("overflow", "auto");

            add(autoAssignmentName);
            final TabSheet tabSheet = new TabSheet();
            tabSheet.setWidthFull();
            tabSheet.add("Details", details);
            add(tabSheet);
        }

        private void setItem(final MgmtAutoAssignmentResponseBody autoAssignment) {
            autoAssignmentName.setText(autoAssignment.getName());
            details.setItem(autoAssignment);
        }
    }

    private static class Actions extends HorizontalLayout {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final String NO_APPROVE_PERMISSION = "Missing APPROVE_AUTO_ASSIGNMENT permission";

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
            if(READY_STATUS.equalsIgnoreCase(autoAssignment.getStatus())) {
                add(Utils.tooltip(new Button(START_COG.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getAutoAssignmentRestApi().start(autoAssignment.getId());
                            refresh();
                        });
                    }
                }, "Start"));
            } else if(RUNNING_STATUS.equalsIgnoreCase(autoAssignment.getStatus())) {
                add(Utils.tooltip(new Button(PAUSE.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getAutoAssignmentRestApi().pause(autoAssignment.getId());
                            refresh();
                        });
                    }
                }, "Pause"));
            } else if(PAUSED_STATUS.equalsIgnoreCase(autoAssignment.getStatus())) {
                add(Utils.tooltip(new Button(START_COG.create()) {

                    {
                        addClickListener(v -> {
                            hawkbitClient.getAutoAssignmentRestApi().resume(autoAssignment.getId());
                            refresh();
                        });
                    }
                }, "Resume"));
            } else if(WAITING_FOR_APPROVAL_STATUS.equalsIgnoreCase(autoAssignment.getStatus())) {
                final boolean canApprove = hasApprovePermission();

                final Button approve = new Button(CHECK.create());
                approve.setEnabled(canApprove);
                approve.addClickListener(v -> new ApprovalDialog(autoAssignment, true, hawkbitClient, this::refresh));
                add(Utils.tooltip(approve, canApprove ? APPROVE_CONFIRMATION : NO_APPROVE_PERMISSION));

                final Button deny = new Button(CLOSE.create());
                deny.setEnabled(canApprove);
                deny.addClickListener(v -> new ApprovalDialog(autoAssignment, false, hawkbitClient, this::refresh));
                add(Utils.tooltip(deny, canApprove ? DENY_CONFIRMATION : NO_APPROVE_PERMISSION));
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

        private static boolean hasApprovePermission() {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_AUTO_ASSIGNMENT_APPROVE".equals(authority.getAuthority()));
        }

    }

    private static class ApprovalDialog extends Utils.BaseDialog<Void> {

        @Serial
        private static final long serialVersionUID = 1L;

        private ApprovalDialog(final MgmtAutoAssignmentResponseBody autoAssignment, final boolean approve,
                final HawkbitMgmtClient hawkbitClient, final Runnable onDone) {
            super(approve ? "Approve Auto Assignment" : "Deny Auto Assignment");

            final Span target = new Span((approve ? APPROVE_CONFIRMATION : DENY_CONFIRMATION) + " auto assignment: " + autoAssignment.getName());
            final TextArea remark = new TextArea("Remark");
            remark.setWidthFull();

            final Button confirm = new Button(approve ? APPROVE_CONFIRMATION : DENY_CONFIRMATION);
            confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            confirm.addClickListener(e -> {
                close();
                final String remarkValue = ObjectUtils.isEmpty(remark.getValue()) ? null : remark.getValue();
                if(approve) {
                    hawkbitClient.getAutoAssignmentRestApi().approve(autoAssignment.getId(), remarkValue);
                } else {
                    hawkbitClient.getAutoAssignmentRestApi().deny(autoAssignment.getId(), remarkValue);
                }
                onDone.run();
            });
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(confirm);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setSpacing(false);
            layout.add(target, remark);
            add(layout);
            open();
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
        private final TextField weight = Utils.textField(Constants.WEIGHT);
        private final TextField approvalDecidedBy = Utils.textField(Constants.APPROVAL_DECIDED_BY);
        private final TextField approvalRemark = Utils.textField(Constants.APPROVAL_REMARK);
        private final Checkbox confirmationRequired = new Checkbox(Constants.CONFIRMATION_REQUIRED);


        private AutoAssignmentDetails(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;

            description.setMinLength(2);
            Stream.of(
                            description,
                            createdBy, createdAt,
                            lastModifiedBy, lastModifiedAt,
                            targetFilter, distributionSet,
                            actonType, startAt, weight,
                            approvalDecidedBy, approvalRemark)
                    .forEach(field -> {
                        field.setReadOnly(true);
                        add(field);
                    });
            confirmationRequired.setReadOnly(true);
            confirmationRequired.setEnabled(false);
            add(confirmationRequired);
            setResponsiveSteps(new ResponsiveStep("0", 2));
            setColspan(description, 2);
        }

        private void setItem(final MgmtAutoAssignmentResponseBody autoAssignment) {
            description.setValue(Objects.requireNonNullElse(autoAssignment.getDescription(), ""));
            weight.setValue(autoAssignment.getWeight() == null ? "" : String.valueOf(autoAssignment.getWeight()));
            approvalDecidedBy.setValue(Objects.requireNonNullElse(autoAssignment.getApprovalDecidedBy(), ""));
            approvalRemark.setValue(Objects.requireNonNullElse(autoAssignment.getApprovalRemark(), ""));
            confirmationRequired.setValue(autoAssignment.isConfirmationRequired());

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

    private static class EditDialog extends Utils.BaseDialog<Void> {

        @Serial
        private static final long serialVersionUID = 1L;

        private final TextField name;
        private final TextArea description;
        private final Button save;

        private EditDialog(final MgmtAutoAssignmentResponseBody autoAssignment, final HawkbitMgmtClient hawkbitClient) {
            super("Edit Auto Assignment");

            name = Utils.textField(Constants.NAME, this::readyToSave);
            name.setWidthFull();
            description = new TextArea(Constants.DESCRIPTION);
            description.setWidthFull();
            description.setMinLength(2);
            description.setValueChangeMode(ValueChangeMode.EAGER);

            save = Utils.tooltip(new Button("Save"), "Save (Enter)");
            name.setValue(Objects.requireNonNullElse(autoAssignment.getName(), ""));
            description.setValue(Objects.requireNonNullElse(autoAssignment.getDescription(), ""));
            save.addClickListener(e -> {
                final MgmtAutoAssignmentRestRequestBodyPut body = new MgmtAutoAssignmentRestRequestBodyPut();
                body.setName(name.getValue());
                body.setDescription(description.getValue());
                hawkbitClient.getAutoAssignmentRestApi().update(autoAssignment.getId(), body);
                close();
            });
            save.addClickShortcut(Key.ENTER);
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(save);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setPadding(true);
            layout.setSpacing(false);
            layout.add(name, description);
            add(layout);
            open();
        }

        private void readyToSave(final Object v) {
            final boolean saveEnabled = !name.isEmpty();
            if (save.isEnabled() != saveEnabled) {
                save.setEnabled(saveEnabled);
            }
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
        private final NumberField weight;
        private final Checkbox confirmationRequired = new Checkbox("Confirmation Required");
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

            weight = Utils.numberField("Weight");
            weight.setMin(0);
            weight.setMax(1000);
            weight.setWidthFull();

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
            layout.add(name, distributionSet, targetFilter, description, actionType, startAt, weight, confirmationRequired);
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
                if (!weight.isEmpty()) {
                    request.setWeight(weight.getValue().intValue());
                }
                request.setConfirmationRequired(confirmationRequired.getValue());
                hawkbitClient.getAutoAssignmentRestApi().create(request);
            });
        }
    }
}
