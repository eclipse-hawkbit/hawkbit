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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.locks.Lock;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.RegexCharacterCollection;
import org.eclipse.hawkbit.repository.SizeConversionHelper;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
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

    private FileUploadId fileUploadId;

    FileTransferHandlerVaadinUpload(final long maxSize, final SoftwareModuleManagement softwareManagement,
            final ArtifactManagement artifactManagement, final VaadinMessageSource i18n, final Lock uploadLock) {
        super(artifactManagement, i18n, uploadLock);

        this.maxSize = maxSize;
        this.softwareModuleManagement = softwareManagement;
    }

    private synchronized FileUploadId setFileUploadId(final String fileName, final SoftwareModule softwareModule) {
        this.fileUploadId = new FileUploadId(fileName, softwareModule);
        return fileUploadId;
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

        final SoftwareModule softwareModule = getSelectedSoftwareModule();
        final FileUploadId fupId = setFileUploadId(event.getFilename(), softwareModule);

        if (getUploadState().isFileInUploadState(this.fileUploadId)) {
            // actual interrupt will happen a bit late so setting the below
            // flag
            interruptUploadDueToDuplicateFile();
            event.getUpload().interruptUpload();
        } else {
            publishUploadStarted(fupId);

            if (RegexCharacterCollection.stringContainsCharacter(event.getFilename(), ILLEGAL_FILENAME_CHARACTERS)) {
                LOG.debug("Filename contains illegal characters {} for upload {}", fupId.getFilename(), fupId);
                interruptUploadDueToIllegalFilename();
                event.getUpload().interruptUpload();
            } else if (isFileAlreadyContainedInSoftwareModule(fupId, softwareModule)) {
                LOG.debug("File {} already contained in Software Module {}", fupId.getFilename(),
                        softwareModule);
                interruptUploadDueToDuplicateFile();
                event.getUpload().interruptUpload();
            }
        }
    }

    private SoftwareModule getSelectedSoftwareModule() {
        final Long lastSelectedSmId = getUploadState().getSmGridLayoutUiState().getSelectedEntityId();

        return softwareModuleManagement.get(lastSelectedSmId)
                .orElseThrow(() -> new IllegalStateException("SoftwareModul with unknown ID selected"));
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
        final PipedOutputStream outputStream = new PipedOutputStream();
        try {
            PipedInputStream inputStream = new PipedInputStream(outputStream);
            publishUploadProgressEvent(fileUploadId, 0, 0);
            startTransferToRepositoryThread(inputStream, fileUploadId, mimeType);
        } catch (final IOException e) {
            LOG.warn("Creating piped Stream failed!", e);
            tryToCloseIOStream(outputStream);
            // input stream is not created in case of an IOException
            interruptUploadDueToUploadFailed();
            publishUploadFailedAndFinishedEvent(fileUploadId);
            return ByteStreams.nullOutputStream();
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
        if (isUploadInterrupted()) {
            publishUploadFailedAndFinishedEvent(fileUploadId);
            return;
        }

        if (readBytes > maxSize || contentLength > maxSize) {
            interruptUploadDueToFileSizeQuotaExceeded(SizeConversionHelper.byteValueToReadableString(maxSize));
            return;
        }

        publishUploadProgressEvent(fileUploadId, readBytes, contentLength);
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

        if (!isUploadInterrupted()) {
            interruptUploadDueToUploadFailed();
        }
        LOG.debug("Upload of file {} failed due to following exception: {}", fileUploadId, event.getReason());
        publishUploadFailedAndFinishedEvent(fileUploadId);
    }

    @Override
    protected void resetState() {
        super.resetState();
        this.fileUploadId = null;
    }
}
