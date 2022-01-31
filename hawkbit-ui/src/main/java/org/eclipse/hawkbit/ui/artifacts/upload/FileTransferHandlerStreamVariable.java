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
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.vaadin.server.StreamVariable;

/**
 * {@link StreamVariable} implementation to read and upload a file. One instance
 * per file stream is used.
 *
 * The handler manages the output to the user and at the same time ensures that
 * the upload does not exceed the configured max file size.
 *
 */
public class FileTransferHandlerStreamVariable extends AbstractFileTransferHandler implements StreamVariable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FileTransferHandlerStreamVariable.class);

    private final long maxSize;
    private final long fileSize;
    private final String mimeType;
    private final FileUploadId fileUploadId;

    private final SoftwareModule selectedSoftwareModule;

    FileTransferHandlerStreamVariable(final String fileName, final long fileSize, final long maxSize,
            final String mimeType, final SoftwareModule selectedSw, final ArtifactManagement artifactManagement,
            final VaadinMessageSource i18n, final Lock uploadLock) {
        super(artifactManagement, i18n, uploadLock);
        this.fileSize = fileSize;
        this.maxSize = maxSize;
        this.mimeType = mimeType;
        this.selectedSoftwareModule = selectedSw;
        this.fileUploadId = new FileUploadId(fileName, selectedSw);

        publishUploadStarted(fileUploadId);
    }

    /**
     * Checks for duplication and invalid file name during file upload process
     * 
     * @param event
     *            StreamingStartEvent
     */
    @Override
    public void streamingStarted(final StreamingStartEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        if (RegexCharacterCollection.stringContainsCharacter(event.getFileName(), ILLEGAL_FILENAME_CHARACTERS)) {
            LOG.debug("Filename contains illegal characters {} for upload {}", fileUploadId.getFilename(),
                    fileUploadId);
            interruptUploadDueToIllegalFilename();
        } else if (isFileAlreadyContainedInSoftwareModule(fileUploadId, selectedSoftwareModule)) {
            LOG.debug("File {} already contained in Software Module {}", fileUploadId.getFilename(),
                    selectedSoftwareModule);
            interruptUploadDueToDuplicateFile();
        }
    }

    /**
     * get the output stream of uploaded file
     *
     * @return OutputStream
     */
    @Override
    public final OutputStream getOutputStream() {
        if (isUploadInterrupted()) {
            return ByteStreams.nullOutputStream();
        }

        // we return the output stream - we cannot close it here
        @SuppressWarnings("squid:S2095")
        final PipedOutputStream outputStream = new PipedOutputStream();
        final PipedInputStream inputStream;
        try {
            inputStream = new PipedInputStream(outputStream);
            publishUploadProgressEvent(fileUploadId, 0, fileSize);
            startTransferToRepositoryThread(inputStream, fileUploadId, mimeType);
        } catch (final IOException e) {
            LOG.warn("Creating piped Stream failed {}.", e.getMessage());
            tryToCloseIOStream(outputStream);
            // input stream is not created in case of and IOException here
            interruptUploadDueToUploadFailed();
            publishUploadFailedAndFinishedEvent(fileUploadId);
            return ByteStreams.nullOutputStream();
        }
        return outputStream;
    }

    /**
     * listen progress.
     * 
     * @return boolean
     */
    @Override
    public boolean listenProgress() {
        return true;
    }

    /**
     * Reports progress in {@link StreamVariable} variant. Interrupts
     *
     * @see com.vaadin.server.StreamVariable#onProgress(com.vaadin.server.StreamVariable.StreamingProgressEvent)
     */
    @Override
    public void onProgress(final StreamingProgressEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        if (isUploadInterrupted()) {
            publishUploadFailedAndFinishedEvent(fileUploadId);
            return;
        }

        if (event.getBytesReceived() > maxSize || event.getContentLength() > maxSize) {
            interruptUploadDueToFileSizeQuotaExceeded(SizeConversionHelper.byteValueToReadableString(maxSize));
            return;
        }

        publishUploadProgressEvent(fileUploadId, event.getBytesReceived(), event.getContentLength());
    }

    /**
     * Upload finished for {@link StreamVariable} variant. Called only in good
     * case.
     *
     * @see com.vaadin.server.StreamVariable#streamingFinished(com.vaadin.server.StreamVariable.StreamingEndEvent)
     */
    @Override
    public void streamingFinished(final StreamingEndEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());
    }

    /**
     * Upload failed for{@link StreamVariable} variant.
     *
     * @param event
     *            StreamingEndEvent
     */
    @Override
    public void streamingFailed(final StreamingErrorEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        if (!isUploadInterrupted()) {
            interruptUploadDueToUploadFailed();
        }
        LOG.debug("Streaming of file {} failed due to following exception: {}", fileUploadId, event.getException());
        publishUploadFailedAndFinishedEvent(fileUploadId);
    }

    /**
     * Verify if upload process is interrupted
     *
     * @return boolean
     */
    @Override
    public boolean isInterrupted() {
        return isUploadInterrupted();
    }
}
