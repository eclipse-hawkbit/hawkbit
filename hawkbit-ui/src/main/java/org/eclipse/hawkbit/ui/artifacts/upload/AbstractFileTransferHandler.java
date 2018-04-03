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

import org.apache.commons.lang3.StringUtils;
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

    private final UIEventBus eventBus;

    public AbstractFileTransferHandler() {
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

    protected void resetAbortedByUserFlag() {
        aborted = false;
    }

    static class NullOutputStream extends OutputStream {
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

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            // do nothing
        }
    }

    protected void publishUploadProgressEvent(final FileUploadId fileUploadId, final long bytesReceived,
            final long fileSize, final String tempFilePath) {

        LOG.info("Upload in progress for file {} - {}", fileUploadId, (double) bytesReceived / (double) fileSize);
        eventBus.publish(this,
                new UploadStatusEvent(UploadStatusEventType.UPLOAD_IN_PROGRESS, new FileUploadProgress(fileUploadId,
                        bytesReceived, fileSize, tempFilePath)));
    }

    protected void publishUploadSucceeded(final FileUploadId fileUploadId, final long fileSize,
            final String tempFilePath) {
        LOG.info("Upload succeeded for file {}", fileUploadId);
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_SUCCESSFUL,
                new FileUploadProgress(fileUploadId, 0, fileSize, tempFilePath)));
    }

    protected void publishUploadFinishedEvent(final FileUploadId fileUploadId, final long fileSize,
            final String tempFilePath) {
        LOG.info("Upload finished for file {}", fileUploadId);
        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_FINISHED,
                new FileUploadProgress(fileUploadId, fileSize, fileSize, tempFilePath)));
    }

    protected void publishUploadFailedEvent(final FileUploadId fileUploadId, final String failureReason,
            final Exception uploadException) {

        eventBus.publish(this, new UploadStatusEvent(UploadStatusEventType.UPLOAD_FAILED,
                new FileUploadProgress(fileUploadId,
                        StringUtils.isEmpty(failureReason) ? uploadException.getMessage() : failureReason)));

        if (isAbortedByUser()) {
            LOG.info("Upload aborted by user for file :{}", fileUploadId);
        } else {
            LOG.info("Upload failed for file {} due to {}", fileUploadId, uploadException);
        }
    }

    protected void assertStateConsistency(final FileUploadId fileUploadId, final String filenameExtractedFromEvent) {
        if (!filenameExtractedFromEvent.equals(fileUploadId.getFilename())) {
            throw new IllegalStateException("Event filename " + filenameExtractedFromEvent + " but stored filename "
                    + fileUploadId.getFilename());
        }
    }

}
