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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress.FileUploadStatus;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.io.ByteStreams;

/**
 * Abstract base class for transferring files from the browser to the
 * repository.
 */
public abstract class AbstractFileTransferHandler implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFileTransferHandler.class);

    private volatile boolean duplicateFile;

    private volatile boolean uploadInterrupted;

    private volatile String tempFilePath;

    private volatile String failureReason;

    private final ArtifactUploadState artifactUploadState;

    private final transient UIEventBus eventBus;

    private final transient ArtifactManagement artifactManagement;

    private final VaadinMessageSource i18n;

    AbstractFileTransferHandler(final ArtifactManagement artifactManagement, final VaadinMessageSource i18n) {
        this.artifactManagement = artifactManagement;
        this.i18n = i18n;
        this.eventBus = SpringContextHelper.getBean(EventBus.UIEventBus.class);
        this.artifactUploadState = SpringContextHelper.getBean(ArtifactUploadState.class);
    }

    protected boolean isDuplicateFile() {
        return duplicateFile;
    }

    protected void setDuplicateFile() {
        uploadInterrupted = true;
        duplicateFile = true;
    }

    protected void setUploadInterrupted() {
        uploadInterrupted = true;
    }

    protected boolean isUploadInterrupted() {
        return uploadInterrupted;
    }

    protected void resetState() {
        duplicateFile = false;
        uploadInterrupted = false;
        failureReason = null;
    }

    protected ArtifactUploadState getUploadState() {
        return artifactUploadState;
    }

    protected VaadinMessageSource getI18n() {
        return i18n;
    }

    private void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    protected void setFailureReasonUploadFailed() {
        setFailureReason(i18n.getMessage("message.upload.failed"));
    }

    protected void setFailureReasonFileSizeExceeded(final long maxSize) {
        setFailureReason(i18n.getMessage("message.uploadedfile.size.exceeded", maxSize));
    }

    protected boolean isFileAlreadyContainedInSoftwareModule(final FileUploadId newFileUploadId,
            final SoftwareModule softwareModule) {
        for (final Artifact artifact : softwareModule.getArtifacts()) {
            final FileUploadId existingId = new FileUploadId(artifact.getFilename(), softwareModule);
            if (existingId.equals(newFileUploadId)) {
                return true;
            }
        }

        return false;
    }

    protected void publishUploadStarted(final FileUploadId fileUploadId) {
        LOG.info("Upload started for file {}", fileUploadId);
        final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId, FileUploadStatus.UPLOAD_STARTED);
        artifactUploadState.updateFileUploadProgress(fileUploadId, fileUploadProgress);
        eventBus.publish(this, fileUploadProgress);
    }

    protected void publishUploadProgressEvent(final FileUploadId fileUploadId, final long bytesReceived,
            final long fileSize, final String tempFilePath) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Upload in progress for file {} - {}%", fileUploadId,
                    String.format("%.0f", (double) bytesReceived / (double) fileSize * 100));
        }
        final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId,
                FileUploadStatus.UPLOAD_IN_PROGRESS, bytesReceived, fileSize, tempFilePath);
        artifactUploadState.updateFileUploadProgress(fileUploadId, fileUploadProgress);
        eventBus.publish(this, fileUploadProgress);
    }

    private void publishUploadSucceeded(final FileUploadId fileUploadId, final long fileSize,
            final String tempFilePath) {
        LOG.info("Upload succeeded for file {}", fileUploadId);
        final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId,
                FileUploadStatus.UPLOAD_SUCCESSFUL, fileSize, fileSize, tempFilePath);
        artifactUploadState.updateFileUploadProgress(fileUploadId, fileUploadProgress);
        eventBus.publish(this, fileUploadProgress);
    }

    protected void publishUploadFinishedEvent(final FileUploadId fileUploadId) {
        LOG.info("Upload finished for file {}", fileUploadId);
        final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId,
                FileUploadStatus.UPLOAD_FINISHED);
        eventBus.publish(this, fileUploadProgress);
    }

    protected void publishUploadFailedEvent(final FileUploadId fileUploadId, final String failureReason,
            final Exception uploadException) {
        LOG.info("Upload failed for file {} due to {}", fileUploadId,
                StringUtils.isBlank(failureReason) ? uploadException.getMessage() : failureReason);
        final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId,
                FileUploadStatus.UPLOAD_FAILED,
                StringUtils.isBlank(failureReason) ? uploadException.getMessage() : failureReason);
        artifactUploadState.updateFileUploadProgress(fileUploadId, fileUploadProgress);
        eventBus.publish(this, fileUploadProgress);
    }

    protected void publishUploadFailedEvent(final FileUploadId fileUploadId, final Exception uploadException) {
        publishUploadFailedEvent(fileUploadId, failureReason, uploadException);
    }

    protected void assertStateConsistency(final FileUploadId fileUploadId, final String filenameExtractedFromEvent) {
        if (!filenameExtractedFromEvent.equals(fileUploadId.getFilename())) {
            throw new IllegalStateException("Event filename " + filenameExtractedFromEvent + " but stored filename "
                    + fileUploadId.getFilename());
        }
    }

    protected OutputStream createOutputStreamForTempFile() throws IOException {

        if (isUploadInterrupted()) {
            return ByteStreams.nullOutputStream();
        }

        final File tempFile = File.createTempFile("spUiArtifactUpload", null);

        // we return the outputstream so we cannot close it here
        @SuppressWarnings("squid:S2095")
        final OutputStream out = new FileOutputStream(tempFile);

        tempFilePath = tempFile.getAbsolutePath();

        return out;
    }

    protected String getTempFilePath() {
        return tempFilePath;
    }

    // Exception squid:S3655 - Optional access is checked in
    // checkIfArtifactDetailsDispalyed subroutine
    @SuppressWarnings("squid:S3655")
    protected void transferArtifactToRepository(final FileUploadId fileUploadId, final long fileSize,
            final String mimeType, final String tempFilePath) {

        final File newFile = new File(tempFilePath);

        final String filename = fileUploadId.getFilename();
        LOG.info("Transfering tempfile {} - {} to repository", filename, tempFilePath);
        try (FileInputStream fis = new FileInputStream(newFile)) {

            artifactManagement.create(fis, fileUploadId.getSoftwareModuleId(), filename, null, null, true, mimeType,
                    fileSize);

            publishUploadSucceeded(fileUploadId, fileSize, tempFilePath);

            eventBus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.ARTIFACTS_CHANGED,
                    fileUploadId.getSoftwareModuleId()));

        } catch (final ArtifactUploadFailedException | InvalidSHA1HashException | InvalidMD5HashException
                | IOException e) {
            publishUploadFailedEvent(fileUploadId, i18n.getMessage("message.upload.failed"), e);
            LOG.error("Failed to transfer file to repository", e);
        } finally {
            LOG.debug("Deleting tempfile {} - {}", filename, newFile.getAbsolutePath());
            if (newFile.exists() && !newFile.delete()) {
                LOG.error("Could not delete temporary file: {}", newFile.getAbsolutePath());
            }
        }

        publishUploadFinishedEvent(fileUploadId);
    }

}
