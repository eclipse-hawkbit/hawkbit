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

public class FileUploadId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String filename;

    private final SoftwareModule softwareModule;

    private final String id;

    public FileUploadId(final String filename, final SoftwareModule softwareModule) {
        this.filename = filename;
        this.softwareModule = softwareModule;
        this.id = createFileUploadIdString(filename, softwareModule);
    }

    private static String createFileUploadIdString(final String filename, final SoftwareModule softwareModule) {
        return new StringBuilder(filename).append(":").append(
                HawkbitCommonUtil.getFormattedNameVersion(softwareModule.getName(), softwareModule.getVersion()))
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

    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }
}
