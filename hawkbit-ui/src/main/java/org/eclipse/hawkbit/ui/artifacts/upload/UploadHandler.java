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

import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.state.CustomFile;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * the upload does not exceed the configued max file size.
 *
 *
 *
 *
 *
 *
 */
public class UploadHandler implements StreamVariable, Receiver, SucceededListener, FailedListener, FinishedListener,
        ProgressListener, StartedListener {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(UploadHandler.class);

    private final long fileSize;
    private final UploadLayout view;
    private final UploadStatusInfoWindow infoWindow;
    private final long maxSize;
    private final Upload upload;

    private volatile String fileName = null;
    private volatile String mimeType = null;
    private volatile boolean interrupted = false;
    private String failureReason;
    private final I18N i18n;

    UploadHandler(final String fileName, final long fileSize, final UploadLayout view,
            final UploadStatusInfoWindow infoWindow, final long maxSize, final Upload upload, final String mimeType) {
        super();
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.view = view;
        this.infoWindow = infoWindow;
        this.maxSize = maxSize;
        this.upload = upload;
        this.mimeType = mimeType;
        this.i18n = SpringContextHelper.getBean(I18N.class);

    }

    /**
     * Create stream for {@link StreamVariable} variant.
     *
     * @see com.vaadin.server.StreamVariable#getOutputStream()
     */
    @Override
    public final OutputStream getOutputStream() {
        try {
            return view.saveUploadedFileDetails(fileName, fileSize, mimeType);
        } catch (final ArtifactUploadFailedException e) {
            LOG.error("Atifact upload failed {} ", e);
            failureReason = e.getMessage();
            interrupted = true;
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
        this.fileName = fileName;
        this.mimeType = mimeType;
        try {
            if (view.validate()) {
                if (view.checkForDuplicate(fileName)) {
                    view.showDuplicateMessage();
                } else {
                    view.increaseNumberOfFileUploadsExpected();
                    return view.saveUploadedFileDetails(fileName, 0, mimeType);
                }
            }
        } catch (final ArtifactUploadFailedException e) {
            LOG.error("Atifact upload failed {} ", e);
            failureReason = e.getMessage();
            upload.interruptUpload();
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
        view.updateFileSize(event.getFilename(), event.getLength());

        // recorded that we now one more uploaded
        view.increaseNumberOfFilesActuallyUpload();

        // inform upload status window
        infoWindow.uploadSucceeded(event.getFilename());
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
        // record that we now one more uploaded
        view.increaseNumberOfFilesActuallyUpload();

        // inform upload status window
        infoWindow.uploadSucceeded(event.getFileName());

        // check if we are finished
        if (view.enableProcessBtn()) {
            infoWindow.uploadSessionFinished();
        }
        view.updateActionCount();

        // display the duplicate message after streaming all files
        view.displayDuplicateValidationMessage();
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
        // check if we are finished
        if (view.enableProcessBtn()) {
            infoWindow.uploadSessionFinished();
        }
        view.updateActionCount();

    }

    /**
     * Upload started for {@link StreamVariable} variant.
     *
     * @see com.vaadin.server.StreamVariable#streamingStarted(com.vaadin.server.StreamVariable.StreamingStartEvent)
     */
    @Override
    public void streamingStarted(final StreamingStartEvent event) {
        LOG.debug("Streaming started for file :{}", fileName);
        infoWindow.uploadStarted(fileName);
    }

    /**
     * Upload started for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.StartedListener#uploadStarted(com.vaadin.ui.Upload.StartedEvent)
     */
    @Override
    public void uploadStarted(final StartedEvent event) {
        // single file session
        if (view.isSoftwareModuleSelected() && !view.checkIfFileIsDuplicate(event.getFilename())) {
            infoWindow.uploadSessionStarted();
            LOG.debug("Upload started for file :{}", event.getFilename());
            infoWindow.uploadStarted(event.getFilename());
        } else {
            failureReason = i18n.get("message.upload.failed");
            upload.interruptUpload();
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
        if (readBytes > maxSize || contentLength > maxSize) {
            LOG.error("User tried to upload more than was allowed ({}).", maxSize);
            view.decreaseNumberOfFileUploadsExpected();
            final SoftwareModule sw = view.getSoftwareModuleSelected();
            view.getFileSelected().remove(new CustomFile(fileName, sw.getName(), sw.getVersion()));
            view.updateActionCount();
            failureReason = i18n.get("message.uploadedfile.size.exceeded", maxSize);
            infoWindow.uploadFailed(fileName, failureReason);
            upload.interruptUpload();
            interrupted = true;
            return;
        }

        infoWindow.updateProgress(fileName, readBytes, contentLength);
        LOG.info("Update progress - {} : {}", fileName, (double) readBytes / (double) contentLength);
    }

    /**
     * Reports progress in {@link StreamVariable} variant. Interrupts
     *
     * @see com.vaadin.server.StreamVariable#onProgress(com.vaadin.server.StreamVariable.StreamingProgressEvent)
     */
    @Override
    public void onProgress(final StreamingProgressEvent event) {
        if (event.getBytesReceived() > maxSize || event.getContentLength() > maxSize) {
            LOG.error("User tried to upload more than was allowed ({}).", maxSize);
            view.decreaseNumberOfFileUploadsExpected();
            final SoftwareModule sw = view.getSoftwareModuleSelected();
            view.getFileSelected().remove(new CustomFile(fileName, sw.getName(), sw.getVersion()));
            view.updateActionCount();
            failureReason = i18n.get("message.uploadedfile.size.exceeded", maxSize);
            infoWindow.uploadFailed(event.getFileName(), failureReason);
            interrupted = true;
            return;
        }

        infoWindow.updateProgress(event.getFileName(), event.getBytesReceived(), event.getContentLength());
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
        LOG.info("Streaming failed for file :{}", event.getFileName());
        view.decreaseNumberOfFileUploadsExpected();
        final SoftwareModule sw = view.getSoftwareModuleSelected();
        view.getFileSelected().remove(new CustomFile(fileName, sw.getName(), sw.getVersion()));
        view.updateActionCount();
        infoWindow.uploadFailed(event.getFileName(), failureReason);
        // check if we are finished
        if (view.enableProcessBtn()) {
            infoWindow.uploadSessionFinished();
        }
        view.displayDuplicateValidationMessage();

        LOG.info("Streaming failed due to  :{}", event.getException());
    }

    /**
     * Upload failed for {@link Upload} variant.
     *
     * @see com.vaadin.ui.Upload.FailedListener#uploadFailed(com.vaadin.ui.Upload.FailedEvent)
     */
    @Override
    public void uploadFailed(final FailedEvent event) {
        LOG.info("Upload failed for file :{}", event.getFilename());
        view.decreaseNumberOfFileUploadsExpected();
        /**
         * If upload interrupted because of duplicate file,do not remove the
         * file already in upload list
         **/
        if (!view.getDuplicateFileNamesList().isEmpty()) {
            final SoftwareModule sw = view.getSoftwareModuleSelected();
            view.getFileSelected().remove(new CustomFile(fileName, sw.getName(), sw.getVersion()));
        }
        view.updateActionCount();
        infoWindow.uploadFailed(event.getFilename(), failureReason);
        LOG.info("Upload failed for file :{}", event.getReason());

    }

    /**
     * to check if upload is interrupted.
     */
    @Override
    public boolean isInterrupted() {
        return interrupted;
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (fileName == null ? 0 : fileName.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UploadHandler)) {
            return false;
        }
        final UploadHandler other = (UploadHandler) obj;
        if (fileName == null && other.fileName != null) {
            return false;
        } else if (!fileName.equals(other.fileName)) {
            return false;
        }
        return true;
    }

}
