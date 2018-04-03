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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;

import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.SucceededEvent;

/**
 * {@link StreamVariable}iImplementation to read and upload a file. One instance
 * per file stream is used.
 *
 * The handler manages the output to the user and at the same time ensures that
 * the upload does not exceed the configured max file size.
 *
 */
public class FileTransferHandlerStreamVariable extends AbstractFileTransferHandler implements StreamVariable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FileTransferHandlerStreamVariable.class);

    private final UploadLogic uploadLogic;
    private final long maxSize;
    private final Upload upload;
    private volatile long fileSize;
    private volatile String fileName;
    private volatile String tempFilePath;
    private volatile String mimeType;
    private volatile SoftwareModule selectedSw;
    private volatile FileUploadId fileUploadId;

    private volatile boolean streamingInterrupted;

    private String failureReason;
    private final VaadinMessageSource i18n;
    private transient EventBus.UIEventBus eventBus;
    private final ArtifactUploadState artifactUploadState;
    private final UINotification uiNotification;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    // TODO rollouts: remove all state handling and UI code, communicate only
    // per events

    FileTransferHandlerStreamVariable(final String fileName, final long fileSize, final UploadLogic uploadLogic,
            final long maxSize, final Upload upload, final String mimeType, final SoftwareModule selectedSw,
            final SoftwareModuleManagement softwareManagement) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadLogic = uploadLogic;
        this.maxSize = maxSize;
        this.upload = upload;
        // is null in case of vaadin.Upload but filled in case of
        // StreamingVariable
        this.mimeType = mimeType;
        // is null in case of vaadin.Upload but filled in case of
        // StreamingVariable
        this.fileUploadId = new FileUploadId(fileName, selectedSw);
        this.selectedSw = selectedSw;
        this.i18n = SpringContextHelper.getBean(VaadinMessageSource.class);
        this.eventBus = SpringContextHelper.getBean(EventBus.UIEventBus.class);
        this.artifactUploadState = SpringContextHelper.getBean(ArtifactUploadState.class);
        this.uiNotification = SpringContextHelper.getBean(UINotification.class);
        this.softwareModuleManagement = softwareManagement;
        eventBus.subscribe(this);
    }

    @Override
    public void streamingStarted(final StreamingStartEvent event) {
        LOG.debug("Streaming started for file :{}", fileName);

        assertStateConsistency(fileUploadId, event.getFileName());

        final FileUploadProgress fileUploadProgress = new FileUploadProgress(fileUploadId, 0, -1);
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_STARTED, fileUploadProgress));
    }

    /**
     * Create stream for {@link StreamVariable} variant.
     *
     * @see com.vaadin.server.StreamVariable#getOutputStream()
     */
    @Override
    public final OutputStream getOutputStream() {
        File tempFile = null;

        try {
            tempFile = File.createTempFile("spUiArtifactUpload", null);

            // we return the outputstream so we cannot close it here
            @SuppressWarnings("squid:S2095")
            final OutputStream out = new FileOutputStream(tempFile);

            fileUploadId.setMimeType(mimeType);
            tempFilePath = tempFile.getAbsolutePath();
            eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_IN_PROGRESS,
                    new FileUploadProgress(fileUploadId, 0, fileSize, tempFilePath)));

            return out;
        } catch (final FileNotFoundException e) {
            LOG.error("Upload failed {}", e);
            failureReason = i18n.getMessage("message.file.not.found");
        } catch (final IOException e) {
            LOG.error("Upload failed {}", e);
            failureReason = i18n.getMessage("message.upload.failed");
        }

        streamingInterrupted = true;

        return new NullOutputStream();
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

        if (streamingInterrupted) {
            return;
        }
        if (isAbortedByUser()) {
            LOG.info("User aborted  the upload for file : {}", event.getFileName());
            failureReason = i18n.getMessage("message.uploadedfile.aborted");
            interruptFileStreaming();
            return;
        }
        if (event.getBytesReceived() > maxSize || event.getContentLength() > maxSize) {
            LOG.error("User tried to upload more than was allowed ({}).", maxSize);
            failureReason = i18n.getMessage("message.uploadedfile.size.exceeded", maxSize);
            interruptFileStreaming();
            return;
        }

        publishUploadProgressEvent(fileUploadId, event.getBytesReceived(), event.getContentLength(), tempFilePath);
    }

    private void interruptFileStreaming() {
        streamingInterrupted = true;
    }

    /**
     * Upload finished for {@link StreamVariable} variant. Called only in good
     * case. So a combination of {@link #uploadSucceeded(SucceededEvent)} and
     * {@link #uploadFinished(FinishedEvent)}.
     *
     * @see com.vaadin.server.StreamVariable#streamingFinished(com.vaadin.server.StreamVariable.StreamingEndEvent)
     */
    @Override
    public void streamingFinished(final StreamingEndEvent event) {
        assertStateConsistency(fileUploadId, event.getFileName());

        publishUploadFinishedEvent(fileUploadId, event.getContentLength(), tempFilePath);

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

        publishUploadFailedEvent(fileUploadId, failureReason, event.getException());
    }

    /**
     * to check if upload is interrupted.
     */
    @Override
    public boolean isInterrupted() {
        return streamingInterrupted;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileTransferHandlerStreamVariable other = (FileTransferHandlerStreamVariable) obj;
        if (fileName == null) {
            if (other.fileName != null) {
                return false;
            }
        } else if (!fileName.equals(other.fileName)) {
            return false;
        }
        return true;
    }
}
