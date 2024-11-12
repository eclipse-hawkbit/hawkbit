/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository.urlhandler;

import lombok.Data;

/**
 * Container for a generated Artifact URL.
 */
@Data
public class ArtifactUrl {

    private final String protocol;
    private final String rel;
    private final String ref;

    /**
     * Constructor.
     *
     * @param protocol string, e.g. ftp, http, https
     * @param rel hypermedia value
     * @param ref hypermedia value
     */
    public ArtifactUrl(final String protocol, final String rel, final String ref) {
        this.protocol = protocol;
        this.rel = rel;
        this.ref = ref;
    }
}