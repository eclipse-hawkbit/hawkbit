/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.details;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.mappers.ArtifactToProxyArtifactMapper;
import org.eclipse.hawkbit.ui.common.data.providers.ArtifactDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyArtifact;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.DeleteSupport;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.MasterEntitySupport;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;

/**
 * Artifact Details grid which is shown on the Upload View.
 */
public class ArtifactDetailsGrid extends AbstractGrid<ProxyArtifact, Long> {
    private static final long serialVersionUID = 1L;

    private static final String ARTIFACT_NAME_ID = "artifactName";
    private static final String ARTIFACT_SIZE_ID = "artifactSize";
    private static final String ARTIFACT_MODIFIED_DATE_ID = "artifactModifiedDate";
    private static final String ARTIFACT_SHA1_ID = "artifactSha1";
    private static final String ARTIFACT_MD5_ID = "artifactMd5";
    private static final String ARTIFACT_SHA256_ID = "artifactSha256";
    private static final String ARTIFACT_DOWNLOAD_BUTTON_ID = "artifactDownloadButton";
    private static final String ARTIFACT_DELETE_BUTTON_ID = "artifactDeleteButton";

    private final UINotification notification;
    private final transient ArtifactManagement artifactManagement;

    private final transient DeleteSupport<ProxyArtifact> artifactDeleteSupport;
    private final transient MasterEntitySupport<ProxySoftwareModule> masterEntitySupport;

    /**
     * Constructor
     *
     * @param eventBus
     *            UIEventBus
     * @param i18n
     *            VaadinMessageSource
     * @param permissionChecker
     *            SpPermissionChecker
     * @param notification
     *            UINotification
     * @param artifactManagement
     *            ArtifactManagement
     */
    public ArtifactDetailsGrid(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final SpPermissionChecker permissionChecker, final UINotification notification,
            final ArtifactManagement artifactManagement) {
        super(i18n, eventBus, permissionChecker);

        this.notification = notification;
        this.artifactManagement = artifactManagement;

        this.artifactDeleteSupport = new DeleteSupport<>(this, i18n, notification, "artifact.details.header",
                "caption.artifacts", ProxyArtifact::getFilename, this::artifactsDeletionCallback,
                UIComponentIdProvider.ARTIFACT_DELETE_CONFIRMATION_DIALOG);

        setFilterSupport(
                new FilterSupport<>(new ArtifactDataProvider(artifactManagement, new ArtifactToProxyArtifactMapper())));
        initFilterMappings();

        this.masterEntitySupport = new MasterEntitySupport<>(getFilterSupport());

        init();
    }

    private void initFilterMappings() {
        getFilterSupport().<Long> addMapping(FilterType.MASTER,
                (filter, masterFilter) -> getFilterSupport().setFilter(masterFilter));
    }

    /**
     * Initial method of grid and set style name
     */
    @Override
    public void init() {
        super.init();

        addStyleName("grid-row-border");
    }

    private boolean artifactsDeletionCallback(final Collection<ProxyArtifact> artifactsToBeDeleted) {
        final Collection<Long> artifactToBeDeletedIds = artifactsToBeDeleted.stream()
                .map(ProxyIdentifiableEntity::getId).collect(Collectors.toList());
        artifactToBeDeletedIds.forEach(artifactManagement::delete);

        eventBus.publish(EventTopics.ENTITY_MODIFIED, this,
                new EntityModifiedEventPayload(EntityModifiedEventType.ENTITY_UPDATED, ProxySoftwareModule.class,
                        getMasterEntitySupport().getMasterId()));

        return true;
    }

    /**
     * @return ID for artifact details table
     */
    @Override
    public String getGridId() {
        return UIComponentIdProvider.UPLOAD_ARTIFACT_DETAILS_TABLE;
    }

    /**
     * setting up the columns with their required definition
     */
    @Override
    public void addColumns() {
        addFilenameColumn().setExpandRatio(2);

        addSizeColumn();

        addModifiedDateColumn();

        GridComponentBuilder.joinToActionColumn(i18n, getDefaultHeaderRow(),
                Arrays.asList(addDownloadColumn(), addDeleteColumn()));
    }

    private Column<ProxyArtifact, String> addFilenameColumn() {
        return GridComponentBuilder.addColumn(this, ProxyArtifact::getFilename).setId(ARTIFACT_NAME_ID)
                .setCaption(i18n.getMessage("artifact.filename.caption"));
    }

    private Column<ProxyArtifact, Long> addSizeColumn() {
        return GridComponentBuilder.addColumn(this, ProxyArtifact::getSize).setId(ARTIFACT_SIZE_ID)
                .setCaption(i18n.getMessage("artifact.filesize.bytes.caption"));
    }

    protected Column<ProxyArtifact, String> addModifiedDateColumn() {
        return GridComponentBuilder.addColumn(this, ProxyArtifact::getModifiedDate).setId(ARTIFACT_MODIFIED_DATE_ID)
                .setCaption(i18n.getMessage("upload.last.modified.date"));
    }

    private Column<ProxyArtifact, Button> addDownloadColumn() {
        return GridComponentBuilder.addIconColumn(this, this::buildDownloadButton, ARTIFACT_DOWNLOAD_BUTTON_ID,
                i18n.getMessage("header.action.download"));
    }

    private Button buildDownloadButton(final ProxyArtifact artifact) {
        final Button downloadButton = GridComponentBuilder.buildActionButton(i18n, clickEvent -> {
        }, VaadinIcons.DOWNLOAD, UIMessageIdProvider.TOOLTIP_ARTIFACT_DOWNLOAD,
                SPUIStyleDefinitions.STATUS_ICON_NEUTRAL,
                UIComponentIdProvider.ARTIFACT_FILE_DOWNLOAD_ICON + "." + artifact.getId(),
                permissionChecker.hasDownloadRepositoryPermission());

        attachFileDownloader(artifact, downloadButton);

        return downloadButton;
    }

    private void attachFileDownloader(final ProxyArtifact artifact, final Button downloadButton) {
        final StreamResource artifactStreamResource = new StreamResource(() -> artifactManagement
                .loadArtifactBinary(artifact.getSha1Hash()).map(AbstractDbArtifact::getFileInputStream).orElse(null),
                artifact.getFilename());

        final FileDownloader fileDownloader = new FileDownloader(artifactStreamResource);
        fileDownloader.setErrorHandler(event -> notification
                .displayValidationError(i18n.getMessage(UIMessageIdProvider.ARTIFACT_DOWNLOAD_FAILURE_MSG)));

        fileDownloader.extend(downloadButton);
    }

    private Column<ProxyArtifact, Button> addDeleteColumn() {
        return GridComponentBuilder.addDeleteColumn(this, i18n, ARTIFACT_DELETE_BUTTON_ID, artifactDeleteSupport,
                UIComponentIdProvider.ARTIFACT_DELET_ICON, e -> permissionChecker.hasDeleteRepositoryPermission());
    }

    @Override
    protected void addMaxColumns() {
        addFilenameColumn().setExpandRatio(2);

        addSizeColumn();

        addSha1Column();
        addMd5Column();
        addSha256Column();

        addModifiedDateColumn();

        GridComponentBuilder.joinToActionColumn(i18n, getDefaultHeaderRow(),
                Arrays.asList(addDownloadColumn(), addDeleteColumn()));

        getColumns().forEach(column -> column.setHidable(true));
    }

    private Column<ProxyArtifact, String> addSha1Column() {
        return GridComponentBuilder.addColumn(this, ProxyArtifact::getSha1Hash).setId(ARTIFACT_SHA1_ID)
                .setCaption(i18n.getMessage("upload.sha1"));
    }

    private Column<ProxyArtifact, String> addMd5Column() {
        return GridComponentBuilder.addColumn(this, ProxyArtifact::getMd5Hash).setId(ARTIFACT_MD5_ID)
                .setCaption(i18n.getMessage("upload.md5"));
    }

    private Column<ProxyArtifact, String> addSha256Column() {
        return GridComponentBuilder.addColumn(this, ProxyArtifact::getSha256Hash).setId(ARTIFACT_SHA256_ID)
                .setCaption(i18n.getMessage("upload.sha256"));
    }

    /**
     * @return software module entity
     */
    public MasterEntitySupport<ProxySoftwareModule> getMasterEntitySupport() {
        return masterEntitySupport;
    }
}
