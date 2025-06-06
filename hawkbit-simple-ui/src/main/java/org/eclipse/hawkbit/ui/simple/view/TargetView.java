/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.view;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.ui.simple.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.simple.MainLayout;
import org.eclipse.hawkbit.ui.simple.view.util.Filter;
import org.eclipse.hawkbit.ui.simple.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.simple.view.util.TableView;
import org.eclipse.hawkbit.ui.simple.view.util.Utils;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;

@PageTitle("Targets")
@Route(value = "targets", layout = MainLayout.class)
@RolesAllowed({ "TARGET_READ" })
@Uses(Icon.class)
public class TargetView extends TableView<MgmtTarget, String> {

    public static final String STATUS = "Status";
    public static final String CONTROLLER_ID = "Controller Id";
    public static final String TAG = "Tag";

    public TargetView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new RawFilter(hawkbitClient), new SimpleFilter(hawkbitClient),
                new SelectionGrid.EntityRepresentation<>(MgmtTarget.class, MgmtTarget::getControllerId) {

                    @Override
                    protected void addColumns(final Grid<MgmtTarget> grid) {
                        grid.addColumn(new ComponentRenderer<Component, MgmtTarget>(TargetStatusCell::new)).setHeader(STATUS).setAutoWidth(true)
                                .setFlexGrow(0);
                        grid.addColumn(MgmtTarget::getControllerId).setHeader(CONTROLLER_ID).setAutoWidth(true);
                        grid.addColumn(MgmtTarget::getName).setHeader(Constants.NAME).setAutoWidth(true);
                        grid.addColumn(MgmtTarget::getTargetTypeName).setHeader(Constants.TYPE).setAutoWidth(true);
                    }
                },
                (query, filter) -> hawkbitClient.getTargetRestApi()
                        .getTargets(
                                query.getOffset(), query.getPageSize(), Constants.NAME_ASC,
                                filter
                        )
                        .getBody()
                        .getContent()
                        .stream(),
                source -> new RegisterDialog(hawkbitClient).result(),
                selectionGrid -> {
                    selectionGrid.getSelectedItems().forEach(toDelete ->
                            hawkbitClient.getTargetRestApi().deleteTarget(toDelete.getControllerId()));
                    return CompletableFuture.completedFuture(null);
                },
                (target) -> {
                    var targetDetailedView = new TargetDetailedView(hawkbitClient);
                    targetDetailedView.setItem(target);
                    return targetDetailedView;
                }
        );

        Function<SelectionGrid<MgmtTarget, String>, CompletionStage<Void>> assignHandler = source -> new AssignDialog(
                hawkbitClient, source.getSelectedItems()).result();

        final Button assignBtn = Utils.tooltip(new Button(VaadinIcon.LINK.create()), "Assign");
        assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        assignBtn.addClickListener(e -> assignHandler
                .apply(selectionGrid)
                .thenAccept(v -> selectionGrid.refreshGrid(false)));
        controlsLayout.addComponentAtIndex(0, assignBtn);
    }

    private static class SimpleFilter implements Filter.Rsql {

        private final HawkbitMgmtClient hawkbitClient;

        private final TextField controllerId;
        private final CheckboxGroup<MgmtTargetType> type;
        private final CheckboxGroup<MgmtTag> tag;

        private SimpleFilter(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;

            controllerId = Utils.textField(CONTROLLER_ID);
            controllerId.setPlaceholder("<controller id filter>");
            type = new CheckboxGroup<>(Constants.TYPE);
            type.setItemLabelGenerator(MgmtTargetType::getName);
            tag = new CheckboxGroup<>(TAG);
            tag.setItemLabelGenerator(MgmtTag::getName);
        }

        @Override
        public List<Component> components() {
            final List<Component> components = new LinkedList<>();
            components.add(controllerId);
            type.setItems(hawkbitClient.getTargetTypeRestApi().getTargetTypes(0, 20, Constants.NAME_ASC, null).getBody().getContent());
            if (!type.getValue().isEmpty()) {
                components.add(type);
            }
            tag.setItems(hawkbitClient.getTargetTagRestApi().getTargetTags(0, 20, Constants.NAME_ASC, null).getBody().getContent());
            if (!tag.isEmpty()) {
                components.add(tag);
            }
            return components;
        }

        @Override
        public String filter() {
            return Filter.filter(
                    Map.of(
                            "controllerid", controllerId.getOptionalValue(),
                            "targettype.name", type.getSelectedItems().stream().map(MgmtTargetType::getName)
                                    .toList(),
                            "tag", tag.getSelectedItems()
                    ));
        }
    }

    @SuppressWarnings({ "java:S1171", "java:S3599" })
    private static class RawFilter implements Filter.Rsql {

        private final TextField textFilter = new TextField("Raw Filter", "<raw filter>");
        private final VerticalLayout layout = new VerticalLayout();
        private final Select<MgmtTargetFilterQuery> savedFilters = new Select<>();

        private RawFilter(final HawkbitMgmtClient hawkbitClient) {
            final Button createBtn = Utils.tooltip(new Button("Save", VaadinIcon.PLUS.create()), "Save");
            final Button updateBtn = Utils.tooltip(new Button(VaadinIcon.HARDDRIVE.create()), "Update");
            updateBtn.setEnabled(false);

            savedFilters.setLabel("Saved Filters");
            savedFilters.addValueChangeListener(e -> {
                if (e.getValue() != null) {
                    textFilter.setValue(e.getValue().getQuery());
                    updateBtn.setEnabled(true);
                    createBtn.setText("Save as");
                } else {
                    textFilter.clear();
                    updateBtn.setEnabled(false);
                    createBtn.setText("Save");
                }
            });
            savedFilters.setEmptySelectionAllowed(true);
            savedFilters.setItems(listFilters(hawkbitClient));
            savedFilters.setItemLabelGenerator(
                    query -> Optional.ofNullable(query).map(MgmtTargetFilterQuery::getName).orElse("<select saved filter>"));
            savedFilters.setWidthFull();

            textFilter.setWidthFull();
            createBtn.addClickListener(createBtnListener(hawkbitClient));
            updateBtn.addClickListener(updateBtnListener(hawkbitClient));

            layout.setSpacing(false);
            layout.setPadding(false);
            final HorizontalLayout textSaveLayout = new HorizontalLayout(textFilter, createBtn, updateBtn);
            textSaveLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
            textSaveLayout.setWidthFull();
            layout.add(savedFilters, textSaveLayout);
            layout.addClassNames(LumoUtility.Gap.SMALL);
        }

        private static List<MgmtTargetFilterQuery> listFilters(HawkbitMgmtClient hawkbitClient) {
            return Optional.ofNullable(
                    hawkbitClient.getTargetFilterQueryRestApi()
                            .getFilters(0, 30, null, null, null)
                            .getBody().getContent()
            ).orElse(Collections.emptyList());
        }

        private ComponentEventListener<ClickEvent<Button>> createBtnListener(HawkbitMgmtClient hawkbitClient) {
            return e ->
                    new Utils.BaseDialog<Void>("Create New Filter") {

                        {
                            final Button finishBtn = Utils.tooltip(new Button("Save"), "Save (Enter)");
                            final TextField name = Utils.textField(
                                    Constants.NAME,
                                    e -> finishBtn.setEnabled(!e.getHasValue().isEmpty())
                            );
                            name.focus();
                            finishBtn.addClickShortcut(Key.ENTER);
                            finishBtn.setEnabled(false);
                            finishBtn.addClickListener(e -> {
                                final MgmtTargetFilterQueryRequestBody createRequest = new MgmtTargetFilterQueryRequestBody();
                                createRequest.setName(name.getValue());
                                createRequest.setQuery(textFilter.getValue());
                                hawkbitClient.getTargetFilterQueryRestApi().createFilter(createRequest);
                                savedFilters.setItems(
                                        listFilters(hawkbitClient));
                                close();
                            });
                            getFooter().add(finishBtn);
                            add(name);
                            open();
                        }
                    };
        }

        private ComponentEventListener<ClickEvent<Button>> updateBtnListener(HawkbitMgmtClient hawkbitClient) {
            return e -> {
                final MgmtTargetFilterQuery selected = savedFilters.getValue();
                if (selected == null) return;

                new Utils.BaseDialog<Void>("Update Filter") {

                    {
                        final Button finishBtn = Utils.tooltip(new Button("Update"), "Update (Enter)");
                        finishBtn.setEnabled(false);

                        final TextField name = Utils.textField(
                                Constants.NAME,
                                e -> finishBtn.setEnabled(!e.getHasValue().isEmpty())
                        );
                        name.focus();
                        name.setValue(selected.getName());

                        final TextArea filterValue = new TextArea("Filter Value");
                        filterValue.setReadOnly(true);
                        filterValue.setValue(textFilter.getValue());
                        filterValue.setWidthFull();

                        finishBtn.addClickShortcut(Key.ENTER);
                        finishBtn.addClickListener(e -> {
                            final MgmtTargetFilterQueryRequestBody updateRequest = new MgmtTargetFilterQueryRequestBody();
                            updateRequest.setName(name.getValue());
                            updateRequest.setQuery(textFilter.getValue());
                            hawkbitClient.getTargetFilterQueryRestApi().updateFilter(selected.getId(), updateRequest);
                            savedFilters.setItems(listFilters(hawkbitClient));
                            close();
                        });
                        getFooter().add(finishBtn);

                        add(name);
                        add(filterValue);
                        open();
                    }
                };
            };
        }

        @Override
        public List<Component> components() {
            return List.of(layout);
        }

        @Override
        public String filter() {
            return textFilter.getOptionalValue().orElse(null);
        }
    }

    protected static class TargetDetailedView extends TabSheet {

        private final TargetDetails targetDetails;
        private final TargetAssignedInstalled targetAssignedInstalled;
        private final TargetTags targetTags;
        private final TargetActions targetActions;

        private TargetDetailedView(HawkbitMgmtClient hawkbitClient) {
            targetDetails = new TargetDetails(hawkbitClient);
            targetAssignedInstalled = new TargetAssignedInstalled(hawkbitClient);
            targetTags = new TargetTags(hawkbitClient);
            targetActions = new TargetActions(hawkbitClient);
            setWidthFull();

            add("Details", targetDetails);
            add("Assigned / Installed", targetAssignedInstalled);
            add("Tags", targetTags);
            add("Action History", targetActions);
        }

        private void setItem(final MgmtTarget target) {
            this.targetDetails.setItem(target);
            this.targetAssignedInstalled.setItem(target);
            this.targetTags.setItem(target);
            this.targetActions.setItem(target);
        }
    }

    private static class TargetDetails extends FormLayout {

        private final transient HawkbitMgmtClient hawkbitClient;
        private final TextArea description = new TextArea(Constants.DESCRIPTION);
        private final TextField createdBy = Utils.textField(Constants.CREATED_BY);
        private final TextField createdAt = Utils.textField(Constants.CREATED_AT);
        private final TextField lastModifiedBy = Utils.textField(Constants.LAST_MODIFIED_BY);
        private final TextField lastModifiedAt = Utils.textField(Constants.LAST_MODIFIED_AT);
        private final TextField securityToken = Utils.textField(Constants.SECURITY_TOKEN);
        private final TextArea targetAttributes = new TextArea(Constants.ATTRIBUTES);
        private MgmtTarget target;

        private TargetDetails(HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;
            description.setMinLength(2);
            Stream.of(
                            description,
                            createdBy, createdAt,
                            lastModifiedBy, lastModifiedAt,
                            securityToken, targetAttributes
                    )
                    .forEach(field -> {
                        field.setReadOnly(true);
                        add(field);
                    });

            setResponsiveSteps(new ResponsiveStep("0", 2));
            setColspan(description, 2);
        }

        private void setItem(final MgmtTarget target) {
            this.target = target;
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            description.setValue(target.getDescription() == null ? "N/A" : target.getDescription());
            createdBy.setValue(target.getCreatedBy());
            createdAt.setValue(new Date(target.getCreatedAt()).toString());
            lastModifiedBy.setValue(target.getLastModifiedBy());
            lastModifiedAt.setValue(new Date(target.getLastModifiedAt()).toString());
            securityToken.setValue(target.getSecurityToken());
            var response = hawkbitClient.getTargetRestApi().getAttributes(target.getControllerId());
            if (response.getStatusCode().is2xxSuccessful()) {
                targetAttributes.setValue(Objects.requireNonNullElse(response.getBody(), Collections.emptyMap()).entrySet().stream()
                        .map(entry -> entry.getKey() + ": " +
                                entry.getValue()).collect(Collectors.joining("\n")));
            } else {
                targetAttributes.setValue("Error occurred fetching attributes from server: " + response.getStatusCode());
            }
        }
    }

    private static class TargetAssignedInstalled extends FormLayout {

        private final transient HawkbitMgmtClient hawkbitClient;
        private final TextArea assigned = new TextArea("Assigned Distribution Set");
        private final TextArea installed = new TextArea("Installed Distribution Set");
        private MgmtTarget target;

        private TargetAssignedInstalled(HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;
            assigned.setReadOnly(true);
            installed.setReadOnly(true);
            assigned.setWidthFull();
            installed.setWidthFull();
            add(assigned, installed);
            setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        }

        private void setItem(final MgmtTarget target) {
            this.target = target;
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            updateDistributionSetInfo(
                    () -> hawkbitClient.getTargetRestApi().getInstalledDistributionSet(target.getControllerId()),
                    installed
            );
            updateDistributionSetInfo(
                    () -> hawkbitClient.getTargetRestApi().getAssignedDistributionSet(target.getControllerId()),
                    assigned
            );
        }

        private void updateDistributionSetInfo(Supplier<ResponseEntity<MgmtDistributionSet>> supplier, TextArea textArea) {
            Optional.ofNullable(supplier.get())
                    .map(ResponseEntity<MgmtDistributionSet>::getBody)
                    .ifPresent(value -> {
                        final String description = """
                                Name:  %s
                                Version: %s
                                %s
                                """.replaceAll("\n", System.lineSeparator());
                        textArea.setValue(description.formatted(
                                value.getName(),
                                value.getVersion(),
                                value.getModules().stream().map(module -> module.getTypeName() + ": " + module.getVersion())
                                        .collect(Collectors.joining(System.lineSeparator()))
                        ));
                    });
        }
    }

    private static class TargetTags extends VerticalLayout {

        private final transient HawkbitMgmtClient hawkbitClient;
        private final ComboBox<MgmtTag> tagSelector = new ComboBox<>(TAG);
        private final HorizontalLayout tagsArea = new HorizontalLayout();
        private Registration changeListener;
        private MgmtTarget target;

        private TargetTags(HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;
            setWidthFull();
            setPadding(false);
            setSpacing(true);
            setAlignItems(FlexComponent.Alignment.STRETCH);
            setJustifyContentMode(JustifyContentMode.CENTER);

            final HorizontalLayout tagSelectorLayout = buildTagSelectionLayout(hawkbitClient);
            add(tagSelectorLayout);

            tagsArea.setWrap(true);
            tagsArea.setWidthFull();
            add(tagsArea);
        }

        private HorizontalLayout buildTagSelectionLayout(HawkbitMgmtClient hawkbitClient) {
            final Button createTagButton = new Button("Create Tag");
            createTagButton.addClickListener(event -> {
                new CreateTagDialog(hawkbitClient, () -> tagSelector.setItems(fetchAvailableTags())).result();
            });

            tagSelector.setWidthFull();
            tagSelector.setItemLabelGenerator(MgmtTag::getName);
            tagSelector.setClearButtonVisible(true);

            final HorizontalLayout tagSelectorLayout = new HorizontalLayout(tagSelector, createTagButton);
            tagSelectorLayout.setWidthFull();
            tagSelectorLayout.setSpacing(true);
            tagSelectorLayout.setAlignItems(Alignment.END);
            tagSelectorLayout.setJustifyContentMode(JustifyContentMode.CENTER);
            return tagSelectorLayout;
        }

        private void setItem(final MgmtTarget target) {
            this.target = target;
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            tagSelector.setItems(fetchAvailableTags());
            if (changeListener != null) {
                changeListener.remove();
            }
            changeListener = tagSelector.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    hawkbitClient.getTargetTagRestApi().assignTarget(event.getValue().getId(), target.getControllerId());
                    refreshTargetTagsList(target);
                    tagSelector.clear();
                }
            });
            refreshTargetTagsList(target);
        }

        private void refreshTargetTagsList(MgmtTarget target) {
            tagsArea.removeAll();

            final List<MgmtTag> tagList = Optional.ofNullable(hawkbitClient.getTargetRestApi().getTags(target.getControllerId()).getBody())
                    .orElse(Collections.emptyList());

            for (MgmtTag tag : tagList) {
                tagsArea.add(buildTargetTagBadge(tag, target.getControllerId()));
            }
        }

        private Span buildTargetTagBadge(MgmtTag tag, String controllerId) {
            Button clearButton = new Button(VaadinIcon.CLOSE_SMALL.create());
            clearButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY_INLINE);
            clearButton.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
            clearButton.getElement().setAttribute("aria-label", "Clear filter: " + tag.getName());
            clearButton.getElement().setAttribute("title", "Clear filter: " + tag.getName());

            Span pill = new Span();
            pill.getElement().getThemeList().add("badge pill");
            pill.getStyle().set("background-color", tag.getColour());
            pill.getStyle().set("margin-inline-end", "var(--lumo-space-xs)");

            Span badge = new Span(pill, new Span(tag.getName()), clearButton);
            badge.getElement().getThemeList().add("badge contrast pill");

            clearButton.addClickListener(event -> {
                hawkbitClient.getTargetTagRestApi().unassignTarget(tag.getId(), controllerId);
                badge.getElement().removeFromParent();
            });

            return badge;
        }

        private List<MgmtTag> fetchAvailableTags() {
            List<MgmtTag> tags = new ArrayList<>();
            int fetched = 0;
            int offset = 0;
            do {
                List<MgmtTag> page = Optional.ofNullable(
                                hawkbitClient.getTargetTagRestApi().getTargetTags(offset, 50, Constants.NAME_ASC, null).getBody())
                        .map(PagedList::getContent)
                        .orElse(Collections.emptyList());
                tags.addAll(page);
                fetched = page.size();
                offset += fetched;
            } while (fetched > 0);
            return tags;
        }
    }

    private static class TargetActions extends Grid<TargetActions.ActionStatusEntry> {

        private final transient HawkbitMgmtClient hawkbitClient;
        private MgmtTarget target;

        private TargetActions(HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;
            setWidthFull();
            addColumn(new ComponentRenderer<Component, ActionStatusEntry>(ActionStatusEntry::getStatusIcon)).setHeader("Status")
                    .setAutoWidth(true)
                    .setFlexGrow(0);
            addColumn(ActionStatusEntry::getDistributionSetName).setHeader("Distribution Set").setAutoWidth(true);
            addColumn(ActionStatusEntry::getLastModifiedAt).setHeader("Last Modified").setAutoWidth(true)
                    .setFlexGrow(0).setComparator(ActionStatusEntry::getLastModifiedAt);
            addColumn(new ComponentRenderer<Component, ActionStatusEntry>(ActionStatusEntry::getForceTypeIcon)).setHeader("Type")
                    .setAutoWidth(true)
                    .setFlexGrow(0);
            addColumn(new ComponentRenderer<Component, ActionStatusEntry>(ActionStatusEntry::getActionsLayout)).setHeader("Actions")
                    .setAutoWidth(true)
                    .setFlexGrow(0);
            addColumn(new ComponentRenderer<Component, ActionStatusEntry>(ActionStatusEntry::getForceQuitLayout)).setHeader("Force Quit")
                    .setAutoWidth(true)
                    .setFlexGrow(0);
        }

        private void setItem(final MgmtTarget target) {
            this.target = target;
        }

        private List<ActionStatusEntry> fetchActions() {
            return this.hawkbitClient.getTargetRestApi().getActionHistory(target.getControllerId(), 0, 30, null, null)
                    .getBody()
                    .getContent()
                    .stream()
                    .map(action -> new ActionStatusEntry(action, () -> setItems(fetchActions())))
                    .filter(value -> value.action != null)
                    .collect(Collectors.toList());
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            setItems(fetchActions());
        }

        private class ActionStatusEntry {

            MgmtAction action;
            MgmtDistributionSet distributionSet;
            Runnable onUpdate;

            public ActionStatusEntry(MgmtAction mgmtAction, Runnable onUpdate) {
                this.action = hawkbitClient.getActionRestApi().getAction(mgmtAction.getId()).getBody();
                this.onUpdate = onUpdate;
                if (action == null) {
                    LoggerFactory.getLogger(ActionStatusEntry.class).error("Unable to fetch the action with id : {}", mgmtAction.getId());
                    return;
                }
                this.action.getLink("distributionset").ifPresent(link -> {
                    try {
                        Long dsId = Long.parseLong(link.getHref().substring(link.getHref().lastIndexOf("/") + 1));
                        this.distributionSet = hawkbitClient.getDistributionSetRestApi().getDistributionSet(dsId).getBody();
                    } catch (NumberFormatException e) {
                        LoggerFactory.getLogger(ActionStatusEntry.class).error("Error parsing distribution set ID", e);
                    }
                });
            }

            private Boolean isActive() {
                return action.getStatus().equals(MgmtAction.ACTION_PENDING);
            }

            private Boolean isCancelingOrCanceled() {
                return action.getType().equals(MgmtAction.ACTION_CANCEL);
            }

            public Component getStatusIcon() {
                HorizontalLayout layout = new HorizontalLayout();
                Icon icon;
                if (isActive()) {
                    if (isCancelingOrCanceled()) {
                        icon = Utils.tooltip(VaadinIcon.ADJUST.create(), "Pending Cancellation");
                        icon.setColor("red");
                    } else {
                        icon = Utils.tooltip(VaadinIcon.ADJUST.create(), "Pending Update");
                        icon.setColor("orange");
                    }
                } else if (action.getType().equals(MgmtAction.ACTION_UPDATE)) {
                    icon = Utils.tooltip(VaadinIcon.CHECK_CIRCLE.create(), "Updated");
                    icon.setColor("green");
                } else {
                    icon = Utils.tooltip(VaadinIcon.CLOSE_CIRCLE.create(), "Canceled");
                    icon.setColor("red");
                }

                icon.addClassNames(LumoUtility.IconSize.SMALL);
                layout.add(icon);
                layout.setWidth(50, Unit.PIXELS);
                layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                return layout;
            }

            public String getDistributionSetName() {
                return distributionSet != null ? distributionSet.getName() : "Distribution Set not found";
            }

            public Instant getLastModifiedAt() {
                return Instant.ofEpochMilli(action.getLastModifiedAt());
            }

            public Icon getForceTypeIcon() {
                Icon icon = switch (action.getForceType()) {
                    case FORCED -> VaadinIcon.BOLT.create();
                    case TIMEFORCED -> VaadinIcon.USER_CLOCK.create();
                    case SOFT -> VaadinIcon.USER_CHECK.create();
                    case DOWNLOAD_ONLY -> VaadinIcon.DOWNLOAD.create();
                };
                return Utils.tooltip(icon, action.getForceType().getName());
            }

            public HorizontalLayout getActionsLayout() {
                HorizontalLayout actionsLayout = new HorizontalLayout();
                actionsLayout.setSpacing(true);

                Button cancelButton = Utils.tooltip(new Button(VaadinIcon.CLOSE.create()), "Cancel Action");
                if (isActive() && !isCancelingOrCanceled()) {
                    cancelButton.addClickListener(e -> {
                        String message = "Are you sure you want to cancel the action ?";
                        promptForConfirmAction(
                                message, onUpdate, () -> {
                                    hawkbitClient.getTargetRestApi().cancelAction(target.getControllerId(), action.getId(), false);
                                }
                        ).open();
                    });
                } else {
                    cancelButton.setEnabled(false);
                }

                Button forceButton = Utils.tooltip(new Button(VaadinIcon.BOLT.create()), "Force Action");
                if (isActive() && !isCancelingOrCanceled() && action.getForceType() != MgmtActionType.FORCED) {
                    forceButton.addClickListener(e -> {
                        String message = "Are you sure you want to force the action ?";
                        promptForConfirmAction(
                                message, onUpdate, () -> {
                                    MgmtActionRequestBodyPut setForced = new MgmtActionRequestBodyPut();
                                    setForced.setForceType(MgmtActionType.FORCED);
                                    hawkbitClient.getTargetRestApi().updateAction(target.getControllerId(), action.getId(), setForced);
                                }
                        ).open();
                    });
                } else {
                    forceButton.setEnabled(false);
                }

                actionsLayout.add(cancelButton, forceButton);
                return actionsLayout;
            }

            public HorizontalLayout getForceQuitLayout() {
                HorizontalLayout forceQuitLayout = new HorizontalLayout();
                forceQuitLayout.setSpacing(true);
                forceQuitLayout.setPadding(true);
                forceQuitLayout.setWidthFull();
                forceQuitLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

                Button forceQuitButton = Utils.tooltip(new Button(VaadinIcon.CLOSE.create()), "Force Cancel");
                forceQuitButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);

                if (isActive() && isCancelingOrCanceled()) {
                    forceQuitButton.addClickListener(e -> {
                        String message = "Are you sure you want to force cancel the action ?";
                        promptForConfirmAction(
                                message, onUpdate, () -> {
                                    hawkbitClient.getTargetRestApi().cancelAction(target.getControllerId(), action.getId(), true);
                                }
                        ).open();
                    });
                } else {
                    forceQuitButton.setEnabled(false);
                }

                forceQuitLayout.add(forceQuitButton);
                return forceQuitLayout;
            }

            private static ConfirmDialog promptForConfirmAction(String message, Runnable refreshActions, Runnable actionConsumer) {
                final ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Confirm Action");
                dialog.setText(message);

                dialog.setCancelable(true);
                dialog.addCancelListener(event -> dialog.close());

                dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
                dialog.setConfirmText("Confirm");
                dialog.addConfirmListener(event -> {
                    actionConsumer.run();
                    refreshActions.run();
                    dialog.close();
                });
                return dialog;
            }
        }
    }

    private static class RegisterDialog extends Utils.BaseDialog<Void> {

        private final Select<MgmtTargetType> type;
        private final TextField controllerId;
        private final TextField name;
        private final TextArea description;

        private RegisterDialog(final HawkbitMgmtClient hawkbitClient) {
            super("Register Target");

            final Button register = Utils.tooltip(new Button("Register"), "Register (Enter)");
            type = new Select<>(
                    "Type",
                    e -> {
                    },
                    hawkbitClient.getTargetTypeRestApi()
                            .getTargetTypes(0, 30, Constants.NAME_ASC, null)
                            .getBody()
                            .getContent()
                            .toArray(new MgmtTargetType[0])
            );
            type.setWidthFull();
            type.setEmptySelectionAllowed(true);
            type.setItemLabelGenerator(item -> item == null ? "" : item.getName());
            controllerId = Utils.textField(
                    CONTROLLER_ID,
                    e -> register.setEnabled(!e.getHasValue().isEmpty())
            );
            controllerId.focus();
            name = Utils.textField(Constants.NAME);
            name.setWidthFull();
            description = new TextArea(Constants.DESCRIPTION);
            description.setMinLength(2);
            description.setWidthFull();

            addCreateClickListener(register, hawkbitClient);
            register.setEnabled(false);
            register.addClickShortcut(Key.ENTER);
            register.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            final Button cancel = Utils.tooltip(new Button("Cancel"), "Cancel (Esc)");
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(register);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setPadding(true);
            layout.setSpacing(false);
            layout.add(type, controllerId, name, description);
            add(layout);
            open();
        }

        private void addCreateClickListener(final Button register, final HawkbitMgmtClient hawkbitClient) {
            register.addClickListener(e -> {
                final MgmtTargetRequestBody request = new MgmtTargetRequestBody()
                        .setControllerId(controllerId.getValue())
                        .setName(name.getValue())
                        .setDescription(description.getValue());
                if (!ObjectUtils.isEmpty(type.getValue())) {
                    request.setTargetType(type.getValue().getId());
                }
                hawkbitClient.getTargetRestApi().createTargets(
                                List.of(request))
                        .getBody()
                        .stream()
                        .findFirst()
                        .orElseThrow()
                        .getControllerId();
                close();
            });
        }
    }

    private static class AssignDialog extends Utils.BaseDialog<Void> {

        private final Select<MgmtDistributionSet> distributionSet;
        private final Select<MgmtActionType> actionType;
        private final DateTimePicker forceTime = new DateTimePicker("Force Time");
        private final Button assign = new Button("Assign");

        private AssignDialog(final HawkbitMgmtClient hawkbitClient, Set<MgmtTarget> selectedTargets) {
            super("Assign Distribution Set");

            distributionSet = new Select<>(
                    "Distribution Set",
                    this::readyToAssign,
                    Optional.ofNullable(
                                    hawkbitClient.getDistributionSetRestApi()
                                            .getDistributionSets(0, 30, Constants.NAME_ASC, null)
                                            .getBody())
                            .map(body -> body.getContent().toArray(new MgmtDistributionSet[0]))
                            .orElseGet(() -> new MgmtDistributionSet[0])
            );
            distributionSet.setRequiredIndicatorVisible(true);
            distributionSet.setItemLabelGenerator(distributionSetO ->
                    distributionSetO.getName() + ":" + distributionSetO.getVersion());
            distributionSet.setWidthFull();

            actionType = Utils.actionTypeControls(forceTime);

            assign.setEnabled(false);
            assign.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addAssignClickListener(hawkbitClient, selectedTargets);
            final Button cancel = Utils.tooltip(new Button("Cancel"), "Cancel (Esc)");
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(assign);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setSpacing(false);
            layout.add(distributionSet, actionType);
            add(layout);
            open();
        }

        private void readyToAssign(final Object v) {
            final boolean assignEnabled = !distributionSet.isEmpty();
            if (assign.isEnabled() != assignEnabled) {
                assign.setEnabled(assignEnabled);
            }
        }

        private void addAssignClickListener(final HawkbitMgmtClient hawkbitClient, final Set<MgmtTarget> selectedTargets) {
            assign.addClickListener(e -> {
                close();
                List<MgmtTargetAssignmentRequestBody> requests = new LinkedList<MgmtTargetAssignmentRequestBody>();

                for (final MgmtTarget target : selectedTargets) {

                    MgmtTargetAssignmentRequestBody request = new MgmtTargetAssignmentRequestBody(target.getControllerId());

                    request.setType(actionType.getValue());
                    if (actionType.getValue() == MgmtActionType.TIMEFORCED) {
                        request.setForcetime(
                                forceTime.isEmpty() ?
                                        System.currentTimeMillis() :
                                        forceTime.getValue().toEpochSecond(ZoneOffset.UTC) * 1000);
                    }

                    requests.add(request);
                }

                hawkbitClient.getDistributionSetRestApi().createAssignedTarget(distributionSet.getValue().getId(), requests, null).getBody();
            });
        }
    }

    private static class CreateTagDialog extends Utils.BaseDialog<Void> {

        private final TextField name;
        private final TextArea description = new TextArea(Constants.DESCRIPTION);

        private CreateTagDialog(final HawkbitMgmtClient hawkbitClient, Runnable onSuccess) {
            super("Create Tag");

            FormLayout formLayout = new FormLayout();
            formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
            final Button create = Utils.tooltip(new Button("Create"), "Create (Enter)");
            final Button cancel = Utils.tooltip(new Button("Cancel"), "Cancel (Esc)");

            Input colorInput = new Input();
            colorInput.setType("color");
            name = Utils.textField("Tag Name", e -> create.setEnabled(!e.getHasValue().isEmpty()));
            formLayout.add(name);
            formLayout.add(description);
            formLayout.addFormItem(colorInput, "Color Selection");
            add(formLayout);

            create.setEnabled(false);
            create.addClickShortcut(Key.ENTER);
            create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            create.addClickListener(e -> {
                hawkbitClient.getTargetTagRestApi().createTargetTags(List.of(
                        new MgmtTagRequestBodyPut()
                                .setName(name.getValue())
                                .setDescription(description.getValue())
                                .setColour(colorInput.getValue())
                ));
                onSuccess.run();
                close();
            });

            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);

            getFooter().add(cancel);
            getFooter().add(create);
            open();
        }
    }

    private static class TargetStatusCell extends HorizontalLayout {

        private TargetStatusCell(MgmtTarget target) {
            MgmtPollStatus pollStatus = target.getPollStatus();
            String targetUpdateStatus = Optional.ofNullable(target.getUpdateStatus()).orElse("unknown");
            add(pollStatusIconMapper(pollStatus), targetUpdateStatusMapper(targetUpdateStatus));
            setWidth(50, Unit.PIXELS);
        }

        private Icon targetUpdateStatusMapper(String targetUpdateStatus) {
            VaadinIcon icon = switch (targetUpdateStatus) {
                case "error" -> VaadinIcon.EXCLAMATION_CIRCLE;
                case "in_sync" -> VaadinIcon.CHECK_CIRCLE;
                case "pending" -> VaadinIcon.ADJUST;
                case "registered" -> VaadinIcon.DOT_CIRCLE;
                default -> VaadinIcon.QUESTION_CIRCLE;
            };

            String color = switch (targetUpdateStatus) {
                case "error" -> "red";
                case "in_sync" -> "green";
                case "pending" -> "orange";
                case "registered" -> "lightblue";
                default -> "blue";
            };

            Icon statusIcon = Utils.tooltip(icon.create(), targetUpdateStatus);
            statusIcon.setColor(color);
            statusIcon.addClassNames(LumoUtility.IconSize.SMALL);
            return statusIcon;
        }

        private Icon pollStatusIconMapper(MgmtPollStatus pollStatus) {
            Icon pollIcon;
            if (pollStatus == null) {
                pollIcon = Utils.tooltip(VaadinIcon.QUESTION_CIRCLE.create(), "No Poll Status");
            } else if (pollStatus.isOverdue()) {
                pollIcon = Utils.tooltip(VaadinIcon.EXCLAMATION_CIRCLE.create(), "Overdue");
            } else {
                pollIcon = Utils.tooltip(VaadinIcon.CLOCK.create(), "In Time");
            }
            pollIcon.addClassNames(LumoUtility.IconSize.SMALL);
            return pollIcon;
        }
    }
}
