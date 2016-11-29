/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.state;

import java.io.Serializable;

/**
 * Custom file to hold details of uploaded file.
 *
 *
 *
 */
public class CustomFile implements Serializable {

    private static final long serialVersionUID = -5902321650745311767L;

    private final String fileName;

    private long fileSize;

    private String filePath;

    private String baseSoftwareModuleName;

    private String baseSoftwareModuleVersion;

    private String mimeType;

    /**
     * Used to specify if the file is uploaded successfully.
     */
    private Boolean isValid = Boolean.TRUE;

    /**
     * Reason if upload fails.
     */
    private String failureReason;

    /**
     * Initialize details.
     *
     * @param fileName
     *            uploaded file name
     * @param fileSize
     *            uploaded file size
     * @param filePath
     *            uploaded file path
     * @param baseSoftwareModuleName
     *            software module name
     * @param baseSoftwareModuleVersion
     *            software module version
     * @param mimeType
     *            the mimeType of the file
     */
    public CustomFile(final String fileName, final long fileSize, final String filePath,
            final String baseSoftwareModuleName, final String baseSoftwareModuleVersion, final String mimeType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.baseSoftwareModuleName = baseSoftwareModuleName;
        this.baseSoftwareModuleVersion = baseSoftwareModuleVersion;
        this.mimeType = mimeType;
    }

    /**
     * Initialize details.
     *
     * @param fileName
     *            uploaded file name
     * @param baseSoftwareModuleName
     *            software module name
     * @param baseSoftwareModuleVersion
     *            software module version
     */
    public CustomFile(final String fileName, final String baseSoftwareModuleName,
            final String baseSoftwareModuleVersion) {
        this.fileName = fileName;
        this.baseSoftwareModuleName = baseSoftwareModuleName;
        this.baseSoftwareModuleVersion = baseSoftwareModuleVersion;
    }

    public String getBaseSoftwareModuleName() {
        return baseSoftwareModuleName;
    }

    public void setBaseSoftwareModuleName(final String baseSoftwareModuleName) {
        this.baseSoftwareModuleName = baseSoftwareModuleName;
    }

    public String getBaseSoftwareModuleVersion() {
        return baseSoftwareModuleVersion;
    }

    public void setBaseSoftwareModuleVersion(final String baseSoftwareModuleVersion) {
        this.baseSoftwareModuleVersion = baseSoftwareModuleVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    /**
     *
     * @return the isValid
     */
    public Boolean getIsValid() {
        return isValid;
    }

    /**
     * @param isValid
     *            the isValid to set
     */
    public void setIsValid(final Boolean isValid) {
        this.isValid = isValid;
    }

    /**
     * @param mimeType
     *            the mimeType to set
     */
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the failureReason
     */
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseSoftwareModuleName == null) ? 0 : baseSoftwareModuleName.hashCode());
        result = prime * result + ((baseSoftwareModuleVersion == null) ? 0 : baseSoftwareModuleVersion.hashCode());
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
        final CustomFile other = (CustomFile) obj;
        if (baseSoftwareModuleName == null) {
            if (other.baseSoftwareModuleName != null) {
                return false;
            }
        } else if (!baseSoftwareModuleName.equals(other.baseSoftwareModuleName)) {
            return false;
        }
        if (baseSoftwareModuleVersion == null) {
            if (other.baseSoftwareModuleVersion != null) {
                return false;
            }
        } else if (!baseSoftwareModuleVersion.equals(other.baseSoftwareModuleVersion)) {
            return false;
        }
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
