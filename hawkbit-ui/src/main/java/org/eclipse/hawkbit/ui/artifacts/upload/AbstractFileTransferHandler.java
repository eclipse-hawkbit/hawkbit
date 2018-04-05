/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Upload;

/**
 * Implementation to read and upload a file. For {@link Upload} one instance is
 * used to handle the upload, in case of {@link StreamVariable} variant one
 * instance per file is used.
 *
 * The handler manages the output to the user and at the same time ensures that
 * the upload does not exceed the configured max file size.
 *
 */
public abstract class AbstractFileTransferHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFileTransferHandler.class);

    private volatile boolean aborted;

    private volatile boolean duplicateFile;

    private volatile boolean uploadInterrupted;

    private final UIEventBus eventBus;

    private final ArtifactManagement artifactManagement;

    private final UploadMessageBuilder uploadMessageBuilder;

    AbstractFileTransferHandler() {
        artifactManagement = null;
        uploadMessageBuilder = null;
        eventBus = null;
    }

    public AbstractFileTransferHandler(final ArtifactManagement artifactManagement,
            final UploadMessageBuilder uploadMessageBuilder) {
        this.artifactManagement = artifactManagement;
        this.uploadMessageBuilder = uploadMessageBuilder;
        this.eventBus = SpringContextHelper.getBean(EventBus.UIEventBus.class);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    protected void onEvent(final UploadStatusEventType event) {
        if (event == UploadStatusEventType.UPLOAD_ABORTED_BY_USER) {
            aborted = true;
        }
    }

    protected boolean isAbortedByUser() {
        return aborted;
    }

    protected void resetAbortedByUser() {
        aborted = false;
    }

    protected boolean isDuplicateFile() {
        return duplicateFile;
    }

    protected void resetDuplicateFile() {
        duplicateFile = false;
    }

    protected void setDuplicateFile() {
        duplicateFile = true;
    }

    protected void setUploadInterrupted() {
        uploadInterrupted = true;
    }

    protected boolean isUploadInterrupted() {
        return uploadInterrupted;
    }

    protected void resetUploadInterrupted() {
        uploadInterrupted = false;
    }

    protected void checkForDuplicateFileInSoftwareModul(final FileUploadId fileUploadId,
            final SoftwareModule softwareModule) {
        if (isFileAlreadyContainedInSoftwareModul(fileUploadId, softwareModule)) {
            setDuplicateFile();
            setUploadInterrupted();
            publishUploadFailedEvent(fileUploadId, uploadMessageBuilder.buildMessageForDuplicateFileError(), null);
        }

    }

    protected boolean isFileAlreadyContainedInSoftwareModul(final FileUploadId newFileUploadId,
            final SoftwareModule softwareModule) {
        for (final Artifact artifact : softwareModule.getArtifacts()) {
            final FileUploadId existingId = new FileUploadId(artifact.getFilename(), softwareModule);
            if (existingId.equals(newFileUploadId)) {
                return true;
            }
        }

        return false;
    }

    static class NullOutputStream extends OutputStream {
        @Override
        public void write(final int i) throws IOException {
            // do nothing
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            // do nothing
        }
    }

    protected void publishUploadStarted(final FileUploadId fileUploadId) {
        LOG.debug("Upload started for file {}", fileUploadId);
        final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId);
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_STARTED, fileUploadProgress));
    }

    protected void publishUploadProgressEvent(final FileUploadId fileUploadId, final long bytesReceived,
            final long fileSize, final String mimeType, final String tempFilePath) {
        // TODO rollouts: set level to trace
        if (LOG.isInfoEnabled()) {
            LOG.info("Upload in progress for file {} - {}%", fileUploadId,
                    String.format("%.0f", (double) bytesReceived / (double) fileSize * 100));
        }
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_IN_PROGRESS,
                new FileUploadProgress(fileUploadId, bytesReceived, fileSize, mimeType, tempFilePath)));
    }

    protected void publishUploadSucceeded(final FileUploadId fileUploadId, final long fileSize, final String mimeType,
            final String tempFilePath) {
        LOG.info("Upload succeeded for file {}", fileUploadId);
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_SUCCESSFUL,
                new FileUploadProgress(fileUploadId, fileSize, fileSize, mimeType, tempFilePath)));
    }

    protected void publishUploadFinishedEvent(final FileUploadId fileUploadId, final long fileSize,
            final String mimeType, final String tempFilePath) {
        LOG.info("Upload finished for file {}", fileUploadId);
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_FINISHED,
                new FileUploadProgress(fileUploadId, fileSize, fileSize, mimeType, tempFilePath)));
    }

    protected void publishUploadFailedEvent(final FileUploadId fileUploadId, final String failureReason,
            final Exception uploadException) {

        eventBus.publish(this,
                new UploadStatusEvent(UploadStatusEventType.UPLOAD_FAILED, new FileUploadProgress(fileUploadId,
                        StringUtils.isBlank(failureReason) ? uploadException.getMessage() : failureReason)));

        if (isAbortedByUser()) {
            LOG.info("Upload aborted by user for file :{}", fileUploadId);
        } else {
            LOG.info("Upload failed for file {} due to {}", fileUploadId,
                    StringUtils.isBlank(failureReason) ? uploadException.getMessage() : failureReason);
        }
    }

    protected void assertStateConsistency(final FileUploadId fileUploadId, final String filenameExtractedFromEvent) {
        if (!filenameExtractedFromEvent.equals(fileUploadId.getFilename())) {
            throw new IllegalStateException("Event filename " + filenameExtractedFromEvent + " but stored filename "
                    + fileUploadId.getFilename());
        }
    }

    // Exception squid:S3655 - Optional access is checked in
    // checkIfArtifactDetailsDispalyed subroutine
    @SuppressWarnings("squid:S3655")
    protected void transferArtifactToRepository(final FileUploadId fileUploadId, final long fileSize,
            final String mimeType, final String tempFilePath) {

        final SoftwareModule softwareModule = fileUploadId.getSoftwareModule();
        final File newFile = new File(tempFilePath);

        final String filename = fileUploadId.getFilename();
        softwareModule.getVersion();
        LOG.info("Transfering tempfile {} - {} to repository", filename, tempFilePath);
        try (FileInputStream fis = new FileInputStream(newFile)) {

            artifactManagement.create(fis, softwareModule.getId(), filename, null, null, true, mimeType);

            publishUploadSucceeded(fileUploadId, fileSize, mimeType, tempFilePath);

            eventBus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.ARTIFACTS_CHANGED, softwareModule));

        } catch (final ArtifactUploadFailedException | InvalidSHA1HashException | InvalidMD5HashException
                | IOException e) {
            // TODO rollouts: i18n
            publishUploadFailedEvent(fileUploadId, e.getMessage(), e);
            LOG.error("Failed to transfer file to repository", e);
        } finally {
            LOG.info("Deleting tempfile {} - {}", filename, newFile.getAbsolutePath());
            if (newFile.exists() && !newFile.delete()) {
                LOG.error("Could not delete temporary file: {}", newFile.getAbsolutePath());
            }
        }

        publishUploadFinishedEvent(fileUploadId, fileSize, mimeType, tempFilePath);
    }

}
