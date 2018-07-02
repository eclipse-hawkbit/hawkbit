/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress.FileUploadStatus;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FailedListener;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

/**
 * Vaadin Upload implementation to read and upload a file. One instance is used
 * to handle all the uploads.
 *
 * The handler manages the output to the user and at the same time ensures that
 * the upload does not exceed the configured max file size.
 *
 */
public class FileTransferHandlerVaadinUpload extends AbstractFileTransferHandler
        implements Receiver, SucceededListener, FailedListener, FinishedListener, ProgressListener, StartedListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FileTransferHandlerVaadinUpload.class);

    private final transient SoftwareModuleManagement softwareModuleManagement;
    private final long maxSize;

    private volatile String mimeType;
    private volatile FileUploadId fileUploadId;

    FileTransferHandlerVaadinUpload(final long maxSize, final SoftwareModuleManagement softwareManagement,
            final ArtifactManagement artifactManagement, final VaadinMessageSource i18n) {
        super(artifactManagement, i18n);
        this.maxSize = maxSize;
        this.softwareModuleManagement = softwareManagement;
    }

    /**
     * Upload started for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.StartedListener#uploadStarted(com.vaadin.ui.Upload.StartedEvent)
     */
    @Override
    public void uploadStarted(final StartedEvent event) {
        // reset internal state here because instance is reused for next upload!
        resetState();
        this.mimeType = null;
        this.fileUploadId = null;

        assertThatOneSoftwareModuleIsSelected();

        // selected software module at the time of this callback is considered
        SoftwareModule softwareModule = null;
        final Optional<Long> selectedSoftwareModuleId = getUploadState().getSelectedBaseSwModuleId();
        final Long softwareModuleId = selectedSoftwareModuleId.orElse(null);
        if (softwareModuleId != null) {
            softwareModule = softwareModuleManagement.get(softwareModuleId).orElse(null);
        }

        this.fileUploadId = new FileUploadId(event.getFilename(), softwareModule);
        this.mimeType = event.getMIMEType();

        if (getUploadState().isFileInUploadState(this.fileUploadId)) {
            setFailureReasonUploadFailed();
            // actual interrupt will happen a bit late so setting the below
            // flag
            setDuplicateFile();
            event.getUpload().interruptUpload();
        } else {
            LOG.info("Uploading file {}", fileUploadId);
            publishUploadStarted(fileUploadId);

            if (isFileAlreadyContainedInSoftwareModule(fileUploadId, softwareModule)) {
                LOG.info("File {} already contained in Software Module {}", fileUploadId.getFilename(), softwareModule);
                getUploadState().updateFileUploadProgress(fileUploadId,
                        new FileUploadProgress(fileUploadId, FileUploadStatus.UPLOAD_FAILED));
                setDuplicateFile();
                event.getUpload().interruptUpload();
            }
        }
    }

    private void assertThatOneSoftwareModuleIsSelected() {
        // FileUpload button should be disabled if no SoftwareModul or more
        // than one is selected!
        if (getUploadState().isNoSoftwareModuleSelected()) {
            throw new IllegalStateException("No SoftwareModul selected");
        } else if (getUploadState().isMoreThanOneSoftwareModulesSelected()) {
            throw new IllegalStateException("More than one SoftwareModul selected but only one is allowed");
        }
    }

    /**
     * Create stream for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.Receiver#receiveUpload(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public OutputStream receiveUpload(final String fileName, final String mimeType) {

        if (isUploadInterrupted()) {
            return ByteStreams.nullOutputStream();
        }

        // we return the outputstream so we cannot close it here
        @SuppressWarnings("squid:S2095")
        OutputStream outputStream = ByteStreams.nullOutputStream();
        try {
            outputStream = createOutputStreamForTempFile();
            this.mimeType = mimeType;
            publishUploadProgressEvent(fileUploadId, 0, 0, getTempFilePath());

        } catch (final IOException e) {
            LOG.error("Creating temp file for upload failed {}.", e);
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e1) {
                    LOG.error("Closing output stream caused an exception {}", e1);
                }
            }

            setFailureReasonUploadFailed();
            setUploadInterrupted();
        }

        return outputStream;
    }

    /**
     * Reports progress in {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.ProgressListener#updateProgress(long, long)
     */
    @Override
    public void updateProgress(final long readBytes, final long contentLength) {
        if (readBytes > maxSize || contentLength > maxSize) {
            LOG.error("User tried to upload more than was allowed ({}).", maxSize);
            setFailureReasonFileSizeExceeded(maxSize);
            setUploadInterrupted();
            return;
        }
        if (isUploadInterrupted()) {
            // Upload interruption is delayed maybe another event is fired
            // before
            return;
        }

        publishUploadProgressEvent(fileUploadId, readBytes, contentLength, getTempFilePath());
    }

    /**
     *
     * Upload sucessfull for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.SucceededListener#uploadSucceeded(com.vaadin.ui.Upload.SucceededEvent)
     */
    @Override
    public void uploadSucceeded(final SucceededEvent event) {
        if (isUploadInterrupted()) {
            // Upload interruption is delayed maybe another event is fired
            // before
            return;
        }
        assertStateConsistency(fileUploadId, event.getFilename());

        transferArtifactToRepository(fileUploadId, event.getLength(), mimeType, getTempFilePath());
    }

    /**
     * Upload finished for {@link Upload} variant. Both for good and error
     * variant.
     *
     * @see com.vaadin.ui.Upload.FinishedListener#uploadFinished(com.vaadin.ui.Upload.FinishedEvent)
     */
    @Override
    public void uploadFinished(final FinishedEvent event) {
        // ignore this event
    }

    /**
     * Upload failed for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.FailedListener#uploadFailed(com.vaadin.ui.Upload.FailedEvent)
     */
    @Override
    public void uploadFailed(final FailedEvent event) {
        assertStateConsistency(fileUploadId, event.getFilename());

        if (isDuplicateFile()) {
            publishUploadFailedEvent(fileUploadId, getI18n().getMessage("message.no.duplicateFiles"),
                    event.getReason());
        } else {
            publishUploadFailedEvent(fileUploadId, event.getReason());
        }
        publishUploadFinishedEvent(fileUploadId);
    }

}
