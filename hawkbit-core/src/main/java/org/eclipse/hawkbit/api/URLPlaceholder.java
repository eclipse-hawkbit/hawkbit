/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

/**
 * Container for variables available to the {@link ArtifactUrlHandler}.
 *
 */
public class URLPlaceholder {
    private final String tenant;
    private final Long tenantId;
    private final String controllerId;
    private final Long targetId;
    private final SoftwareData softwareData;

    /**
     * Constructor.
     * 
     * @param tenant
     *            of the client
     * @param tenantId
     *            of teh tenant
     * @param controllerId
     *            of the target
     * @param targetId
     *            of the target
     * @param softwareData
     *            information about the artifact and software module that can be
     *            accessed by the URL.
     */
    public URLPlaceholder(final String tenant, final Long tenantId, final String controllerId, final Long targetId,
            final SoftwareData softwareData) {
        this.tenant = tenant;
        this.tenantId = tenantId;
        this.controllerId = controllerId;
        this.targetId = targetId;
        this.softwareData = softwareData;
    }

    /**
     * Information about the artifact and software module that can be accessed
     * by the URL.
     *
     */
    public static class SoftwareData {
        private Long softwareModuleId;
        private String filename;
        private Long artifactId;
        private String sha1Hash;

        /**
         * Constructor.
         * 
         * @param softwareModuleId
         *            of the module the artifact belongs to
         * @param filename
         *            of the artifact
         * @param artifactId
         *            of the artifact
         * @param sha1Hash
         *            of the artifact
         */
        public SoftwareData(final Long softwareModuleId, final String filename, final Long artifactId,
                final String sha1Hash) {
            this.softwareModuleId = softwareModuleId;
            this.filename = filename;
            this.artifactId = artifactId;
            this.sha1Hash = sha1Hash;
        }

        public Long getSoftwareModuleId() {
            return softwareModuleId;
        }

        public void setSoftwareModuleId(final Long softwareModuleId) {
            this.softwareModuleId = softwareModuleId;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(final String filename) {
            this.filename = filename;
        }

        public Long getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(final Long artifactId) {
            this.artifactId = artifactId;
        }

        public String getSha1Hash() {
            return sha1Hash;
        }

        public void setSha1Hash(final String sha1Hash) {
            this.sha1Hash = sha1Hash;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((filename == null) ? 0 : filename.hashCode());
            result = prime * result + ((sha1Hash == null) ? 0 : sha1Hash.hashCode());
            result = prime * result + ((softwareModuleId == null) ? 0 : softwareModuleId.hashCode());
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
            final SoftwareData other = (SoftwareData) obj;
            if (artifactId == null) {
                if (other.artifactId != null) {
                    return false;
                }
            } else if (!artifactId.equals(other.artifactId)) {
                return false;
            }
            if (filename == null) {
                if (other.filename != null) {
                    return false;
                }
            } else if (!filename.equals(other.filename)) {
                return false;
            }
            if (sha1Hash == null) {
                if (other.sha1Hash != null) {
                    return false;
                }
            } else if (!sha1Hash.equals(other.sha1Hash)) {
                return false;
            }
            if (softwareModuleId == null) {
                if (other.softwareModuleId != null) {
                    return false;
                }
            } else if (!softwareModuleId.equals(other.softwareModuleId)) {
                return false;
            }
            return true;
        }

    }

    public String getTenant() {
        return tenant;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getControllerId() {
        return controllerId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public SoftwareData getSoftwareData() {
        return softwareData;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((controllerId == null) ? 0 : controllerId.hashCode());
        result = prime * result + ((softwareData == null) ? 0 : softwareData.hashCode());
        result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
        result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
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
        final URLPlaceholder other = (URLPlaceholder) obj;
        if (controllerId == null) {
            if (other.controllerId != null) {
                return false;
            }
        } else if (!controllerId.equals(other.controllerId)) {
            return false;
        }
        if (softwareData == null) {
            if (other.softwareData != null) {
                return false;
            }
        } else if (!softwareData.equals(other.softwareData)) {
            return false;
        }
        if (targetId == null) {
            if (other.targetId != null) {
                return false;
            }
        } else if (!targetId.equals(other.targetId)) {
            return false;
        }
        if (tenant == null) {
            if (other.tenant != null) {
                return false;
            }
        } else if (!tenant.equals(other.tenant)) {
            return false;
        }
        if (tenantId == null) {
            if (other.tenantId != null) {
                return false;
            }
        } else if (!tenantId.equals(other.tenantId)) {
            return false;
        }
        return true;
    }

}
