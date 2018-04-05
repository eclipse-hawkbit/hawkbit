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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.hawkbit.repository.ArtifactManagement;
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

    private final UploadLogic uploadLogic;
    private final long maxSize;
    private final Upload upload;
    private volatile String fileName;
    private volatile String tempFilePath;
    private volatile String mimeType;
    private volatile SoftwareModule selectedSw;
    private volatile FileUploadId fileUploadId;

    private String failureReason;
    private final VaadinMessageSource i18n;
    private transient EventBus.UIEventBus eventBus;
    private final ArtifactUploadState artifactUploadState;
    private final UINotification uiNotification;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final UploadMessageBuilder uploadMessageBuilder;

    // TODO rollouts: remove all state handling and UI code, communicate only
    // per events

    FileTransferHandlerVaadinUpload(final UploadLogic uploadLogic, final long maxSize, final Upload upload,
            final SoftwareModuleManagement softwareManagement, final ArtifactManagement artifactManagement,
            final UploadMessageBuilder uploadMessageBuilder) {
        super(artifactManagement, uploadMessageBuilder);
        this.uploadLogic = uploadLogic;
        this.maxSize = maxSize;
        this.upload = upload;
        this.uploadMessageBuilder = uploadMessageBuilder;
        this.mimeType = mimeType;
        this.i18n = SpringContextHelper.getBean(VaadinMessageSource.class);
        this.eventBus = SpringContextHelper.getBean(EventBus.UIEventBus.class);
        this.artifactUploadState = SpringContextHelper.getBean(ArtifactUploadState.class);
        this.uiNotification = SpringContextHelper.getBean(UINotification.class);
        this.softwareModuleManagement = softwareManagement;
        eventBus.subscribe(this);
    }

    /**
     * Upload started for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.StartedListener#uploadStarted(com.vaadin.ui.Upload.StartedEvent)
     */
    @Override
    public void uploadStarted(final StartedEvent event) {
        resetAbortedByUser();
        resetDuplicateFile();
        resetUploadInterrupted();
        this.failureReason = null;
        this.fileName = fileName;
        this.mimeType = mimeType;

        assertThatOneSoftwareModulIsSelected();

        // selected software module at the time of this callback is considered
        artifactUploadState.getSelectedBaseSwModuleId().ifPresent(selectedSwId -> {
            this.selectedSw = softwareModuleManagement.get(selectedSwId).orElse(null);
        });
        
        this.fileName = event.getFilename();
        this.fileUploadId = new FileUploadId(fileName, selectedSw);

        LOG.info("File {} uploading for SW module {}", fileName, selectedSw);

        if (uploadLogic.isFileInUploadState(this.fileUploadId)) {
            failureReason = uploadMessageBuilder.buidlMessageForGenericUploadFailed();
            // actual interrupt will happen a bit late so setting the below
            // flag
            interruptFileUpload();
            setDuplicateFile();
        } else {
            publishUploadStarted(fileUploadId);
            checkForDuplicateFileInSoftwareModul(fileUploadId, selectedSw);
        }
    }

    private void assertThatOneSoftwareModulIsSelected() {
        // FileUpload button should be disabled if no SoftwareModul or more
        // than one is selected!
        if (uploadLogic.isNoSoftwareModuleSelected()) {
            throw new IllegalStateException("No SoftwareModul selected");
        } else if (uploadLogic.isMoreThanOneSoftwareModulesSelected()) {
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
            return new NullOutputStream();
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("spUiArtifactUpload", null);

            // we return the outputstream so we cannot close it here
            @SuppressWarnings("squid:S2095")
            final OutputStream out = new FileOutputStream(tempFile);

            tempFilePath = tempFile.getAbsolutePath();
            eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_IN_PROGRESS,
                    new FileUploadProgress(fileUploadId, 0, tempFile.length(), mimeType, tempFilePath)));

            return out;
        } catch (final FileNotFoundException e) {
            LOG.error("Upload failed {}", e);
            failureReason = i18n.getMessage("message.file.not.found");

        } catch (final IOException e) {
            LOG.error("Upload failed {}", e);
            failureReason = i18n.getMessage("message.upload.failed");
        }

        interruptFileUpload();
        return new NullOutputStream();
    }

    /**
     * Reports progress in {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.ProgressListener#updateProgress(long, long)
     */
    @Override
    public void updateProgress(final long readBytes, final long contentLength) {
        // Update progress is called after upload interrupted in
        // uploadStarted method
        if (isAbortedByUser()) {
            LOG.info("User aborted file upload for file : {}", fileName);
            failureReason = uploadMessageBuilder.buildMessageForUploadAbortedByUser();
            interruptFileUpload();
            return;
        }
        if (readBytes > maxSize || contentLength > maxSize) {
            LOG.error("User tried to upload more than was allowed ({}).", maxSize);
            failureReason = uploadMessageBuilder.buildMessageForFileSizeExceeded(maxSize);
            interruptFileUpload();
            return;
        }
        if (isUploadInterrupted()) {
            return;
        }

        publishUploadProgressEvent(fileUploadId, readBytes, contentLength, mimeType, tempFilePath);
    }

    private void interruptFileUpload() {
        upload.interruptUpload();
        setUploadInterrupted();
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
            return;
        }
        assertStateConsistency(fileUploadId, event.getFilename());

        transferArtifactToRepository(fileUploadId, event.getLength(), event.getMIMEType(),
                tempFilePath);
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

        publishUploadFailedEvent(fileUploadId, failureReason, event.getReason());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(fileUploadId).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final FileTransferHandlerVaadinUpload other = (FileTransferHandlerVaadinUpload) obj;
        return new EqualsBuilder().append(fileUploadId, other.fileUploadId).isEquals();
    }
}
