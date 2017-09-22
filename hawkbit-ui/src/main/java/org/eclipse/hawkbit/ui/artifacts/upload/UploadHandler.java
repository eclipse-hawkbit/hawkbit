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

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.event.UploadFileStatus;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.server.StreamVariable;
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
 * Implementation to read file selected for upload. both for {@link Upload} and
 * {@link StreamVariable} upload variants.
 *
 * The handler manages the output to the user and at the same time ensures that
 * the upload does not exceed the configured max file size.
 *
 */
public class UploadHandler implements StreamVariable, Receiver, SucceededListener, FailedListener, FinishedListener,
        ProgressListener, StartedListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(UploadHandler.class);

    private final long fileSize;
    private final UploadLayout view;
    private final long maxSize;
    private final Upload upload;

    private volatile String fileName;
    private volatile String mimeType;
    private volatile boolean streamingInterrupted;
    private volatile boolean uploadInterrupted;
    private volatile boolean aborted;

    private String failureReason;
    private final VaadinMessageSource i18n;
    private transient EventBus.UIEventBus eventBus;
    private final SoftwareModule selectedSw;
    private SoftwareModule selectedSwForUpload;
    private final ArtifactUploadState artifactUploadState;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    UploadHandler(final String fileName, final long fileSize, final UploadLayout view, final long maxSize,
            final Upload upload, final String mimeType, final SoftwareModule selectedSw,
            final SoftwareModuleManagement softwareManagement) {
        super();
        this.aborted = false;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.view = view;
        this.maxSize = maxSize;
        this.upload = upload;
        this.mimeType = mimeType;
        this.selectedSw = selectedSw;
        this.i18n = SpringContextHelper.getBean(VaadinMessageSource.class);
        this.eventBus = SpringContextHelper.getBean(EventBus.UIEventBus.class);
        this.artifactUploadState = SpringContextHelper.getBean(ArtifactUploadState.class);
        this.softwareModuleManagement = softwareManagement;
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadStatusEventType event) {
        if (event == UploadStatusEventType.ABORT_UPLOAD) {
            aborted = true;
        }
    }

    /**
     * Create stream for {@link StreamVariable} variant.
     *
     * @see com.vaadin.server.StreamVariable#getOutputStream()
     */
    @Override
    public final OutputStream getOutputStream() {
        try {
            streamingInterrupted = false;
            failureReason = null;
            return view.saveUploadedFileDetails(fileName, fileSize, mimeType, selectedSw);
        } catch (final ArtifactUploadFailedException e) {
            LOG.error("Atifact upload failed {} ", e);
            failureReason = e.getMessage();
            streamingInterrupted = true;
            return new NullOutputStream();
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
        aborted = false;
        failureReason = null;
        this.fileName = fileName;
        this.mimeType = mimeType;
        // reset has directory flag before upload
        view.setHasDirectory(false);
        try {
            if (view.checkIfSoftwareModuleIsSelected() && !view.checkForDuplicate(fileName, selectedSwForUpload)) {
                view.increaseNumberOfFileUploadsExpected();
                final OutputStream saveUploadedFileDetails = view.saveUploadedFileDetails(fileName, 0, mimeType,
                        selectedSwForUpload);
                eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.RECEIVE_UPLOAD,
                        new UploadFileStatus(fileName, 0, -1, selectedSwForUpload)));
                return saveUploadedFileDetails;
            }
        } catch (final ArtifactUploadFailedException e) {
            LOG.error("Atifact upload failed {} ", e);
            failureReason = e.getMessage();
            upload.interruptUpload();
            uploadInterrupted = true;
        }
        // if final validation fails ,final no upload ,return NullOutputStream
        return new NullOutputStream();
    }

    /**
     *
     * Upload sucessfull for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.SucceededListener#uploadSucceeded(com.vaadin.ui.Upload.SucceededEvent)
     */
    @Override
    public void uploadSucceeded(final SucceededEvent event) {
        LOG.debug("Streaming finished for file :{}", event.getFilename());
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_SUCCESSFUL,
                new UploadFileStatus(event.getFilename(), 0, event.getLength(), selectedSwForUpload)));
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
        LOG.debug("Streaming finished for file :{}", event.getFileName());
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_STREAMING_FINISHED,
                new UploadFileStatus(event.getFileName(), 0, event.getContentLength(), selectedSw)));
    }

    /**
     * Upload finished for {@link Upload} variant. Both for good and error
     * variant.
     *
     * @see com.vaadin.ui.Upload.FinishedListener#uploadFinished(com.vaadin.ui.Upload.FinishedEvent)
     */
    @Override
    public void uploadFinished(final FinishedEvent event) {
        LOG.debug("Upload finished for file :{}", event.getFilename());
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_FINISHED,
                new UploadFileStatus(event.getFilename())));
    }

    /**
     * Upload started for {@link StreamVariable} variant.
     *
     * @see com.vaadin.server.StreamVariable#streamingStarted(com.vaadin.server.StreamVariable.StreamingStartEvent)
     */
    @Override
    public void streamingStarted(final StreamingStartEvent event) {
        LOG.debug("Streaming started for file :{}", fileName);
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_STARTED,
                new UploadFileStatus(fileName, 0, 0, selectedSw)));
    }

    /**
     * Upload started for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.StartedListener#uploadStarted(com.vaadin.ui.Upload.StartedEvent)
     */
    @Override
    public void uploadStarted(final StartedEvent event) {
        uploadInterrupted = false;
        selectedSwForUpload = null;

        final Optional<Long> selectedBaseSwModuleId = artifactUploadState.getSelectedBaseSwModuleId();
        if (selectedBaseSwModuleId.isPresent()) {
            selectedSwForUpload = softwareModuleManagement.get(selectedBaseSwModuleId.get()).orElse(null);
        }

        if (selectedSwForUpload != null && view.checkIfSoftwareModuleIsSelected()
                && !view.checkIfFileIsDuplicate(event.getFilename(), selectedSwForUpload)) {
            LOG.debug("Upload started for file :{}", event.getFilename());
            eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_STARTED,
                    new UploadFileStatus(event.getFilename(), 0, 0, selectedSwForUpload)));
        } else {
            failureReason = i18n.getMessage("message.upload.failed");
            upload.interruptUpload();
            // actual interrupt will happen a bit late so setting the below
            // flag
            uploadInterrupted = true;
        }
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
     * Reports progress in {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.ProgressListener#updateProgress(long, long)
     */
    @Override
    public void updateProgress(final long readBytes, final long contentLength) {
        // Update progress is called event after upload interrupted in
        // uploadStarted method
        if (!uploadInterrupted) {
            if (aborted) {
                LOG.info("User aborted file upload for file : {}", fileName);
                failureReason = i18n.getMessage("message.uploadedfile.aborted");
                interruptFileUpload();
                return;
            }
            if (readBytes > maxSize || contentLength > maxSize) {
                LOG.error("User tried to upload more than was allowed ({}).", maxSize);
                failureReason = i18n.getMessage("message.uploadedfile.size.exceeded", maxSize);
                interruptFileUpload();
                return;
            }
            eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_IN_PROGRESS,
                    new UploadFileStatus(fileName, readBytes, contentLength, selectedSwForUpload)));
            LOG.info("Update progress - {} : {}", fileName, (double) readBytes / (double) contentLength);
        }
    }

    /**
     * Reports progress in {@link StreamVariable} variant. Interrupts
     *
     * @see com.vaadin.server.StreamVariable#onProgress(com.vaadin.server.StreamVariable.StreamingProgressEvent)
     */
    @Override
    public void onProgress(final StreamingProgressEvent event) {
        if (aborted) {
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
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_IN_PROGRESS,
                new UploadFileStatus(fileName, event.getBytesReceived(), event.getContentLength(), selectedSw)));
        // Logging to solve sonar issue
        LOG.trace("Streaming in progress for file :{}", event.getFileName());
    }

    /**
     * Upload failed for{@link StreamVariable} variant.
     *
     * @param event
     *            StreamingEndEvent
     */
    @Override
    public void streamingFailed(final StreamingErrorEvent event) {
        if (failureReason == null) {
            failureReason = event.getException().getMessage();
        }
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_STREAMING_FAILED,
                new UploadFileStatus(fileName, failureReason, selectedSw)));

        if (!aborted) {
            LOG.info("Streaming failed for file :{}", event.getFileName());
            LOG.info("Streaming failed due to  :{}", event.getException());
        }
    }

    /**
     * Upload failed for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.FailedListener#uploadFailed(com.vaadin.ui.Upload.FailedEvent)
     */
    @Override
    public void uploadFailed(final FailedEvent event) {
        /**
         * If upload failed due to no selected software UPLOAD_FAILED event need
         * not be published.
         */
        if (selectedSwForUpload != null) {
            if (failureReason == null) {
                failureReason = event.getReason().getMessage();
            }
            eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_FAILED,
                    new UploadFileStatus(fileName, failureReason, selectedSwForUpload)));
            if (!aborted) {
                LOG.info("Upload failed for file :{}", event.getFilename());
                LOG.info("Upload failed for file :{}", event.getReason());
            }
        }
    }

    /**
     * to check if upload is interrupted.
     */
    @Override
    public boolean isInterrupted() {
        return streamingInterrupted;
    }

    private static class NullOutputStream extends OutputStream {
        /**
         * null output stream.
         * 
         * @param i
         *            byte
         */
        @Override
        public void write(final int i) throws IOException {
            // do nothing
        }
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
        final UploadHandler other = (UploadHandler) obj;
        if (fileName == null) {
            if (other.fileName != null) {
                return false;
            }
        } else if (!fileName.equals(other.fileName)) {
            return false;
        }
        return true;
    }

    private void interruptFileStreaming() {
        streamingInterrupted = true;
    }

    private void interruptFileUpload() {
        upload.interruptUpload();
        uploadInterrupted = true;
    }

}
