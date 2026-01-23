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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadEvent;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssignment;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.ui.HawkbitMgmtClient;
import org.eclipse.hawkbit.ui.MainLayout;
import org.eclipse.hawkbit.ui.view.util.Filter;
import org.eclipse.hawkbit.ui.view.util.SelectionGrid;
import org.eclipse.hawkbit.ui.view.util.TableView;
import org.eclipse.hawkbit.ui.view.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

@PageTitle("Software Modules")
@Route(value = "software_modules", layout = MainLayout.class)
@RolesAllowed({ "SOFTWARE_MODULE_READ" })
@Uses(Icon.class)
@Slf4j
public class SoftwareModuleView extends TableView<MgmtSoftwareModule, Long> {

    @Autowired
    public SoftwareModuleView(final HawkbitMgmtClient hawkbitClient) {
        this(true, hawkbitClient);
    }

    public SoftwareModuleView(final boolean isParent, final HawkbitMgmtClient hawkbitClient) {
        super(
                new SoftwareModuleFilter(hawkbitClient),
                new SelectionGrid.EntityRepresentation<>(MgmtSoftwareModule.class, MgmtSoftwareModule::getId) {

                    private final SoftwareModuleDetails details = new SoftwareModuleDetails(hawkbitClient);

                    @Override
                    protected void addColumns(final Grid<MgmtSoftwareModule> grid) {
                        grid.addColumn(MgmtSoftwareModule::getId).setHeader(Constants.ID).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getName).setHeader(Constants.NAME).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getVersion).setHeader(Constants.VERSION).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getTypeName).setHeader(Constants.TYPE).setAutoWidth(true);
                        grid.addColumn(MgmtSoftwareModule::getVendor).setHeader(Constants.VENDOR).setAutoWidth(true);

                        grid.setItemDetailsRenderer(new ComponentRenderer<>(() -> details, SoftwareModuleDetails::setItem));
                    }
                },
                (query, rsqlFilter) -> Optional.ofNullable(
                        hawkbitClient.getSoftwareModuleRestApi()
                                .getSoftwareModules(rsqlFilter, query.getOffset(), query.getPageSize(), Constants.NAME_ASC)
                                .getBody())
                        .stream().map(PagedList::getContent).flatMap(List::stream),
                isParent ? v -> new CreateDialog(hawkbitClient).result() : null,
                isParent ? selectionGrid -> {
                    selectionGrid.getSelectedItems().forEach(
                            module -> hawkbitClient.getSoftwareModuleRestApi().deleteSoftwareModule(module.getId()));
                    selectionGrid.refreshGrid(false);
                    return CompletableFuture.completedFuture(null);
                } : null);
    }

    public Set<MgmtSoftwareModule> getSelection() {
        return selectionGrid.getSelectedItems();
    }

    private static SelectionGrid<MgmtArtifact, Long> createArtifactGrid() {
        return new SelectionGrid<>(
                new SelectionGrid.EntityRepresentation<>(MgmtArtifact.class, MgmtArtifact::getId) {

                    @Override
                    protected void addColumns(final Grid<MgmtArtifact> grid) {
                        grid.addColumn(MgmtArtifact::getId).setHeader(Constants.ID).setAutoWidth(true);
                        grid.addColumn(MgmtArtifact::getProvidedFilename).setHeader(Constants.NAME).setAutoWidth(true);
                        grid.addColumn(MgmtArtifact::getSize).setHeader("Size").setAutoWidth(true);
                    }
                });
    }

    private static class SoftwareModuleFilter implements Filter.Rsql {

        private final TextField name = Utils.textField(Constants.NAME);
        private final CheckboxGroup<MgmtSoftwareModuleType> type = new CheckboxGroup<>(Constants.TYPE);

        private SoftwareModuleFilter(final HawkbitMgmtClient hawkbitClient) {
            name.setPlaceholder("<name filter>");
            type.setItemLabelGenerator(MgmtSoftwareModuleType::getName);
            type.setItems(Optional.ofNullable(
                    hawkbitClient.getSoftwareModuleTypeRestApi()
                            .getTypes(null, 0, 20, Constants.NAME_ASC)
                            .getBody())
                    .map(PagedList::getContent)
                    .orElseGet(Collections::emptyList));
        }

        @Override
        public List<Component> components() {
            return List.of(name, type);
        }

        @Override
        public String filter() {
            return Filter.filter(
                    Map.of(
                            "name", name.getOptionalValue(),
                            "type", type.getSelectedItems().stream().map(MgmtSoftwareModuleType::getKey).toList()
                    ));
        }
    }

    private static class SoftwareModuleDetails extends FormLayout {

        private final transient HawkbitMgmtClient hawkbitClient;

        private final TextArea description = new TextArea(Constants.DESCRIPTION);
        private final TextField createdBy = Utils.textField(Constants.CREATED_BY);
        private final TextField createdAt = Utils.textField(Constants.CREATED_AT);
        private final TextField lastModifiedBy = Utils.textField(Constants.LAST_MODIFIED_BY);
        private final TextField lastModifiedAt = Utils.textField(Constants.LAST_MODIFIED_AT);
        private final SelectionGrid<MgmtArtifact, Long> artifactGrid;

        private SoftwareModuleDetails(final HawkbitMgmtClient hawkbitClient) {
            this.hawkbitClient = hawkbitClient;

            description.setMinLength(2);
            artifactGrid = createArtifactGrid();
            Stream.of(description, createdBy, createdAt, lastModifiedBy, lastModifiedAt).forEach(field -> {
                field.setReadOnly(true);
                add(field);
            });
            add(artifactGrid);

            setResponsiveSteps(new ResponsiveStep("0", 2));
            setColspan(description, 2);
            setColspan(artifactGrid, 2);
        }

        private void setItem(final MgmtSoftwareModule softwareModule) {
            description.setValue(Objects.requireNonNullElse(softwareModule.getDescription(), ""));
            createdBy.setValue(softwareModule.getCreatedBy());
            createdAt.setValue(Utils.localDateTimeFromTs(softwareModule.getCreatedAt()));
            lastModifiedBy.setValue(softwareModule.getLastModifiedBy());
            lastModifiedAt.setValue(Utils.localDateTimeFromTs(softwareModule.getLastModifiedAt()));

            artifactGrid.setItems(query -> Optional.ofNullable(
                    hawkbitClient.getSoftwareModuleRestApi()
                            .getArtifacts(softwareModule.getId(), null, null)
                            .getBody())
                    .stream()
                    .flatMap(Collection::stream)
                    .skip(query.getOffset())
                    .limit(query.getPageSize()));
            artifactGrid.setSelectionMode(Grid.SelectionMode.NONE);
        }
    }

    private static class CreateDialog extends Utils.BaseDialog<Void> {

        private final Select<MgmtSoftwareModuleType> type;
        private final TextField name;
        private final TextField version;
        private final TextField vendor;
        private final TextArea description;
        private final Checkbox enableArtifactEncryption;
        private final Checkbox createDistributionSet;
        private final Select<MgmtDistributionSetType> distType;
        private final Checkbox distRequiredMigrationStep;
        private final Button create;

        private CreateDialog(final HawkbitMgmtClient hawkbitClient) {
            super("Create Software Module");

            type = new Select<>(
                    Constants.TYPE,
                    this::readyToCreate,
                    Optional.ofNullable(
                            hawkbitClient.getSoftwareModuleTypeRestApi()
                                    .getTypes(null, 0, 30, Constants.NAME_ASC)
                                    .getBody())
                            .map(body -> body.getContent().toArray(new MgmtSoftwareModuleType[0]))
                            .orElseGet(() -> new MgmtSoftwareModuleType[0]));
            type.setWidthFull();
            type.setRequiredIndicatorVisible(true);
            type.setItemLabelGenerator(MgmtSoftwareModuleType::getName);
            type.focus();

            name = Utils.textField(Constants.NAME, this::readyToCreate);
            version = Utils.textField(Constants.VERSION, this::readyToCreate);
            vendor = Utils.textField(Constants.VENDOR);
            description = new TextArea(Constants.DESCRIPTION);
            description.setWidthFull();
            description.setMinLength(2);
            enableArtifactEncryption = new Checkbox("Enable artifact encryption");

            distType = new Select<>("Distribution Set Type", this::readyToCreate);
            distType.setWidthFull();
            distType.setRequiredIndicatorVisible(true);
            distType.setItemLabelGenerator(MgmtDistributionSetType::getName);
            distType.setVisible(false);

            distRequiredMigrationStep = new Checkbox("Required Migration Step");
            distRequiredMigrationStep.setVisible(false);

            createDistributionSet = new Checkbox("Create single software module distribution set");
            createDistributionSet.setHelperText("Create single software module distribution set with this software module");
            createDistributionSet.addValueChangeListener(e -> {
                if (Boolean.TRUE.equals(createDistributionSet.getValue()) && distType.isEmpty()) {
                    distType.setItems(
                            Optional.ofNullable(
                                    hawkbitClient.getDistributionSetTypeRestApi()
                                            .getDistributionSetTypes(null, 0, 30, Constants.NAME_ASC)
                                            .getBody())
                                    .map(body -> body.getContent().toArray(new MgmtDistributionSetType[0]))
                                    .orElseGet(() -> new MgmtDistributionSetType[0]));
                }
                distType.setVisible(createDistributionSet.getValue());
                distRequiredMigrationStep.setVisible(createDistributionSet.getValue());
                readyToCreate(e);
            });

            create = Utils.tooltip(new Button("Create"), "Create (Enter)");
            create.setEnabled(false);
            addCreateClickListener(hawkbitClient);
            create.addClickShortcut(Key.ENTER);
            create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            final Button cancel = Utils.tooltip(new Button(CANCEL), CANCEL_ESC);
            cancel.addClickListener(e -> close());
            cancel.addClickShortcut(Key.ESCAPE);
            getFooter().add(cancel);
            getFooter().add(create);

            final VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.setSpacing(false);
            layout.add(
                    type, name, version, vendor, description, enableArtifactEncryption,
                    createDistributionSet, distType, distRequiredMigrationStep);
            add(layout);
            open();
        }

        private void readyToCreate(final Object v) {
            final boolean createEnabled = !type.isEmpty() && !name.isEmpty() && !version.isEmpty() && (!createDistributionSet
                    .getValue() || !distType.isEmpty());
            if (create.isEnabled() != createEnabled) {
                create.setEnabled(createEnabled);
            }
        }

        private void addCreateClickListener(final HawkbitMgmtClient hawkbitClient) {
            create.addClickListener(e -> {
                close();
                final long softwareModuleId = Optional.ofNullable(
                        hawkbitClient.getSoftwareModuleRestApi().createSoftwareModules(
                                List.of(new MgmtSoftwareModuleRequestBodyPost()
                                        .setType(type.getValue().getKey())
                                        .setName(name.getValue())
                                        .setVersion(version.getValue())
                                        .setVendor(vendor.getValue())
                                        .setDescription(description.getValue())
                                        .setEncrypted(enableArtifactEncryption.getValue())))
                                .getBody())
                        .stream().flatMap(Collection::stream)
                        .findFirst()
                        .orElseThrow()
                        .getId();
                if (Boolean.TRUE.equals(createDistributionSet.getValue())) {
                    final long distributionSetId = Optional.ofNullable(
                            hawkbitClient.getDistributionSetRestApi()
                                    .createDistributionSets(
                                            List.of((MgmtDistributionSetRequestBodyPost) new MgmtDistributionSetRequestBodyPost()
                                                    .setType(distType.getValue().getKey())
                                                    .setName(name.getValue())
                                                    .setVersion(version.getValue())
                                                    .setDescription(description.getValue())
                                                    .setRequiredMigrationStep(distRequiredMigrationStep.getValue())))
                                    .getBody())
                            .stream()
                            .flatMap(Collection::stream)
                            .findFirst()
                            .orElseThrow()
                            .getId();
                    hawkbitClient.getDistributionSetRestApi().assignSoftwareModules(
                            distributionSetId, List.of((MgmtSoftwareModuleAssignment) new MgmtSoftwareModuleAssignment()
                                    .setId(softwareModuleId))).getBody();
                }
                new AddArtifactsDialog(softwareModuleId, hawkbitClient).open();
            });
        }
    }

    private static class AddArtifactsDialog extends Utils.BaseDialog<Void> {

        private final transient Set<MgmtArtifact> artifacts = Collections.synchronizedSet(new HashSet<>());

        private AddArtifactsDialog(
                final long softwareModuleId,
                final HawkbitMgmtClient hawkbitClient) {
            super("Add Artifacts");

            final SelectionGrid<MgmtArtifact, Long> artifactGrid = createArtifactGrid();
            artifactGrid.setItems(query -> {
                query.getOffset(); // to keep vaadin contract
                return artifacts.stream().limit(query.getLimit());
            });
            artifactGrid.setSelectionMode(Grid.SelectionMode.NONE);

            final Upload uploadBtn = new Upload(uploadEvent -> {
                final MgmtArtifact artifact = hawkbitClient.getSoftwareModuleRestApi()
                        .uploadArtifact(
                                softwareModuleId,
                                new MultipartFileImpl(uploadEvent),
                                uploadEvent.getFileName(), null, null, null)
                        .getBody();
                artifacts.add(artifact);
                artifactGrid.refreshGrid(false);
            });
            uploadBtn.setMaxFiles(10);
            uploadBtn.setWidthFull();
            uploadBtn.setDropAllowed(true);

            final Button finishBtn = Utils.tooltip(new Button("Finish"), "Finish (Enter)");
            finishBtn.addClickListener(e -> close());
            finishBtn.addClickShortcut(Key.ENTER);
            finishBtn.setHeightFull();
            finishBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(finishBtn);

            final VerticalLayout layout = new VerticalLayout(artifactGrid, uploadBtn);
            layout.setSizeFull();
            layout.setSpacing(false);
            add(layout);
        }

        private static class MultipartFileImpl implements MultipartFile {

            private final UploadEvent uploadEvent;

            public MultipartFileImpl(final UploadEvent uploadEvent) {
                this.uploadEvent = uploadEvent;
            }

            @Override
            public String getName() {
                return uploadEvent.getFileName();
            }

            @Override
            public String getOriginalFilename() {
                return getName();
            }

            @Override
            public String getContentType() {
                return uploadEvent.getContentType();
            }

            @Override
            public boolean isEmpty() {
                return uploadEvent.getFileSize() == 0;
            }

            @Override
            public long getSize() {
                return uploadEvent.getFileSize();
            }

            @Override
            public byte[] getBytes() throws IOException {
                log.warn("Multipart file getBytes() is called. Whole input stream is loaded into the memory!");
                try (final InputStream is = getInputStream()) {
                    return is.readAllBytes();
                }
            }

            @Override
            public InputStream getInputStream() {
                return uploadEvent.getInputStream();
            }

            @Override
            public void transferTo(final File dest) throws IllegalStateException {
                throw new UnsupportedOperationException();
            }
        }
    }
}