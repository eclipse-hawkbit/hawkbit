/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.api;

import lombok.Data;

import java.util.Objects;

/**
 * Container for variables available to the {@link ArtifactUrlHandler}.
 */
@Data
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
     */
    @Data
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
    }
}
