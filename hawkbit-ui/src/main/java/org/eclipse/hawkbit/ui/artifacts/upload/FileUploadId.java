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
import java.util.Objects;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * The {@link FileUploadId} identifies a file that is uploaded for a
 * {@link SoftwareModule}.
 *
 */
public class FileUploadId implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String filename;

    private final Long softwareModuleId;
    private final String softwareModuleName;
    private final String softwareModuleVersion;

    /**
     * Creates a new {@link FileUploadId} instance.
     * 
     * @param filename
     *            the name of the file
     * @param softwareModule
     *            the {@link SoftwareModule} for which the file is uploaded
     */
    public FileUploadId(final String filename, final SoftwareModule softwareModule) {
        this.filename = filename;
        this.softwareModuleId = softwareModule.getId();
        this.softwareModuleName = softwareModule.getName();
        this.softwareModuleVersion = softwareModule.getVersion();
    }

    public String getFilename() {
        return filename;
    }

    public Long getSoftwareModuleId() {
        return softwareModuleId;
    }

    public String getSoftwareModuleName() {
        return softwareModuleName;
    }

    public String getSoftwareModuleVersion() {
        return softwareModuleVersion;
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
        return Objects.equals(this.getFilename(), other.getFilename())
                && Objects.equals(this.getSoftwareModuleId(), other.getSoftwareModuleId())
                && Objects.equals(this.getSoftwareModuleName(), other.getSoftwareModuleName())
                && Objects.equals(this.getSoftwareModuleVersion(), other.getSoftwareModuleVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFilename(), getSoftwareModuleId(), getSoftwareModuleName(), getSoftwareModuleVersion());
    }

    @Override
    public String toString() {
        return new StringBuilder(filename).append(":")
                .append(HawkbitCommonUtil.getFormattedNameVersion(softwareModuleName, softwareModuleVersion))
                .toString();
    }
}
