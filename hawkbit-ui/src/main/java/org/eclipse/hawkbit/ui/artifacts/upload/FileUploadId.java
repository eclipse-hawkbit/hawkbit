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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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

    private final String softwareModuleName;

    private final String softwareModuleVersion;

    private Long softwareModuleId;

    private final String id;

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
        this.softwareModuleName = softwareModule.getName();
        this.softwareModuleVersion = softwareModule.getVersion();
        this.softwareModuleId = softwareModule.getId();
        this.id = createFileUploadIdString(filename, softwareModuleName, softwareModuleVersion);
    }

    /**
     * Creates a new {@link FileUploadId} instance.
     * 
     * @param filename
     *            the name of the file
     * @param softwareModuleName
     *            the name of a {@link SoftwareModule} for which the file is
     *            uploaded
     * @param softwareModuleVersion
     *            the version of a {@link SoftwareModule} for which the file is
     *            uploaded
     */
    public FileUploadId(final String filename, final String softwareModuleName, final String softwareModuleVersion) {
        this.filename = filename;
        this.softwareModuleName = softwareModuleName;
        this.softwareModuleVersion = softwareModuleVersion;
        this.id = createFileUploadIdString(filename, softwareModuleName, softwareModuleVersion);
    }

    private static String createFileUploadIdString(final String filename, final String softwareModuleName,
            final String softwareModuleVersion) {
        return new StringBuilder(filename).append(":")
                .append(HawkbitCommonUtil.getFormattedNameVersion(softwareModuleName, softwareModuleVersion))
                .toString();
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
        return new EqualsBuilder().append(id, other.id).isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).toHashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getSoftwareModuleName() {
        return softwareModuleName;
    }

    public String getSoftwareModuleVersion() {
        return softwareModuleVersion;
    }

    public Long getSoftwareModuleId() {
        return softwareModuleId;
    }
}
