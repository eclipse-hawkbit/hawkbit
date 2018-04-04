/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

public class FileUploadId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String filename;

    private final SoftwareModule softwareModule;

    private final String id;

    public FileUploadId(final String filename, final SoftwareModule softwareModule) {
        this.filename = filename;
        this.softwareModule = softwareModule;
        this.id = createId(filename, softwareModule);
    }

    private String createId(final String filename, final SoftwareModule softwareModule) {
        return UploadLogic.createFileUploadId(filename, softwareModule);
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
        final FileUploadId other = (FileUploadId) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }
}
