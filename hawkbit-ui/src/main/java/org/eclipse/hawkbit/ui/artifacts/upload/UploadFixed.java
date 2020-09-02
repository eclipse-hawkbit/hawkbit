/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.OutputStream;

import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Upload;

/**
 * The {@link Upload} class has a bug.The lifecycle methods of the registered
 * handler
 * com.vaadin.ui.Upload.StartedListener#uploadStarted(com.vaadin.ui.Upload.StartedEvent)
 * etc are called even the upload was interrupted. This bug is fixed in this
 * class.
 *
 */
public class UploadFixed extends Upload {

    private static final long serialVersionUID = 1L;

    private boolean uploadInterrupted;

    /**
     * Stops the file upload
     */
    @Override
    public void interruptUpload() {
        super.interruptUpload();
        uploadInterrupted = true;
    }

    @Override
    protected StreamVariable getStreamVariable() {
        return new StreamVariableFixed(super.getStreamVariable());
    }

    private class StreamVariableFixed implements StreamVariable {
        private static final long serialVersionUID = 1L;
        private final StreamVariable originalStreamVariable;

        /**
         * Constructor for StreamVariableFixed
         *
         * @param originalStreamVariable
         *          StreamVariable
         */
        public StreamVariableFixed(final StreamVariable originalStreamVariable) {
            this.originalStreamVariable = originalStreamVariable;
        }

        @Override
        public boolean listenProgress() {
            // this fixes the vaadin bug
            return originalStreamVariable.listenProgress() && !uploadInterrupted;
        }

        @Override
        public void onProgress(final StreamingProgressEvent event) {
            originalStreamVariable.onProgress(event);
        }

        @Override
        public boolean isInterrupted() {
            return uploadInterrupted;
        }

        @Override
        public OutputStream getOutputStream() {
            return originalStreamVariable.getOutputStream();
        }

        @Override
        public void streamingStarted(final StreamingStartEvent event) {
            originalStreamVariable.streamingStarted(event);
        }

        @Override
        public void streamingFinished(final StreamingEndEvent event) {
            originalStreamVariable.streamingFinished(event);
        }

        @Override
        public void streamingFailed(final StreamingErrorEvent event) {
            originalStreamVariable.streamingFailed(event);
            uploadInterrupted = false;
        }
    }
}
