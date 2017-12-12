/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation to authenticate a tenant.
 */

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfTenantSecurityToken {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    @JsonProperty(required = false)
    private String tenant;
    @JsonProperty(required = false)
    private final Long tenantId;
    @JsonProperty(required = false)
    private final String controllerId;
    @JsonProperty(required = false)
    private final Long targetId;

    @JsonProperty(required = false)
    private Map<String, String> headers;

    @JsonProperty(required = false)
    private final FileResource fileResource;

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant for the security token
     * @param tenantId
     *            alternative tenant identification by technical ID
     * @param controllerId
     *            the ID of the controller for the security token
     * @param targetId
     *            alternative target identification by technical ID
     * @param fileResource
     *            the file to obtain
     */
    @JsonCreator
    public DmfTenantSecurityToken(@JsonProperty("tenant") final String tenant,
            @JsonProperty("tenantId") final Long tenantId, @JsonProperty("controllerId") final String controllerId,
            @JsonProperty("targetId") final Long targetId,
            @JsonProperty("fileResource") final FileResource fileResource) {
        this.tenant = tenant;
        this.tenantId = tenantId;
        this.controllerId = controllerId;
        this.targetId = targetId;
        this.fileResource = fileResource;
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant for the security token
     * @param controllerId
     *            the ID of the controller for the security token
     * @param fileResource
     *            the file to obtain
     */
    public DmfTenantSecurityToken(final String tenant, final String controllerId, final FileResource fileResource) {
        this(tenant, null, controllerId, null, fileResource);
    }

    /**
     * Constructor.
     * 
     * @param tenantId
     *            the tenant for the security token
     * @param targetId
     *            target identification by technical ID
     * @param fileResource
     *            the file to obtain
     */
    public DmfTenantSecurityToken(final Long tenantId, final Long targetId, final FileResource fileResource) {
        this(null, tenantId, null, targetId, fileResource);
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    public String getControllerId() {
        return controllerId;
    }

    public Map<String, String> getHeaders() {
        if (headers == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(headers);
    }

    public FileResource getFileResource() {
        return fileResource;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getTargetId() {
        return targetId;
    }

    /**
     * Gets a header value.
     * 
     * @param name
     *            of header
     * @return the value
     */
    public String getHeader(final String name) {
        if (headers == null) {
            return null;
        }

        return headers.get(name);
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.headers.putAll(headers);
    }

    /**
     * Associates the specified header value with the specified name.
     * 
     * @param name
     *            of the header
     * @param value
     *            of the header
     * 
     * @return the previous value associated with the <tt>name</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>name</tt>.
     */
    public String putHeader(final String name, final String value) {
        if (headers == null) {
            headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
        return headers.put(name, value);
    }

    /**
     * File resource descriptor which is used to ask for the resource to
     * download e.g. The lookup of the file can be different e.g. by SHA1 hash
     * or by filename.
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileResource {
        @JsonProperty(required = false)
        private String sha1;
        @JsonProperty(required = false)
        private Long artifactId;
        @JsonProperty(required = false)
        private String filename;
        @JsonProperty(required = false)
        private SoftwareModuleFilenameResource softwareModuleFilenameResource;

        public String getSha1() {
            return sha1;
        }

        public void setSha1(final String sha1) {
            this.sha1 = sha1;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(final String filename) {
            this.filename = filename;
        }

        public SoftwareModuleFilenameResource getSoftwareModuleFilenameResource() {
            return softwareModuleFilenameResource;
        }

        public void setSoftwareModuleFilenameResource(
                final SoftwareModuleFilenameResource softwareModuleFilenameResource) {
            this.softwareModuleFilenameResource = softwareModuleFilenameResource;
        }

        public Long getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(final Long artifactId) {
            this.artifactId = artifactId;
        }

        /**
         * factory method to create a file resource for an SHA1 lookup.
         * 
         * @param sha1
         *            the SHA1 key of the file to obtain
         * @return the {@link FileResource} with SHA1 key set
         */
        public static FileResource createFileResourceBySha1(final String sha1) {
            final FileResource resource = new FileResource();
            resource.sha1 = sha1;
            return resource;
        }

        /**
         * factory method to create a file resource for an artifact ID lookup.
         * 
         * @param artifactId
         *            the artifact IF key of the file to obtain
         * @return the {@link FileResource} with SHA1 key set
         */
        public static FileResource createFileResourceByArtifactId(final Long artifactId) {
            final FileResource resource = new FileResource();
            resource.artifactId = artifactId;
            return resource;
        }

        /**
         * factory method to create a file resource for an filename lookup.
         * 
         * @param filename
         *            the filename of the file to obtain
         * @return the {@link FileResource} with filename set
         */
        public static FileResource createFileResourceByFilename(final String filename) {
            final FileResource resource = new FileResource();
            resource.filename = filename;
            return resource;
        }

        /**
         * factory method to create a file resource for an softwaremodule +
         * filename lookup, because an filename is not globally unique but
         * within a softwaremodule.
         * 
         * @param softwareModuleId
         *            the ID of the software module which contains the artifact
         * @param filename
         *            the name of file to obtain within the software module
         * @return the {@link FileResource} with artifactId set
         */
        public static FileResource softwareModuleFilename(final Long softwareModuleId, final String filename) {
            final FileResource resource = new FileResource();
            resource.softwareModuleFilenameResource = new SoftwareModuleFilenameResource(softwareModuleId, filename);
            return resource;
        }

        @Override
        public String toString() {
            return "FileResource [sha1=" + sha1 + ", artifactId=" + artifactId + ", filename=" + filename + "]";
        }

        /**
         * Inner class which holds the pointer to an artifact based on the
         * softwaremoduleId and the filename.
         */
        @JsonInclude(Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SoftwareModuleFilenameResource {
            @JsonProperty(required = false)
            private Long softwareModuleId;
            @JsonProperty(required = false)
            private String filename;

            /**
             * Constructor.
             * 
             * @param softwareModuleId
             *            the ID of the softwaremodule
             * @param filename
             *            the name of the file of the artifact within the
             *            softwaremodule
             */
            @JsonCreator
            public SoftwareModuleFilenameResource(@JsonProperty("softwareModuleId") final Long softwareModuleId,
                    @JsonProperty("filename") final String filename) {
                this.softwareModuleId = softwareModuleId;
                this.filename = filename;
            }

            public Long getSoftwareModuleId() {
                return softwareModuleId;
            }

            public String getFilename() {
                return filename;
            }

            public void setSoftwareModuleId(final Long softwareModuleId) {
                this.softwareModuleId = softwareModuleId;
            }

            public void setFilename(final String filename) {
                this.filename = filename;
            }
        }
    }
}
