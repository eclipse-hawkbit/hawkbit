/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.urlhandler;

/**
 * Container for a generated Artifact URL.
 */
public record ArtifactUrl(String protocol, String rel, String ref) {

    /**
     * Constructor.
     *
     * @param protocol string, e.g. ftp, http, https
     * @param rel hypermedia value
     * @param ref hypermedia value
     */
    public ArtifactUrl {
    }
}