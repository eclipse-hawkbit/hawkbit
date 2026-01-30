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
import java.util.ArrayList;
import java.util.Collections;
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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionStatus;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAttributes;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.component.TargetActionsHistory;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.LinkedTextArea;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;

@PageTitle("Targets")
@Route(value = "targets", layout = MainLayout.class)
@RolesAllowed({ "TARGET_READ" })
@Uses(Icon.class)
public class TargetView extends TableView<TargetView.TargetWithDs, String> {

    public static final String STATUS = "Status";
    public static final String UPDATE = "Sync";
    public static final String CONTROLLER_ID = "Controller Id";
    public static final String FILTER = "Filter";
    public static final String TAG = "Tag";

    public static final String GREEN = "green";
    public static final String RED = "red";
    public static final String ORANGE = "orange";
    public static final String GRAY = "gray";
    public static final String BROWN = "brown";
    public static final String TEAL = "teal";
    public static final String PURPLE = "purple";
    public static final String CORAL = "coral";
    public static final String BLACK = "black";

    public TargetView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new SimpleFilter(hawkbitClient), new RawFilter(hawkbitClient),
                new SelectionGrid.EntityRepresentation<>(TargetWithDs.class, TargetWithDs::getControllerId) {

                    @Override
                    protected void addColumns(final Grid<TargetWithDs> grid) {
                        grid.addColumn(new ComponentRenderer<>(TargetStatusCell::new))
                                .setHeader(STATUS)
                                .setAutoWidth(true)
                                .setFlexGrow(0).setKey("lastControllerRequestAt").setSortable(true);
                        grid.addColumn(new ComponentRenderer<>(TargetUpdateStatusCell::new))
                                .setHeader(UPDATE)
                                .setAutoWidth(true)
                                .setFlexGrow(0).setKey("updateStatus").setSortable(true);
                        grid.addColumn(MgmtTarget::getControllerId).setHeader(CONTROLLER_ID).setAutoWidth(true).setKey("id").setSortable(true);
                        grid.addColumn(Utils.localDateTimeRenderer(MgmtTarget::getLastModifiedAt)).setHeader(LAST_MODIFIED_AT).setAutoWidth(
                                true).setKey("lastModifiedAt").setSortable(true);
                        grid.addColumn(MgmtTarget::getName).setHeader(Constants.NAME).setAutoWidth(true).setKey("name").setSortable(true);
                        grid.addColumn(MgmtTarget::getTargetTypeName).setHeader(Constants.TYPE).setAutoWidth(true).setKey("targetType")
                                .setSortable(true);
                        grid.addColumn(TargetWithDs::getDsName).setHeader(Constants.DISTRIBUTION_SET).setAutoWidth(true);
                        grid.addColumn(TargetWithDs::getDsVersion).setHeader(Constants.VERSION).setAutoWidth(true).setKey("installedds")
                                .setSortable(true);
                    }
                },
                (query, filter) -> hawkbitClient.getTargetRestApi()
                        .getTargets(filter, query.getOffset(), query.getPageSize(), Utils.getSortParam(query.getSortOrders(),
                                "lastModifiedAt:desc"))
                        .getBody()
                        .getContent()
                        .stream().map(m -> TargetWithDs.from(hawkbitClient, m)),
                source -> new RegisterDialog(hawkbitClient).result(),
                selectionGrid -> {
                    selectionGrid.getSelectedItems()
                            .forEach(toDelete -> hawkbitClient.getTargetRestApi().deleteTarget(toDelete.getControllerId()));
                    return CompletableFuture.completedFuture(null);
                },
                target -> {
                    final TargetDetailedView targetDetailedView = new TargetDetailedView(hawkbitClient);
                    targetDetailedView.setItem(target);
                    return targetDetailedView;
                }
        );

        final Function<SelectionGrid<TargetWithDs, String>, CompletionStage<Void>> assignHandler = source -> new AssignDialog(
                hawkbitClient, source.getSelectedItems()).result();

        final Button assignBtn = Utils.tooltip(new Button(VaadinIcon.LINK.create()), "Assign");
        assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        assignBtn.addClickListener(e -> assignHandler.apply(selectionGrid).thenAccept(v -> selectionGrid.refreshGrid(false)));
        controlsLayout.addComponentAtIndex(0, assignBtn);
    }

    private static class SimpleFilter implements Filter.Rsql {

        private final HawkbitMgmtClient hawkbitClient;

        private final TextField textFilter;
        private final CheckboxGroup<MgmtTargetType> type;
        private final CheckboxGroup<MgmtTag> tag;

        private SimpleFilter(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;

            textFilter = Utils.textField(FILTER);
            textFilter.setPlaceholder("<controller id/name filter>");
            type = new CheckboxGroup<>(Constants.TYPE);
            type.setItemLabelGenerator(MgmtTargetType::getName);
            tag = new CheckboxGroup<>(TAG);
            tag.setItemLabelGenerator(MgmtTag::getName);
        }

        @Override
        public List<Component> components() {
            final List<Component> components = new LinkedList<>();
            components.add(textFilter);
            type.setItems(hawkbitClient.getTargetTypeRestApi().getTargetTypes(null, 0, 20, Constants.NAME_ASC).getBody().getContent());
            if (!((ListDataProvider) type.getDataProvider()).getItems().isEmpty()) {
                components.add(type);
            }
            tag.setItems(hawkbitClient.getTargetTagRestApi().getTargetTags(null, 0, 20, Constants.NAME_ASC).getBody().getContent());
            if (!((ListDataProvider) tag.getDataProvider()).getItems().isEmpty()) {
                components.add(tag);
            }
            return components;
        }

        @Override
        public String filter() {
            return Filter.filter(
                    Map.of(
                            List.of("controllerid", "name"), textFilter.getOptionalValue().map(s -> "*" + s + "*"),
                            "targettype.name", type.getSelectedItems().stream().map(MgmtTargetType::getName)
                                    .toList(),
                            "tag", tag.getSelectedItems().stream().map(t -> "\"" + t.getName() + "\"").toList()));
        }
    }

    @SuppressWarnings({ "java:S1171", "java:S3599" })
    private static class RawFilter implements Filter.Rsql, Filter.RsqlRw {

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
            savedFilters.setItemLabelGenerator(query -> Optional.ofNullable(query).map(MgmtTargetFilterQuery::getName).orElse(
                    "<select saved filter>"));
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
            return Optional
                    .ofNullable(
                            hawkbitClient.getTargetFilterQueryRestApi()
                                    .getFilters(null, 0, 30, null, null)
                                    .getBody()
                                    .getContent())
                    .orElse(Collections.emptyList());
        }

        private ComponentEventListener<ClickEvent<Button>> createBtnListener(HawkbitMgmtClient hawkbitClient) {
            return e -> new Utils.BaseDialog<Void>("Create New Filter") {

                {
                    final Button finishBtn = Utils.tooltip(new Button("Save"), "Save (Enter)");
                    final TextField name = Utils.textField(Constants.NAME, e -> finishBtn.setEnabled(!e.getHasValue().isEmpty()));
                    name.focus();
                    finishBtn.addClickShortcut(Key.ENTER);
                    finishBtn.setEnabled(false);
                    finishBtn.addClickListener(e -> {
                        final MgmtTargetFilterQueryRequestBody createRequest = new MgmtTargetFilterQueryRequestBody();
                        createRequest.setName(name.getValue());
                        createRequest.setQuery(textFilter.getValue());
                        hawkbitClient.getTargetFilterQueryRestApi().createFilter(createRequest);
                        savedFilters.setItems(listFilters(hawkbitClient));
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
                if (selected == null) {
                    return;
                }

                new Utils.BaseDialog<Void>("Update Filter") {

                    {
                        final Button finishBtn = Utils.tooltip(new Button("Update"), "Update (Enter)");
                        finishBtn.setEnabled(false);

                        final TextField name = Utils.textField(Constants.NAME, e -> finishBtn.setEnabled(!e.getHasValue().isEmpty()));
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

        @Override
        public void setFilter(String filter) {
            textFilter.setValue(filter);
        }
    }

    protected static class TargetDetailedView extends VerticalLayout {

        private final Span targetId;
        private final TargetDetails targetDetails;
        private final TargetAssignedInstalled targetAssignedInstalled;
        private final TargetTags targetTags;
        private final TargetMetadata targetMetadata;
        private final TargetActionsHistoryLayout targetActionsHistoryLayout;

        private TargetDetailedView(final HawkbitMgmtClient hawkbitClient) {
            final TabSheet tabSheet = new TabSheet();
            targetId = new Span();
            targetDetails = new TargetDetails(hawkbitClient);
            targetAssignedInstalled = new TargetAssignedInstalled(hawkbitClient);
            targetTags = new TargetTags(hawkbitClient);
            targetMetadata = new TargetMetadata(hawkbitClient);
            targetActionsHistoryLayout = new TargetActionsHistoryLayout(hawkbitClient);
            setWidthFull();

            add(targetId);
            tabSheet.add("Details", targetDetails);
            tabSheet.add("Assigned / Installed", targetAssignedInstalled);
            tabSheet.add("Tags", targetTags);
            tabSheet.add("Metadata", targetMetadata);
            tabSheet.add("Action History", targetActionsHistoryLayout);
            add(tabSheet);
        }

        private void setItem(final MgmtTarget target) {
            this.targetId.setText(target.getControllerId());
            this.targetDetails.setItem(target);
            this.targetAssignedInstalled.setItem(target);
            this.targetTags.setItem(target);
            this.targetMetadata.setItem(target);
            this.targetActionsHistoryLayout.setItem(target);
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
        private final TextField lastPoll = Utils.textField(Constants.LAST_POLL);
        private final TextField group = Utils.textField(Constants.GROUP);
        private final TextField targetAddress = Utils.textField(Constants.ADDRESS);
        private final TextArea targetAttributes = new TextArea(Constants.ATTRIBUTES);
        private transient MgmtTarget target;

        private TargetDetails(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;
            description.setMinLength(2);
            Stream.of(
                    description,
                    createdBy, createdAt,
                    lastModifiedBy, lastModifiedAt,
                    securityToken, lastPoll, group, targetAddress, targetAttributes
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
        protected void onAttach(final AttachEvent attachEvent) {
            description.setValue(target.getDescription() == null ? "N/A" : target.getDescription());
            createdBy.setValue(target.getCreatedBy());
            createdAt.setValue(Utils.localDateTimeFromTs(target.getCreatedAt()));
            lastModifiedBy.setValue(target.getLastModifiedBy());
            lastModifiedAt.setValue(Utils.localDateTimeFromTs(target.getLastModifiedAt()));
            securityToken.setValue(Objects.requireNonNullElse(target.getSecurityToken(), ""));
            group.setValue(target.getGroup() != null ? target.getGroup() : "");
            targetAddress.setValue(target.getAddress() != null ? target.getAddress() : "");

            final MgmtPollStatus pollStatus = target.getPollStatus();
            lastPoll.setValue(pollStatus == null ? NOT_AVAILABLE_NULL : Utils.localDateTimeFromTs(pollStatus.getLastRequestAt()));
            final ResponseEntity<MgmtTargetAttributes> response = hawkbitClient.getTargetRestApi().getAttributes(target.getControllerId());
            if (response.getStatusCode().is2xxSuccessful()) {
                targetAttributes.setValue(Objects.requireNonNullElse(response.getBody(), Collections.emptyMap()).entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining("\n")));
            } else {
                targetAttributes.setValue("Error occurred fetching attributes from server: " + response.getStatusCode());
            }
        }
    }

    private static class TargetAssignedInstalled extends FormLayout {

        private final transient HawkbitMgmtClient hawkbitClient;
        private final LinkedTextArea assigned = new LinkedTextArea("Assigned Distribution Set", "/distribution_sets?");
        private final LinkedTextArea installed = new LinkedTextArea("Installed Distribution Set", "/distribution_sets?");
        private transient MgmtTarget target;

        private TargetAssignedInstalled(HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;
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
            updateDistributionSetInfo(() -> hawkbitClient.getTargetRestApi().getInstalledDistributionSet(target.getControllerId()), installed);
            updateDistributionSetInfo(() -> hawkbitClient.getTargetRestApi().getAssignedDistributionSet(target.getControllerId()), assigned);
        }

        private void updateDistributionSetInfo(Supplier<ResponseEntity<MgmtDistributionSet>> supplier, LinkedTextArea textArea) {
            Optional.ofNullable(supplier.get())
                    .map(ResponseEntity<MgmtDistributionSet>::getBody)
                    .ifPresentOrElse(value -> {
                        final String description = """
                                Name:  %s
                                Version: %s
                                %s
                                """.replace("\n", System.lineSeparator());
                        textArea.setValueWithLink(description.formatted(
                                value.getName(),
                                value.getVersion(),
                                value.getModules().stream().map(module -> module.getTypeName() + ": " + module.getVersion())
                                        .collect(Collectors.joining(System.lineSeparator()))
                        ), "q=id%3D%3D" + value.getId().toString());
                    },
                            () -> textArea.setValueWithLink("", null));
        }
    }

    private static class TargetTags extends VerticalLayout {

        private final transient HawkbitMgmtClient hawkbitClient;
        private final ComboBox<MgmtTag> tagSelector = new ComboBox<>(TAG);
        private final HorizontalLayout tagsArea = new HorizontalLayout();
        private Registration changeListener;
        private transient MgmtTarget target;

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
            createTagButton.addClickListener(event -> new CreateTagDialog(hawkbitClient, () -> tagSelector.setItems(fetchAvailableTags()))
                    .result());

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
                        hawkbitClient.getTargetTagRestApi().getTargetTags(null, offset, 50, Constants.NAME_ASC).getBody())
                        .map(PagedList::getContent)
                        .orElse(Collections.emptyList());
                tags.addAll(page);
                fetched = page.size();
                offset += fetched;
            } while (fetched > 0);
            return tags;
        }
    }

    private static class TargetMetadata extends VerticalLayout {

        public static final String KEY = "Key";
        public static final String VALUE = "Value";

        private final transient HawkbitMgmtClient hawkbitClient;
        private final Grid<MgmtMetadata> metadataArea = new Grid<>();
        private transient MgmtTarget target;

        private TargetMetadata(HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;
            setWidthFull();
            setPadding(false);
            setSpacing(true);
            setAlignItems(FlexComponent.Alignment.STRETCH);
            setJustifyContentMode(JustifyContentMode.CENTER);

            metadataArea.setEmptyStateText("No metadata found");
            metadataArea.addColumn(MgmtMetadata::getKey).setHeader(KEY).setAutoWidth(true);
            metadataArea.addColumn(MgmtMetadata::getValue).setHeader(VALUE).setAutoWidth(true);
            metadataArea.addComponentColumn(metadata -> Utils.deleteButton("Delete metadata", () -> {
                hawkbitClient.getTargetRestApi().deleteMetadata(target.getControllerId(), metadata.getKey());
                refreshMetadatas();
            })).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
            metadataArea.setWidthFull();
            add(metadataArea);

            final Button addBtn = new Button("Add");
            addBtn.addClickListener(e -> new AddMetadataDialog(hawkbitClient, target, this::refreshMetadatas).result());
            addBtn.setEnabled(true);
            final HorizontalLayout tools = new HorizontalLayout(); 
            tools.setWidthFull();
            tools.add(addBtn);
            add(tools);
        }

        private void setItem(final MgmtTarget target) {
            this.target = target;
        }

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            refreshMetadatas();
        }

        private void refreshMetadatas() {
            metadataArea.setItems(
                    Optional.ofNullable(
                            hawkbitClient.getTargetRestApi().getMetadata(target.getControllerId()).getBody())
                            .map(PagedList::getContent)
                            .orElse(Collections.emptyList()));
        }
    }

    public static class TargetActionsHistoryLayout extends VerticalLayout {

        private final TargetActionsHistory targetActionsHistory;

        public TargetActionsHistoryLayout(HawkbitMgmtClient hawkbitMgmtClient) {
            ActionStepsGrid actionStepsGrid = new ActionStepsGrid(hawkbitMgmtClient);
            targetActionsHistory = new TargetActionsHistory(hawkbitMgmtClient, actionStepsGrid);
            add(targetActionsHistory);
            add(actionStepsGrid);
        }

        public void setItem(MgmtTarget target) {
            targetActionsHistory.setItem(target);
        }

        public static class ActionStepsGrid extends Grid<ActionStepsGrid.ActionStepEntry> {

            private final transient HawkbitMgmtClient hawkbitClient;
            private transient MgmtTarget target;
            private transient Long actionId;

            private ActionStepsGrid(final HawkbitMgmtClient hawkbitClient) {

                this.hawkbitClient = hawkbitClient;
                setWidthFull();
                addColumn(new ComponentRenderer<>(ActionStepEntry::getStatusIcon)).setHeader(STATUS).setAutoWidth(true)
                        .setFlexGrow(0);
                addColumn(Utils.localDateTimeRenderer(ActionStepEntry::getLastModifiedAt)).setHeader("Time")
                        .setAutoWidth(true).setFlexGrow(0).setComparator(ActionStepEntry::getLastModifiedAt);
                addColumn(new ComponentRenderer<>(ActionStepEntry::getMessage)).setHeader("Message").setAutoWidth(true).setFlexGrow(0);
            }

            private List<ActionStepEntry> fetchActionSteps() {
                if (actionId == null) {
                    return new ArrayList<>();
                }
                return hawkbitClient.getTargetRestApi()
                        .getActionStatusList(target.getControllerId(), actionId, 0, 30, null).getBody().getContent()
                        .stream().map(ActionStepEntry::new)
                        .toList();
            }

            @Override
            protected void onAttach(AttachEvent attachEvent) {
                setItems(fetchActionSteps());
            }

            public void setActionId(Long id) {
                actionId = id;
                setItems(fetchActionSteps());
            }

            public void setTarget(MgmtTarget target) {
                this.target = target;
                actionId = null;
            }

            private static class ActionStepEntry extends Object {

                final MgmtActionStatus status;

                public ActionStepEntry(final MgmtActionStatus status) {
                    this.status = status;
                }

                public Long getLastModifiedAt() {
                    return status.getReportedAt();
                }

                public Component getStatusIcon() {
                    final HorizontalLayout layout = new HorizontalLayout();
                    final Icon icon;

                    switch (status.getType()) {
                        case FINISHED -> icon = Utils.iconColored(VaadinIcon.CHECK_CIRCLE, "Finished", GREEN);
                        case ERROR -> icon = Utils.iconColored(VaadinIcon.CLOSE_CIRCLE, "Error", RED);
                        case WARNING -> icon = Utils.iconColored(VaadinIcon.WARNING, "Warning", ORANGE);
                        case RUNNING -> icon = Utils.iconColored(VaadinIcon.ADJUST, "Running", GREEN);
                        case RETRIEVED -> icon = Utils.iconColored(VaadinIcon.CIRCLE_THIN, "Retrieved", GREEN);
                        case CANCELED -> icon = Utils.iconColored(VaadinIcon.CLOSE_CIRCLE_O, "Canceled", GRAY);
                        case CANCELING -> icon = Utils.iconColored(VaadinIcon.CLOSE_CIRCLE, "Cancelling", BROWN);
                        case DOWNLOAD -> icon = Utils.iconColored(VaadinIcon.CLOUD_DOWNLOAD_O, "Download", TEAL);
                        case DOWNLOADED -> icon = Utils.iconColored(VaadinIcon.CLOUD_DOWNLOAD, "Downloaded", PURPLE);
                        case WAIT_FOR_CONFIRMATION -> icon = Utils.iconColored(VaadinIcon.QUESTION_CIRCLE, "Wait for confirmation", CORAL);
                        default -> icon = Utils.iconColored(VaadinIcon.CIRCLE_THIN, status.getType().getName().toLowerCase(), BLACK);
                    }

                    icon.addClassNames(LumoUtility.IconSize.SMALL);
                    layout.add(icon);
                    layout.setWidth(50, Unit.PIXELS);
                    layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
                    return layout;
                }

                public VerticalLayout getMessage() {
                    return new VerticalLayout(status.getMessages().stream().map(Span::new).toArray(Span[]::new));
                }
            }
        }
    }

    private static class RegisterDialog extends Utils.BaseDialog<Void> {

        private final Select<MgmtTargetType> type;
        private final TextField controllerId;
        private final TextField name;
        private final TextArea description;
        private final TextField group;

        private RegisterDialog(final HawkbitMgmtClient hawkbitClient) {
            super("Register Target");

            final Button register = Utils.tooltip(new Button("Register"), "Register (Enter)");
            type = new Select<>(
                    "Type",
                    e -> {},
                    hawkbitClient.getTargetTypeRestApi()
                            .getTargetTypes(null, 0, 30, Constants.NAME_ASC)
                            .getBody()
                            .getContent()
                            .toArray(new MgmtTargetType[0])
            );
            type.setWidthFull();
            type.setEmptySelectionAllowed(true);
            type.setItemLabelGenerator(item -> item == null ? "" : item.getName());
            controllerId = Utils.textField(CONTROLLER_ID, e -> register.setEnabled(!e.getHasValue().isEmpty()));
            controllerId.focus();
            name = Utils.textField(Constants.NAME);
            name.setWidthFull();
            description = new TextArea(Constants.DESCRIPTION);
            description.setMinLength(2);
            description.setWidthFull();
            group = Utils.textField(Constants.GROUP);
            group.setWidthFull();

            addCreateClickListener(register, hawkbitClient);
            register.setEnabled(false);
            register.addClickShortcut(Key.ENTER);
            register.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(register);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setPadding(true);
            layout.setSpacing(false);
            layout.add(type, controllerId, name, description, group);
            add(layout);
            open();
        }

        private void addCreateClickListener(final Button register, final HawkbitMgmtClient hawkbitClient) {
            register.addClickListener(e -> {
                final MgmtTargetRequestBody request = new MgmtTargetRequestBody()
                        .setControllerId(controllerId.getValue())
                        .setName(name.getValue())
                        .setDescription(description.getValue())
                        .setGroup(group.getValue());
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

        private final ComboBox<MgmtDistributionSet> distributionSet;
        private final Select<MgmtActionType> actionType;
        private final DateTimePicker forceTime = new DateTimePicker("Force Time");
        private final Button assign = new Button("Assign");

        private AssignDialog(final HawkbitMgmtClient hawkbitClient, Set<TargetWithDs> selectedTargets) {
            super("Assign Distribution Set");

            distributionSet = Utils.nameComboBox(
                    "Distribution Set",
                    this::readyToAssign,
                    query -> hawkbitClient.getDistributionSetRestApi()
                            .getDistributionSets(query.getFilter().orElse(null), query.getOffset(), query.getPageSize(), Constants.NAME_ASC)
                            .getBody().getContent().stream());
            distributionSet.setRequiredIndicatorVisible(true);
            distributionSet.setItemLabelGenerator(distributionSetO -> distributionSetO.getName() + ":" + distributionSetO.getVersion());
            distributionSet.setWidthFull();

            actionType = Utils.actionTypeControls(MgmtActionType.FORCED, forceTime);

            assign.setEnabled(false);
            assign.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addAssignClickListener(hawkbitClient, selectedTargets);
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
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

        private void addAssignClickListener(final HawkbitMgmtClient hawkbitClient, final Set<TargetWithDs> selectedTargets) {
            assign.addClickListener(e -> {
                close();

                final List<MgmtTargetAssignmentRequestBody> requests = new LinkedList<>();
                for (final MgmtTarget target : selectedTargets) {
                    final MgmtTargetAssignmentRequestBody request = new MgmtTargetAssignmentRequestBody(target.getControllerId());

                    request.setType(actionType.getValue());
                    if (actionType.getValue() == MgmtActionType.TIMEFORCED) {
                        request.setForcetime(
                                forceTime.isEmpty() ? System.currentTimeMillis() : forceTime.getValue().toEpochSecond(ZoneOffset.UTC) * 1000);
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

            final FormLayout formLayout = new FormLayout();
            formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
            final Button create = Utils.tooltip(new Button("Create"), "Create (Enter)");
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);

            final Input colorInput = new Input();
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
                                .setColour(colorInput.getValue())));
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

    private static class AddMetadataDialog extends Utils.BaseDialog<Void> {

        private final TextField key;
        private final TextField value;

        private AddMetadataDialog(final HawkbitMgmtClient hawkbitClient, MgmtTarget target, Runnable onSuccess) {
            super("Add Metadata");

            final FormLayout formLayout = new FormLayout();
            formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
            final Button add = Utils.tooltip(new Button("Add"), "Add (Enter)");
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);

            key = Utils.textField(TargetMetadata.KEY, e -> add.setEnabled(!e.getHasValue().isEmpty()));
            formLayout.add(key);
            value = Utils.textField(TargetMetadata.VALUE);
            formLayout.add(value);
            add(formLayout);

            add.setEnabled(false);
            add.addClickShortcut(Key.ENTER);
            add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add.addClickListener(e -> {
                hawkbitClient.getTargetRestApi().createMetadata(target.getControllerId(), List.of(
                        new MgmtMetadata()
                                .setKey(key.getValue())
                                .setValue(value.getValue())));
                onSuccess.run();
                close();
            });

            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);

            getFooter().add(cancel);
            getFooter().add(add);
            open();
        }
    }

    private static class TargetStatusCell extends HorizontalLayout {

        private TargetStatusCell(final MgmtTarget target) {
            final MgmtPollStatus pollStatus = target.getPollStatus();
            add(pollStatusIconMapper(pollStatus));
            setWidth(25, Unit.PIXELS);
        }

        private Icon pollStatusIconMapper(MgmtPollStatus pollStatus) {
            final Icon pollIcon;
            if (pollStatus == null) {
                pollIcon = Utils.tooltip(VaadinIcon.QUESTION_CIRCLE.create(), "No Poll Status");
            } else if (pollStatus.isOverdue()) {
                pollIcon = Utils.tooltip(VaadinIcon.EXCLAMATION_CIRCLE.create(), "Overdue " + Utils.durationFromMillis(pollStatus
                        .getLastRequestAt()));
            } else {
                pollIcon = Utils.tooltip(VaadinIcon.CLOCK.create(), "In Time " + Utils.durationFromMillis(pollStatus.getLastRequestAt()));
            }
            pollIcon.addClassNames(LumoUtility.IconSize.SMALL);
            return pollIcon;
        }
    }

    private static class TargetUpdateStatusCell extends HorizontalLayout {

        private TargetUpdateStatusCell(final MgmtTarget target) {
            final String targetUpdateStatus = Optional.ofNullable(target.getUpdateStatus()).orElse("unknown");
            add(targetUpdateStatusMapper(targetUpdateStatus));
            setWidth(25, Unit.PIXELS);
        }

        private Icon targetUpdateStatusMapper(final String targetUpdateStatus) {
            final VaadinIcon icon = switch (targetUpdateStatus) {
                case "error" -> VaadinIcon.EXCLAMATION_CIRCLE;
                case "in_sync" -> VaadinIcon.CHECK_CIRCLE;
                case "pending" -> VaadinIcon.ADJUST;
                case "registered" -> VaadinIcon.DOT_CIRCLE;
                default -> VaadinIcon.QUESTION_CIRCLE;
            };

            final String color = switch (targetUpdateStatus) {
                case "error" -> RED;
                case "in_sync" -> GREEN;
                case "pending" -> ORANGE;
                case "registered" -> "lightblue";
                default -> "blue";
            };

            final Icon statusIcon = Utils.tooltip(icon.create(), targetUpdateStatus.replace("_", " "));
            statusIcon.setColor(color);
            statusIcon.addClassNames(LumoUtility.IconSize.SMALL);
            return statusIcon;
        }
    }

    // todo change /targets api to reduce api calls ?
    @EqualsAndHashCode(callSuper = true)
    public static class TargetWithDs extends MgmtTarget {

        TargetWithDs() {
            super();
        }

        Optional<MgmtDistributionSet> ds;
        static ObjectMapper objectMapper = new ObjectMapper();

        public static TargetWithDs from(final HawkbitMgmtClient hawkbitClient, MgmtTarget target) {
            TargetWithDs targetWithDs = objectMapper.convertValue(target, TargetWithDs.class);

            targetWithDs.ds = Optional.ofNullable(hawkbitClient.getTargetRestApi().getInstalledDistributionSet(targetWithDs
                    .getControllerId())
                    .getBody());
            return targetWithDs;
        }

        public String getDsVersion() {
            return ds.map(MgmtDistributionSet::getVersion).orElse("");
        }

        public String getDsName() {
            return ds.map(MgmtDistributionSet::getName).orElse("");
        }
    }
}