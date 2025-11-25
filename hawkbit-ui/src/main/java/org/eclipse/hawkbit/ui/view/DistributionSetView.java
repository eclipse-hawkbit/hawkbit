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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssignment;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;

@PageTitle("Distribution Sets")
@Route(value = "distribution_sets", layout = MainLayout.class)
@RolesAllowed({ "DISTRIBUTION_SET_READ" })
@Uses(Icon.class)
public class DistributionSetView extends TableView<MgmtDistributionSet, Long> {

    public DistributionSetView(final HawkbitMgmtClient hawkbitClient) {
        super(
                new DistributionSetFilter(hawkbitClient),
                new DistributionSetRawFilter(),
                new SelectionGrid.EntityRepresentation<>(MgmtDistributionSet.class, MgmtDistributionSet::getId) {

                    private final DistributionSetDetails details = new DistributionSetDetails(hawkbitClient);

                    @Override
                    protected void addColumns(Grid<MgmtDistributionSet> grid) {
                        var createdAtCol = grid.addColumn(Utils.localDateTimeRenderer(MgmtDistributionSet::getCreatedAt)).setHeader(
                                Constants.CREATED_AT).setAutoWidth(true).setKey("createdAt").setSortable(true);
                        grid.addColumn(MgmtDistributionSet::getName).setHeader(Constants.NAME).setAutoWidth(true).setKey("name").setSortable(
                                true);
                        grid.addColumn(MgmtDistributionSet::getVersion).setHeader(Constants.VERSION).setAutoWidth(true).setKey("version")
                                .setSortable(true);
                        grid.addColumn(MgmtDistributionSet::getTypeName).setHeader(Constants.TYPE).setAutoWidth(true).setKey("typename")
                                .setSortable(true);
                        grid.sort(List.of(new GridSortOrder<>(createdAtCol, SortDirection.DESCENDING)));

                        grid.setItemDetailsRenderer(new ComponentRenderer<>(
                                () -> details, DistributionSetDetails::setItem));
                    }
                },
                (query, rsqlFilter) -> Optional.ofNullable(
                        hawkbitClient.getDistributionSetRestApi()
                                .getDistributionSets(rsqlFilter, query.getOffset(), query.getPageSize(), Utils.getSortParam(query
                                        .getSortOrders()))
                                .getBody())
                        .stream().flatMap(body -> body.getContent().stream()),
                e -> new CreateDialog(hawkbitClient).result(),
                selectionGrid -> {
                    selectionGrid.getSelectedItems().forEach(
                            distributionSet -> hawkbitClient.getDistributionSetRestApi()
                                    .deleteDistributionSet(distributionSet.getId()));
                    return CompletableFuture.completedFuture(null);
                }, null);
    }

    private static SelectionGrid<MgmtSoftwareModule, Long> selectSoftwareModuleGrid() {
        return new SelectionGrid<>(
                new SelectionGrid.EntityRepresentation<>(
                        MgmtSoftwareModule.class, MgmtSoftwareModule::getId) {

                    @Override
                    protected void addColumns(Grid<MgmtSoftwareModule> grid) {
                        grid.addColumn(MgmtSoftwareModule::getId).setHeader(Constants.ID).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getName).setHeader(Constants.NAME).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getVersion).setHeader(Constants.VERSION).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getTypeName).setHeader(Constants.TYPE).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getVendor).setHeader(Constants.VENDOR).setAutoWidth(true);
                    }
                });
    }

    private static class DistributionSetRawFilter implements Filter.Rsql, Filter.RsqlRw {

        private final TextField name = Utils.textField("Name");

        private DistributionSetRawFilter() {
            name.setPlaceholder("<rsql filter>");
        }

        @Override
        public List<Component> components() {
            return List.of(name);
        }

        @Override
        public String filter() {
            return name.getOptionalValue().orElse(null);
        }

        @Override
        public void setFilter(String filter) {
            name.setValue(filter);
        }
    }

    private static class DistributionSetFilter implements Filter.Rsql {

        private final TextField textFilter = Utils.textField("Filter");
        private final CheckboxGroup<MgmtDistributionSetType> type = new CheckboxGroup<>("Type");
        private final CheckboxGroup<MgmtTag> tag = new CheckboxGroup<>("Tag");

        private DistributionSetFilter(final HawkbitMgmtClient hawkbitClient) {
            textFilter.setPlaceholder("<name/version filter>");
            type.setItemLabelGenerator(MgmtDistributionSetType::getName);
            type.setItems(Optional.ofNullable(
                    hawkbitClient.getDistributionSetTypeRestApi()
                            .getDistributionSetTypes(null, 0, 20, Constants.NAME_ASC)
                            .getBody())
                    .map(PagedList::getContent)
                    .orElseGet(Collections::emptyList));
            tag.setItemLabelGenerator(MgmtTag::getName);
            tag.setItems(Optional.ofNullable(
                    hawkbitClient.getDistributionSetTagRestApi()
                            .getDistributionSetTags(null, 0, 20, Constants.NAME_ASC)
                            .getBody())
                    .map(PagedList::getContent)
                    .orElseGet(Collections::emptyList));
        }

        @Override
        public List<Component> components() {
            return List.of(textFilter, type);
        }

        @Override
        public String filter() {
            return Filter.filter(
                    Map.of(
                            List.of("version", "name"), textFilter.getOptionalValue().map(s -> "*" + s + "*"),
                            "type", type.getSelectedItems().stream().map(MgmtDistributionSetType::getKey).toList(),
                            "tag", tag.getSelectedItems().stream().map(MgmtTag::getName).toList()));
        }
    }

    private static class DistributionSetDetails extends FormLayout {

        private final transient HawkbitMgmtClient hawkbitClient;

        private final TextArea description = new TextArea("Description");
        private final TextField createdBy = Utils.textField("Created by");
        private final TextField createdAt = Utils.textField("Created at");
        private final TextField lastModifiedBy = Utils.textField("Last modified by");
        private final TextField lastModifiedAt = Utils.textField("Last modified at");
        private final TextArea metadata = new TextArea("Metadata");
        private final SelectionGrid<MgmtSoftwareModule, Long> softwareModulesGrid = selectSoftwareModuleGrid();

        private DistributionSetDetails(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;

            description.setMinLength(2);
            Stream.of(
                    description,
                    createdBy, createdAt,
                    lastModifiedBy, lastModifiedAt, metadata)
                    .forEach(field -> {
                        field.setReadOnly(true);
                        add(field);
                    });
            add(softwareModulesGrid);

            setResponsiveSteps(new ResponsiveStep("0", 2));
            setColspan(description, 2);
            setColspan(softwareModulesGrid, 2);
        }

        private void setItem(final MgmtDistributionSet distributionSet) {
            description.setValue(distributionSet.getDescription());
            createdBy.setValue(distributionSet.getCreatedBy());
            createdAt.setValue(Utils.localDateTimeFromTs(distributionSet.getCreatedAt()));
            lastModifiedBy.setValue(distributionSet.getLastModifiedBy());
            lastModifiedAt.setValue(Utils.localDateTimeFromTs(distributionSet.getLastModifiedAt()));
            metadata.setValue(Optional.ofNullable(
                    hawkbitClient.getDistributionSetRestApi().getMetadata(distributionSet.getId()).getBody())
                    .map(body -> body.getContent().stream()
                            .map(b -> String.format("%s: %s\n", b.getKey(), b.getValue())).collect(
                                    Collectors.joining())).orElse(""));

            softwareModulesGrid.setItems(query -> Optional.ofNullable(
                    hawkbitClient.getDistributionSetRestApi()
                            .getAssignedSoftwareModules(
                                    distributionSet.getId(),
                                    query.getOffset(), query.getLimit(), Constants.NAME_ASC)
                            .getBody()).stream().flatMap(body -> body.getContent().stream()));
            softwareModulesGrid.setSelectionMode(Grid.SelectionMode.NONE);
        }
    }

    private static class CreateDialog extends Utils.BaseDialog<Void> {

        private final transient HawkbitMgmtClient hawkbitClient;

        private final Select<MgmtDistributionSetType> type;
        private final TextField name;
        private final TextField version;
        private final TextArea description;
        private final Checkbox requiredMigrationStep;
        private final Button create;

        private CreateDialog(final HawkbitMgmtClient hawkbitClient) {
            super("Create Distribution Set");
            this.hawkbitClient = hawkbitClient;

            type = new Select<>(
                    "Type",
                    this::readyToCreate,
                    Optional.ofNullable(
                            hawkbitClient.getDistributionSetTypeRestApi()
                                    .getDistributionSetTypes(null, 0, 30, Constants.CREATED_AT_DESC)
                                    .getBody())
                            .map(body -> body.getContent().toArray(new MgmtDistributionSetType[0]))
                            .orElseGet(() -> new MgmtDistributionSetType[0]));
            type.focus();
            type.setWidthFull();
            type.setRequiredIndicatorVisible(true);
            type.setItemLabelGenerator(MgmtDistributionSetType::getName);
            name = Utils.textField(Constants.NAME, this::readyToCreate);
            version = Utils.textField(Constants.VERSION, this::readyToCreate);
            final TextField vendor = Utils.textField(Constants.VENDOR);
            description = new TextArea(Constants.DESCRIPTION);
            description.setWidthFull();
            description.setMinLength(2);
            requiredMigrationStep = new Checkbox("Required Migration Step");

            create = Utils.tooltip(new Button("Create"), "Create (Enter)");
            create.setEnabled(false);
            addCreateClickListener();
            create.addClickShortcut(Key.ENTER);
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
            cancel.addClickListener(e -> close());
            create.addClickShortcut(Key.ESCAPE);
            create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(cancel);
            getFooter().add(create);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setPadding(true);
            layout.setSpacing(false);
            layout.add(type, name, version, vendor, description, requiredMigrationStep);
            add(layout);
            open();
        }

        private void readyToCreate(final Object v) {
            final boolean createEnabled = !type.isEmpty() && !name.isEmpty() && !version.isEmpty();
            if (create.isEnabled() != createEnabled) {
                create.setEnabled(createEnabled);
            }
        }

        private void addCreateClickListener() {
            create.addClickListener(e -> {
                close();
                final long distributionSetId = Optional.ofNullable(
                        hawkbitClient.getDistributionSetRestApi()
                                .createDistributionSets(
                                        List.of((MgmtDistributionSetRequestBodyPost) new MgmtDistributionSetRequestBodyPost()
                                                .setType(type.getValue().getKey())
                                                .setName(name.getValue())
                                                .setVersion(version.getValue())
                                                .setDescription(description.getValue())
                                                .setRequiredMigrationStep(requiredMigrationStep.getValue())))
                                .getBody())
                        .stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .orElseThrow()
                        .getId();
                new AddSoftwareModulesDialog(distributionSetId, hawkbitClient).open();
            });
        }
    }

    @SuppressWarnings({ "java:S1171", "java:S3599" })
    private static class AddSoftwareModulesDialog extends Utils.BaseDialog<Void> {

        private final transient Set<MgmtSoftwareModule> softwareModules = Collections.synchronizedSet(new HashSet<>());

        private AddSoftwareModulesDialog(final long distributionSetId, final HawkbitMgmtClient hawkbitClient) {
            super("Add Software Modules");

            final SelectionGrid<MgmtSoftwareModule, Long> softwareModulesGrid = selectSoftwareModuleGrid();
            softwareModulesGrid.setItems(query -> {
                query.getOffset(); // to keep vaadin contract
                return softwareModules.stream().limit(query.getLimit());
            });

            final Component addRemoveControls = Utils.addRemoveControls(
                    v -> new Utils.BaseDialog<Void>("Add Software Modules") {

                        {
                            final SoftwareModuleView softwareModulesView = new SoftwareModuleView(false, hawkbitClient);
                            add(softwareModulesView);
                            final Button addBtn = new Button("Add");
                            addBtn.addClickListener(e -> {
                                softwareModules.addAll(softwareModulesView.getSelection());
                                softwareModulesGrid.refreshGrid(false);
                                close();
                            });
                            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                            getFooter().add(addBtn);
                            open();
                        }
                    }.result(),
                    v -> {
                        Utils.remove(softwareModulesGrid.getSelectedItems(), softwareModules, MgmtSoftwareModule::getId);
                        softwareModulesGrid.refreshGrid(false);
                        return CompletableFuture.completedFuture(null);
                    },
                    softwareModulesGrid, true);
            final Button finishBtn = Utils.tooltip(new Button("Finish"), "Finish (Esc)");
            finishBtn.addClickListener(e -> {
                hawkbitClient.getDistributionSetRestApi().assignSoftwareModules(
                        distributionSetId, softwareModules.stream().map(softwareModule -> {
                            final MgmtSoftwareModuleAssignment assignment = new MgmtSoftwareModuleAssignment();
                            assignment.setId(softwareModule.getId());
                            return assignment;
                        }).toList());
                close();
            });
            finishBtn.addClickShortcut(Key.ENTER);
            finishBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(finishBtn);

            final HorizontalLayout addRemove = new HorizontalLayout(addRemoveControls);
            addRemove.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            addRemove.setWidthFull();

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setSpacing(false);
            layout.add(softwareModulesGrid, addRemove);
            add(layout);
        }
    }
}
