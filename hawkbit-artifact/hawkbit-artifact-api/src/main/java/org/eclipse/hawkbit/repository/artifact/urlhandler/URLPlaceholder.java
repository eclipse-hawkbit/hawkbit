/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.urlhandler;

/**
 * Container for variables available to the {@link ArtifactUrlHandler}.
 */
public record URLPlaceholder(String tenant, Long tenantId, String controllerId, Long targetId, SoftwareData softwareData) {

    /**
     * Information about the artifact and software module that can be accessed by the URL.
     */
    public record SoftwareData(Long softwareModuleId, String filename, Long artifactId, String sha1Hash) {}
}